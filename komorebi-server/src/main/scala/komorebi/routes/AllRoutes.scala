package komorebi.routes

import cats.effect.IO
import cats.syntax.all.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import io.circe.{Json, Encoder, Decoder}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import komorebi.domain.*
import komorebi.domain.TapirSchemas.given
import komorebi.clients.*
import komorebi.db.*
import komorebi.agents.*
import komorebi.supervisor.Engine
import komorebi.guidance.GuidanceGenerator

case class ErrorResponse(success: Boolean = false, error: String)
object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder
  given Decoder[ErrorResponse] = deriveDecoder
  given sttp.tapir.Schema[ErrorResponse] = sttp.tapir.Schema.derived
  def from(msg: String): ErrorResponse = ErrorResponse(success = false, error = msg)

class AllRoutes(
  sessionRepo: SessionRepository,
  memoryRepo: UserMemoryRepository,
  llm: LLMClient,
  tts: TTSClient,
  stt: STTClient,
):

  private def postEndpoint(path: String) =
    endpoint.post.in("api" / path).in(jsonBody[Json]).out(jsonBody[Json]).errorOut(jsonBody[ErrorResponse])

  private def patchEndpoint(path: String) =
    endpoint.patch.in("api" / path).in(jsonBody[Json]).out(jsonBody[Json]).errorOut(jsonBody[ErrorResponse])

  private def ok[A: Encoder](data: A): Json =
    Json.obj("success" -> true.asJson, "data" -> data.asJson)

  private def wrapError(e: Throwable): ErrorResponse =
    ErrorResponse.from(Option(e.getMessage).getOrElse("Internal error"))

  // ── Reflect ─────────────────────────────────────────────────────────────

  private def reflectRoute = Http4sServerInterpreter[IO]().toRoutes(
    postEndpoint( "reflect").serverLogic { body =>
      val messages = body.hcursor.downField("messages").as[List[ConversationMessage]].getOrElse(Nil)
      val locale = body.hcursor.downField("locale").as[String].getOrElse("en")
      (for
        sessions <- sessionRepo.getRecent(1)
        memory   <- memoryRepo.get()
        result   <- ReflectionAgent.reflectionTurn(messages, locale, sessions.nonEmpty, memory, llm)
      yield Right(ok(result))).handleError(e => Left(wrapError(e)))
    }
  )

  // ── Personalize ─────────────────────────────────────────────────────────

  private def personalizeRoute = Http4sServerInterpreter[IO]().toRoutes(
    postEndpoint( "personalize").serverLogic { body =>
      val profile = body.hcursor.downField("profile").as[ReflectionProfile].getOrElse(
        ReflectionProfile(mood = 3, tension = 3, selfCritical = false, intent = SessionIntent.Checkin))
      val locale = body.hcursor.downField("locale").as[String].getOrElse("en")
      (for
        sessions <- sessionRepo.getRecent(10)
        hints     = PersonalizationHintsQuery.aggregateHints(sessions)
        memory   <- memoryRepo.get()
        plan     <- PersonalizationAgent.produceSessionPlan(profile, hints, memory, locale, llm)
      yield Right(ok(Json.obj("plan" -> plan.asJson)))).handleError(e => Left(wrapError(e)))
    }
  )

  // ── Guidance ────────────────────────────────────────────────────────────

  private def guidanceRoute = Http4sServerInterpreter[IO]().toRoutes(
    postEndpoint( "guidance").serverLogic { body =>
      val c = body.hcursor
      val mode = c.downField("mode").as[GuidanceMode].getOrElse(GuidanceMode.Breath)
      val duration = c.downField("duration").as[GuidanceDuration].getOrElse(GuidanceDuration.Sixty)
      val riskLevel = c.downField("riskLevel").as[RiskLevel].getOrElse(RiskLevel.None)
      val supervisorMessage = c.downField("supervisorMessage").as[String].getOrElse("")
      val locale = c.downField("locale").as[String].getOrElse("en")
      val plan = c.downField("plan").as[SessionPlan].toOption

      val script = plan match
        case Some(p) => AlignmentAgent.generateAlignedGuidance(p, locale, llm)
        case _       => GuidanceGenerator.generateGuidance(mode, duration, riskLevel, supervisorMessage, locale, llm)

      script.map(s => Right(ok(Json.obj("script" -> s.asJson)))).handleError(e => Left(wrapError(e)))
    }
  )

  // ── Session POST ────────────────────────────────────────────────────────

  private def sessionPostRoute = Http4sServerInterpreter[IO]().toRoutes(
    postEndpoint( "session").serverLogic { body =>
      body.as[SaveSessionData] match
        case Left(err) => IO.pure(Left(ErrorResponse.from(s"Invalid input: $err")))
        case Right(data) =>
          sessionRepo.save(data).map(id =>
            Right(ok(Json.obj("id" -> id.asJson)))
          ).handleError(e => Left(wrapError(e)))
    }
  )

  // ── Session PATCH ───────────────────────────────────────────────────────

  private def sessionPatchRoute = Http4sServerInterpreter[IO]().toRoutes(
    patchEndpoint( "session").serverLogic { body =>
      val c = body.hcursor
      val id = c.downField("id").as[String].getOrElse("")
      val postOutcome = c.downField("postOutcome").as[PostOutcome].getOrElse(PostOutcome(false, false))
      val locale = c.downField("locale").as[String].getOrElse("en")
      (for
        _ <- sessionRepo.updatePostOutcome(id, postOutcome, None)
        // Background memory update
        _ <- (for
          memory  <- memoryRepo.get()
          session <- sessionRepo.getById(id)
          _ <- session match
            case Some(s) =>
              val profile = ReflectionAgent.checkinToProfile(s.checkin)
              val plan = SessionPlan(
                riskLevel = s.supervisorDecision.riskLevel, patterns = s.supervisorDecision.patterns,
                action = s.supervisorDecision.action, recommendedMode = s.supervisorDecision.recommendedMode,
                guidanceDuration = s.supervisorDecision.guidanceDuration, message = s.supervisorDecision.message,
                mood = s.checkin.mood, goal = s.checkin.intent, guidanceLevel = GuidanceLevel.Moderate,
                practiceHistorySummary = "", reflectionSummary = "", guidanceHints = Nil,
              )
              MemoryAgent.updateUserMemory(memory, profile, plan, postOutcome, locale, llm)
                .flatMap(memoryRepo.save(_))
            case _ => IO.unit
        yield ()).start
      yield Right(ok(Json.obj("id" -> id.asJson)))).handleError(e => Left(wrapError(e)))
    }
  )

  // ── History ─────────────────────────────────────────────────────────────

  private def historyRoute = Http4sServerInterpreter[IO]().toRoutes(
    endpoint.get.in("api" / "history").in(query[Option[Int]]("limit"))
      .out(jsonBody[Json]).errorOut(jsonBody[ErrorResponse])
      .serverLogic { limitOpt =>
        val limit = limitOpt.getOrElse(20).min(50)
        (for
          sessions <- sessionRepo.getRecent(limit)
          hints     = PersonalizationHintsQuery.aggregateHints(sessions)
        yield Right(ok(Json.obj(
          "sessions" -> sessions.asJson,
          "hints" -> hints.asJson,
        )))).handleError(e => Left(wrapError(e)))
      }
  )

  // ── Checkin ─────────────────────────────────────────────────────────────

  private def checkinRoute = Http4sServerInterpreter[IO]().toRoutes(
    postEndpoint( "checkin").serverLogic { body =>
      val c = body.hcursor
      val checkin = CheckinData(
        mood = c.downField("mood").as[Int].getOrElse(3),
        tension = c.downField("tension").as[Int].getOrElse(3),
        selfCritical = c.downField("selfCritical").as[Boolean].getOrElse(false),
        intent = c.downField("intent").as[SessionIntent].getOrElse(SessionIntent.Checkin),
        lastSessionOutcome = c.downField("lastSessionOutcome").as[LastSessionOutcome].toOption,
        freeText = c.downField("freeText").as[String].toOption,
      )
      val locale = c.downField("locale").as[String].getOrElse("en")
      (for
        sessions <- sessionRepo.getRecent(10)
        hints     = PersonalizationHintsQuery.aggregateHints(sessions)
        decision <- Engine.evaluateCheckin(checkin, hints, None, locale, llm)
      yield Right(ok(Json.obj(
        "decision" -> decision.asJson,
        "hints" -> hints.asJson,
      )))).handleError(e => Left(wrapError(e)))
    }
  )

  // ── Supervisor ──────────────────────────────────────────────────────────

  private def supervisorRoute = Http4sServerInterpreter[IO]().toRoutes(
    postEndpoint( "supervisor").serverLogic { body =>
      val c = body.hcursor
      val userReport = c.downField("userReport").as[String].getOrElse("")
      val checkin = c.downField("checkin").as[CheckinData].getOrElse(
        CheckinData(3, 3, false, SessionIntent.Checkin))
      val locale = c.downField("locale").as[String].getOrElse("en")
      (for
        sessions <- sessionRepo.getRecent(10)
        hints     = PersonalizationHintsQuery.aggregateHints(sessions)
        decision <- Engine.evaluateCheckin(checkin, hints, Some(userReport), locale, llm)
      yield Right(ok(Json.obj("decision" -> decision.asJson)))).handleError(e => Left(wrapError(e)))
    }
  )

  // ── TTS ─────────────────────────────────────────────────────────────────

  private def ttsRoute = Http4sServerInterpreter[IO]().toRoutes(
    endpoint.post.in("api" / "tts").in(jsonBody[Json])
      .out(byteArrayBody).errorOut(jsonBody[ErrorResponse])
      .serverLogic { body =>
        val text = body.hcursor.downField("text").as[String].getOrElse("")
        if !tts.isConfigured then IO.pure(Left(ErrorResponse.from("TTS not configured")))
        else tts.textToSpeech(text).map(Right(_)).handleError(e => Left(wrapError(e)))
      }
  )

  // ── STT Token ───────────────────────────────────────────────────────────

  private def sttTokenRoute = Http4sServerInterpreter[IO]().toRoutes(
    endpoint.post.in("api" / "stt-token").out(jsonBody[Json]).errorOut(jsonBody[ErrorResponse])
      .serverLogic { _ =>
        if !stt.isConfigured then IO.pure(Left(ErrorResponse.from("STT not configured")))
        else stt.getSingleUseToken.map(token =>
          Right(ok(Json.obj("token" -> token.asJson)))
        ).handleError(e => Left(wrapError(e)))
      }
  )

  // ── Combined routes ─────────────────────────────────────────────────────

  def routes: HttpRoutes[IO] =
    reflectRoute <+> personalizeRoute <+> guidanceRoute <+>
    sessionPostRoute <+> sessionPatchRoute <+> historyRoute <+>
    checkinRoute <+> supervisorRoute <+> ttsRoute <+> sttTokenRoute
