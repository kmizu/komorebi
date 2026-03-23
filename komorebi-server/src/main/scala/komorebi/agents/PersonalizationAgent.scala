package komorebi.agents

import cats.effect.IO
import komorebi.domain.*
import komorebi.clients.LLMClient
import komorebi.supervisor.{Rules, Safety}
import komorebi.prompts.PersonalizationPrompts
import io.circe.parser.parse

object PersonalizationAgent:

  private val ValidModes: Set[String] = Set("breath", "body", "sound", "external", "reset", "abort")
  private val ValidDurations: Set[Int] = Set(30, 60, 180)
  private val ValidIntents: Set[String] = Set("calming", "grounding", "checkin")
  private val ValidGuidanceLevels: Set[String] = Set("minimal", "moderate", "detailed")

  def defaultGuidanceLevel(profile: ReflectionProfile, sessionCount: Int): GuidanceLevel =
    if sessionCount <= 2 then GuidanceLevel.Detailed
    else if profile.tension >= 4 then GuidanceLevel.Detailed
    else if profile.emotionalTone == EmotionalTone.Distressed then GuidanceLevel.Moderate
    else if sessionCount >= 10 then GuidanceLevel.Minimal
    else GuidanceLevel.Moderate

  def produceSessionPlan(
    profile: ReflectionProfile,
    hints: PersonalizationHints,
    memory: Option[UserMemory],
    locale: String,
    llm: LLMClient,
  ): IO[SessionPlan] =
    val textToCheck = (List(profile.freeText) ++ profile.anchors).filter(_.nonEmpty).mkString(" ")

    // Crisis check
    if textToCheck.nonEmpty && Rules.detectCrisis(textToCheck) then
      return IO.pure(crisisPlan(profile, locale))

    // Rule-based pattern detection
    val rulePatterns = Rules.detectPatternsFromText(textToCheck)
    val statePatterns = scala.collection.mutable.ListBuffer.empty[HarmfulPattern]
    if profile.selfCritical && profile.tension >= 4 then statePatterns += HarmfulPattern.Perfectionism
    if profile.lastSessionOutcome.contains(LastSessionOutcome.Pressuring) then statePatterns += HarmfulPattern.CompulsiveContinuation
    if profile.tension >= 5 && profile.selfCritical then statePatterns += HarmfulPattern.EscalatingFrustration
    if profile.themes.exists(_.contains("breath")) then statePatterns += HarmfulPattern.BreathTension

    val allRulePatterns = (rulePatterns ++ statePatterns.toList).distinct
    val ruleRiskLevel = Rules.assessRisk(allRulePatterns, profile.tension)

    val sessionCount = memory.map(_.sessionCount).getOrElse(0)
    val guidanceLevel = defaultGuidanceLevel(profile, sessionCount)

    val shouldUseLLM = llm.isConfigured &&
      (profile.freeText.length > 10 || ruleRiskLevel != RiskLevel.None ||
       profile.selfCritical || profile.themes.nonEmpty)

    val llmEnrichment: IO[Option[LLMResult]] =
      if shouldUseLLM then
        val prompt = PersonalizationPrompts.buildPersonalizationPrompt(
          profile, allRulePatterns, ruleRiskLevel, hints, memory, locale)
        llm.complete("You are a mindfulness personalization system.", prompt, 600)
          .attempt.map(_.toOption.flatMap(parseLLMResult))
      else IO.pure(Option.empty)

    llmEnrichment.map { llmOpt =>
      val finalPatterns = llmOpt.map(r => (allRulePatterns ++ r.patterns).distinct).getOrElse(allRulePatterns)
      val finalRiskLevel = llmOpt.map(r => RiskLevel.max(ruleRiskLevel, r.riskLevel)).getOrElse(ruleRiskLevel)
      val reflectionSummary = llmOpt.flatMap(_.reflectionSummary).getOrElse(profile.freeText.take(120))
      val guidanceHints = llmOpt.flatMap(_.guidanceHints).filter(_.nonEmpty)
        .getOrElse(if profile.anchors.nonEmpty then List(s"Person mentioned: ${profile.anchors.mkString(", ")}") else Nil)
      val practiceHistorySummary = llmOpt.flatMap(_.practiceHistorySummary).getOrElse("")
      val llmGuidanceLevel = llmOpt.flatMap(_.guidanceLevel).getOrElse(guidanceLevel)
      val llmMode = llmOpt.flatMap(_.recommendedMode)
      val llmDuration = llmOpt.flatMap(_.guidanceDuration)
      val llmGoal = llmOpt.flatMap(_.goal)

      val checkinForSafety = CheckinData(
        mood = profile.mood, tension = profile.tension,
        selfCritical = profile.selfCritical, intent = profile.intent,
        lastSessionOutcome = profile.lastSessionOutcome, freeText = Some(profile.freeText),
      )
      val safety = Safety.applySafetyLayers(finalRiskLevel, finalPatterns, checkinForSafety, hints)

      SessionPlan(
        riskLevel = finalRiskLevel,
        patterns = finalPatterns,
        action = safety.action,
        recommendedMode = if safety.action == SupervisorAction.Proceed then llmMode.getOrElse(safety.recommendedMode) else safety.recommendedMode,
        guidanceDuration = if safety.action == SupervisorAction.Proceed then llmDuration.getOrElse(safety.guidanceDuration) else safety.guidanceDuration,
        message = safety.message,
        mood = profile.mood,
        goal = llmGoal.getOrElse(profile.intent),
        guidanceLevel = llmGuidanceLevel,
        practiceHistorySummary = practiceHistorySummary,
        reflectionSummary = reflectionSummary,
        guidanceHints = guidanceHints,
      )
    }

  private case class LLMResult(
    riskLevel: RiskLevel,
    patterns: List[HarmfulPattern],
    reflectionSummary: Option[String],
    guidanceHints: Option[List[String]],
    practiceHistorySummary: Option[String],
    guidanceLevel: Option[GuidanceLevel],
    recommendedMode: Option[GuidanceMode],
    guidanceDuration: Option[GuidanceDuration],
    goal: Option[SessionIntent],
  )

  private def parseLLMResult(raw: String): Option[LLMResult] =
    val jsonStr = """\{[\s\S]*\}""".r.findFirstIn(raw)
    jsonStr.flatMap { s =>
      parse(s).toOption.map { json =>
        val c = json.hcursor
        LLMResult(
          riskLevel = c.downField("riskLevel").as[String].toOption
            .flatMap(RiskLevel.fromJson(_).toOption).getOrElse(RiskLevel.None),
          patterns = c.downField("patterns").as[List[String]].toOption
            .getOrElse(Nil).flatMap(HarmfulPattern.fromJson(_).toOption),
          reflectionSummary = c.downField("reflectionSummary").as[String].toOption,
          guidanceHints = c.downField("guidanceHints").as[List[String]].toOption,
          practiceHistorySummary = c.downField("practiceHistorySummary").as[String].toOption,
          guidanceLevel = c.downField("guidanceLevel").as[String].toOption
            .flatMap(GuidanceLevel.fromJson(_).toOption),
          recommendedMode = c.downField("recommendedMode").as[String].toOption
            .filter(ValidModes.contains).flatMap(GuidanceMode.fromJson(_).toOption),
          guidanceDuration = c.downField("guidanceDuration").as[Int].toOption
            .filter(ValidDurations.contains).flatMap(GuidanceDuration(_)),
          goal = c.downField("goal").as[String].toOption
            .filter(ValidIntents.contains).flatMap(SessionIntent.fromJson(_).toOption),
        )
      }
    }

  private def crisisPlan(profile: ReflectionProfile, locale: String): SessionPlan =
    val message = if locale == "ja" then
      "このアプリは今あなたが経験していることに対する適切なサポートではありません。信頼できる人や危機相談窓口に連絡してください。"
    else
      "This app is not the right support for what you're going through right now. Please reach out to someone you trust or a crisis line."
    SessionPlan(
      riskLevel = RiskLevel.Crisis, patterns = Nil, action = SupervisorAction.Crisis,
      recommendedMode = GuidanceMode.Abort, guidanceDuration = GuidanceDuration.Thirty,
      mood = profile.mood, goal = profile.intent, guidanceLevel = GuidanceLevel.Detailed,
      practiceHistorySummary = "", message = message, reflectionSummary = "", guidanceHints = Nil,
    )
