package komorebi.prompts

import komorebi.domain.*

object MemoryPrompts:

  def buildMemoryUpdatePrompt(
    current: Option[UserMemory],
    profile: ReflectionProfile,
    plan: SessionPlan,
    outcome: PostOutcome,
    locale: String,
  ): String =
    val currentSection = current.filter(_.sessionCount > 0) match
      case Some(m) =>
        s"""Current knowledge (${m.sessionCount} sessions so far):
           |- Core patterns: ${if m.corePatterns.isEmpty then "none" else m.corePatterns.mkString(", ")}
           |- What helps: ${if m.whatHelps.isEmpty then "none" else m.whatHelps.mkString(", ")}
           |- What hurts: ${if m.whatHurts.isEmpty then "none" else m.whatHurts.mkString(", ")}
           |- Life context: ${if m.lifeContext.isEmpty then "none" else m.lifeContext.mkString(", ")}
           |- Language notes: ${if m.languageNotes.isEmpty then "none" else m.languageNotes.mkString(", ")}
           |- Practice journey: ${if m.trajectory.isEmpty then "early days" else m.trajectory}""".stripMargin
      case _ =>
        "No prior knowledge — this is an early session."

    s"""You are updating a counselor's case notes after a session. Be a careful, thoughtful observer.
       |
       |$currentSection
       |
       |This session:
       |- What they shared: "${profile.freeText}"
       |- Themes: ${if profile.themes.isEmpty then "none" else profile.themes.mkString(", ")}
       |- Specific details: ${if profile.anchors.isEmpty then "none" else profile.anchors.mkString(", ")}
       |- Mood: ${profile.mood}/5, Tension: ${profile.tension}/5
       |- Self-critical: ${profile.selfCritical}
       |- Emotional tone: ${EmotionalTone.toJson(profile.emotionalTone)}
       |- Patterns detected: ${if plan.patterns.isEmpty then "none" else plan.patterns.map(HarmfulPattern.toJson).mkString(", ")}
       |- Risk level: ${RiskLevel.toJson(plan.riskLevel)}
       |- Practice mode used: ${GuidanceMode.toJson(plan.recommendedMode)} (${plan.guidanceDuration.value}s)
       |- Outcome: ${if outcome.feltBetter then "felt better" else "added pressure"}
       |- Would practice again: ${outcome.wouldContinue}
       |- Post-session notes: "${outcome.notes.getOrElse("none")}"
       |
       |Update the knowledge. Only add genuinely new insights — don't repeat what's already there. Be concise.
       |
       |Return ONLY valid JSON:
       |{
       |  "corePatterns": ["<recurring harmful pattern>", ...],
       |  "whatHelps": ["<what works for this person>", ...],
       |  "whatHurts": ["<what makes things worse>", ...],
       |  "lifeContext": ["<recurring stressor or context>", ...],
       |  "languageNotes": ["<how to speak with this person>", ...],
       |  "trajectory": "<1 sentence on their practice journey>"
       |}
       |
       |Rules:
       |- Maximum 5 items per array
       |- Only include patterns that have appeared more than once (or are very strong signals)
       |- Keep everything factual and grounded""".stripMargin
