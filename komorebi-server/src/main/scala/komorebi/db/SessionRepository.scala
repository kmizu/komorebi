package komorebi.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import komorebi.domain.*
import io.circe.syntax.*
import io.circe.parser.decode
import java.util.UUID

class SessionRepository(xa: Transactor[IO]):

  def save(data: SaveSessionData): IO[String] =
    val id = UUID.randomUUID().toString
    val createdAt = java.time.Instant.now().toString
    val patternsJson = data.supervisorDecision.patterns.map(HarmfulPattern.toJson).asJson.noSpaces

    sql"""INSERT INTO sessions (
      id, created_at,
      checkin_mood, checkin_tension, checkin_self_critical, checkin_intent,
      checkin_last_outcome, checkin_free_text,
      risk_level, patterns, action, recommended_mode, supervisor_message, guidance_duration,
      guidance_mode, guidance_text, guidance_is_preset,
      reflection_profile, reflection_summary
    ) VALUES (
      $id, $createdAt,
      ${data.checkin.mood}, ${data.checkin.tension}, ${data.checkin.selfCritical},
      ${SessionIntent.toJson(data.checkin.intent)},
      ${data.checkin.lastSessionOutcome.map(LastSessionOutcome.toJson)},
      ${data.checkin.freeText},
      ${RiskLevel.toJson(data.supervisorDecision.riskLevel)},
      $patternsJson,
      ${SupervisorAction.toJson(data.supervisorDecision.action)},
      ${GuidanceMode.toJson(data.supervisorDecision.recommendedMode)},
      ${data.supervisorDecision.message},
      ${data.supervisorDecision.guidanceDuration.value},
      ${GuidanceMode.toJson(data.guidance.mode)},
      ${data.guidance.text},
      ${data.guidance.isPreset},
      ${data.reflectionProfile},
      ${data.reflectionSummary}
    )""".update.run.transact(xa).as(id)

  def updatePostOutcome(id: String, post: PostOutcome, summary: Option[String]): IO[Unit] =
    sql"""UPDATE sessions SET
      post_felt_better = ${post.feltBetter},
      post_would_continue = ${post.wouldContinue},
      post_notes = ${post.notes},
      summary = $summary
    WHERE id = $id""".update.run.transact(xa).void

  def getRecent(limit: Int = 10): IO[List[SessionRecord]] =
    sql"""SELECT * FROM sessions ORDER BY created_at DESC LIMIT $limit"""
      .query[SessionRow].to[List].transact(xa).map(_.map(rowToSession))

  def getById(id: String): IO[Option[SessionRecord]] =
    sql"""SELECT * FROM sessions WHERE id = $id LIMIT 1"""
      .query[SessionRow].option.transact(xa).map(_.map(rowToSession))

  private def rowToSession(r: SessionRow): SessionRecord =
    val patterns = decode[List[String]](r.patterns)
      .getOrElse(Nil)
      .flatMap(s => HarmfulPattern.fromJson(s).toOption)

    SessionRecord(
      id = r.id,
      createdAt = r.createdAt,
      checkin = CheckinData(
        mood = r.checkinMood,
        tension = r.checkinTension,
        selfCritical = r.checkinSelfCritical,
        intent = SessionIntent.fromJson(r.checkinIntent).getOrElse(SessionIntent.Checkin),
        lastSessionOutcome = r.checkinLastOutcome.flatMap(s => LastSessionOutcome.fromJson(s).toOption),
        freeText = r.checkinFreeText,
      ),
      supervisorDecision = SupervisorDecision(
        riskLevel = RiskLevel.fromJson(r.riskLevel).getOrElse(RiskLevel.None),
        patterns = patterns,
        action = SupervisorAction.fromJson(r.action).getOrElse(SupervisorAction.Proceed),
        recommendedMode = GuidanceMode.fromJson(r.recommendedMode).getOrElse(GuidanceMode.Breath),
        message = r.supervisorMessage,
        guidanceDuration = GuidanceDuration(r.guidanceDuration).getOrElse(GuidanceDuration.Sixty),
      ),
      guidance = GuidanceScript(
        mode = GuidanceMode.fromJson(r.guidanceMode).getOrElse(GuidanceMode.Breath),
        duration = GuidanceDuration(r.guidanceDuration).getOrElse(GuidanceDuration.Sixty),
        text = r.guidanceText,
        isPreset = r.guidanceIsPreset,
      ),
      postOutcome = r.postFeltBetter.map(fb => PostOutcome(
        feltBetter = fb,
        wouldContinue = r.postWouldContinue.getOrElse(false),
        notes = r.postNotes,
      )),
      summary = r.summary,
      reflectionProfile = r.reflectionProfile,
      reflectionSummary = r.reflectionSummary,
    )

// Raw DB row
private[db] case class SessionRow(
  id: String, createdAt: String,
  checkinMood: Int, checkinTension: Int, checkinSelfCritical: Boolean,
  checkinIntent: String, checkinLastOutcome: Option[String], checkinFreeText: Option[String],
  riskLevel: String, patterns: String, action: String,
  recommendedMode: String, supervisorMessage: String, guidanceDuration: Int,
  guidanceMode: String, guidanceText: String, guidanceIsPreset: Boolean,
  postFeltBetter: Option[Boolean], postWouldContinue: Option[Boolean], postNotes: Option[String],
  summary: Option[String],
  reflectionProfile: Option[String], reflectionSummary: Option[String],
)

final case class SaveSessionData(
  checkin: CheckinData,
  supervisorDecision: SupervisorDecision,
  guidance: GuidanceScript,
  reflectionProfile: Option[String] = Option.empty,
  reflectionSummary: Option[String] = Option.empty,
)

object SaveSessionData:
  given io.circe.Encoder[SaveSessionData] = io.circe.generic.semiauto.deriveEncoder
  given io.circe.Decoder[SaveSessionData] = io.circe.generic.semiauto.deriveDecoder
