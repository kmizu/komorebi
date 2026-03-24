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

/**
 * Split guidance text into sentences for staggered TTS playback.
 * Sentences are spoken at intervals across the session duration,
 * so the user isn't left in silence.
 */
function splitIntoSegments(text: string): string[] {
  // Split on sentence-ending punctuation (EN + JA)
  const raw = text.split(/(?<=[。.!?！？])\s*/).filter(s => s.trim().length > 0);
  if (raw.length <= 1) return [text];
  return raw;
}

function formatTime(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return m > 0 ? `${m}:${s.toString().padStart(2, '0')}` : `${s}`;
}

export function GuidancePlayer({ guidance, decision, checkin: _checkin, onEnd, onWorse }: GuidancePlayerProps) {
  const t = useTranslations('guidance');
  const [currentSegment, setCurrentSegment] = useState(0);
  const [remaining, setRemaining] = useState<number>(guidance.duration);
  const [loadingAudio, setLoadingAudio] = useState(false);
  const [audioError, setAudioError] = useState('');
  const [escalating, setEscalating] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);
  const segmentsRef = useRef(splitIntoSegments(guidance.text));
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);
  const endCalledRef = useRef(false);

  // ── Session countdown timer ───────────────────────────────────────────
  useEffect(() => {
    const iv = setInterval(() => {
      setRemaining(prev => {
        if (prev <= 1) {
          clearInterval(iv);
          if (!endCalledRef.current) {
            endCalledRef.current = true;
            setTimeout(onEnd, 300);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(iv);
  }, [onEnd]);

  const segments = segmentsRef.current;
  const totalSegments = segments.length;

  // Load and play a single TTS segment
  const playSegment = useCallback(async (index: number) => {
    if (!mountedRef.current || index >= totalSegments) return;

    setLoadingAudio(index === 0);
    try {
      const res = await fetch('/api/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: segments[index] }),
      });

      if (!mountedRef.current) return;

      if (!res.ok) {
        const json = await res.json().catch(() => ({}));
        throw new Error(json.error ?? 'TTS request failed');
      }

      const blob = await res.blob();
      const url = URL.createObjectURL(blob);

      if (!mountedRef.current) return;

      // Play via a fresh audio element each time
      const audio = new Audio(url);
      audioRef.current = audio;

      audio.onended = () => {
        URL.revokeObjectURL(url);
        if (!mountedRef.current) return;

        const nextIdx = index + 1;
        if (nextIdx < totalSegments) {
          // Pause between segments: distribute across session duration
          const pauseMs = totalSegments > 1
            ? Math.min((guidance.duration * 1000) / totalSegments, 8000)
            : 0;
          timerRef.current = setTimeout(() => {
            if (mountedRef.current) {
              setCurrentSegment(nextIdx);
              playSegment(nextIdx);
            }
          }, pauseMs);
        }
      };

      audio.play().catch(() => {});
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      const msg = err instanceof Error ? err.message : 'Audio unavailable';
      if (msg.includes('not configured')) {
        setAudioError(t('audioUnavailable'));
      } else {
        setAudioError(t('audioError'));
      }
    } finally {
      if (mountedRef.current) setLoadingAudio(false);
    }
  }, [segments, totalSegments, guidance.duration, t]);

  // Start staggered playback on mount
  useEffect(() => {
    mountedRef.current = true;
    playSegment(0);

    return () => {
      mountedRef.current = false;
      if (timerRef.current) clearTimeout(timerRef.current);
      audioRef.current?.pause();
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleWorse = async () => {
    setEscalating(true);
    try {
      await onWorse('This is making things worse.');
    } catch {
      // continue regardless
    } finally {
      setEscalating(false);
    }
  };

  const guideMode = guidance.mode || decision.recommendedMode;
  const showGuide = guideMode !== 'abort';

  return (
    <div style={{ maxWidth: '32rem', margin: '0 auto' }}>
      {/* Session countdown */}
      <div style={{
        textAlign: 'center',
        marginBottom: '0.8rem',
      }}>
        <span style={{
          fontSize: '1.6rem',
          fontWeight: 300,
          color: remaining <= 5 ? 'var(--komorebi)' : 'var(--sage-d)',
          fontFamily: 'var(--font-cormorant), Georgia, serif',
          letterSpacing: '0.04em',
          transition: 'color 0.5s',
        }}>
          {formatTime(remaining)}
        </span>
      </div>

      {/* Visual guide — breathing ring for breath, ambient pulse for others */}
      {showGuide && <BreathingGuide mode={guideMode as 'breath' | 'sound' | 'body' | 'external' | 'reset' | 'abort'} />}

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
      </div>

      {/* Audio status */}
      <div style={{ marginBottom: '1rem', textAlign: 'center' }}>
        {loadingAudio && (
          <p style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>{t('loadingAudio')}</p>
        )}
        {!loadingAudio && !audioError && totalSegments > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '4px', padding: '0.3rem 0' }}>
            {segments.map((_, i) => (
              <div
                key={i}
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  background: i <= currentSegment ? 'var(--sage)' : 'var(--cream-d)',
                  transition: 'background 0.4s',
                }}
              />
            ))}
          </div>
        )}
        {audioError && (
          <p style={{ fontSize: '0.72rem', color: 'var(--ink-soft)', marginTop: '0.4rem' }}>
            {audioError}
          </p>
        )}
      </div>

      {/* Worse button — always visible, one tap to escalate */}
      <button
        onClick={handleWorse}
        disabled={escalating}
        style={{
          width: '100%',
          padding: '0.65rem',
          fontSize: '0.82rem',
          color: escalating ? 'var(--ink-soft)' : '#c47070',
          background: 'none',
          border: '1px solid var(--cream-d)',
          borderRadius: '100px',
          cursor: escalating ? 'wait' : 'pointer',
          transition: 'border-color 0.2s, color 0.2s',
          marginBottom: '0.5rem',
        }}
        onMouseOver={e => {
          if (!escalating) {
            e.currentTarget.style.borderColor = '#e0b8b8';
            e.currentTarget.style.background = 'rgba(196,112,112,0.04)';
          }
        }}
        onMouseOut={e => {
          e.currentTarget.style.borderColor = 'var(--cream-d)';
          e.currentTarget.style.background = 'none';
        }}
      >
        {escalating ? t('adjusting') : t('worseBtn')}
      </button>

      {/* End early button */}
      <button
        onClick={() => { endCalledRef.current = true; onEnd(); }}
        style={{
          width: '100%',
          padding: '0.8rem',
          fontSize: '0.82rem',
          color: 'var(--ink-soft)',
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          transition: 'color 0.2s',
        }}
        onMouseOver={e => { e.currentTarget.style.color = 'var(--ink-mid)'; }}
        onMouseOut={e => { e.currentTarget.style.color = 'var(--ink-soft)'; }}
      >
        {t('endEarly')}
      </button>
    </div>
  );
}
