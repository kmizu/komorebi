package komorebi.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import komorebi.domain.UserMemory
import io.circe.syntax.*
import io.circe.parser.decode

class UserMemoryRepository(xa: Transactor[IO]):

  def get(userId: String = "default"): IO[Option[UserMemory]] =
    sql"""SELECT memory FROM user_memory WHERE user_id = $userId LIMIT 1"""
      .query[String].option.transact(xa).map(_.flatMap(s => decode[UserMemory](s).toOption))

  def save(memory: UserMemory, userId: String = "default"): IO[Unit] =
    val json = memory.asJson.noSpaces
    val now = java.time.Instant.now().toString
    sql"""INSERT OR REPLACE INTO user_memory (user_id, memory, updated_at)
          VALUES ($userId, $json, $now)""".update.run.transact(xa).void
