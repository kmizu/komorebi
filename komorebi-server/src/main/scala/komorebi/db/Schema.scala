package komorebi.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO

object Schema:

  val createSessionsTable: Update0 = sql"""
    CREATE TABLE IF NOT EXISTS sessions (
      id TEXT PRIMARY KEY,
      created_at TEXT NOT NULL,
      checkin_mood INTEGER NOT NULL,
      checkin_tension INTEGER NOT NULL,
      checkin_self_critical INTEGER NOT NULL,
      checkin_intent TEXT NOT NULL,
      checkin_last_outcome TEXT,
      checkin_free_text TEXT,
      risk_level TEXT NOT NULL,
      patterns TEXT NOT NULL,
      action TEXT NOT NULL,
      recommended_mode TEXT NOT NULL,
      supervisor_message TEXT NOT NULL,
      guidance_duration INTEGER NOT NULL,
      guidance_mode TEXT NOT NULL,
      guidance_text TEXT NOT NULL,
      guidance_is_preset INTEGER NOT NULL,
      post_felt_better INTEGER,
      post_would_continue INTEGER,
      post_notes TEXT,
      summary TEXT,
      reflection_profile TEXT,
      reflection_summary TEXT
    )
  """.update

  val createUserMemoryTable: Update0 = sql"""
    CREATE TABLE IF NOT EXISTS user_memory (
      user_id TEXT PRIMARY KEY,
      memory TEXT NOT NULL,
      updated_at TEXT NOT NULL
    )
  """.update

  def initialize(xa: Transactor[IO]): IO[Unit] =
    val program = for
      _ <- createSessionsTable.run
      _ <- createUserMemoryTable.run
    yield ()
    program.transact(xa)
