package komorebi.prompts

import komorebi.domain.*

object GuidancePrompts:

  private val ModeDescriptions: Map[GuidanceMode, String] = Map(
    GuidanceMode.Breath   -> "gentle breath awareness — notice without trying to change anything",
    GuidanceMode.Sound    -> "sounds in the environment — whatever is already there, no special searching",
    GuidanceMode.Body     -> "weight and pressure of body against surfaces — chair, floor, hands in lap",
    GuidanceMode.External -> "something visible in the room — a surface, shape, texture, or object",
    GuidanceMode.Reset    -> "ultra-short reset — simple noticing, no goals",
    GuidanceMode.Abort    -> "gentle stopping script — brief, non-alarming, easy way to end",
  )

  private val ApproximateWords: Map[GuidanceDuration, Int] = Map(
    GuidanceDuration.Thirty -> 45,
    GuidanceDuration.Sixty -> 90,
    GuidanceDuration.OneEighty -> 270,
  )

  def buildGuidancePrompt(
    mode: GuidanceMode,
    duration: GuidanceDuration,
    riskLevel: RiskLevel,
    supervisorMessage: String,
  ): String =
    val words = ApproximateWords.getOrElse(duration, 90)
    val modeDesc = ModeDescriptions.getOrElse(mode, GuidanceMode.toJson(mode))

    s"""Write a ${duration.value}-second mindfulness guidance script.
       |
       |Mode: ${GuidanceMode.toJson(mode)} — $modeDesc
       |Duration: ${duration.value} seconds (~$words words when read slowly)
       |Risk level: ${RiskLevel.toJson(riskLevel)}
       |Supervisor note: "$supervisorMessage"
       |
       |Style rules (strictly enforced):
       |- Write exactly as if speaking aloud to a calm adult
       |- Use plain, everyday language — no spiritual vocabulary
       |- Avoid: "just", "simply", "relax", "let go", "surrender", "accept", "chakra", "energy", "healing", "universe", "sacred", "cosmic"
       |- Avoid: "you should", "you must", "try to", "you'll feel better", "this will help"
       |- Avoid: long paragraphs — use short sentences
       |- Do not promise any outcome or benefit
       |- Do not tell the user how they should feel
       |- One concrete instruction at a time
       |- End naturally — no abrupt cut-off
       |
       |Return ONLY the script text. No labels, no explanation, no quotes around it.""".stripMargin

  def buildSummaryPrompt(
    checkinFreeText: Option[String],
    postFeltBetter: Boolean,
    postWouldContinue: Boolean,
    postNotes: Option[String],
  ): String =
    s"""Write a 1-sentence session summary for a mindfulness log. Factual and brief (max 15 words).
       |
       |User pre-session notes: "${checkinFreeText.getOrElse("none")}"
       |Felt better after: $postFeltBetter
       |Would practice again today: $postWouldContinue
       |User post notes: "${postNotes.getOrElse("none")}"
       |
       |Return only the summary sentence. No quotes, no label, no explanation.""".stripMargin
