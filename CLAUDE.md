# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Komorebi (木漏れ日) is a personalized mindfulness companion that detects harmful mental patterns and adapts guidance to prevent practice from becoming counterproductive. Inspired by the MindfulAgents 3-agent framework. Supports English and Japanese.

## Commands

```bash
npm run dev              # Start Next.js dev server on :3000
npm run build            # Production build
npm run lint             # ESLint
npm test                 # Run all tests (vitest)
npm run test:watch       # Watch mode
npm run test:coverage    # Coverage report (target: 80%+)

# Run a single test file
npx vitest run lib/supervisor/rules.test.ts

# Quick start (installs deps, copies .env.example if needed)
./start.sh
```

## Architecture

```
Reflection → Personalization → Alignment → Post-reflection → Memory
(3-turn chat)  (plan from history)  (guidance gen)  (outcome capture)  (background)
```

### Core Pipeline

- **Reflection Agent** (`lib/agents/reflection.ts`) — 3-turn conversational check-in, extracts `ReflectionProfile`
- **Personalization Agent** (`lib/agents/personalization.ts`) — Builds `SessionPlan` from profile + last 10 sessions + pattern history
- **Alignment Agent** (`lib/agents/alignment.ts`) — Generates guidance, validates style constraints
- **Memory Agent** (`lib/agents/memory.ts`) — Updates long-term user memory (case notes) post-session

### Supervisor (`lib/supervisor/`)

Orchestrates safety evaluation: crisis check → rule-based pattern detection → LLM enrichment → 3-layer safety model.

- **Rules run first, LLM augments but never downgrades risk.** Crisis detection is synchronous with no LLM call.
- **3-layer safety:** Crisis (stop everything) → Distress (external grounding) → Subtle (modify guidance quietly)
- 10 harmful patterns detected: perfectionism, forced_acceptance, overmonitoring, performance_framing, should_language, compulsive_continuation, breath_tension, self_scoring, rumination, escalating_frustration

### Guidance (`lib/guidance/`)

- `presets.ts` — Hardcoded scripts for all modes/durations. `abort` and `reset` always use presets (no LLM latency in critical moments).
- `generator.ts` — LLM generation with style violation detection. Falls back to preset if >2 violations remain after auto-correction.
- **Forbidden in guidance:** spiritual language, "just", "should", "relax", "let go", evaluation words. See `docs/prompting.md`.

### Database (`lib/db/`)

Drizzle ORM + LibSQL (embedded SQLite, no native modules). Flat schema — single `sessions` table with JSON fields, `userMemory` table for case notes. No joins.

## Key Design Decisions

- **Graceful degradation:** Fully functional without API keys (rule-based detection + preset scripts)
- **LLM models:** OpenAI gpt-5.4 (main) / gpt-5.4-mini (chat) via `lib/llm/client.ts`
- **All domain types are `readonly`** — defined in `lib/types.ts` and `lib/agents/types.ts`
- **i18n:** next-intl with `[locale]` routing, translations in `messages/en.json` and `messages/ja.json`
- **Bilingual keyword detection:** Pattern rules include both English and Japanese keywords

## Environment Variables

Only `OPENAI_API_KEY` enables LLM features. `ELEVENLABS_API_KEY` + `ELEVENLABS_VOICE_ID` enable TTS. All optional — app works without them.

## Test Structure

Tests live alongside source: `lib/supervisor/rules.test.ts`, `lib/supervisor/safety.test.ts`. Coverage excludes `lib/db/`, `lib/llm/client.ts`, `lib/tts/client.ts` (external I/O). Vitest with v8 coverage provider.

## Prompt Templates

All LLM prompts in `prompts/` directory (TS) and `komorebi-server/src/main/scala/komorebi/prompts/` (Scala). The supervisor prompt intentionally avoids empathy framing — it acts as a clinical observer, not a supportive friend. See `docs/prompting.md` for full style guide.

## Scala 3 Server (`komorebi-server/`)

Full server-side rewrite in Scala 3. The Next.js frontend proxies API calls to this backend when `SCALA_BACKEND` env var is set.

### Commands

```bash
cd komorebi-server
sbt compile              # Compile
sbt test                 # Run all tests (munit)
sbt "testOnly *RulesSpec"  # Run a single test class
sbt coverage test coverageReport  # Coverage report (target: 80%+)
sbt run                  # Start server on :8080

# Enable Next.js proxy to Scala backend
SCALA_BACKEND=http://localhost:8080 npm run dev
```

### Stack

- **Scala 3.3.4** + **sbt 1.10.6**
- **Tapir** + **http4s ember** — API endpoints
- **Doobie** + **SQLite (xerial)** — Database
- **sttp** + **cats-effect 3** — HTTP clients (OpenAI, ElevenLabs)
- **circe** — JSON serialization
- **munit** — Testing

### Module Structure

```
komorebi-server/src/main/scala/komorebi/
├── domain/     # Types.scala (enums), Models.scala (case classes), TapirSchemas.scala
├── supervisor/ # Rules.scala, Safety.scala, Engine.scala — pure safety core
├── guidance/   # Presets.scala, GuidanceGenerator.scala
├── agents/     # ReflectionAgent, PersonalizationAgent, AlignmentAgent, MemoryAgent
├── prompts/    # All LLM prompt builders (pure String functions)
├── db/         # Schema, SessionRepository, UserMemoryRepository, PersonalizationHintsQuery
├── clients/    # LLMClient, TTSClient, STTClient (trait + impl + NoOp)
├── routes/     # AllRoutes.scala — all Tapir endpoint definitions
├── config/     # AppConfig.scala — env var based config
└── Main.scala  # http4s server bootstrap
```

### Key Patterns

- **All domain types are immutable case classes and Scala 3 enums** with circe codecs
- **Pure functions first:** Rules, Safety, Presets, PersonalizationHintsQuery are all pure — no IO, no side effects, fully unit-testable
- **Trait-based clients:** `LLMClient`, `TTSClient`, `STTClient` are traits with real + NoOp implementations for graceful degradation
- **Env vars:** `KOMOREBI_PORT` (default 8080), `OPENAI_API_KEY`, `ELEVENLABS_API_KEY`, `KOMOREBI_DB_PATH`
