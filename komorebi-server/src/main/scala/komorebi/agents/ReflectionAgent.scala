package komorebi.agents

import cats.effect.IO
import komorebi.domain.*
import komorebi.clients.{LLMClient, ChatMessage}
import komorebi.supervisor.Rules
import komorebi.prompts.ReflectionPrompts
import io.circe.parser.parse

object ReflectionAgent:

  private val Scripted = Map(
    "en" -> List(
      "Hi. How are you feeling right now? Take a moment — there's no rush.",
      "Thanks for sharing. Is there anything weighing on you today — something in your body, your mind, or just a general feeling?",
      "One last thing — what would feel most useful right now? Calming down, grounding, or just checking in?",
    ),
    "ja" -> List(
      "こんにちは。今、どんな気持ちですか？ゆっくり教えてください。",
      "ありがとう。今日、特に気になっていることはありますか？体の感覚でも、気持ちのことでも、なんとなくでも。",
      "最後にひとつ — 今日は何が一番役立ちそうですか？落ち着くこと、グラウンディング、それとも確認するだけ？",
    ),
  )

  private val ScriptedDone = Map(
    "en" -> "Thanks for sharing. Let me check what might work well for you today.",
    "ja" -> "ありがとう、話してくれて。今日の練習を確認しますね。",
  )

  def reflectionTurn(
    messages: List[ConversationMessage],
    locale: String,
    hasHistory: Boolean,
    memory: Option[UserMemory],
    llm: LLMClient,
  ): IO[ReflectionTurnResult] =
    // Crisis check on latest user message
    val crisisCheck = messages.lastOption match
      case Some(m) if m.role == ConversationRole.User && Rules.detectCrisis(m.content) =>
        Some(crisisResult(locale, countUserTurns(messages)))
      case _ => Option.empty

    crisisCheck match
      case Some(result) => IO.pure(result)
      case _ =>
        if !llm.isConfigured then IO.pure(scriptedTurn(messages, locale))
        else llmTurn(messages, locale, hasHistory, memory, llm)

  private def llmTurn(
    messages: List[ConversationMessage],
    locale: String,
    hasHistory: Boolean,
    memory: Option[UserMemory],
    llm: LLMClient,
  ): IO[ReflectionTurnResult] =
    val systemPrompt = ReflectionPrompts.buildReflectionSystemPrompt(locale, hasHistory, memory)
    val chatHistory = messages.map { m =>
      val role = if m.role == ConversationRole.Agent then "assistant" else "user"
      ChatMessage(role, m.content)
    }

    llm.completeChat(systemPrompt, chatHistory, 200).attempt.flatMap {
      case Left(_) => IO.pure(scriptedTurn(messages, locale))
      case Right(raw) =>
        val userTurnCount = countUserTurns(messages)

        if raw.contains("[CRISIS_DETECTED]") then
          IO.pure(crisisResult(locale, userTurnCount))
        else
          val cleanMessage = raw.replace("[REFLECTION_COMPLETE]", "").replace("[CRISIS_DETECTED]", "").trim
          val isDone = raw.contains("[REFLECTION_COMPLETE]") || userTurnCount >= 3

          if isDone then
            val fullConversation = messages :+ ConversationMessage(ConversationRole.Agent, cleanMessage)
            extractProfile(fullConversation, locale, llm).map { profile =>
              ReflectionTurnResult(cleanMessage, userTurnCount, done = true, crisis = false, Some(profile))
            }
          else
            IO.pure(ReflectionTurnResult(cleanMessage, userTurnCount, done = false, crisis = false))
    }

  private def extractProfile(
    messages: List[ConversationMessage],
    locale: String,
    llm: LLMClient,
  ): IO[ReflectionProfile] =
    val prompt = ReflectionPrompts.buildExtractionPrompt(messages, locale)
    llm.complete("You extract structured data from conversations. Return only valid JSON.", prompt, 400)
      .attempt.map {
        case Left(_) => heuristicProfile(messages, locale)
        case Right(raw) =>
          val jsonStr = """\{[\s\S]*\}""".r.findFirstIn(raw)
          jsonStr.flatMap(s => parse(s).toOption).flatMap(_.as[ReflectionProfile].toOption)
            .getOrElse(heuristicProfile(messages, locale))
      }

  def scriptedTurn(messages: List[ConversationMessage], locale: String): ReflectionTurnResult =
    val questions = Scripted.getOrElse(locale, Scripted("en"))
    val userTurnCount = countUserTurns(messages)

    if userTurnCount < questions.length then
      ReflectionTurnResult(questions(userTurnCount), userTurnCount, done = false, crisis = false)
    else
      val profile = heuristicProfile(messages, locale)
      val closingMsg = ScriptedDone.getOrElse(locale, ScriptedDone("en"))
      ReflectionTurnResult(closingMsg, userTurnCount, done = true, crisis = false, Some(profile))

  def heuristicProfile(messages: List[ConversationMessage], locale: String): ReflectionProfile =
    val userText = messages.filter(_.role == ConversationRole.User).map(_.content.toLowerCase).mkString(" ")

    val highTension = List("stress", "anxious", "anxiety", "worried", "nervous", "overwhelm",
      "exhaust", "tired", "heavy", "ストレス", "不安", "疲れ", "心配", "しんどい", "きつい")
    val lowTension = List("good", "fine", "okay", "calm", "peaceful", "comfortable",
      "元気", "大丈夫", "落ち着き", "いい", "のんびり")
    val selfCriticalWords = List("fail", "bad", "wrong", "should have", "must", "ダメ", "できない", "だめ", "失敗")

    val tensionHits = highTension.count(w => userText.contains(w))
    val calmHits = lowTension.count(w => userText.contains(w))
    val criticalHits = selfCriticalWords.count(w => userText.contains(w))

    val tension: Int =
      if tensionHits >= 3 then 5
      else if tensionHits >= 1 then 4
      else if calmHits >= 1 then 2
      else 3

    val mood = Math.max(1, Math.min(5, 6 - tension))

    val lastUser = messages.filter(_.role == ConversationRole.User).lastOption.map(_.content.toLowerCase).getOrElse("")
    val intent =
      if "calm|breath|relax|落ち着|呼吸".r.findFirstIn(lastUser).isDefined then SessionIntent.Calming
      else if "ground|body|earth|体|グラウンド".r.findFirstIn(lastUser).isDefined then SessionIntent.Grounding
      else SessionIntent.Checkin

    val emotionalTone =
      if tension >= 4 then EmotionalTone.Distressed
      else if mood >= 4 then EmotionalTone.Positive
      else EmotionalTone.Neutral

    ReflectionProfile(
      mood = mood,
      tension = tension,
      selfCritical = criticalHits > 0,
      intent = intent,
      freeText = messages.filter(_.role == ConversationRole.User).map(_.content).mkString(" ").take(300),
      emotionalTone = emotionalTone,
    )

  def checkinToProfile(data: CheckinData): ReflectionProfile =
    val tone =
      if data.tension >= 4 then EmotionalTone.Distressed
      else if data.mood >= 4 then EmotionalTone.Positive
      else EmotionalTone.Neutral
    ReflectionProfile(
      mood = data.mood,
      tension = data.tension,
      selfCritical = data.selfCritical,
      intent = data.intent,
      lastSessionOutcome = data.lastSessionOutcome,
      freeText = data.freeText.getOrElse(""),
      emotionalTone = tone,
    )

  private def countUserTurns(messages: List[ConversationMessage]): Int =
    messages.count(_.role == ConversationRole.User)

  private def crisisResult(locale: String, userTurnCount: Int): ReflectionTurnResult =
    val msg = if locale == "ja" then
      "このアプリは今必要なサポートを提供できません。信頼できる人や危機相談窓口に連絡してください。"
    else
      "This app isn't the right support for what you're going through. Please reach out to someone you trust or a crisis line."
    ReflectionTurnResult(msg, userTurnCount, done = false, crisis = true)
