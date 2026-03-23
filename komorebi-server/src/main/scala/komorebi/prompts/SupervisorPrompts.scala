package komorebi.prompts

import komorebi.domain.*

object SupervisorPrompts:

  private val HarmfulPatterns: List[String] = List(
    "perfectionism", "forced_acceptance", "overmonitoring", "performance_framing",
    "should_language", "compulsive_continuation", "breath_tension",
    "self_scoring", "rumination", "escalating_frustration",
  )

  def buildSupervisorPrompt(
    checkin: CheckinData,
    rulePatterns: List[HarmfulPattern],
    hints: PersonalizationHints,
    ruleRiskLevel: RiskLevel,
    midSessionText: Option[String],
    locale: String,
  ): String =
    val patternNames = rulePatterns.map(HarmfulPattern.toJson)
    val midSection = midSessionText.map(t => s"""- Mid-session report: "$t"""").getOrElse("")

    s"""You are a mindfulness supervision system. Your role is NOT to teach meditation. Your role is to detect when someone's approach to mindfulness practice is becoming harmful — specifically patterns of meta-suffering, self-monitoring, perfectionism, and compulsive effort.
       |
       |Current user state:
       |- Mood level: ${checkin.mood}/5
       |- Tension level: ${checkin.tension}/5
       |- Self-critical: ${checkin.selfCritical}
       |- Intent: ${SessionIntent.toJson(checkin.intent)}
       |- Last session: ${checkin.lastSessionOutcome.map(LastSessionOutcome.toJson).getOrElse("unknown")}
       |- What they wrote: "${checkin.freeText.getOrElse("(nothing)")}"
       |$midSection
       |
       |Rule-based detection found: [${patternNames.mkString(", ")}]
       |Preliminary risk level: ${RiskLevel.toJson(ruleRiskLevel)}
       |
       |Personalization context:
       |- Patterns seen recently: ${hints.recentPatterns.map(HarmfulPattern.toJson).mkString(", ")}
       |- Mode that helped before: ${hints.preferredMode.map(GuidanceMode.toJson).getOrElse("unknown")}
       |- Mode to avoid: ${hints.avoidMode.map(GuidanceMode.toJson).getOrElse("none")}
       |- Notes: ${if hints.notes.isEmpty then "none" else hints.notes.mkString("; ")}
       |
       |Your task: Analyze the user's state and detect harmful patterns. Return ONLY a valid JSON object — no markdown, no explanation.
       |
       |Harmful patterns to check for: ${HarmfulPatterns.mkString(", ")}
       |
       |JSON format (exact schema, no extras):
       |{
       |  "riskLevel": "none" | "low" | "moderate" | "high",
       |  "patterns": string[],
       |  "reasoning": "1-2 sentence explanation (internal only, not shown to user)"
       |}
       |
       |Rules:
       |- Do NOT set riskLevel to "crisis" — crisis is handled separately by keyword detection
       |- "none": user seems genuinely okay, no concerning patterns
       |- "low": mild pattern, proceed with light guidance
       |- "moderate": pattern is interfering — recommend shorter session or anchor switch
       |- "high": clear risk — stop inward practice, use external grounding only
       |- "reasoning" is for logging only, keep it brief and factual
       |- Only include patterns you actually detect — don't pad the list""".stripMargin

  def buildMidSessionPrompt(userReport: String, currentRiskLevel: RiskLevel): String =
    s"""You are a mindfulness safety monitor. The user just sent a mid-session report saying things are getting worse.
       |
       |Their report: "$userReport"
       |Current risk level: ${RiskLevel.toJson(currentRiskLevel)}
       |
       |Should the session stop or switch to external grounding? Return ONLY valid JSON:
       |{
       |  "shouldEscalate": true | false,
       |  "newRiskLevel": "moderate" | "high",
       |  "reasoning": "brief"
       |}""".stripMargin

  def buildSummaryPrompt(
    checkinFreeText: Option[String],
    postOutcome: PostOutcome,
  ): String =
    s"""Write a 1-sentence session summary for a mindfulness supervision log. Be factual and brief (max 15 words).
       |
       |Pre-session notes: "${checkinFreeText.getOrElse("none")}"
       |Felt better after: ${postOutcome.feltBetter}
       |Would continue: ${postOutcome.wouldContinue}
       |Post notes: "${postOutcome.notes.getOrElse("none")}"
       |
       |Return ONLY the summary sentence. No quotes, no labels.""".stripMargin
