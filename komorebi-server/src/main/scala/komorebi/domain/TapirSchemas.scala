package komorebi.domain

import sttp.tapir.Schema

// Tapir Schema instances for all domain enums (needed for JSON endpoint definitions)
object TapirSchemas:
  given Schema[RiskLevel] = Schema.string
  given Schema[SupervisorAction] = Schema.string
  given Schema[GuidanceMode] = Schema.string
  given Schema[GuidanceDuration] = Schema.schemaForInt.map(n => GuidanceDuration(n))(_.value)
  given Schema[SessionIntent] = Schema.string
  given Schema[LastSessionOutcome] = Schema.string
  given Schema[HarmfulPattern] = Schema.string
  given Schema[EmotionalTone] = Schema.string
  given Schema[GuidanceLevel] = Schema.string
  given Schema[ConversationRole] = Schema.string
