package komorebi.clients

import cats.effect.IO
import sttp.client3.*
import sttp.model.Part
import io.circe.parser.parse

trait STTClient:
  def isConfigured: Boolean
  def speechToText(audioData: Array[Byte], language: Option[String]): IO[String]
  def getSingleUseToken: IO[String]

class ElevenLabsSTTClient(
  apiKey: String,
  backend: SttpBackend[IO, Any],
) extends STTClient:

  override def isConfigured: Boolean = true

  override def speechToText(audioData: Array[Byte], language: Option[String]): IO[String] =
    val filePart = multipart("file", audioData).fileName("audio.webm").contentType("audio/webm")
    val modelPart = multipart("model_id", "scribe_v1")
    val langParts = language.map(l => multipart("language_code", l)).toList

    val request = basicRequest
      .post(uri"https://api.elevenlabs.io/v1/speech-to-text")
      .header("xi-api-key", apiKey)
      .multipartBody(filePart :: modelPart :: langParts)
      .response(asStringAlways)

    request.send(backend).flatMap { response =>
      if response.code.isSuccess then
        parse(response.body).flatMap(_.hcursor.downField("text").as[String]) match
          case Right(text) => IO.pure(text.trim)
          case Left(err)   => IO.raiseError(RuntimeException(s"Failed to parse STT response: $err"))
      else
        IO.raiseError(RuntimeException(s"ElevenLabs STT error ${response.code}"))
    }

  override def getSingleUseToken: IO[String] =
    val request = basicRequest
      .post(uri"https://api.elevenlabs.io/v1/single-use-token/realtime_scribe")
      .header("xi-api-key", apiKey)
      .response(asStringAlways)

    request.send(backend).flatMap { response =>
      if response.code.isSuccess then
        parse(response.body).flatMap(_.hcursor.downField("token").as[String]) match
          case Right(token) => IO.pure(token)
          case Left(err)    => IO.raiseError(RuntimeException(s"Failed to parse token: $err"))
      else
        IO.raiseError(RuntimeException(s"ElevenLabs token error ${response.code}"))
    }

class NoOpSTTClient extends STTClient:
  override def isConfigured: Boolean = false
  override def speechToText(audioData: Array[Byte], language: Option[String]): IO[String] =
    IO.raiseError(RuntimeException("STT not configured"))
  override def getSingleUseToken: IO[String] =
    IO.raiseError(RuntimeException("STT not configured"))
