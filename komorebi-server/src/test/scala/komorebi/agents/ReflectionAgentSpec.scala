package komorebi.agents

import munit.FunSuite
import komorebi.domain.*

class ReflectionAgentSpec extends FunSuite:

  // ── Scripted fallback ───────────────────────────────────────────────────

  test("scripted turn returns first question for empty messages"):
    val result = ReflectionAgent.scriptedTurn(Nil, "en")
    assert(result.agentMessage.contains("How are you feeling"))
    assertEquals(result.userTurnCount, 0)
    assert(!result.done)
    assert(!result.crisis)

  test("scripted turn returns Japanese first question"):
    val result = ReflectionAgent.scriptedTurn(Nil, "ja")
    assert(result.agentMessage.contains("どんな気持ち"))

  test("scripted turn returns second question after one user message"):
    val messages = List(
      ConversationMessage(ConversationRole.Agent, "How are you?"),
      ConversationMessage(ConversationRole.User, "I'm feeling okay"),
    )
    val result = ReflectionAgent.scriptedTurn(messages, "en")
    assert(result.agentMessage.contains("weighing on you"))
    assertEquals(result.userTurnCount, 1)

  test("scripted turn completes after 3 user messages"):
    val messages = List(
      ConversationMessage(ConversationRole.Agent, "How are you?"),
      ConversationMessage(ConversationRole.User, "Fine"),
      ConversationMessage(ConversationRole.Agent, "Anything weighing?"),
      ConversationMessage(ConversationRole.User, "No"),
      ConversationMessage(ConversationRole.Agent, "What would help?"),
      ConversationMessage(ConversationRole.User, "Just checking in"),
    )
    val result = ReflectionAgent.scriptedTurn(messages, "en")
    assert(result.done)
    assert(result.profile.isDefined)

  // ── Heuristic profile ─────────────────────────────────────────────────

  test("heuristic profile detects high tension"):
    val messages = List(
      ConversationMessage(ConversationRole.User, "I'm feeling really stressed and anxious and overwhelmed"),
    )
    val profile = ReflectionAgent.heuristicProfile(messages, "en")
    assertEquals(profile.tension, 5)
    assert(profile.mood <= 2)
    assertEquals(profile.emotionalTone, EmotionalTone.Distressed)

  test("heuristic profile detects calm state"):
    val messages = List(
      ConversationMessage(ConversationRole.User, "I'm feeling good and calm today"),
    )
    val profile = ReflectionAgent.heuristicProfile(messages, "en")
    assertEquals(profile.tension, 2)
    assertEquals(profile.emotionalTone, EmotionalTone.Positive)

  test("heuristic profile detects self-critical"):
    val messages = List(
      ConversationMessage(ConversationRole.User, "I feel like I failed at everything"),
    )
    val profile = ReflectionAgent.heuristicProfile(messages, "en")
    assert(profile.selfCritical)

  test("heuristic profile detects calming intent"):
    val messages = List(
      ConversationMessage(ConversationRole.User, "I want to try some breathing"),
    )
    val profile = ReflectionAgent.heuristicProfile(messages, "en")
    assertEquals(profile.intent, SessionIntent.Calming)

  test("heuristic profile detects grounding intent"):
    val messages = List(
      ConversationMessage(ConversationRole.User, "I need some body grounding"),
    )
    val profile = ReflectionAgent.heuristicProfile(messages, "en")
    assertEquals(profile.intent, SessionIntent.Grounding)

  // ── checkinToProfile ──────────────────────────────────────────────────

  test("checkinToProfile converts correctly"):
    val data = CheckinData(mood = 2, tension = 4, selfCritical = true, intent = SessionIntent.Calming)
    val profile = ReflectionAgent.checkinToProfile(data)
    assertEquals(profile.mood, 2)
    assertEquals(profile.tension, 4)
    assert(profile.selfCritical)
    assertEquals(profile.intent, SessionIntent.Calming)
    assertEquals(profile.emotionalTone, EmotionalTone.Distressed)

  test("checkinToProfile positive mood"):
    val data = CheckinData(mood = 4, tension = 2, selfCritical = false, intent = SessionIntent.Checkin)
    val profile = ReflectionAgent.checkinToProfile(data)
    assertEquals(profile.emotionalTone, EmotionalTone.Positive)
