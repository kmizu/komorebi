package komorebi.agents

import cats.effect.IO
import komorebi.domain.*
import komorebi.clients.LLMClient
import komorebi.guidance.Presets
import komorebi.prompts.AlignmentPrompts
import scala.util.matching.Regex

object AlignmentAgent:

  private val StyleViolations: List[Regex] = List(
    """\bjust\b""".r, """\brelax\b""".r, """\blet go\b""".r, """\bsurrender\b""".r,
    """\bchakra\b""".r, """\benergy\b""".r, """\bhealing\b""".r, """\buniverse\b""".r,
    """\bsacred\b""".r, """\byou should\b""".r, """\byou must\b""".r, """\btry to\b""".r,
    """\byou'll feel\b""".r, """\bthis will help\b""".r,
  )

  def countViolations(text: String): Int =
    val lower = text.toLowerCase
    StyleViolations.count(_.findFirstIn(lower).isDefined)

  def fixViolations(text: String): String =
    text
      .replaceAll("(?i)\\bjust\\s+", "")
      .replaceAll("(?i)\\byou should\\b", "")
      .replaceAll("(?i)\\byou must\\b", "")
      .replaceAll("(?i)\\btry to\\b", "")

  def generateAlignedGuidance(
    plan: SessionPlan,
    locale: String,
    llm: LLMClient,
  ): IO[GuidanceScript] =
    val mode = plan.recommendedMode
    val duration = plan.guidanceDuration

    if Presets.isAlwaysPreset(mode) then
      IO.pure(GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true))
    else if !llm.isConfigured then
      IO.pure(GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true))
    else
      val prompt = AlignmentPrompts.buildAlignmentPrompt(mode, duration, plan, locale)
      val lang = if locale == "ja" then "Japanese" else "English"
      llm.complete(
        s"You write short, concrete mindfulness guidance scripts in $lang. No spiritual language. No promises. No pressure.",
        prompt, 500
      ).attempt.map {
        case Left(_) =>
          GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true)
        case Right(raw) =>
          var text = raw.trim
          if countViolations(text) > 0 then
            text = fixViolations(text)
            if countViolations(text) > 2 then
              GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true)
            else
              GuidanceScript(mode, duration, text, isPreset = false)
          else
            GuidanceScript(mode, duration, text, isPreset = false)
      }
