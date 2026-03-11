# Architecture

## Overview

mindfulness-supervisor is a Next.js 14 app (App Router) with four logical modules:

```
Reflection  →  Supervisor  →  Guidance  →  Memory
(check-in)     (evaluation)   (scripts)    (SQLite)
```

## Modules

### 1. Reflection (check-in)
- `components/CheckinForm.tsx` — collects mood, tension, self-critical flag, intent, last outcome, free text
- `app/api/checkin/route.ts` — validates and dispatches to supervisor engine

### 2. Supervisor (core)
- `lib/supervisor/rules.ts` — keyword detection for 10 harmful patterns + crisis check
- `lib/supervisor/engine.ts` — orchestrates: crisis → rules → LLM (if available) → safety layers
- `lib/supervisor/safety.ts` — 3-layer safety model (pure function)
- `lib/llm/client.ts` — Anthropic SDK wrapper
- `prompts/supervisor.ts` — LLM prompts for supervisor

Rule-based detection runs first. Crisis detection is synchronous and does NOT involve the LLM.
LLM enrichment is applied only when: free text is substantial, risk signals are elevated, or user is self-critical.

### 3. Guidance
- `lib/guidance/presets.ts` — hardcoded scripts for all modes and durations (abort/reset always use these)
- `lib/guidance/generator.ts` — LLM generation with style validation and preset fallback
- `prompts/guidance.ts` — guidance and summary LLM prompts

Style violations (spiritual language, "you should", "just", etc.) are detected post-generation and fixed or replaced with presets.

### 4. Memory
- `lib/db/schema.ts` — Drizzle ORM schema (flat sessions table)
- `lib/db/connection.ts` — LibSQL singleton with inline schema init
- `lib/db/queries.ts` — repository-pattern functions

Personalization is derived from the last 10 sessions: which anchors correlated with relief, which with pressure, recent patterns, average tension.

## Session state machine

The session page uses `useReducer` with typed states:

```
checkin → review → session → post → done
```

Mid-session escalation (`reportWorse`) stays in the `session` step but replaces guidance + decision.

## Tech decisions

- **LibSQL** instead of better-sqlite3: avoids native module compilation, works with Node 21
- **Graceful degradation**: app is fully functional without API keys
- **Preset scripts for abort/reset**: no LLM latency in critical moments
- **Flat DB schema**: no joins needed for single-user local MVP
