import type {
  RiskLevel,
  SupervisorAction,
  GuidanceMode,
  GuidanceDuration,
  HarmfulPattern,
  CheckinData,
  PersonalizationHints,
} from '@/lib/types';

export interface SafetyRecommendation {
  readonly action: SupervisorAction;
  readonly recommendedMode: GuidanceMode;
  readonly guidanceDuration: GuidanceDuration;
  readonly message: string;
}

/**
 * 3-layer safety model. Pure function — no side effects.
 *
 * Layer 3 (Crisis):     Stop everything, show crisis resources
 * Layer 2 (Distress):   Stop inward practice, move to external grounding
 * Layer 1 (Subtle):     Soften, shorten, switch anchor
 */
export function applySafetyLayers(
  riskLevel: RiskLevel,
  patterns: readonly HarmfulPattern[],
  checkin: CheckinData,
  hints: PersonalizationHints
): SafetyRecommendation {
  // ── Layer 3: Crisis ─────────────────────────────────────────────────────────
  if (riskLevel === 'crisis') {
    return {
      action: 'crisis',
      recommendedMode: 'abort',
      guidanceDuration: 30,
      message:
        'This app is not the right support for what you\'re going through right now. Please reach out to someone you trust, or contact a crisis line. You don\'t have to handle this alone.',
    };
  }

  // ── Layer 2: Distress escalation ────────────────────────────────────────────
  if (riskLevel === 'high' || patterns.includes('compulsive_continuation')) {
    const mode = selectSafeMode(hints, patterns, true);
    return {
      action: 'stop',
      recommendedMode: mode,
      guidanceDuration: 30,
      message: 'Let\'s stop here. Stepping back is the right move. No need to push through.',
    };
  }

  if (patterns.includes('escalating_frustration')) {
    return {
      action: 'switch',
      recommendedMode: 'external',
      guidanceDuration: 30,
      message: 'Let\'s shift to something outside. Just look at a surface or shape in the room.',
    };
  }

  // ── Layer 1: Subtle meta-suffering ──────────────────────────────────────────
  if (riskLevel === 'moderate') {
    if (patterns.includes('overmonitoring') || patterns.includes('forced_acceptance')) {
      const mode = selectSafeMode(hints, patterns, false);
      return {
        action: 'switch',
        recommendedMode: mode,
        guidanceDuration: 60,
        message: 'Less internal focus for now. Let\'s anchor to something external.',
      };
    }

    if (patterns.includes('perfectionism') || patterns.includes('performance_framing') || patterns.includes('self_scoring')) {
      return {
        action: 'shorten',
        recommendedMode: selectMode(hints, patterns),
        guidanceDuration: 60,
        message: 'Shorter and simpler. There\'s no correct way to do this.',
      };
    }

    if (patterns.includes('breath_tension')) {
      return {
        action: 'switch',
        recommendedMode: preferNonBreath(hints),
        guidanceDuration: 60,
        message: 'Breath focus isn\'t working well today. Let\'s try something else.',
      };
    }

    if (patterns.includes('should_language')) {
      return {
        action: 'soften',
        recommendedMode: selectMode(hints, patterns),
        guidanceDuration: 60,
        message: 'Nothing is required here. This isn\'t a task.',
      };
    }

    return {
      action: 'soften',
      recommendedMode: selectMode(hints, patterns),
      guidanceDuration: 60,
      message: 'Keep it light. No goals to reach today.',
    };
  }

  // ── Low / None risk ─────────────────────────────────────────────────────────
  if (riskLevel === 'low') {
    return {
      action: 'proceed',
      recommendedMode: selectMode(hints, patterns),
      guidanceDuration: 60,
      message: 'Conditions look reasonable. Keep it simple.',
    };
  }

  // None
  return {
    action: 'proceed',
    recommendedMode: selectMode(hints, patterns),
    guidanceDuration: mapIntentToDuration(checkin.intent),
    message: 'Ready when you are.',
  };
}

// ── Mode selection helpers ────────────────────────────────────────────────────

function selectMode(hints: PersonalizationHints, patterns: readonly HarmfulPattern[]): GuidanceMode {
  if (patterns.includes('breath_tension')) return preferNonBreath(hints);
  if (hints.preferredMode && hints.preferredMode !== hints.avoidMode) return hints.preferredMode;
  return 'breath';
}

function selectSafeMode(
  hints: PersonalizationHints,
  patterns: readonly HarmfulPattern[],
  external: boolean
): GuidanceMode {
  if (external) {
    if (hints.preferredMode === 'external' || hints.preferredMode === 'sound') return hints.preferredMode;
    return 'external';
  }
  return preferNonBreath(hints);
}

function preferNonBreath(hints: PersonalizationHints): GuidanceMode {
  if (hints.preferredMode && hints.preferredMode !== 'breath') return hints.preferredMode;
  return 'sound';
}

function mapIntentToDuration(intent: CheckinData['intent']): GuidanceDuration {
  switch (intent) {
    case 'calming': return 180;
    case 'grounding': return 60;
    case 'checkin': return 30;
  }
}
