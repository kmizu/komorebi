# komorebi / 木漏れ日

[日本語](README-ja.md)

A personalized mindfulness companion that builds a long-term picture of how practice works for you — and notices when it might be making things harder.

Inspired by the 3-agent architecture described in [MindfulAgents: A Multi-Agent Framework for Mindfulness Practice](https://arxiv.org/abs/2603.06926) — implemented as a single local app with a Reflection Agent, Personalization Agent, and Expert Alignment Agent.

## What it does

Before each session, a counselor-style conversational agent listens to how you are. Your responses are used to detect when practice is likely to add pressure rather than ease it — self-judgment, internal surveillance, forced acceptance, compulsive continuation, performance framing — and to personalize the session accordingly. Over time, it learns what helps you and what doesn't.

1. **Conversation** — a 3-turn dialogue before the session (Reflection Agent)
2. **For today** — recommends mode and duration based on your state and history (Personalization Agent + long-term memory)
3. **Practice** — short guided session (30s / 1min / 3min), personalized to your state (Expert Alignment Agent)
   - "This is making it harder" button always visible
4. **After** — did this ease the pressure, or add to it?
5. **History** — past sessions; memory updates in the background after each one

## Setup

```bash
./start.sh    # Starts backend (:8080) + frontend (:3000)
./stop.sh     # Stops both
```

Open http://localhost:3000 (or /en, /ja for localized).

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `OPENAI_API_KEY` | No | LLM-based agents (gpt-5.4). Without this, rule-based detection + preset scripts. |
| `ELEVENLABS_API_KEY` | No | Voice playback. Without this, guidance is text-only. |
| `ELEVENLABS_VOICE_ID` | No | Voice ID. Defaults to a calm English voice. |
| `KOMOREBI_PORT` | No | Scala server port. Default: 8080. |
| `BACKEND_PORT` | No | Port the Next.js proxy targets. Default: 8080. |

The app works fully without API keys. Pattern detection uses keyword rules, guidance uses preset scripts.

## Architecture

**Frontend:** Next.js (React, Tailwind CSS, next-intl for i18n)

**Backend:** Scala 3 (Tapir + http4s + Doobie + SQLite + sttp + circe)

Three agents run in sequence each session:

- **Reflection Agent** — 3-turn conversational check-in, extracts a `ReflectionProfile`
- **Personalization Agent** — builds a `SessionPlan` using the profile + long-term `UserMemory` + rule-based pattern detection (6 dimensions from paper §3.3)
- **Expert Alignment Agent** — generates personalized guidance text from the plan

A 3-layer safety model (Crisis → Distress → Subtle) evaluates risk using rule-based keyword detection first; LLM enrichment can add patterns but never downgrade risk. Long-term memory (counselor-style case notes) accumulates across sessions and shapes future recommendations.

## Data

Sessions and user memory stored locally in `data/mindfulness.db` (SQLite via Doobie + xerial JDBC).
TTS audio cached in `data/tts-cache/`. Neither is committed to git.
