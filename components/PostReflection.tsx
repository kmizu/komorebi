'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import type { PostOutcome } from '@/lib/types';

interface PostReflectionProps {
  sessionId: string;
  onSubmit: (sessionId: string, outcome: PostOutcome) => Promise<void>;
  loading?: boolean;
}

export function PostReflection({ sessionId, onSubmit, loading }: PostReflectionProps) {
  const t = useTranslations('post');
  const [feltBetter, setFeltBetter] = useState<boolean | null>(null);
  const [wouldContinue, setWouldContinue] = useState<boolean | null>(null);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (feltBetter === null || wouldContinue === null) {
      setError(t('requiredError'));
      return;
    }
    setError('');
    try {
      await onSubmit(sessionId, {
        feltBetter,
        wouldContinue,
        notes: notes.trim() || undefined,
      });
    } catch {
      setError(t('submitError'));
    }
  };

  const choiceBtn = (selected: boolean): React.CSSProperties => ({
    flex: 1,
    padding: '0.6rem',
    fontSize: '0.85rem',
    borderRadius: '100px',
    border: 'none',
    cursor: 'pointer',
    transition: 'background 0.2s, color 0.2s',
    background: selected ? 'var(--sage)' : 'var(--warm-l)',
    color: selected ? '#fff' : 'var(--ink-mid)',
    fontFamily: 'inherit',
    fontWeight: 300,
  });

  return (
    <form onSubmit={handleSubmit} style={{ maxWidth: '32rem', margin: '0 auto' }}>
      {/* Pressure question */}
      <div style={{ marginBottom: '1.75rem' }}>
        <p style={{ fontSize: '0.88rem', color: 'var(--ink-mid)', marginBottom: '0.6rem' }}>
          {t('pressureQuestion')}
        </p>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button
            type="button"
            onClick={() => setFeltBetter(true)}
            style={choiceBtn(feltBetter === true)}
          >
            {t('reducedIt')}
          </button>
          <button
            type="button"
            onClick={() => setFeltBetter(false)}
            style={choiceBtn(feltBetter === false)}
          >
            {t('addedToIt')}
          </button>
        </div>
      </div>

      {/* Continue question */}
      <div style={{ marginBottom: '1.75rem' }}>
        <p style={{ fontSize: '0.88rem', color: 'var(--ink-mid)', marginBottom: '0.6rem' }}>
          {t('continueQuestion')}
        </p>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button
            type="button"
            onClick={() => setWouldContinue(true)}
            style={choiceBtn(wouldContinue === true)}
          >
            {t('yes')}
          </button>
          <button
            type="button"
            onClick={() => setWouldContinue(false)}
            style={choiceBtn(wouldContinue === false)}
          >
            {t('no')}
          </button>
        </div>
      </div>

      {/* Notes */}
      <div style={{ marginBottom: '1.75rem' }}>
        <p style={{ fontSize: '0.88rem', color: 'var(--ink-mid)', marginBottom: '0.6rem' }}>
          {t('notesLabel')}{' '}
          <span style={{ color: 'var(--ink-soft)' }}>{t('optional')}</span>
        </p>
        <textarea
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="..."
          rows={2}
          maxLength={400}
          style={{
            width: '100%',
            padding: '0.6rem 0.75rem',
            fontSize: '0.85rem',
            color: 'var(--ink)',
            background: 'var(--warm-l)',
            border: '1px solid var(--cream-d)',
            borderRadius: '0.5rem',
            resize: 'none',
            fontFamily: 'inherit',
            fontWeight: 300,
          }}
        />
      </div>

      {error && (
        <p style={{ fontSize: '0.82rem', color: '#c47070', marginBottom: '1rem' }}>{error}</p>
      )}

      <button
        type="submit"
        disabled={loading}
        style={{
          width: '100%',
          padding: '0.8rem',
          fontSize: '0.82rem',
          letterSpacing: '0.06em',
          color: '#fff',
          background: 'var(--sage)',
          border: 'none',
          borderRadius: '100px',
          cursor: loading ? 'wait' : 'pointer',
          opacity: loading ? 0.7 : 1,
          transition: 'background 0.2s',
        }}
      >
        {loading ? t('saving') : t('done')}
      </button>
    </form>
  );
}
