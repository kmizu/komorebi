'use client';

import { useState, useRef, useCallback, useEffect } from 'react';
import { useTranslations } from 'next-intl';
import type { GuidanceScript, CheckinData, SupervisorDecision } from '@/lib/types';
import { BreathingGuide } from './BreathingGuide';

interface GuidancePlayerProps {
  guidance: GuidanceScript;
  decision: SupervisorDecision;
  checkin: CheckinData;
  onEnd: () => void;
  onWorse: (report: string) => Promise<void>;
}

export function GuidancePlayer({ guidance, decision, checkin: _checkin, onEnd, onWorse }: GuidancePlayerProps) {
  const t = useTranslations('guidance');
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [loadingAudio, setLoadingAudio] = useState(false);
  const [audioError, setAudioError] = useState('');
  const [showWorseForm, setShowWorseForm] = useState(false);
  const [worseText, setWorseText] = useState('');
  const [escalating, setEscalating] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  const loadAudio = useCallback(async () => {
    setLoadingAudio(true);
    setAudioError('');
    try {
      const res = await fetch('/api/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: guidance.text }),
      });

      if (!res.ok) {
        const json = await res.json().catch(() => ({}));
        throw new Error(json.error ?? 'TTS request failed');
      }

      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      setAudioUrl(url);

      setTimeout(() => {
        audioRef.current?.play().catch(() => {});
      }, 100);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Audio unavailable';
      if (msg.includes('not configured')) {
        setAudioError(t('audioUnavailable'));
      } else {
        setAudioError(t('audioError'));
      }
    } finally {
      setLoadingAudio(false);
    }
  }, [guidance.text, t]);

  // Auto-load & play TTS on mount
  useEffect(() => {
    loadAudio();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleWorse = async () => {
    setEscalating(true);
    try {
      await onWorse(worseText || 'This is making things worse.');
    } catch {
      // continue regardless
    } finally {
      setEscalating(false);
      setShowWorseForm(false);
      setWorseText('');
    }
  };

  const isBreath = decision.recommendedMode === 'breath' || guidance.mode === 'breath';
  const durLabel = guidance.duration === 30 ? t('dur30') : guidance.duration === 60 ? t('dur60') : t('dur180');

  return (
    <div style={{ maxWidth: '32rem', margin: '0 auto' }}>
      {/* Breathing guide — only for breath mode */}
      {isBreath && <BreathingGuide />}

      {/* Guidance text card */}
      <div style={{
        padding: '1.4rem 1.5rem',
        background: '#fff',
        borderRadius: '0.75rem',
        border: '1px solid var(--cream-d)',
        boxShadow: '0 1px 10px rgba(107,130,113,0.05)',
        marginBottom: '1.25rem',
      }}>
        <p style={{
          margin: 0,
          fontSize: '0.88rem',
          color: 'var(--ink)',
          lineHeight: 1.9,
          fontWeight: 300,
          whiteSpace: 'pre-wrap',
        }}>
          {guidance.text}
        </p>
        {guidance.isPreset && (
          <p style={{ margin: '0.75rem 0 0', fontSize: '0.72rem', color: 'var(--ink-soft)' }}>
            {durLabel}
          </p>
        )}
      </div>

      {/* Audio */}
      <div style={{ marginBottom: '1rem' }}>
        {!audioUrl ? (
          <div style={{ textAlign: 'center' }}>
            {loadingAudio && (
              <p style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>{t('loadingAudio')}</p>
            )}
          </div>
        ) : (
          <audio ref={audioRef} src={audioUrl} controls style={{ width: '100%', height: '36px' }} />
        )}
        {audioError && (
          <p style={{ fontSize: '0.72rem', color: 'var(--ink-soft)', textAlign: 'center', marginTop: '0.4rem' }}>
            {audioError}
          </p>
        )}
      </div>

      {/* Worse form */}
      {!showWorseForm ? (
        <button
          onClick={() => setShowWorseForm(true)}
          style={{
            width: '100%',
            padding: '0.65rem',
            fontSize: '0.82rem',
            color: 'var(--ink-soft)',
            background: 'none',
            border: '1px solid var(--cream-d)',
            borderRadius: '100px',
            cursor: 'pointer',
            transition: 'border-color 0.2s, color 0.2s',
            marginBottom: '0.5rem',
          }}
          onMouseOver={e => {
            e.currentTarget.style.borderColor = 'var(--ink-soft)';
            e.currentTarget.style.color = 'var(--ink-mid)';
          }}
          onMouseOut={e => {
            e.currentTarget.style.borderColor = 'var(--cream-d)';
            e.currentTarget.style.color = 'var(--ink-soft)';
          }}
        >
          {t('worseBtn')}
        </button>
      ) : (
        <div style={{
          padding: '1rem 1.25rem',
          border: '1px solid var(--cream-d)',
          borderRadius: '0.75rem',
          marginBottom: '0.5rem',
        }}>
          <p style={{ margin: '0 0 0.75rem', fontSize: '0.85rem', color: 'var(--ink-mid)' }}>
            {t('worseTitle')}
          </p>
          <textarea
            value={worseText}
            onChange={e => setWorseText(e.target.value)}
            placeholder={t('worsePlaceholder')}
            rows={2}
            maxLength={300}
            style={{
              width: '100%',
              padding: '0.6rem 0.75rem',
              fontSize: '0.85rem',
              background: 'var(--warm-l)',
              border: '1px solid var(--cream-d)',
              borderRadius: '0.5rem',
              resize: 'none',
              fontFamily: 'inherit',
              fontWeight: 300,
              color: 'var(--ink)',
              marginBottom: '0.75rem',
            }}
          />
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              onClick={handleWorse}
              disabled={escalating}
              style={{
                flex: 1,
                padding: '0.6rem',
                fontSize: '0.82rem',
                color: '#fff',
                background: 'var(--sage)',
                border: 'none',
                borderRadius: '100px',
                cursor: escalating ? 'wait' : 'pointer',
                opacity: escalating ? 0.7 : 1,
                transition: 'background 0.2s',
              }}
            >
              {escalating ? t('adjusting') : t('adjustOrStop')}
            </button>
            <button
              onClick={() => setShowWorseForm(false)}
              style={{
                padding: '0.6rem 1rem',
                fontSize: '0.82rem',
                color: 'var(--ink-soft)',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
              }}
            >
              {t('cancel')}
            </button>
          </div>
        </div>
      )}

      {/* Done button */}
      <button
        onClick={onEnd}
        style={{
          width: '100%',
          padding: '0.8rem',
          fontSize: '0.82rem',
          color: 'var(--ink-mid)',
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          transition: 'color 0.2s',
        }}
        onMouseOver={e => { e.currentTarget.style.color = 'var(--ink)'; }}
        onMouseOut={e => { e.currentTarget.style.color = 'var(--ink-mid)'; }}
      >
        {t('done')}
      </button>
    </div>
  );
}
