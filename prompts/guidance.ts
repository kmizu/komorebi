import type { GuidanceMode, GuidanceDuration, RiskLevel } from '@/lib/types';

const MODE_DESCRIPTIONS: Record<GuidanceMode, string> = {
  breath: 'gentle breath awareness — notice without trying to change anything',
  sound: 'sounds in the environment — whatever is already there, no special searching',
  body: 'weight and pressure of body against surfaces — chair, floor, hands in lap',
  external: 'something visible in the room — a surface, shape, texture, or object',
  reset: 'ultra-short reset — simple noticing, no goals',
  abort: 'gentle stopping script — brief, non-alarming, easy way to end',
};

const APPROXIMATE_WORDS: Record<GuidanceDuration, number> = {
  30: 45,
  60: 90,
  180: 270,
};

export function buildGuidancePrompt(
  mode: GuidanceMode,
  duration: GuidanceDuration,
  riskLevel: RiskLevel,
  supervisorMessage: string
): string {
  const words = APPROXIMATE_WORDS[duration];

  return `Write a ${duration}-second mindfulness guidance script.

Mode: ${mode} — ${MODE_DESCRIPTIONS[mode]}
Duration: ${duration} seconds (~${words} words when read slowly)
Risk level: ${riskLevel}
Supervisor note: "${supervisorMessage}"

Style rules (strictly enforced):
- Write exactly as if speaking aloud to a calm adult
- Use plain, everyday language — no spiritual vocabulary
- Avoid: "just", "simply", "relax", "let go", "surrender", "accept", "chakra", "energy", "healing", "universe", "sacred", "cosmic"
- Avoid: "you should", "you must", "try to", "you'll feel better", "this will help"
- Avoid: long paragraphs — use short sentences
- Do not promise any outcome or benefit
- Do not tell the user how they should feel
- One concrete instruction at a time
- End naturally — no abrupt cut-off

Return ONLY the script text. No labels, no explanation, no quotes around it.`;
}

export function buildSummaryPrompt(
  checkinFreeText: string | undefined,
  postFeltBetter: boolean,
  postWouldContinue: boolean,
  postNotes: string | undefined
): string {
  return `Write a 1-sentence session summary for a mindfulness log. Factual and brief (max 15 words).

User pre-session notes: "${checkinFreeText ?? 'none'}"
Felt better after: ${postFeltBetter}
Would practice again today: ${postWouldContinue}
User post notes: "${postNotes ?? 'none'}"

Return only the summary sentence. No quotes, no label, no explanation.`;
}
