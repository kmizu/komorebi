package komorebi.domain

import io.circe.{Encoder, Decoder, KeyEncoder, KeyDecoder}

// ── Risk level with ordering ────────────────────────────────────────────────

enum RiskLevel derives CanEqual:
  case None, Low, Moderate, High, Crisis

object RiskLevel:
  val order: Map[RiskLevel, Int] = Map(
    None -> 0, Low -> 1, Moderate -> 2, High -> 3, Crisis -> 4
  )

  given Ordering[RiskLevel] = Ordering.by(order)

  def max(a: RiskLevel, b: RiskLevel): RiskLevel =
    if order(a) >= order(b) then a else b

  given Encoder[RiskLevel] = Encoder.encodeString.contramap(toJson)
  given Decoder[RiskLevel] = Decoder.decodeString.emap(fromJson)

  def toJson(r: RiskLevel): String = r match
    case None     => "none"
    case Low      => "low"
    case Moderate => "moderate"
    case High     => "high"
    case Crisis   => "crisis"

  def fromJson(s: String): Either[String, RiskLevel] = s match
    case "none"     => Right(None)
    case "low"      => Right(Low)
    case "moderate" => Right(Moderate)
    case "high"     => Right(High)
    case "crisis"   => Right(Crisis)
    case other      => Left(s"Unknown risk level: $other")

// ── Supervisor action ───────────────────────────────────────────────────────

enum SupervisorAction derives CanEqual:
  case Proceed, Soften, Shorten, Switch, Stop, Crisis

object SupervisorAction:
  given Encoder[SupervisorAction] = Encoder.encodeString.contramap(toJson)
  given Decoder[SupervisorAction] = Decoder.decodeString.emap(fromJson)

  def toJson(a: SupervisorAction): String = a match
    case Proceed => "proceed"
    case Soften  => "soften"
    case Shorten => "shorten"
    case Switch  => "switch"
    case Stop    => "stop"
    case Crisis  => "crisis"

  def fromJson(s: String): Either[String, SupervisorAction] = s match
    case "proceed" => Right(Proceed)
    case "soften"  => Right(Soften)
    case "shorten" => Right(Shorten)
    case "switch"  => Right(Switch)
    case "stop"    => Right(Stop)
    case "crisis"  => Right(Crisis)
    case other     => Left(s"Unknown action: $other")

// ── Guidance mode ───────────────────────────────────────────────────────────

enum GuidanceMode derives CanEqual:
  case Breath, Sound, Body, External, Reset, Abort

object GuidanceMode:
  given Encoder[GuidanceMode] = Encoder.encodeString.contramap(toJson)
  given Decoder[GuidanceMode] = Decoder.decodeString.emap(fromJson)
  given KeyEncoder[GuidanceMode] = KeyEncoder.instance(toJson)
  given KeyDecoder[GuidanceMode] = KeyDecoder.instance(s => fromJson(s).toOption)

  def toJson(m: GuidanceMode): String = m match
    case Breath   => "breath"
    case Sound    => "sound"
    case Body     => "body"
    case External => "external"
    case Reset    => "reset"
    case Abort    => "abort"

  def fromJson(s: String): Either[String, GuidanceMode] = s match
    case "breath"   => Right(Breath)
    case "sound"    => Right(Sound)
    case "body"     => Right(Body)
    case "external" => Right(External)
    case "reset"    => Right(Reset)
    case "abort"    => Right(Abort)
    case other      => Left(s"Unknown mode: $other")

// ── Guidance duration ───────────────────────────────────────────────────────

opaque type GuidanceDuration = Int

object GuidanceDuration:
  val Thirty: GuidanceDuration  = 30
  val Sixty: GuidanceDuration   = 60
  val OneEighty: GuidanceDuration = 180
  val all: List[GuidanceDuration] = List(30, 60, 180)

  def apply(n: Int): Option[GuidanceDuration] = n match
    case 30 | 60 | 180 => Some(n)
    case _              => Option.empty

  def unsafeFrom(n: Int): GuidanceDuration = apply(n).getOrElse(
    throw IllegalArgumentException(s"Invalid duration: $n")
  )

  given Encoder[GuidanceDuration] = Encoder.encodeInt.contramap(_.value)
  given Decoder[GuidanceDuration] = Decoder.decodeInt.emap(n =>
    apply(n).toRight(s"Invalid duration: $n")
  )

  extension (d: GuidanceDuration) def value: Int = d

// ── Session intent ──────────────────────────────────────────────────────────

enum SessionIntent derives CanEqual:
  case Calming, Grounding, Checkin

object SessionIntent:
  given Encoder[SessionIntent] = Encoder.encodeString.contramap(toJson)
  given Decoder[SessionIntent] = Decoder.decodeString.emap(fromJson)

  def toJson(i: SessionIntent): String = i match
    case Calming   => "calming"
    case Grounding => "grounding"
    case Checkin   => "checkin"

  def fromJson(s: String): Either[String, SessionIntent] = s match
    case "calming"   => Right(Calming)
    case "grounding" => Right(Grounding)
    case "checkin"   => Right(Checkin)
    case other       => Left(s"Unknown intent: $other")

// ── Last session outcome ────────────────────────────────────────────────────

enum LastSessionOutcome derives CanEqual:
  case Relieving, Neutral, Pressuring

object LastSessionOutcome:
  given Encoder[LastSessionOutcome] = Encoder.encodeString.contramap(toJson)
  given Decoder[LastSessionOutcome] = Decoder.decodeString.emap(fromJson)

  def toJson(o: LastSessionOutcome): String = o match
    case Relieving  => "relieving"
    case Neutral    => "neutral"
    case Pressuring => "pressuring"

  def fromJson(s: String): Either[String, LastSessionOutcome] = s match
    case "relieving"  => Right(Relieving)
    case "neutral"    => Right(Neutral)
    case "pressuring" => Right(Pressuring)
    case other        => Left(s"Unknown outcome: $other")

// ── Harmful pattern ─────────────────────────────────────────────────────────

enum HarmfulPattern derives CanEqual:
  case Perfectionism, ForcedAcceptance, Overmonitoring, PerformanceFraming,
       ShouldLanguage, CompulsiveContinuation, BreathTension, SelfScoring,
       Rumination, EscalatingFrustration

object HarmfulPattern:
  given Encoder[HarmfulPattern] = Encoder.encodeString.contramap(toJson)
  given Decoder[HarmfulPattern] = Decoder.decodeString.emap(fromJson)

  def toJson(p: HarmfulPattern): String = p match
    case Perfectionism          => "perfectionism"
    case ForcedAcceptance       => "forced_acceptance"
    case Overmonitoring         => "overmonitoring"
    case PerformanceFraming     => "performance_framing"
    case ShouldLanguage         => "should_language"
    case CompulsiveContinuation => "compulsive_continuation"
    case BreathTension          => "breath_tension"
    case SelfScoring            => "self_scoring"
    case Rumination             => "rumination"
    case EscalatingFrustration  => "escalating_frustration"

  def fromJson(s: String): Either[String, HarmfulPattern] = s match
    case "perfectionism"           => Right(Perfectionism)
    case "forced_acceptance"       => Right(ForcedAcceptance)
    case "overmonitoring"          => Right(Overmonitoring)
    case "performance_framing"     => Right(PerformanceFraming)
    case "should_language"         => Right(ShouldLanguage)
    case "compulsive_continuation" => Right(CompulsiveContinuation)
    case "breath_tension"          => Right(BreathTension)
    case "self_scoring"            => Right(SelfScoring)
    case "rumination"              => Right(Rumination)
    case "escalating_frustration"  => Right(EscalatingFrustration)
    case other                     => Left(s"Unknown pattern: $other")

// ── Emotional tone ──────────────────────────────────────────────────────────

enum EmotionalTone derives CanEqual:
  case Distressed, Neutral, Positive, Mixed

object EmotionalTone:
  given Encoder[EmotionalTone] = Encoder.encodeString.contramap(toJson)
  given Decoder[EmotionalTone] = Decoder.decodeString.emap(fromJson)

  def toJson(t: EmotionalTone): String = t match
    case Distressed => "distressed"
    case Neutral    => "neutral"
    case Positive   => "positive"
    case Mixed      => "mixed"

  def fromJson(s: String): Either[String, EmotionalTone] = s match
    case "distressed" => Right(Distressed)
    case "neutral"    => Right(Neutral)
    case "positive"   => Right(Positive)
    case "mixed"      => Right(Mixed)
    case other        => Left(s"Unknown tone: $other")

// ── Guidance level ──────────────────────────────────────────────────────────

enum GuidanceLevel derives CanEqual:
  case Minimal, Moderate, Detailed

object GuidanceLevel:
  given Encoder[GuidanceLevel] = Encoder.encodeString.contramap(toJson)
  given Decoder[GuidanceLevel] = Decoder.decodeString.emap(fromJson)

  def toJson(l: GuidanceLevel): String = l match
    case Minimal  => "minimal"
    case Moderate => "moderate"
    case Detailed => "detailed"

  def fromJson(s: String): Either[String, GuidanceLevel] = s match
    case "minimal"  => Right(Minimal)
    case "moderate" => Right(Moderate)
    case "detailed" => Right(Detailed)
    case other      => Left(s"Unknown level: $other")
