import { describe, it, expect } from 'vitest';
import { detectCrisis, detectPatternsFromText, assessRisk } from './rules';

// ── detectCrisis ─────────────────────────────────────────────────────────────

describe('detectCrisis', () => {
  it('returns true for "want to die"', () => {
    expect(detectCrisis("I want to die")).toBe(true);
  });

  it('returns true for "kill myself"', () => {
    expect(detectCrisis("I want to kill myself")).toBe(true);
  });

  it('returns true for "self-harm"', () => {
    expect(detectCrisis("I want to self-harm")).toBe(true);
  });

  it('returns true for Japanese crisis keywords', () => {
    expect(detectCrisis("死にたい")).toBe(true);
    expect(detectCrisis("消えたい")).toBe(true);
  });

  it('returns true case-insensitively', () => {
    expect(detectCrisis("I WANT TO DIE")).toBe(true);
  });

  it('returns false for normal distress text', () => {
    expect(detectCrisis("I feel anxious and tense")).toBe(false);
    expect(detectCrisis("I'm really struggling today")).toBe(false);
  });

  it('returns false for empty string', () => {
    expect(detectCrisis("")).toBe(false);
  });
});

// ── detectPatternsFromText ────────────────────────────────────────────────────

describe('detectPatternsFromText', () => {
  it('returns empty array for empty text', () => {
    expect(detectPatternsFromText("")).toEqual([]);
    expect(detectPatternsFromText("   ")).toEqual([]);
  });

  it('detects perfectionism', () => {
    const result = detectPatternsFromText("I keep failing at this, I'm not good enough");
    expect(result).toContain('perfectionism');
  });

  it('detects forced_acceptance', () => {
    const result = detectPatternsFromText("I know I should accept this feeling");
    expect(result).toContain('forced_acceptance');
  });

  it('detects overmonitoring', () => {
    const result = detectPatternsFromText("I keep checking whether I'm breathing right");
    expect(result).toContain('overmonitoring');
  });

  it('detects performance_framing', () => {
    const result = detectPatternsFromText("I want to optimize my meditation performance");
    expect(result).toContain('performance_framing');
  });

  it('detects should_language only when 3+ occurrences', () => {
    const two = detectPatternsFromText("I should be calmer and I must try harder");
    expect(two).not.toContain('should_language');

    const three = detectPatternsFromText("I should relax, I must calm down, I need to stop worrying");
    expect(three).toContain('should_language');
  });

  it('detects compulsive_continuation', () => {
    const result = detectPatternsFromText("It's not working but I should continue anyway");
    expect(result).toContain('compulsive_continuation');
  });

  it('detects breath_tension', () => {
    const result = detectPatternsFromText("Focusing on breath makes me more tense");
    // "focus on breath" + "more tense" → check breath_tension keyword
    expect(detectPatternsFromText("breathing makes me anxious")).toContain('breath_tension');
  });

  it('detects self_scoring', () => {
    const result = detectPatternsFromText("I want to rate myself on how well I did");
    expect(result).toContain('self_scoring');
  });

  it('detects multiple patterns at once', () => {
    const text = "I keep failing and I must accept this. Breathing makes me anxious.";
    const result = detectPatternsFromText(text);
    expect(result.length).toBeGreaterThanOrEqual(2);
  });

  it('returns no duplicates', () => {
    const text = "I keep failing and failing, not good enough";
    const result = detectPatternsFromText(text);
    const unique = [...new Set(result)];
    expect(result.length).toBe(unique.length);
  });

  it('detects Japanese perfectionism', () => {
    const result = detectPatternsFromText("ちゃんとできていない");
    expect(result).toContain('perfectionism');
  });
});

// ── assessRisk ────────────────────────────────────────────────────────────────

describe('assessRisk', () => {
  it('returns "none" with no patterns and low tension', () => {
    expect(assessRisk([], 1)).toBe('none');
    expect(assessRisk([], 4)).toBe('none');
  });

  it('returns "low" with no patterns and max tension (5)', () => {
    expect(assessRisk([], 5)).toBe('low');
  });

  it('returns "low" with one mild pattern', () => {
    expect(assessRisk(['perfectionism'], 2)).toBe('low');
  });

  it('returns "moderate" with one moderate-severity pattern', () => {
    expect(assessRisk(['overmonitoring'], 2)).toBe('moderate');
    expect(assessRisk(['forced_acceptance'], 2)).toBe('moderate');
    expect(assessRisk(['breath_tension'], 2)).toBe('moderate');
  });

  it('returns "moderate" with 2 patterns', () => {
    expect(assessRisk(['perfectionism', 'self_scoring'], 2)).toBe('moderate');
  });

  it('returns "high" with 3+ patterns', () => {
    expect(assessRisk(['perfectionism', 'self_scoring', 'performance_framing'], 2)).toBe('high');
  });

  it('returns "moderate" for compulsive_continuation with low tension', () => {
    expect(assessRisk(['compulsive_continuation'], 2)).toBe('moderate');
  });

  it('returns "high" for compulsive_continuation with high tension (4+)', () => {
    expect(assessRisk(['compulsive_continuation'], 4)).toBe('high');
    expect(assessRisk(['compulsive_continuation'], 5)).toBe('high');
  });

  it('returns "high" for escalating_frustration with high tension', () => {
    expect(assessRisk(['escalating_frustration'], 4)).toBe('high');
  });
});
