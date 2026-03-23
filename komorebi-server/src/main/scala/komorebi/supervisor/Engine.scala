package komorebi.supervisor

import cats.effect.IO
import komorebi.domain.*
import komorebi.clients.LLMClient
import komorebi.prompts.SupervisorPrompts
import io.circe.parser.parse

object Engine:

  def evaluateCheckin(
    checkin: CheckinData,
    hints: PersonalizationHints,
    midSessionText: Option[String],
    locale: String,
    llm: LLMClient,
  ): IO[SupervisorDecision] =
    val textToCheck = List(checkin.freeText, midSessionText).flatten.mkString(" ")

    // SAFETY FIRST: Crisis detection is synchronous
    if textToCheck.nonEmpty && Rules.detectCrisis(textToCheck) then
      return IO.pure(crisisDecision(locale))

    // Rule-based pattern detection
    val rulePatterns = Rules.detectPatternsFromText(textToCheck)

    val statePatterns = scala.collection.mutable.ListBuffer.empty[HarmfulPattern]
    if checkin.selfCritical && checkin.tension >= 4 then statePatterns += HarmfulPattern.Perfectionism
    if checkin.lastSessionOutcome.contains(LastSessionOutcome.Pressuring) then statePatterns += HarmfulPattern.CompulsiveContinuation
    if checkin.tension >= 5 && checkin.selfCritical then statePatterns += HarmfulPattern.EscalatingFrustration

    val allRulePatterns = (rulePatterns ++ statePatterns.toList).distinct
    val ruleRiskLevel = Rules.assessRisk(allRulePatterns, checkin.tension)

    // LLM enrichment
    val shouldUseLLM = llm.isConfigured &&
      (checkin.freeText.exists(_.length > 20) ||
       ruleRiskLevel != RiskLevel.None ||
       checkin.selfCritical ||
       midSessionText.isDefined)

    val enrichment: IO[Option[(RiskLevel, List[HarmfulPattern])]] =
      if shouldUseLLM then
        callLLMSupervisor(checkin, allRulePatterns, hints, ruleRiskLevel, midSessionText, locale, llm)
          .attempt.map(_.toOption)
      else IO.pure(Option.empty)

    enrichment.map { llmOpt =>
      val (finalRiskLevel, finalPatterns) = llmOpt match
        case Some((llmRisk, llmPatterns)) =>
          (RiskLevel.max(ruleRiskLevel, llmRisk), (allRulePatterns ++ llmPatterns).distinct)
        case _ =>
          (ruleRiskLevel, allRulePatterns)

      val safety = Safety.applySafetyLayers(finalRiskLevel, finalPatterns, checkin, hints)

      SupervisorDecision(
        riskLevel = finalRiskLevel,
        patterns = finalPatterns,
        action = safety.action,
        recommendedMode = safety.recommendedMode,
        message = safety.message,
        guidanceDuration = safety.guidanceDuration,
      )
    }

  private def callLLMSupervisor(
    checkin: CheckinData,
    rulePatterns: List[HarmfulPattern],
    hints: PersonalizationHints,
    ruleRiskLevel: RiskLevel,
    midSessionText: Option[String],
    locale: String,
    llm: LLMClient,
  ): IO[(RiskLevel, List[HarmfulPattern])] =
    val prompt = SupervisorPrompts.buildSupervisorPrompt(checkin, rulePatterns, hints, ruleRiskLevel, midSessionText, locale)
    llm.complete("You are a mindfulness supervision system.", prompt, 512).map { raw =>
      val jsonStr = """\{[\s\S]*\}""".r.findFirstIn(raw)
        .getOrElse(throw RuntimeException("No JSON in LLM response"))
      val json = parse(jsonStr).getOrElse(throw RuntimeException("Invalid JSON"))
      val c = json.hcursor
      val risk = c.downField("riskLevel").as[String].toOption
        .flatMap(RiskLevel.fromJson(_).toOption).getOrElse(ruleRiskLevel)
      val patterns = c.downField("patterns").as[List[String]].toOption
        .getOrElse(Nil).flatMap(HarmfulPattern.fromJson(_).toOption)
      (risk, patterns)
    }

  private def crisisDecision(locale: String): SupervisorDecision =
    val message = if locale == "ja" then
      "このアプリは今あなたが経験していることに対する適切なサポートではありません。信頼できる人や危機相談窓口に連絡してください。一人で抱え込まないでください。"
    else
      "This app is not the right support for what you're going through right now. Please reach out to someone you trust, or contact a crisis line. You don't have to handle this alone."
    SupervisorDecision(
      riskLevel = RiskLevel.Crisis, patterns = Nil, action = SupervisorAction.Crisis,
      recommendedMode = GuidanceMode.Abort, guidanceDuration = GuidanceDuration.Thirty, message = message,
    )
