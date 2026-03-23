package komorebi.supervisor

import munit.FunSuite
import komorebi.domain.*

class RulesSpec extends FunSuite:

  // ── detectCrisis ──────────────────────────────────────────────────────────

  test("detectCrisis returns true for English crisis keywords"):
    val keywords = List(
      "hurt myself", "harm myself", "self harm", "self-harm",
      "want to die", "suicidal", "end my life", "kill myself",
      "not worth living", "can't go on", "can't take it anymore",
      "wish i was dead", "better off dead",
    )
    keywords.foreach { kw =>
      assert(Rules.detectCrisis(s"I feel like $kw"), s"Should detect: $kw")
    }

  test("detectCrisis returns true for Japanese crisis keywords"):
    val keywords = List("死にたい", "消えたい", "自殺", "死んだほうが", "消えてしまいたい")
    keywords.foreach { kw =>
      assert(Rules.detectCrisis(s"もう$kw"), s"Should detect: $kw")
    }

  test("detectCrisis is case-insensitive"):
    assert(Rules.detectCrisis("I want to KILL MYSELF"))
    assert(Rules.detectCrisis("SUICIDAL thoughts"))

  test("detectCrisis returns false for normal text"):
    assert(!Rules.detectCrisis("I'm feeling stressed but okay"))
    assert(!Rules.detectCrisis("I want to improve my practice"))
    assert(!Rules.detectCrisis(""))

  // ── detectPatternsFromText ────────────────────────────────────────────────

  test("empty text returns no patterns"):
    assert(Rules.detectPatternsFromText("").isEmpty)
    assert(Rules.detectPatternsFromText("   ").isEmpty)

  test("detects perfectionism"):
    val result = Rules.detectPatternsFromText("I'm doing it wrong, not good enough")
    assert(result.contains(HarmfulPattern.Perfectionism))

  test("detects perfectionism Japanese keywords"):
    val result = Rules.detectPatternsFromText("ちゃんとできてない")
    assert(result.contains(HarmfulPattern.Perfectionism))

  test("detects forced_acceptance"):
    val result = Rules.detectPatternsFromText("I should accept this and just let go")
    assert(result.contains(HarmfulPattern.ForcedAcceptance))

  test("detects overmonitoring"):
    val result = Rules.detectPatternsFromText("I keep checking if I'm calm enough")
    assert(result.contains(HarmfulPattern.Overmonitoring))

  test("detects performance_framing"):
    val result = Rules.detectPatternsFromText("I want to optimize my productivity")
    assert(result.contains(HarmfulPattern.PerformanceFraming))

  test("detects compulsive_continuation"):
    val result = Rules.detectPatternsFromText("it's not working but I should continue anyway")
    assert(result.contains(HarmfulPattern.CompulsiveContinuation))

  test("detects breath_tension"):
    val result = Rules.detectPatternsFromText("breathing makes me more anxious")
    assert(result.contains(HarmfulPattern.BreathTension))

  test("detects self_scoring"):
    val result = Rules.detectPatternsFromText("I'd rate myself 3 out of 10")
    assert(result.contains(HarmfulPattern.SelfScoring))

  test("detects rumination via keywords"):
    val result = Rules.detectPatternsFromText("I can't stop thinking about it, replaying it over and over")
    assert(result.contains(HarmfulPattern.Rumination))

  test("detects escalating_frustration"):
    val result = Rules.detectPatternsFromText("This is getting more frustrated and irritating")
    assert(result.contains(HarmfulPattern.EscalatingFrustration))

  test("detects escalating_frustration Japanese"):
    val result = Rules.detectPatternsFromText("余計にもっとイライラする")
    assert(result.contains(HarmfulPattern.EscalatingFrustration))

  // ── should_language special case ──────────────────────────────────────────

  test("should_language requires >= 3 occurrences"):
    val twoOccurrences = "I should do this and I must try"
    assert(!Rules.detectPatternsFromText(twoOccurrences).contains(HarmfulPattern.ShouldLanguage))

    val threeOccurrences = "I should do this, I must try, and I have to succeed"
    assert(Rules.detectPatternsFromText(threeOccurrences).contains(HarmfulPattern.ShouldLanguage))

  test("should_language detects Japanese べき"):
    val text = "すべきことが多い。やるべきだ。べきことをしなければ"
    assert(Rules.detectPatternsFromText(text).contains(HarmfulPattern.ShouldLanguage))

  // ── Multiple patterns ─────────────────────────────────────────────────────

  test("detects multiple patterns in same text"):
    val text = "I'm doing it wrong, I keep checking if I'm calm, and it's not working but I push through"
    val patterns = Rules.detectPatternsFromText(text)
    assert(patterns.contains(HarmfulPattern.Perfectionism))
    assert(patterns.contains(HarmfulPattern.Overmonitoring))
    assert(patterns.contains(HarmfulPattern.CompulsiveContinuation))

  test("no duplicates in results"):
    val text = "correctly properly right way the right way doing it wrong not doing it right"
    val patterns = Rules.detectPatternsFromText(text)
    assertEquals(patterns.count(_ == HarmfulPattern.Perfectionism), 1)

  // ── assessRisk ────────────────────────────────────────────────────────────

  test("no patterns + low tension = none"):
    assertEquals(Rules.assessRisk(Nil, 3), RiskLevel.None)

  test("no patterns + tension 5 = low"):
    assertEquals(Rules.assessRisk(Nil, 5), RiskLevel.Low)

  test("single mild pattern = low"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.Perfectionism), 3),
      RiskLevel.Low
    )

  test("single moderate-severity pattern = moderate"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.ForcedAcceptance), 3),
      RiskLevel.Moderate
    )

  test("overmonitoring = moderate"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.Overmonitoring), 3),
      RiskLevel.Moderate
    )

  test("breath_tension = moderate"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.BreathTension), 2),
      RiskLevel.Moderate
    )

  test("two patterns = moderate"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.Perfectionism, HarmfulPattern.ShouldLanguage), 3),
      RiskLevel.Moderate
    )

  test("three patterns = high"):
    assertEquals(
      Rules.assessRisk(
        List(HarmfulPattern.Perfectionism, HarmfulPattern.ShouldLanguage, HarmfulPattern.SelfScoring),
        3
      ),
      RiskLevel.High
    )

  test("compulsive_continuation with tension 4 = high"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.CompulsiveContinuation), 4),
      RiskLevel.High
    )

  test("compulsive_continuation with low tension = moderate"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.CompulsiveContinuation), 2),
      RiskLevel.Moderate
    )

  test("escalating_frustration with tension 4 = high"):
    assertEquals(
      Rules.assessRisk(List(HarmfulPattern.EscalatingFrustration), 4),
      RiskLevel.High
    )
