package komorebi

import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import com.comcast.ip4s.*
import doobie.*
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import komorebi.config.AppConfig
import komorebi.db.*
import komorebi.clients.*
import komorebi.routes.AllRoutes

object Main extends IOApp.Simple:

  override def run: IO[Unit] =
    val config = AppConfig.fromEnv

    val resources = for
      // sttp backend for HTTP clients
      sttpBackend <- HttpClientCatsBackend.resource[IO]()

      // SQLite transactor (single connection for SQLite)
      xa <- Resource.pure[IO, Transactor[IO]](
        Transactor.fromDriverManager[IO](
          "org.sqlite.JDBC",
          s"jdbc:sqlite:${config.dbPath}",
          logHandler = None,
        )
      )

      // Initialize DB schema
      _ <- Resource.eval(Schema.initialize(xa))

      // Repositories
      sessionRepo = SessionRepository(xa)
      memoryRepo  = UserMemoryRepository(xa)

      // External clients
      llmClient: LLMClient = config.openaiApiKey match
        case Some(key) => OpenAILLMClient(key, config.openaiModel, config.openaiChatModel, sttpBackend)
        case _         => NoOpLLMClient()

      ttsClient: TTSClient = config.elevenLabsApiKey match
        case Some(key) => ElevenLabsTTSClient(key, config.elevenLabsVoiceId, config.ttsCacheDir, sttpBackend)
        case _         => NoOpTTSClient()

      sttClient: STTClient = config.elevenLabsApiKey match
        case Some(key) => ElevenLabsSTTClient(key, sttpBackend)
        case _         => NoOpSTTClient()

      // Routes with CORS
      allRoutes = AllRoutes(sessionRepo, memoryRepo, llmClient, ttsClient, sttClient)
      corsRoutes = CORS.policy
        .withAllowOriginAll
        .withAllowMethodsAll
        .withAllowHeadersAll
        .httpRoutes(allRoutes.routes)

      // Server
      server <- EmberServerBuilder.default[IO]
        .withHost(host"0.0.0.0")
        .withPort(Port.fromInt(config.port).getOrElse(port"8080"))
        .withHttpApp(corsRoutes.orNotFound)
        .build
    yield server

    resources.use { server =>
      IO.println(s"Komorebi server started at ${server.addressIp4s}") *> IO.never
    }
