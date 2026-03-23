package komorebi.agents

import cats.effect.IO
import komorebi.domain.*
import komorebi.clients.LLMClient
import komorebi.prompts.MemoryPrompts
import io.circe.parser.parse

object MemoryAgent:

  def updateUserMemory(
    current: Option[UserMemory],
    profile: ReflectionProfile,
    plan: SessionPlan,
    outcome: PostOutcome,
    locale: String,
    llm: LLMClient,
  ): IO[UserMemory] =
    val base = current.getOrElse(UserMemory.empty)

    if !llm.isConfigured then
      IO.pure(rulesBasedUpdate(base, profile, plan, outcome))
    else
      val prompt = MemoryPrompts.buildMemoryUpdatePrompt(current, profile, plan, outcome, locale)
      llm.complete("You maintain a counselor's case notes. Be precise and conservative.", prompt, 600)
        .attempt.map {
          case Left(_) => rulesBasedUpdate(base, profile, plan, outcome)
          case Right(raw) =>
            val jsonStr = """\{[\s\S]*\}""".r.findFirstIn(raw)
            jsonStr.flatMap(s => parse(s).toOption).map { json =>
              val c = json.hcursor
              UserMemory(
                version = base.version + 1,
                lastUpdated = java.time.Instant.now().toString,
                sessionCount = base.sessionCount + 1,
                corePatterns = dedup(c.downField("corePatterns").as[List[String]].getOrElse(base.corePatterns)),
                whatHelps = dedup(c.downField("whatHelps").as[List[String]].getOrElse(base.whatHelps)),
                whatHurts = dedup(c.downField("whatHurts").as[List[String]].getOrElse(base.whatHurts)),
                lifeContext = dedup(c.downField("lifeContext").as[List[String]].getOrElse(base.lifeContext)),
                languageNotes = dedup(c.downField("languageNotes").as[List[String]].getOrElse(base.languageNotes)),
                trajectory = c.downField("trajectory").as[String].getOrElse(base.trajectory),
              )
            }.getOrElse(rulesBasedUpdate(base, profile, plan, outcome))
        }

  def rulesBasedUpdate(
    base: UserMemory,
    profile: ReflectionProfile,
    plan: SessionPlan,
    outcome: PostOutcome,
  ): UserMemory =
    val whatHelps = if outcome.feltBetter then
      dedup(base.whatHelps :+ GuidanceMode.toJson(plan.recommendedMode))
    else base.whatHelps

    val whatHurts = if !outcome.feltBetter && plan.recommendedMode == GuidanceMode.Breath then
      dedup(base.whatHurts :+ "breath focus")
    else base.whatHurts

    val corePatterns = if plan.patterns.nonEmpty then
      dedup(base.corePatterns ++ plan.patterns.map(p => HarmfulPattern.toJson(p).replace('_', ' ')))
    else base.corePatterns

    val lifeContext = if profile.themes.nonEmpty then
      dedup(base.lifeContext ++ profile.themes)
    else base.lifeContext

    val count = base.sessionCount + 1

    UserMemory(
      version = base.version + 1,
      lastUpdated = java.time.Instant.now().toString,
      sessionCount = count,
      corePatterns = corePatterns.take(5),
      whatHelps = whatHelps.take(5),
      whatHurts = whatHurts.take(5),
      lifeContext = lifeContext.take(5),
      languageNotes = base.languageNotes,
      trajectory = if count <= 3 then "early sessions" else if base.trajectory.isEmpty then "ongoing practice" else base.trajectory,
    )

  private def dedup(list: List[String]): List[String] = list.distinct
