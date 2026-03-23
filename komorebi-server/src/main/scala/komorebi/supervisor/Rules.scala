package komorebi.supervisor

import komorebi.domain.*
import scala.util.matching.Regex

object Rules:

  // ── Keyword dictionaries ──────────────────────────────────────────────────

  private val PatternKeywords: Map[HarmfulPattern, List[String]] = Map(
    HarmfulPattern.Perfectionism -> List(
      "correctly", "properly", "right way", "the right way", "wrong way",
      "failing", "failed at", "messed up", "mess up", "doing it wrong",
      "not doing it right", "should be better", "not good enough", "good enough",
      "need to get this right", "getting it right", "doing this right",
      "i failed", "can't do this right", "not good at this",
      "ちゃんと", "うまくできない", "正しく", "失敗", "できてない",
    ),
    HarmfulPattern.ForcedAcceptance -> List(
      "i should accept", "i know i should accept", "trying to accept", "have to accept",
      "must accept", "supposed to accept", "need to accept", "force myself",
      "forcing myself", "just accept", "i should just accept", "have to let go",
      "supposed to let go", "just let go", "i must let go",
      "受け入れなければ", "受け入れるべき", "手放さなければ",
    ),
    HarmfulPattern.Overmonitoring -> List(
      "keep checking", "keep monitoring", "constantly checking", "checking whether",
      "checking if i", "monitoring myself", "watching myself", "observing myself observing",
      "meta", "am i doing this right", "tracking my", "tracking whether",
      "checking my breath", "checking my thoughts", "watching my thoughts",
      "am i calm", "checking if i'm calm", "see if it's working",
      "自分を観察", "確認し続ける", "チェックしている",
    ),
    HarmfulPattern.PerformanceFraming -> List(
      "productive", "productivity", "optimize", "optimization", "performance",
      "performing", "results", "getting results", "efficient", "efficiency",
      "track my progress", "measure", "score", "scoring", "rating",
      "how well i", "doing well", "improve my", "work on my",
      "効率", "成果", "パフォーマンス",
    ),
    HarmfulPattern.ShouldLanguage -> List(
      // Detected by counting — listed for reference only
      "should", "must", "have to", "need to", "supposed to", "obligated",
      "なければならない", "すべき", "べき",
    ),
    HarmfulPattern.CompulsiveContinuation -> List(
      "feel worse but", "getting worse but", "not helping but", "maybe i should continue",
      "push through", "keep going anyway", "not working but", "should continue anyway",
      "continue even though", "keep going even though", "still continuing", "forcing myself to continue",
      "even though it's hard", "even though it hurts", "even though it's not working",
      "続けるべきか", "苦しいけど続ける", "悪化してるけど",
    ),
    HarmfulPattern.BreathTension -> List(
      "breath makes me", "breathing makes me", "focus on breath", "breath is making",
      "concentrating on breath makes", "watching my breath makes", "breath tension",
      "breath anxiety", "anxious with breath", "tense with breath", "uncomfortable with breath",
      "breath feels wrong", "can't breathe right", "breathing feels forced",
      "呼吸に集中すると", "呼吸が苦しい", "息が",
    ),
    HarmfulPattern.SelfScoring -> List(
      "rate myself", "rating myself", "score myself", "scoring myself",
      "give myself", "out of 10", "10/10", "how well i did", "did i do well",
      "grade myself", "evaluate myself", "assess myself", "measure how",
      "how good i was", "how bad i was",
      "点数", "自己採点",
    ),
    HarmfulPattern.Rumination -> List(
      "keep thinking about", "can't stop thinking", "going over", "replay",
      "over and over", "circular", "same thought", "stuck on", "looping",
    ),
    HarmfulPattern.EscalatingFrustration -> List(
      "getting more frustrated", "more frustrated", "increasingly frustrated",
      "more tense", "getting tenser", "frustrating", "annoying", "irritating",
      "making me angry", "getting angry", "makes me more stressed",
      "余計に", "もっとイライラ", "ストレスが増える",
    ),
  )

  private val CrisisKeywords: List[String] = List(
    "hurt myself", "harm myself", "self harm", "self-harm",
    "want to die", "suicidal", "end my life", "kill myself",
    "not worth living", "can't go on", "can't take it anymore",
    "wish i was dead", "better off dead",
    "死にたい", "消えたい", "自殺", "死んだほうが", "消えてしまいたい",
  )

  // should_language uses a regex count instead of simple keyword match
  private val ShouldLanguageRegex: Regex =
    """(?i)\b(should|must|have to|need to|supposed to)\b|べき|なければ""".r

  // ── Detection functions ───────────────────────────────────────────────────

  def detectCrisis(text: String): Boolean =
    val lower = text.toLowerCase
    CrisisKeywords.exists(kw => lower.contains(kw))

  def detectPatternsFromText(text: String): List[HarmfulPattern] =
    if text.trim.isEmpty then return Nil

    val lower = text.toLowerCase
    val found = scala.collection.mutable.LinkedHashSet.empty[HarmfulPattern]

    PatternKeywords.foreach { (pattern, keywords) =>
      pattern match
        case HarmfulPattern.ShouldLanguage =>
          val count = ShouldLanguageRegex.findAllIn(lower).size
          if count >= 3 then found += HarmfulPattern.ShouldLanguage

        case HarmfulPattern.Rumination | HarmfulPattern.EscalatingFrustration =>
          // Keyword-only basic signal (LLM enriches later)
          if keywords.exists(kw => lower.contains(kw)) then found += pattern

        case _ =>
          if keywords.exists(kw => lower.contains(kw)) then found += pattern
    }

    found.toList

  def assessRisk(patterns: List[HarmfulPattern], tension: Int): RiskLevel =
    if patterns.isEmpty then
      return if tension >= 5 then RiskLevel.Low else RiskLevel.None

    val highSeverity = Set(HarmfulPattern.CompulsiveContinuation, HarmfulPattern.EscalatingFrustration)
    if patterns.exists(highSeverity.contains) then
      return if tension >= 4 then RiskLevel.High else RiskLevel.Moderate

    if patterns.length >= 3 then return RiskLevel.High
    if patterns.length >= 2 then return RiskLevel.Moderate

    val moderateSeverity = Set(HarmfulPattern.ForcedAcceptance, HarmfulPattern.Overmonitoring, HarmfulPattern.BreathTension)
    if patterns.exists(moderateSeverity.contains) then return RiskLevel.Moderate

    RiskLevel.Low
