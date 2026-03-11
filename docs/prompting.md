# Prompting Guide

## Supervisor Prompt

### Goal

Extract a structured `SupervisorDecision` from unstructured check-in data. The LLM should act as a clinical-style observer, not a supportive friend.

### Key Design Choices

**1. No empathy framing in the system prompt.**

The supervisor is not asked to "be compassionate" or "understand the user's feelings." This would bias the model toward reassurance over accuracy. A supervisor who says "that sounds hard, but try anyway" has failed at their job.

**2. Explicit prohibited outputs.**

The prompt lists things the supervisor must NOT do:
- Encourage pushing through discomfort
- Reframe harmful patterns as growth opportunities
- Add pressure to the user's existing pressure
- Make clinical diagnoses

**3. Structured JSON output.**

The supervisor always returns:
```json
{
  "riskLevel": "moderate",
  "patterns": ["perfectionism", "should_language"],
  "action": "soften",
  "recommendedMode": "sound",
  "message": "...",
  "guidanceDuration": 60
}
```

The message field is user-facing. It should be 1-2 sentences, neutral, not alarming.

**4. Pattern detection augments, never replaces, rules.**

The rule-based detector runs first. The LLM can add patterns or escalate risk, but cannot downgrade a crisis or high-risk determination made by rules.

### Tone Constraints for `message`

- 1-2 sentences maximum
- No "I can see that..." framing
- No therapeutic language ("sit with", "honor", "hold space")
- No "just" minimizers ("just breathe", "just notice")
- State what will happen, not what the user should feel

Good: "The session will use a shorter sound-based practice."
Bad: "It sounds like you're being hard on yourself. Let's try to just relax for a moment."

---

## Guidance Generation Prompt

### Goal

Generate a short mindfulness script that achieves sensory redirection without adding psychological pressure.

### Style Rules (enforced by `checkStyleViolations`)

**Forbidden words/phrases:**
- "just" (minimizer)
- "relax", "let go" (forced release)
- "accept", "surrender" (forced acceptance)
- "should", "must", "try to" (imperative/pressure)
- "deeper", "further" (performance framing)
- "good", "well done", "right" (evaluation)
- Spiritual language: "energy", "being", "presence", "flow", "center"
- "you might notice" repeated more than once (overuse)

**Required qualities:**
- Sensory and specific: "the weight of your hands on your legs" not "feel your body"
- Permissive not directive: "you could..." not "now you will..."
- Brief pauses indicated with line breaks, not explicit "pause here" instructions
- No duration-counting (counting to 4, counting breaths)

### Duration-Specific Guidelines

**30 seconds:** One anchor only. Single sensation. No transitions.
> "The weight of your hands. That's it."

**60 seconds:** One anchor with gentle elaboration. One transition maximum.
> "Notice the sound in the room furthest from you. Not what it is — just that it's there. After a moment, see if there's another."

**3 minutes:** Two anchors with natural transition. Can include brief external-to-internal or internal-to-external move.

### Mode-Specific Guidelines

| Mode | Anchor | Avoid |
|---|---|---|
| `breath` | Breath sensation | Counting, rhythm instructions |
| `sound` | Environmental sounds | Identifying/labeling sounds |
| `body` | Physical sensations | Internal organs, heartbeat |
| `external` | Visual field, surfaces, temperature | Any inward instruction |
| `reset` | Self-compassion framing | Practice evaluation |
| `abort` | Complete stop | Nothing — preset only |

### Fallback to Presets

If style violations remain after auto-correction, the system falls back to a preset script. This is the intended behavior — preset scripts have been manually vetted and always meet the style requirements.

---

## Summary Generation Prompt

The summary is generated after the session is complete (post-outcome submitted). It receives:
- Pre-session check-in data
- Supervisor decision + patterns
- Post-session outcome

Output: 1-2 sentences, past tense, no evaluation.

Good: "Session used a 60-second sound practice following moderate tension and some self-critical framing. Ended with reduced pressure."
Bad: "Great session! You did really well managing your perfectionism today."

The summary is shown in session history. It should help the user remember what happened, not judge it.
