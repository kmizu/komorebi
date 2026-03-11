# Safety Model

## Overview

The safety model is the core differentiator of this app. Most mindfulness apps treat "more practice = better". This app treats mindfulness as a tool that can cause harm when applied in the wrong context.

The model operates in three layers, evaluated in order. A higher layer always overrides a lower one.

---

## Layer 3: Crisis (Stop Everything)

**Trigger:** Any crisis keyword detected in pre-session or mid-session text.

**Action:** `crisis` — terminate session immediately, show crisis resources.

**Crisis keywords include:**
- Self-harm language ("hurt myself", "end it", "don't want to be here")
- Suicidal ideation ("want to die", "kill myself")
- Severe hopelessness ("no point", "nothing will help")

**Design principles:**
- This runs BEFORE any LLM call. No latency, no ambiguity.
- The app does not attempt to provide emotional support — it redirects to professionals.
- Crisis banner shows country-appropriate hotline numbers.
- No session is saved when crisis is detected (nothing to "review later").

---

## Layer 2: Active Distress

**Trigger:** Risk level `high` OR pattern `compulsive_continuation` detected.

**Action:** `stop` — end inward practice, offer external grounding.

**Recommended mode:** `external` or `reset`

**Rationale:** When someone is already in distress, any instruction to observe internal states can amplify the distress. "Notice your anxiety" is not neutral when anxiety is already overwhelming. The safest intervention is to redirect attention outward: sounds in the room, physical contact with surfaces, temperature.

**Patterns that escalate to Layer 2:**
- `compulsive_continuation` — "it's not working but I need to push through"
- `escalating_frustration` — frustration that intensifies during practice
- Combined moderate patterns that suggest active overwhelm

---

## Layer 1: Subtle Harmful Patterns

**Trigger:** Risk level `moderate` OR specific problematic patterns detected.

**Action:** Modify guidance without stopping session.

**Pattern-specific responses:**

| Pattern | Response |
|---|---|
| `perfectionism` | Shorten duration, soften instruction, remove evaluation language |
| `self_scoring` | Switch anchor (away from breath → external sound), shorten |
| `should_language` | Remove imperative framing, increase permission language |
| `forced_acceptance` | Stop "acceptance" framing entirely, use neutral observation |
| `overmonitoring` | Switch from internal to external anchor |
| `performance_framing` | Reset — stop, short grounding, reframe purpose |
| `breath_tension` | Mandatory mode switch away from breath anchor |

---

## Risk Level Assessment

```
none     → No patterns detected, tension 0-3
low      → 1 minor pattern, tension 3-5
moderate → 2+ patterns, OR tension ≥6, OR 1 high-salience pattern
high     → crisis-adjacent patterns, OR tension ≥8, OR compulsive continuation
crisis   → crisis keywords present
```

Risk level is derived from both rule-based detection and LLM assessment (when configured). The higher of the two always wins.

---

## Why Not Just "Stop When Things Are Bad"

The subtlety of Layer 1 is intentional. Many harmful patterns are invisible to the person experiencing them:

- Someone with high perfectionism doesn't experience their practice as perfectionist — they experience it as "trying hard."
- Someone with forced acceptance doesn't feel they're forcing — they feel they're "doing it right."
- Someone overmonitoring doesn't feel they're overmonitoring — they feel "deeply present."

A blanket "stop" would be both unhelpful and confusing. Instead, the guidance is modified to quietly remove the harmful element without calling it out.

The only exception is Layer 2+ where stopping IS the safe choice, and even then, the message is framed neutrally: "enough for now" not "you were practicing wrong."

---

## Personalization Impact on Safety

Prior session history modifies safety thresholds:

- **Repeated harmful patterns**: If the same pattern appears across 3+ sessions, it's flagged more aggressively on the next session even if text signals are weak.
- **Prior `addedPressure` outcomes**: If recent sessions consistently ended with "added to pressure", the supervisor starts at a higher risk baseline.
- **Mode avoidance**: If breath practice consistently preceded negative outcomes, `breath` mode is avoided even for "normal" sessions.

This is the "personalized" in personalized mindfulness safety.
