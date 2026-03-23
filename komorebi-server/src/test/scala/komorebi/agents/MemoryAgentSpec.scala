package komorebi.agents

import munit.FunSuite
import komorebi.domain.*

class MemoryAgentSpec extends FunSuite:

  private val emptyMemory = UserMemory.empty

  private val defaultProfile = ReflectionProfile(
    mood = 3, tension = 3, selfCritical = false, intent = SessionIntent.Calming,
  )

  private val defaultPlan = SessionPlan(
    riskLevel = RiskLevel.Low,
    patterns = List(HarmfulPattern.Perfectionism),
    action = SupervisorAction.Proceed,
    recommendedMode = GuidanceMode.Breath,
    guidanceDuration = GuidanceDuration.Sixty,
    mood = 3, goal = SessionIntent.Calming,
    guidanceLevel = GuidanceLevel.Moderate,
    practiceHistorySummary = "", message = "", reflectionSummary = "", guidanceHints = Nil,
  )

  test("rulesBasedUpdate increments session count"):
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assertEquals(result.sessionCount, 1)
    assertEquals(result.version, 1)

  test("rulesBasedUpdate adds mode to whatHelps when felt better"):
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assert(result.whatHelps.contains("breath"))

  test("rulesBasedUpdate does not add to whatHelps when not better"):
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = false, wouldContinue = false))
    assert(result.whatHelps.isEmpty)

  test("rulesBasedUpdate adds breath focus to whatHurts"):
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = false, wouldContinue = false))
    assert(result.whatHurts.contains("breath focus"))

  test("rulesBasedUpdate adds patterns to corePatterns"):
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assert(result.corePatterns.contains("perfectionism"))

  test("rulesBasedUpdate adds themes to lifeContext"):
    val profile = defaultProfile.copy(themes = List("work_stress"))
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, profile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assert(result.lifeContext.contains("work_stress"))

  test("rulesBasedUpdate caps arrays at 5"):
    val bigMemory = emptyMemory.copy(
      whatHelps = List("a", "b", "c", "d", "e"),
    )
    val result = MemoryAgent.rulesBasedUpdate(bigMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assert(result.whatHelps.size <= 5)

  test("rulesBasedUpdate sets trajectory for early sessions"):
    val result = MemoryAgent.rulesBasedUpdate(emptyMemory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assertEquals(result.trajectory, "early sessions")

  test("rulesBasedUpdate deduplicates"):
    val memory = emptyMemory.copy(whatHelps = List("breath"))
    val result = MemoryAgent.rulesBasedUpdate(memory, defaultProfile,
      defaultPlan, PostOutcome(feltBetter = true, wouldContinue = true))
    assertEquals(result.whatHelps.count(_ == "breath"), 1)
