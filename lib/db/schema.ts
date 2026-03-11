import { sqliteTable, text, integer } from 'drizzle-orm/sqlite-core';

export const sessions = sqliteTable('sessions', {
  id: text('id').primaryKey(),
  createdAt: text('created_at').notNull(),

  // Checkin data (stored flat for easy querying)
  checkinMood: integer('checkin_mood').notNull(),
  checkinTension: integer('checkin_tension').notNull(),
  checkinSelfCritical: integer('checkin_self_critical', { mode: 'boolean' }).notNull(),
  checkinIntent: text('checkin_intent').notNull(),
  checkinLastOutcome: text('checkin_last_outcome'),
  checkinFreeText: text('checkin_free_text'),

  // Supervisor decision (JSON fields for arrays)
  riskLevel: text('risk_level').notNull(),
  patterns: text('patterns').notNull(),         // JSON: HarmfulPattern[]
  action: text('action').notNull(),
  recommendedMode: text('recommended_mode').notNull(),
  supervisorMessage: text('supervisor_message').notNull(),
  guidanceDuration: integer('guidance_duration').notNull(),

  // Guidance used
  guidanceMode: text('guidance_mode').notNull(),
  guidanceText: text('guidance_text').notNull(),
  guidanceIsPreset: integer('guidance_is_preset', { mode: 'boolean' }).notNull(),

  // Post-session (nullable, updated via PATCH)
  postFeltBetter: integer('post_felt_better', { mode: 'boolean' }),
  postWouldContinue: integer('post_would_continue', { mode: 'boolean' }),
  postNotes: text('post_notes'),

  // Summary (generated after post-reflection)
  summary: text('summary'),
});
