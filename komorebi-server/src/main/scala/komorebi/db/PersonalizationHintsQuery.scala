package komorebi.db

import komorebi.domain.*

object PersonalizationHintsQuery:

  /** Pure function: aggregate hints from recent sessions. */
  def aggregateHints(sessions: List[SessionRecord]): PersonalizationHints =
    if sessions.isEmpty then return PersonalizationHints.empty

    // Aggregate patterns appearing >= 2 times
    val allPatterns = sessions.flatMap(_.supervisorDecision.patterns)
    val patternCounts = allPatterns.groupBy(identity).view.mapValues(_.size).toMap
    val recentPatterns = patternCounts.filter(_._2 >= 2).keys.toList

    // Find preferred/avoid modes from outcomes
    val withPost = sessions.filter(_.postOutcome.isDefined)
    val goodSessions = withPost.filter(_.postOutcome.exists(_.feltBetter))
    val badSessions = withPost.filter(_.postOutcome.exists(!_.feltBetter))

    def topMode(list: List[SessionRecord]): Option[GuidanceMode] =
      if list.isEmpty then Option.empty
      else
        val counts = list.groupBy(_.guidance.mode).view.mapValues(_.size).toMap
        Some(counts.maxBy(_._2)._1)

    val preferredMode = topMode(goodSessions)
    val avoidMode = topMode(badSessions)

    val avgTension = sessions.map(_.checkin.tension.toDouble).sum / sessions.size
    val lastRiskLevel = sessions.headOption.map(_.supervisorDecision.riskLevel)

    // Build notes
    val notes = scala.collection.mutable.ListBuffer.empty[String]
    preferredMode.foreach(m => notes += s"${GuidanceMode.toJson(m)} anchor worked recently")
    avoidMode.filter(a => !preferredMode.contains(a)).foreach(m =>
      notes += s"avoid ${GuidanceMode.toJson(m)} — correlated with difficulty"
    )
    if recentPatterns.contains(HarmfulPattern.Perfectionism) then
      notes += "perfectionism pattern noticed recently"
    if recentPatterns.contains(HarmfulPattern.BreathTension) then
      notes += "breath focus has increased tension recently"
    if avgTension >= 4 then
      notes += "recent sessions have been high-tension — keep short"

    PersonalizationHints(
      recentPatterns = recentPatterns,
      preferredMode = preferredMode,
      avoidMode = avoidMode,
      avgTension = avgTension,
      sessionCount = sessions.size,
      lastRiskLevel = lastRiskLevel,
      notes = notes.toList,
    )
