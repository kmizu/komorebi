package komorebi.prompts

import komorebi.domain.*

object PersonalizationPrompts:

  private val HarmfulPatterns: List[String] = List(
    "perfectionism", "forced_acceptance", "overmonitoring", "performance_framing",
    "should_language", "compulsive_continuation", "breath_tension",
    "self_scoring", "rumination", "escalating_frustration",
  )

  private def buildPracticeHistorySection(
    hints: PersonalizationHints,
    memory: Option[UserMemory],
  ): String =
    val lines = scala.collection.mutable.ListBuffer.empty[String]
    memory.filter(_.sessionCount > 0) match
      case Some(m) =>
        lines += s"Session count: ${m.sessionCount}"
        if m.whatHelps.nonEmpty then lines += s"What has worked for them: ${m.whatHelps.mkString(", ")}"
        if m.whatHurts.nonEmpty then lines += s"What has not worked: ${m.whatHurts.mkString(", ")}"
        if m.corePatterns.nonEmpty then lines += s"Recurring patterns: ${m.corePatterns.mkString(", ")}"
        if m.lifeContext.nonEmpty then lines += s"Life context: ${m.lifeContext.mkString(", ")}"
        if m.languageNotes.nonEmpty then lines += s"How to speak with them: ${m.languageNotes.mkString(", ")}"
        if m.trajectory.nonEmpty then lines += s"Practice trajectory: ${m.trajectory}"
      case _ =>
        lines += "First or early session — no long-term history yet."

    if hints.recentPatterns.nonEmpty then lines += s"Patterns in recent sessions: ${hints.recentPatterns.map(HarmfulPattern.toJson).mkString(", ")}"
    if hints.preferredMode.isDefined then lines += s"Anchor that has worked recently: ${hints.preferredMode.map(GuidanceMode.toJson).get}"
    if hints.avoidMode.isDefined then lines += s"Anchor to avoid (correlated with difficulty): ${hints.avoidMode.map(GuidanceMode.toJson).get}"

    lines.mkString("\n")

  def buildPersonalizationPrompt(
    profile: ReflectionProfile,
    rulePatterns: List[HarmfulPattern],
    ruleRiskLevel: RiskLevel,
    hints: PersonalizationHints,
    memory: Option[UserMemory],
    locale: String,
  ): String =
    val lang = if locale == "ja" then "Japanese" else "English"
    val practiceHistory = buildPracticeHistorySection(hints, memory)
    val sessionCount = memory.map(_.sessionCount).getOrElse(0)
    val defaultLevel: String =
      if sessionCount <= 2 then "detailed"
      else if profile.tension >= 4 then "detailed"
      else if profile.emotionalTone == EmotionalTone.Distressed then "moderate"
      else if sessionCount >= 10 then "minimal"
      else "moderate"

    s"""You are the Personalization Agent in the MindfulAgents system (§3.3).
       |Your role: assess the session across 6 explicit dimensions, detect harmful patterns, and produce a session plan.
       |
       |═══ REFLECTION DATA ═══
       |What they shared in conversation:
       |  Mood: ${profile.mood}/5 | Tension: ${profile.tension}/5
       |  Self-critical: ${profile.selfCritical} | Emotional tone: ${EmotionalTone.toJson(profile.emotionalTone)}
       |  Intent expressed: ${SessionIntent.toJson(profile.intent)}
       |  Technique mentioned: ${profile.mentionedTechnique.getOrElse("none")}
       |  Last session: ${profile.lastSessionOutcome.map(LastSessionOutcome.toJson).getOrElse("unknown")}
       |  In their words: "${profile.freeText}"
       |  Themes: ${if profile.themes.isEmpty then "none" else profile.themes.mkString(", ")}
       |  Specific details: ${if profile.anchors.isEmpty then "none" else profile.anchors.mkString(", ")}
       |
       |═══ SAFETY BASELINE (rule-based) ═══
       |  Patterns detected: ${if rulePatterns.isEmpty then "none" else rulePatterns.map(HarmfulPattern.toJson).mkString(", ")}
       |  Risk level floor: ${RiskLevel.toJson(ruleRiskLevel)}
       |
       |═══ PRACTICE HISTORY (§3.3 dimension 6) ═══
       |$practiceHistory
       |
       |═══ YOUR TASK ═══
       |Assess these 6 dimensions explicitly, then produce a session plan.
       |
       |DIMENSION 1 — MOOD (already assessed): ${profile.mood}/5
       |DIMENSION 2 — GOAL: What does this person actually need? (calming / grounding / checkin)
       |DIMENSION 3 — TECHNIQUE: Which anchor? (breath | body | sound | external | reset | abort)
       |DIMENSION 4 — DURATION: How long? (30 | 60 | 180 seconds)
       |DIMENSION 5 — GUIDANCE LEVEL: How much scaffolding? (default: $defaultLevel)
       |DIMENSION 6 — PRACTICE HISTORY (already provided above)
       |
       |Return ONLY valid JSON:
       |{
       |  "riskLevel": "none" | "low" | "moderate" | "high",
       |  "patterns": string[],
       |  "goal": "calming" | "grounding" | "checkin",
       |  "recommendedMode": "breath" | "body" | "sound" | "external" | "reset",
       |  "guidanceDuration": 30 | 60 | 180,
       |  "guidanceLevel": "minimal" | "moderate" | "detailed",
       |  "practiceHistorySummary": "<one sentence>",
       |  "reasoning": "<one sentence, internal only>",
       |  "reflectionSummary": "<one warm sentence in $lang>",
       |  "guidanceHints": ["<specific instruction>", ...]
       |}
       |
       |Rules:
       |- Do NOT set riskLevel to "crisis" — keyword detection handles that
       |- riskLevel must be >= "${RiskLevel.toJson(ruleRiskLevel)}"
       |- guidanceHints: 2-4 max, actionable
       |- Harmful patterns: ${HarmfulPatterns.mkString(", ")}""".stripMargin
