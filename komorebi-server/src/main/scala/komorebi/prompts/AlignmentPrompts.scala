package komorebi.prompts

import komorebi.domain.*

object AlignmentPrompts:

  private def getModeDescription(mode: GuidanceMode): String = mode match
    case GuidanceMode.Breath   => "noticing breath without changing it"
    case GuidanceMode.Sound    => "passive listening to environmental sounds"
    case GuidanceMode.Body     => "weight, pressure, contact sensations"
    case GuidanceMode.External => "visual field and surfaces in the room"
    case GuidanceMode.Reset    => "brief grounding, short and simple"
    case GuidanceMode.Abort    => "safe stop — do not use this function for abort mode"

  def buildAlignmentPrompt(
    mode: GuidanceMode,
    duration: GuidanceDuration,
    plan: SessionPlan,
    locale: String,
  ): String =
    val lang = if locale == "ja" then "Japanese" else "English"
    val durationDesc = duration.value match
      case 30  => "30 seconds"
      case 60  => "1 minute"
      case 180 => "3 minutes"
      case _   => s"${duration.value} seconds"

    val hintsSection = if plan.guidanceHints.nonEmpty then
      s"\nPersonalization context:\n${plan.guidanceHints.map(h => s"- $h").mkString("\n")}\n"
    else ""

    val verbosityNote = plan.guidanceLevel match
      case GuidanceLevel.Detailed => "Guidance level: DETAILED — more words, step-by-step, supportive scaffolding."
      case GuidanceLevel.Minimal  => "Guidance level: MINIMAL — spare, open-ended, give space. Few words."
      case GuidanceLevel.Moderate => "Guidance level: MODERATE — clear and grounded, not overwhelming."

    s"""Write a $durationDesc mindfulness guidance script.
       |
       |Mode: ${GuidanceMode.toJson(mode)} (${getModeDescription(mode)})
       |Risk level: ${RiskLevel.toJson(plan.riskLevel)}
       |State summary: ${plan.reflectionSummary}
       |$verbosityNote
       |$hintsSection
       |Style rules (non-negotiable):
       |- Write in $lang
       |- NO "just", "relax", "let go", "accept", "surrender"
       |- NO spiritual language: "energy", "being", "presence", "healing", "universe"
       |- NO imperative pressure: "you should", "you must", "try to"
       |- NO evaluation: "good", "well done", "right", "perfect"
       |- NO promises: "you'll feel", "this will help", "this will heal"
       |- Sensory and specific — name actual sensations, sounds, surfaces
       |- Permissive, not directive: "you could..." not "now you will..."
       |- Duration-appropriate: 30s = one anchor only; 1min = one anchor with gentle variation; 3min = two anchors with transition
       |
       |Return ONLY the guidance text. No title, no label, no explanation.""".stripMargin
