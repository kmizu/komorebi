import type { HarmfulPattern, RiskLevel } from '@/lib/types';

// ── Keyword dictionaries ─────────────────────────────────────────────────────

const PATTERN_KEYWORDS: Record<HarmfulPattern, readonly string[]> = {
  perfectionism: [
    'correctly', 'properly', 'right way', 'the right way', 'wrong way',
    'failing', 'failed at', 'messed up', 'mess up', 'doing it wrong',
    'not doing it right', 'should be better', 'not good enough', 'good enough',
    'need to get this right', 'getting it right', 'doing this right',
    'i failed', 'can\'t do this right', 'not good at this',
    // Japanese
    'ちゃんと', 'うまくできない', '正しく', '失敗', 'できてない',
  ],

  forced_acceptance: [
    'i should accept', 'i know i should accept', 'trying to accept', 'have to accept',
    'must accept', 'supposed to accept', 'need to accept', 'force myself',
    'forcing myself', 'just accept', 'i should just accept', 'have to let go',
    'supposed to let go', 'just let go', 'i must let go',
    // Japanese
    '受け入れなければ', '受け入れるべき', '手放さなければ',
  ],

  overmonitoring: [
    'keep checking', 'keep monitoring', 'constantly checking', 'checking whether',
    'checking if i', 'monitoring myself', 'watching myself', 'observing myself observing',
    'meta', 'am i doing this right', 'tracking my', 'tracking whether',
    'checking my breath', 'checking my thoughts', 'watching my thoughts',
    'am i calm', 'checking if i\'m calm', 'see if it\'s working',
    // Japanese
    '自分を観察', '確認し続ける', 'チェックしている',
  ],

  performance_framing: [
    'productive', 'productivity', 'optimize', 'optimization', 'performance',
    'performing', 'results', 'getting results', 'efficient', 'efficiency',
    'track my progress', 'measure', 'score', 'scoring', 'rating',
    'how well i', 'doing well', 'improve my', 'work on my',
    // Japanese
    '効率', '成果', 'パフォーマンス',
  ],

  should_language: [
    // Detected by counting occurrences — see detectPatternsFromText
    // Listed here for reference only; handled separately
    'should', 'must', 'have to', 'need to', 'supposed to', 'obligated',
    // Japanese
    'なければならない', 'すべき', 'べき',
  ],

  compulsive_continuation: [
    'feel worse but', 'getting worse but', 'not helping but', 'maybe i should continue',
    'push through', 'keep going anyway', 'not working but', 'should continue anyway',
    'continue even though', 'keep going even though', 'still continuing', 'forcing myself to continue',
    'even though it\'s hard', 'even though it hurts', 'even though it\'s not working',
    // Japanese
    ' 続けるべきか', '苦しいけど続ける', '悪化してるけど',
  ],

  breath_tension: [
    'breath makes me', 'breathing makes me', 'focus on breath', 'breath is making',
    'concentrating on breath makes', 'watching my breath makes', 'breath tension',
    'breath anxiety', 'anxious with breath', 'tense with breath', 'uncomfortable with breath',
    'breath feels wrong', 'can\'t breathe right', 'breathing feels forced',
    // Japanese
    '呼吸に集中すると', '呼吸が苦しい', '息が',
  ],

  self_scoring: [
    'rate myself', 'rating myself', 'score myself', 'scoring myself',
    'give myself', 'out of 10', '10/10', 'how well i did', 'did i do well',
    'grade myself', 'evaluate myself', 'assess myself', 'measure how',
    'how good i was', 'how bad i was',
    // Japanese
    '点数', '自己採点',
  ],

  rumination: [
    // Detected by LLM — too nuanced for simple keywords
    // Listed here for reference
    'keep thinking about', 'can\'t stop thinking', 'going over', 'replay',
    'over and over', 'circular', 'same thought', 'stuck on', 'looping',
  ],

  escalating_frustration: [
    // Detected by LLM + tension level combination
    'getting more frustrated', 'more frustrated', 'increasingly frustrated',
    'more tense', 'getting tenser', 'frustrating', 'annoying', 'irritating',
    'making me angry', 'getting angry', 'makes me more stressed',
    // Japanese
    '余計に', 'もっとイライラ', 'ストレスが増える',
  ],
};

const CRISIS_KEYWORDS: readonly string[] = [
  'hurt myself', 'harm myself', 'self harm', 'self-harm',
  'want to die', 'suicidal', 'end my life', 'kill myself',
  'not worth living', 'can\'t go on', 'can\'t take it anymore',
  'wish i was dead', 'better off dead',
  // Japanese
  '死にたい', '消えたい', '自殺', '死んだほうが', '消えてしまいたい',
];

// ── Detection functions ──────────────────────────────────────────────────────

export function detectCrisis(text: string): boolean {
  const lower = text.toLowerCase();
  return CRISIS_KEYWORDS.some(kw => lower.includes(kw));
}

export function detectPatternsFromText(text: string): readonly HarmfulPattern[] {
  if (!text.trim()) return [];

  const lower = text.toLowerCase();
  const found: HarmfulPattern[] = [];

  for (const [pattern, keywords] of Object.entries(PATTERN_KEYWORDS) as [HarmfulPattern, readonly string[]][]) {
    if (pattern === 'should_language') {
      // Special case: count occurrences
      const count = (lower.match(/\b(should|must|have to|need to|supposed to|べき|なければ)\b/g) ?? []).length;
      if (count >= 3) found.push('should_language');
      continue;
    }

    if (pattern === 'rumination' || pattern === 'escalating_frustration') {
      // Handled by LLM — but check keywords as a basic signal
      const matched = keywords.some(kw => lower.includes(kw));
      if (matched) found.push(pattern);
      continue;
    }

    const matched = keywords.some(kw => lower.includes(kw));
    if (matched) found.push(pattern);
  }

  return [...new Set(found)];
}

export function assessRisk(patterns: readonly HarmfulPattern[], tension: number): RiskLevel {
  if (patterns.length === 0) {
    return tension >= 5 ? 'low' : 'none';
  }

  // High-severity patterns
  const highSeverity: HarmfulPattern[] = ['compulsive_continuation', 'escalating_frustration'];
  if (patterns.some(p => highSeverity.includes(p))) {
    return tension >= 4 ? 'high' : 'moderate';
  }

  // Moderate severity: 2+ patterns or specific combos
  if (patterns.length >= 3) return 'high';
  if (patterns.length >= 2) return 'moderate';

  // Single pattern
  const moderate: HarmfulPattern[] = ['forced_acceptance', 'overmonitoring', 'breath_tension'];
  if (patterns.some(p => moderate.includes(p))) return 'moderate';

  return 'low';
}
