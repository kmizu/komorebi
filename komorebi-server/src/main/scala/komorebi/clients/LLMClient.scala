package komorebi.clients

import cats.effect.IO
import io.circe.*
import io.circe.syntax.*
import io.circe.parser.parse
import sttp.client3.*
import sttp.client3.circe.*

trait LLMClient:
  def isConfigured: Boolean
  def complete(system: String, userMessage: String, maxTokens: Int = 512): IO[String]
  def completeChat(system: String, messages: List[ChatMessage], maxTokens: Int = 512): IO[String]

final case class ChatMessage(role: String, content: String)

object ChatMessage:
  given Encoder[ChatMessage] = Encoder.forProduct2("role", "content")(m => (m.role, m.content))

class OpenAILLMClient(
  apiKey: String,
  model: String,
  chatModel: String,
  backend: SttpBackend[IO, Any],
) extends LLMClient:

  override def isConfigured: Boolean = true

  override def complete(system: String, userMessage: String, maxTokens: Int): IO[String] =
    val body = Json.obj(
      "model" -> model.asJson,
      "max_tokens" -> maxTokens.asJson,
      "messages" -> Json.arr(
        Json.obj("role" -> "system".asJson, "content" -> system.asJson),
        Json.obj("role" -> "user".asJson, "content" -> userMessage.asJson),
      ),
    )
    callApi(body)

  override def completeChat(system: String, messages: List[ChatMessage], maxTokens: Int): IO[String] =
    val allMessages = Json.obj("role" -> "system".asJson, "content" -> system.asJson) +:
      messages.map(m => Json.obj("role" -> m.role.asJson, "content" -> m.content.asJson))
    val body = Json.obj(
      "model" -> chatModel.asJson,
      "max_tokens" -> maxTokens.asJson,
      "messages" -> allMessages.asJson,
    )
    callApi(body)

  private def callApi(body: Json): IO[String] =
    val request = basicRequest
      .post(uri"https://api.openai.com/v1/chat/completions")
      .header("Authorization", s"Bearer $apiKey")
      .contentType("application/json")
      .body(body.noSpaces)
      .response(asStringAlways)

    request.send(backend).flatMap { response =>
      if response.code.isSuccess then
        parse(response.body) match
          case Right(json) =>
            json.hcursor.downField("choices").downArray.downField("message").downField("content").as[String] match
              case Right(text) => IO.pure(text)
              case Left(err)   => IO.raiseError(RuntimeException(s"Failed to parse LLM response: $err"))
          case Left(err) => IO.raiseError(RuntimeException(s"Failed to parse JSON: $err"))
      else
        IO.raiseError(RuntimeException(s"OpenAI API error ${response.code}: ${response.body.take(500)}"))
    }

class NoOpLLMClient extends LLMClient:
  override def isConfigured: Boolean = false
  override def complete(system: String, userMessage: String, maxTokens: Int): IO[String] =
    IO.raiseError(RuntimeException("LLM not configured"))
  override def completeChat(system: String, messages: List[ChatMessage], maxTokens: Int): IO[String] =
    IO.raiseError(RuntimeException("LLM not configured"))
