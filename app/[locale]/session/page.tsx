'use client';

import { useState } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSessionMachine } from '@/hooks/useSessionMachine';
import { ReflectionChat } from '@/components/ReflectionChat';
import { SupervisorReview } from '@/components/SupervisorReview';
import { GuidancePlayer } from '@/components/GuidancePlayer';
import { PostReflection } from '@/components/PostReflection';
import { Link } from '@/i18n/navigation';
import type { ReflectionProfile } from '@/lib/types';

export default function SessionPage() {
  const t = useTranslations('session');
  const locale = useLocale();

  const {
    state,
    personalize,
    startSession,
    reportWorse,
    endSession,
    submitPost,
    reset,
  } = useSessionMachine(locale);

  const [loading, setLoading] = useState(false);

  const withLoading = <T,>(fn: () => Promise<T>) => async () => {
    setLoading(true);
    try {
      await fn();
    } finally {
      setLoading(false);
    }
  };

  const handleReflectionDone = async (profile: ReflectionProfile) => {
    setLoading(true);
    try {
      await personalize(profile);
    } finally {
      setLoading(false);
    }
  };

  const stepKeys = ['checkin', 'review', 'practice', 'reflect'] as const;
  const stepOrder = ['reflecting', 'review', 'session', 'post', 'done'];

  return (
    <div className="space-y-8">
      {/* Step indicator */}
      <div className="flex gap-2 items-center text-xs text-stone-400">
        {stepKeys.map((key, i) => {
          const currentIdx = stepOrder.indexOf(state.step);
          const done = currentIdx > i;
          const active = stepOrder[i] === state.step;
          return (
            <span
              key={key}
              className={`${active ? 'text-stone-700' : done ? 'text-stone-400' : 'text-stone-200'}`}
            >
              {i > 0 && <span className="mr-2">·</span>}
              {t(`steps.${key}`)}
            </span>
          );
        })}
      </div>

      {state.step === 'reflecting' && (
        <div key="reflecting" className="animate-fade-in">
          <ReflectionChat
            locale={locale}
            onDone={handleReflectionDone}
            loading={loading}
          />
        </div>
      )}

      {state.step === 'review' && (
        <div key="review" className="animate-fade-in">
          <SupervisorReview
            decision={state.decision}
            checkin={{
              mood: state.profile.mood,
              tension: state.profile.tension,
              selfCritical: state.profile.selfCritical,
              intent: state.profile.intent,
              lastSessionOutcome: state.profile.lastSessionOutcome,
              freeText: state.profile.freeText,
            }}
            loading={loading}
            onStart={withLoading(() => startSession(state.decision, state.profile))}
            onDecline={() => reset()}
          />
        </div>
      )}

      {state.step === 'session' && (
        <div key="session" className="animate-fade-in">
          <GuidancePlayer
            guidance={state.guidance}
            decision={state.decision}
            checkin={{
              mood: state.profile.mood,
              tension: state.profile.tension,
              selfCritical: state.profile.selfCritical,
              intent: state.profile.intent,
              lastSessionOutcome: state.profile.lastSessionOutcome,
              freeText: state.profile.freeText,
            }}
            onEnd={endSession}
            onWorse={async (report) => {
              await reportWorse(report, state.profile);
            }}
          />
        </div>
      )}

      {state.step === 'post' && (
        <div key="post" className="animate-fade-in">
          <PostReflection
            sessionId={state.sessionId}
            loading={loading}
            onSubmit={async (id, outcome) => {
              setLoading(true);
              try {
                await submitPost(id, outcome);
              } finally {
                setLoading(false);
              }
            }}
          />
        </div>
      )}

      {state.step === 'done' && (
        <div key="done" className="animate-fade-in" style={{ maxWidth: '32rem', margin: '0 auto', textAlign: 'center' }}>
          <p style={{ color: 'var(--ink-mid)', marginBottom: '1.5rem' }}>{t('done')}</p>
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
            <button
              onClick={reset}
              style={{
                padding: '0.65rem 1.6rem',
                fontSize: '0.82rem',
                color: '#fff',
                background: 'var(--sage)',
                border: 'none',
                borderRadius: '100px',
                cursor: 'pointer',
                transition: 'background 0.2s',
              }}
            >
              {t('anotherSession')}
            </button>
            <Link
              href="/history"
              style={{
                padding: '0.65rem 1.6rem',
                fontSize: '0.82rem',
                color: 'var(--ink-mid)',
                background: 'var(--warm-l)',
                borderRadius: '100px',
                textDecoration: 'none',
                transition: 'background 0.2s',
              }}
            >
              {t('viewHistory')}
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
