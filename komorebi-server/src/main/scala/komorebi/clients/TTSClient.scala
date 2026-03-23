package komorebi.clients

import cats.effect.IO
import sttp.client3.*
import io.circe.syntax.*
import io.circe.Json
import java.security.MessageDigest
import java.nio.file.{Files, Path, Paths}

trait TTSClient:
  def isConfigured: Boolean
  def textToSpeech(text: String): IO[Array[Byte]]

class ElevenLabsTTSClient(
  apiKey: String,
  voiceId: String,
  cacheDir: String,
  backend: SttpBackend[IO, Any],
) extends TTSClient:

  override def isConfigured: Boolean = true

  override def textToSpeech(text: String): IO[Array[Byte]] =
    val key = sha256(text + voiceId)
    val cachePath = Paths.get(cacheDir, s"$key.mp3")

    IO.blocking(Files.exists(cachePath)).flatMap {
      case true  => IO.blocking(Files.readAllBytes(cachePath))
      case false => fetchAndCache(text, cachePath)
    }

  private def fetchAndCache(text: String, cachePath: Path): IO[Array[Byte]] =
    val body = Json.obj(
      "text" -> text.asJson,
      "model_id" -> "eleven_multilingual_v2".asJson,
      "voice_settings" -> Json.obj(
        "stability" -> 0.80.asJson,
        "similarity_boost" -> 0.75.asJson,
        "style" -> 0.0.asJson,
        "use_speaker_boost" -> false.asJson,
      ),
    )

    val request = basicRequest
      .post(uri"https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
      .header("xi-api-key", apiKey)
      .contentType("application/json")
      .body(body.noSpaces)
      .response(asByteArrayAlways)

    request.send(backend).flatMap { response =>
      if response.code.isSuccess then
        val data = response.body
        IO.blocking {
          Files.createDirectories(cachePath.getParent)
          Files.write(cachePath, data)
        }.as(data)
      else
        IO.raiseError(RuntimeException(s"ElevenLabs API error ${response.code}"))
    }

  private def sha256(s: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(s.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString

class NoOpTTSClient extends TTSClient:
  override def isConfigured: Boolean = false
  override def textToSpeech(text: String): IO[Array[Byte]] =
    IO.raiseError(RuntimeException("TTS not configured"))
