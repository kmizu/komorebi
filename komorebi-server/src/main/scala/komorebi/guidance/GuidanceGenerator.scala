package komorebi.guidance

import cats.effect.IO
import komorebi.domain.*
import komorebi.clients.LLMClient
import komorebi.prompts.GuidancePrompts
import scala.util.matching.Regex

object GuidanceGenerator:

  private val StyleViolations: List[Regex] = List(
    """\bjust\b""".r, """\brelax\b""".r, """\blet go\b""".r, """\bsurrender\b""".r,
    """\bchakra\b""".r, """\benergy\b""".r, """\bhealing\b""".r, """\buniverse\b""".r,
    """\bsacred\b""".r, """\bcosmic\b""".r,
    """\byou should\b""".r, """\byou must\b""".r, """\btry to\b""".r,
    """\byou'll feel\b""".r, """\bthis will help\b""".r, """\bwill heal\b""".r,
  )

  private def checkStyleViolations(text: String): List[String] =
    val lower = text.toLowerCase
    StyleViolations.filter(_.findFirstIn(lower).isDefined).map(_.toString)

  private def fixCommonViolations(text: String): String =
    text
      .replaceAll("(?i)\\bjust\\s+", "")
      .replaceAll("(?i)\\byou should\\b", "")
      .replaceAll("(?i)\\byou must\\b", "")
      .replaceAll("(?i)\\btry to\\b", "")

  def generateGuidance(
    mode: GuidanceMode,
    duration: GuidanceDuration,
    riskLevel: RiskLevel,
    supervisorMessage: String,
    locale: String,
    llm: LLMClient,
  ): IO[GuidanceScript] =
    if Presets.isAlwaysPreset(mode) then
      IO.pure(GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true))
    else if !llm.isConfigured then
      IO.pure(GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true))
    else
      val prompt = GuidancePrompts.buildGuidancePrompt(mode, duration, riskLevel, supervisorMessage)
      val lang = if locale == "ja" then "Japanese" else "English"
      llm.complete(
        s"You write short, concrete mindfulness guidance scripts in $lang. No spiritual language. No promises.",
        prompt, 400
      ).attempt.map {
        case Left(_) =>
          GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true)
        case Right(raw) =>
          val violations = checkStyleViolations(raw)
          var text = raw.trim
          if violations.nonEmpty then
            text = fixCommonViolations(text)
            val remaining = checkStyleViolations(text)
            if remaining.length > 2 then
              GuidanceScript(mode, duration, Presets.getPreset(mode, duration, locale), isPreset = true)
            else
              GuidanceScript(mode, duration, text, isPreset = false)
          else
            GuidanceScript(mode, duration, text, isPreset = false)
      }
