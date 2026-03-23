package komorebi.config

final case class AppConfig(
  port: Int = 8080,
  dbPath: String = "data/mindfulness.db",
  openaiApiKey: Option[String] = Option.empty,
  openaiModel: String = "gpt-5.4",
  openaiChatModel: String = "gpt-5.4-mini",
  elevenLabsApiKey: Option[String] = Option.empty,
  elevenLabsVoiceId: String = "EXAVITQu4vr4xnSDxMaL",
  ttsCacheDir: String = "data/tts-cache",
  corsOrigin: String = "http://localhost:3000",
)

object AppConfig:
  def fromEnv: AppConfig =
    AppConfig(
      port = sys.env.get("KOMOREBI_PORT").flatMap(_.toIntOption).getOrElse(8080),
      dbPath = sys.env.getOrElse("KOMOREBI_DB_PATH", "data/mindfulness.db"),
      openaiApiKey = sys.env.get("OPENAI_API_KEY").filter(_.nonEmpty),
      openaiModel = sys.env.getOrElse("OPENAI_MODEL", "gpt-5.4"),
      openaiChatModel = sys.env.getOrElse("OPENAI_CHAT_MODEL", "gpt-5.4-mini"),
      elevenLabsApiKey = sys.env.get("ELEVENLABS_API_KEY").filter(_.nonEmpty),
      elevenLabsVoiceId = sys.env.getOrElse("ELEVENLABS_VOICE_ID", "EXAVITQu4vr4xnSDxMaL"),
      ttsCacheDir = sys.env.getOrElse("KOMOREBI_TTS_CACHE", "data/tts-cache"),
      corsOrigin = sys.env.getOrElse("KOMOREBI_CORS_ORIGIN", "http://localhost:3000"),
    )
