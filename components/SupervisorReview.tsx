'use client';

import type { SupervisorDecision, CheckinData } from '@/lib/types';
import { CrisisBanner } from './CrisisBanner';

interface SupervisorReviewProps {
  decision: SupervisorDecision;
  checkin: CheckinData;
  onStart: () => Promise<void>;
  onDecline: () => void;
  loading?: boolean;
}

const RISK_COLORS = {
  none: 'border-stone-200',
  low: 'border-stone-200',
  moderate: 'border-amber-200',
  high: 'border-orange-300',
  crisis: 'border-stone-300',
};

const MODE_LABELS: Record<string, string> = {
  breath: 'Breath awareness',
  sound: 'Sound anchor',
  body: 'Body anchor',
  external: 'External focus',
  reset: 'Quick reset',
  abort: 'Stop safely',
};

const DURATION_LABELS: Record<number, string> = {
  30: '30 seconds',
  60: '1 minute',
  180: '3 minutes',
};

export function SupervisorReview({
  decision,
  checkin,
  onStart,
  onDecline,
  loading,
}: SupervisorReviewProps) {
  if (decision.riskLevel === 'crisis') {
    return <CrisisBanner message={decision.message} />;
  }

  const borderColor = RISK_COLORS[decision.riskLevel] ?? 'border-stone-200';

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <div className={`border rounded-lg p-6 space-y-4 ${borderColor}`}>
        <p className="text-stone-700 leading-relaxed">{decision.message}</p>

        {decision.action !== 'proceed' && (
          <div className="flex gap-4 text-sm text-stone-500">
            <span>
              Mode: <span className="text-stone-700">{MODE_LABELS[decision.recommendedMode] ?? decision.recommendedMode}</span>
            </span>
            <span>
              Duration: <span className="text-stone-700">{DURATION_LABELS[decision.guidanceDuration]}</span>
            </span>
          </div>
        )}

        {decision.patterns.length > 0 && (
          <div className="text-xs text-stone-400">
            Noticed: {decision.patterns.join(', ').replace(/_/g, ' ')}
          </div>
        )}
      </div>

      {decision.action !== 'stop' && decision.action !== 'crisis' ? (
        <div className="flex gap-3">
          <button
            onClick={onStart}
            disabled={loading}
            className="flex-1 py-3 text-sm text-white bg-stone-700 rounded hover:bg-stone-800 disabled:opacity-50 transition-colors"
          >
            {loading ? 'Starting...' : 'Begin'}
          </button>
          <button
            onClick={onDecline}
            className="px-4 py-3 text-sm text-stone-500 hover:text-stone-700 transition-colors"
          >
            Not today
          </button>
        </div>
      ) : (
        <button
          onClick={onDecline}
          className="w-full py-3 text-sm text-stone-600 bg-stone-100 rounded hover:bg-stone-200 transition-colors"
        >
          That&apos;s okay — I&apos;ll stop here
        </button>
      )}
    </div>
  );
}
