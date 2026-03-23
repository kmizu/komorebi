package komorebi.domain

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.*

// ── Checkin data ────────────────────────────────────────────────────────────

final case class CheckinData(
  mood: Int,             // 1-5
  tension: Int,          // 1-5
  selfCritical: Boolean,
  intent: SessionIntent,
  lastSessionOutcome: Option[LastSessionOutcome] = Option.empty,
  freeText: Option[String] = Option.empty,
)

object CheckinData:
  given Encoder[CheckinData] = deriveEncoder
  given Decoder[CheckinData] = deriveDecoder

// ── Supervisor decision ─────────────────────────────────────────────────────

final case class SupervisorDecision(
  riskLevel: RiskLevel,
  patterns: List[HarmfulPattern],
  action: SupervisorAction,
  recommendedMode: GuidanceMode,
  message: String,
  guidanceDuration: GuidanceDuration,
)

object SupervisorDecision:
  given Encoder[SupervisorDecision] = deriveEncoder
  given Decoder[SupervisorDecision] = deriveDecoder

// ── Guidance script ─────────────────────────────────────────────────────────

final case class GuidanceScript(
  mode: GuidanceMode,
  duration: GuidanceDuration,
  text: String,
  isPreset: Boolean,
)

object GuidanceScript:
  given Encoder[GuidanceScript] = deriveEncoder
  given Decoder[GuidanceScript] = deriveDecoder

// ── Post outcome ────────────────────────────────────────────────────────────

final case class PostOutcome(
  feltBetter: Boolean,
  wouldContinue: Boolean,
  notes: Option[String] = Option.empty,
)

object PostOutcome:
  given Encoder[PostOutcome] = deriveEncoder
  given Decoder[PostOutcome] = deriveDecoder

// ── Session record ──────────────────────────────────────────────────────────

final case class SessionRecord(
  id: String,
  createdAt: String,
  checkin: CheckinData,
  supervisorDecision: SupervisorDecision,
  guidance: GuidanceScript,
  postOutcome: Option[PostOutcome] = Option.empty,
  summary: Option[String] = Option.empty,
  reflectionProfile: Option[String] = Option.empty,
  reflectionSummary: Option[String] = Option.empty,
)

object SessionRecord:
  given Encoder[SessionRecord] = deriveEncoder
  given Decoder[SessionRecord] = deriveDecoder

// ── Personalization hints ───────────────────────────────────────────────────

final case class PersonalizationHints(
  recentPatterns: List[HarmfulPattern],
  preferredMode: Option[GuidanceMode],
  avoidMode: Option[GuidanceMode],
  avgTension: Double,
  sessionCount: Int,
  lastRiskLevel: Option[RiskLevel],
  notes: List[String],
)

object PersonalizationHints:
  val empty: PersonalizationHints = PersonalizationHints(
    recentPatterns = Nil,
    preferredMode = Option.empty,
    avoidMode = Option.empty,
    avgTension = 3.0,
    sessionCount = 0,
    lastRiskLevel = Option.empty,
    notes = Nil,
  )

  given Encoder[PersonalizationHints] = deriveEncoder
  given Decoder[PersonalizationHints] = deriveDecoder

// ── API response ────────────────────────────────────────────────────────────

final case class ApiResponse[A](
  success: Boolean,
  data: Option[A] = Option.empty,
  error: Option[String] = Option.empty,
)

object ApiResponse:
  def ok[A](data: A): ApiResponse[A] =
    ApiResponse(success = true, data = Some(data))

  def fail[A](error: String): ApiResponse[A] =
    ApiResponse(success = false, error = Some(error))

  given [A: Encoder]: Encoder[ApiResponse[A]] = deriveEncoder
  given [A: Decoder]: Decoder[ApiResponse[A]] = deriveDecoder

// ── Conversation message ────────────────────────────────────────────────────

enum ConversationRole derives CanEqual:
  case Agent, User

object ConversationRole:
  given Encoder[ConversationRole] = Encoder.encodeString.contramap {
    case Agent => "agent"
    case User  => "user"
  }
  given Decoder[ConversationRole] = Decoder.decodeString.emap {
    case "agent" => Right(Agent)
    case "user"  => Right(User)
    case other   => Left(s"Unknown role: $other")
  }

final case class ConversationMessage(
  role: ConversationRole,
  content: String,
)

object ConversationMessage:
  given Encoder[ConversationMessage] = deriveEncoder
  given Decoder[ConversationMessage] = deriveDecoder

// ── Reflection profile ──────────────────────────────────────────────────────

final case class ReflectionProfile(
  mood: Int,
  tension: Int,
  selfCritical: Boolean,
  intent: SessionIntent,
  lastSessionOutcome: Option[LastSessionOutcome] = Option.empty,
  freeText: String = "",
  themes: List[String] = Nil,
  anchors: List[String] = Nil,
  emotionalTone: EmotionalTone = EmotionalTone.Neutral,
  mentionedTechnique: Option[String] = Option.empty,
)

object ReflectionProfile:
  given Encoder[ReflectionProfile] = deriveEncoder
  given Decoder[ReflectionProfile] = deriveDecoder

// ── Session plan ────────────────────────────────────────────────────────────

final case class SessionPlan(
  riskLevel: RiskLevel,
  patterns: List[HarmfulPattern],
  action: SupervisorAction,
  mood: Int,
  goal: SessionIntent,
  recommendedMode: GuidanceMode,
  guidanceDuration: GuidanceDuration,
  guidanceLevel: GuidanceLevel,
  practiceHistorySummary: String,
  message: String,
  reflectionSummary: String,
  guidanceHints: List[String],
)

object SessionPlan:
  given Encoder[SessionPlan] = deriveEncoder
  given Decoder[SessionPlan] = deriveDecoder

// ── User memory ─────────────────────────────────────────────────────────────

final case class UserMemory(
  version: Int,
  lastUpdated: String,
  sessionCount: Int,
  corePatterns: List[String],
  whatHelps: List[String],
  whatHurts: List[String],
  lifeContext: List[String],
  languageNotes: List[String],
  trajectory: String,
)

object UserMemory:
  val empty: UserMemory = UserMemory(
    version = 0,
    lastUpdated = "",
    sessionCount = 0,
    corePatterns = Nil,
    whatHelps = Nil,
    whatHurts = Nil,
    lifeContext = Nil,
    languageNotes = Nil,
    trajectory = "",
  )

  given Encoder[UserMemory] = deriveEncoder
  given Decoder[UserMemory] = deriveDecoder

// ── Safety recommendation ───────────────────────────────────────────────────

final case class SafetyRecommendation(
  action: SupervisorAction,
  recommendedMode: GuidanceMode,
  guidanceDuration: GuidanceDuration,
  message: String,
)

// ── Reflection turn result ──────────────────────────────────────────────────

final case class ReflectionTurnResult(
  agentMessage: String,
  userTurnCount: Int,
  done: Boolean,
  crisis: Boolean,
  profile: Option[ReflectionProfile] = Option.empty,
)

object ReflectionTurnResult:
  given Encoder[ReflectionTurnResult] = deriveEncoder
  given Decoder[ReflectionTurnResult] = deriveDecoder
