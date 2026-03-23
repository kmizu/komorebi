package komorebi.supervisor

import munit.FunSuite
import komorebi.domain.*

class SafetySpec extends FunSuite:

  private val defaultCheckin = CheckinData(
    mood = 3, tension = 3, selfCritical = false, intent = SessionIntent.Calming,
  )

  private val emptyHints = PersonalizationHints.empty

  // ── Layer 3: Crisis ───────────────────────────────────────────────────────

  test("crisis returns abort mode with crisis action"):
    val result = Safety.applySafetyLayers(RiskLevel.Crisis, Nil, defaultCheckin, emptyHints)
    assertEquals(result.action, SupervisorAction.Crisis)
    assertEquals(result.recommendedMode, GuidanceMode.Abort)
    assertEquals(result.guidanceDuration, GuidanceDuration.Thirty)

  // ── Layer 2: Distress ─────────────────────────────────────────────────────

  test("high risk returns stop with external mode"):
    val result = Safety.applySafetyLayers(RiskLevel.High, Nil, defaultCheckin, emptyHints)
    assertEquals(result.action, SupervisorAction.Stop)
    assertEquals(result.recommendedMode, GuidanceMode.External)
    assertEquals(result.guidanceDuration, GuidanceDuration.Thirty)

  test("compulsive_continuation triggers stop even at moderate risk"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.CompulsiveContinuation),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Stop)

  test("escalating_frustration triggers switch to external"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.EscalatingFrustration),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Switch)
    assertEquals(result.recommendedMode, GuidanceMode.External)

  // ── Layer 1: Subtle ───────────────────────────────────────────────────────

  test("overmonitoring at moderate = switch"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.Overmonitoring),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Switch)

  test("forced_acceptance at moderate = switch"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.ForcedAcceptance),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Switch)

  test("perfectionism at moderate = shorten"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.Perfectionism),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Shorten)

  test("performance_framing at moderate = shorten"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.PerformanceFraming),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Shorten)

  test("self_scoring at moderate = shorten"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.SelfScoring),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Shorten)

  test("breath_tension at moderate = switch to non-breath"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.BreathTension),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Switch)
    assert(result.recommendedMode != GuidanceMode.Breath)

  test("should_language at moderate = soften"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.ShouldLanguage),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Soften)

  test("moderate risk with no specific pattern = soften"):
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.Rumination),
      defaultCheckin, emptyHints,
    )
    assertEquals(result.action, SupervisorAction.Soften)

  // ── Low / None ────────────────────────────────────────────────────────────

  test("low risk = proceed"):
    val result = Safety.applySafetyLayers(RiskLevel.Low, Nil, defaultCheckin, emptyHints)
    assertEquals(result.action, SupervisorAction.Proceed)
    assertEquals(result.guidanceDuration, GuidanceDuration.Sixty)

  test("none risk = proceed with intent-based duration"):
    val calming = Safety.applySafetyLayers(RiskLevel.None, Nil, defaultCheckin, emptyHints)
    assertEquals(calming.guidanceDuration, GuidanceDuration.OneEighty)

    val grounding = Safety.applySafetyLayers(
      RiskLevel.None, Nil,
      defaultCheckin.copy(intent = SessionIntent.Grounding), emptyHints,
    )
    assertEquals(grounding.guidanceDuration, GuidanceDuration.Sixty)

    val checkinResult = Safety.applySafetyLayers(
      RiskLevel.None, Nil,
      defaultCheckin.copy(intent = SessionIntent.Checkin), emptyHints,
    )
    assertEquals(checkinResult.guidanceDuration, GuidanceDuration.Thirty)

  // ── Mode selection ────────────────────────────────────────────────────────

  test("preferred mode is used when available"):
    val hints = emptyHints.copy(preferredMode = Some(GuidanceMode.Sound))
    val result = Safety.applySafetyLayers(RiskLevel.None, Nil, defaultCheckin, hints)
    assertEquals(result.recommendedMode, GuidanceMode.Sound)

  test("breath_tension forces non-breath mode"):
    val hints = emptyHints.copy(preferredMode = Some(GuidanceMode.Breath))
    val result = Safety.applySafetyLayers(
      RiskLevel.Moderate,
      List(HarmfulPattern.BreathTension),
      defaultCheckin, hints,
    )
    assert(result.recommendedMode != GuidanceMode.Breath)

  test("high risk prefers external or sound from hints"):
    val hints = emptyHints.copy(preferredMode = Some(GuidanceMode.Sound))
    val result = Safety.applySafetyLayers(RiskLevel.High, Nil, defaultCheckin, hints)
    assertEquals(result.recommendedMode, GuidanceMode.Sound)
