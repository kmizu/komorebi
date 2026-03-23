package komorebi.db

import munit.FunSuite
import komorebi.domain.*

class PersonalizationHintsSpec extends FunSuite:

  private def makeSession(
    mode: GuidanceMode = GuidanceMode.Breath,
    patterns: List[HarmfulPattern] = Nil,
    feltBetter: Option[Boolean] = None,
    tension: Int = 3,
    riskLevel: RiskLevel = RiskLevel.None,
  ): SessionRecord =
    SessionRecord(
      id = java.util.UUID.randomUUID().toString,
      createdAt = java.time.Instant.now().toString,
      checkin = CheckinData(mood = 3, tension = tension, selfCritical = false, intent = SessionIntent.Calming),
      supervisorDecision = SupervisorDecision(
        riskLevel = riskLevel, patterns = patterns, action = SupervisorAction.Proceed,
        recommendedMode = mode, message = "", guidanceDuration = GuidanceDuration.Sixty,
      ),
      guidance = GuidanceScript(mode, GuidanceDuration.Sixty, "text", isPreset = true),
      postOutcome = feltBetter.map(fb => PostOutcome(feltBetter = fb, wouldContinue = true)),
    )

  test("empty sessions return empty hints"):
    val hints = PersonalizationHintsQuery.aggregateHints(Nil)
    assertEquals(hints, PersonalizationHints.empty)

  test("aggregates patterns appearing 2+ times"):
    val sessions = List(
      makeSession(patterns = List(HarmfulPattern.Perfectionism)),
      makeSession(patterns = List(HarmfulPattern.Perfectionism, HarmfulPattern.ShouldLanguage)),
      makeSession(patterns = List(HarmfulPattern.ShouldLanguage)),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assert(hints.recentPatterns.contains(HarmfulPattern.Perfectionism))
    assert(hints.recentPatterns.contains(HarmfulPattern.ShouldLanguage))

  test("does not include patterns appearing only once"):
    val sessions = List(
      makeSession(patterns = List(HarmfulPattern.Perfectionism)),
      makeSession(patterns = Nil),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assert(!hints.recentPatterns.contains(HarmfulPattern.Perfectionism))

  test("finds preferred mode from good outcomes"):
    val sessions = List(
      makeSession(mode = GuidanceMode.Sound, feltBetter = Some(true)),
      makeSession(mode = GuidanceMode.Sound, feltBetter = Some(true)),
      makeSession(mode = GuidanceMode.Breath, feltBetter = Some(false)),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assertEquals(hints.preferredMode, Some(GuidanceMode.Sound))

  test("finds avoid mode from bad outcomes"):
    val sessions = List(
      makeSession(mode = GuidanceMode.Breath, feltBetter = Some(false)),
      makeSession(mode = GuidanceMode.Sound, feltBetter = Some(true)),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assertEquals(hints.avoidMode, Some(GuidanceMode.Breath))

  test("calculates average tension"):
    val sessions = List(
      makeSession(tension = 2),
      makeSession(tension = 4),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assertEquals(hints.avgTension, 3.0)

  test("sets lastRiskLevel from most recent session"):
    val sessions = List(
      makeSession(riskLevel = RiskLevel.High),
      makeSession(riskLevel = RiskLevel.Low),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assertEquals(hints.lastRiskLevel, Some(RiskLevel.High))

  test("generates notes for high tension"):
    val sessions = List(
      makeSession(tension = 5),
      makeSession(tension = 4),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assert(hints.notes.exists(_.contains("high-tension")))

  test("generates notes for preferred mode"):
    val sessions = List(
      makeSession(mode = GuidanceMode.Sound, feltBetter = Some(true)),
    )
    val hints = PersonalizationHintsQuery.aggregateHints(sessions)
    assert(hints.notes.exists(_.contains("sound")))
