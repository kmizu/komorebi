package komorebi.supervisor

import komorebi.domain.*

object Safety:

  /**
   * 3-layer safety model. Pure function — no side effects.
   *
   * Layer 3 (Crisis):   Stop everything, show crisis resources
   * Layer 2 (Distress): Stop inward practice, move to external grounding
   * Layer 1 (Subtle):   Soften, shorten, switch anchor
   */
  def applySafetyLayers(
    riskLevel: RiskLevel,
    patterns: List[HarmfulPattern],
    checkin: CheckinData,
    hints: PersonalizationHints,
  ): SafetyRecommendation =

    // ── Layer 3: Crisis ─────────────────────────────────────────────────
    if riskLevel == RiskLevel.Crisis then
      return SafetyRecommendation(
        action = SupervisorAction.Crisis,
        recommendedMode = GuidanceMode.Abort,
        guidanceDuration = GuidanceDuration.Thirty,
        message = "This app is not the right support for what you're going through right now. " +
          "Please reach out to someone you trust, or contact a crisis line. You don't have to handle this alone.",
      )

    // ── Layer 2: Distress escalation ────────────────────────────────────
    if riskLevel == RiskLevel.High || patterns.contains(HarmfulPattern.CompulsiveContinuation) then
      val mode = selectSafeMode(hints, patterns, external = true)
      return SafetyRecommendation(
        action = SupervisorAction.Stop,
        recommendedMode = mode,
        guidanceDuration = GuidanceDuration.Thirty,
        message = "Let's stop here. Stepping back is the right move. No need to push through.",
      )

    if patterns.contains(HarmfulPattern.EscalatingFrustration) then
      return SafetyRecommendation(
        action = SupervisorAction.Switch,
        recommendedMode = GuidanceMode.External,
        guidanceDuration = GuidanceDuration.Thirty,
        message = "Let's shift to something outside. Just look at a surface or shape in the room.",
      )

    // ── Layer 1: Subtle meta-suffering ──────────────────────────────────
    if riskLevel == RiskLevel.Moderate then
      if patterns.contains(HarmfulPattern.Overmonitoring) || patterns.contains(HarmfulPattern.ForcedAcceptance) then
        val mode = selectSafeMode(hints, patterns, external = false)
        return SafetyRecommendation(
          action = SupervisorAction.Switch,
          recommendedMode = mode,
          guidanceDuration = GuidanceDuration.Sixty,
          message = "Less internal focus for now. Let's anchor to something external.",
        )

      if patterns.contains(HarmfulPattern.Perfectionism) ||
         patterns.contains(HarmfulPattern.PerformanceFraming) ||
         patterns.contains(HarmfulPattern.SelfScoring) then
        return SafetyRecommendation(
          action = SupervisorAction.Shorten,
          recommendedMode = selectMode(hints, patterns),
          guidanceDuration = GuidanceDuration.Sixty,
          message = "Shorter and simpler. There's no correct way to do this.",
        )

      if patterns.contains(HarmfulPattern.BreathTension) then
        return SafetyRecommendation(
          action = SupervisorAction.Switch,
          recommendedMode = preferNonBreath(hints),
          guidanceDuration = GuidanceDuration.Sixty,
          message = "Breath focus isn't working well today. Let's try something else.",
        )

      if patterns.contains(HarmfulPattern.ShouldLanguage) then
        return SafetyRecommendation(
          action = SupervisorAction.Soften,
          recommendedMode = selectMode(hints, patterns),
          guidanceDuration = GuidanceDuration.Sixty,
          message = "Nothing is required here. This isn't a task.",
        )

      // Default moderate
      return SafetyRecommendation(
        action = SupervisorAction.Soften,
        recommendedMode = selectMode(hints, patterns),
        guidanceDuration = GuidanceDuration.Sixty,
        message = "Keep it light. No goals to reach today.",
      )

    // ── Low risk ────────────────────────────────────────────────────────
    if riskLevel == RiskLevel.Low then
      return SafetyRecommendation(
        action = SupervisorAction.Proceed,
        recommendedMode = selectMode(hints, patterns),
        guidanceDuration = GuidanceDuration.Sixty,
        message = "Conditions look reasonable. Keep it simple.",
      )

    // ── None risk ───────────────────────────────────────────────────────
    SafetyRecommendation(
      action = SupervisorAction.Proceed,
      recommendedMode = selectMode(hints, patterns),
      guidanceDuration = mapIntentToDuration(checkin.intent),
      message = "Ready when you are.",
    )

  // ── Mode selection helpers ──────────────────────────────────────────────

  private def selectMode(hints: PersonalizationHints, patterns: List[HarmfulPattern]): GuidanceMode =
    if patterns.contains(HarmfulPattern.BreathTension) then preferNonBreath(hints)
    else hints.preferredMode match
      case Some(mode) if !hints.avoidMode.contains(mode) => mode
      case _ => GuidanceMode.Breath

  private def selectSafeMode(
    hints: PersonalizationHints,
    @annotation.unused patterns: List[HarmfulPattern],
    external: Boolean,
  ): GuidanceMode =
    if external then
      hints.preferredMode match
        case Some(GuidanceMode.External) | Some(GuidanceMode.Sound) => hints.preferredMode.get
        case _ => GuidanceMode.External
    else
      preferNonBreath(hints)

  private def preferNonBreath(hints: PersonalizationHints): GuidanceMode =
    hints.preferredMode match
      case Some(mode) if mode != GuidanceMode.Breath => mode
      case _ => GuidanceMode.Sound

  private def mapIntentToDuration(intent: SessionIntent): GuidanceDuration =
    intent match
      case SessionIntent.Calming   => GuidanceDuration.OneEighty
      case SessionIntent.Grounding => GuidanceDuration.Sixty
      case SessionIntent.Checkin   => GuidanceDuration.Thirty
