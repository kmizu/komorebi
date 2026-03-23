'use client';

import { useState, useEffect, useRef } from 'react';
import { useTranslations } from 'next-intl';

/**
 * Visual breathing guide: inhale → hold → exhale cycle with
 * expanding/contracting ring + countdown + phase label.
 */

const PHASES = [
  { key: 'inhale',  sec: 4, scale: 1.35, innerScale: 1.2, ringOpacity: 0.22, glowOpacity: 0.25 },
  { key: 'hold',    sec: 4, scale: 1.35, innerScale: 1.2, ringOpacity: 0.32, glowOpacity: 0.32 },
  { key: 'exhale',  sec: 6, scale: 1.0,  innerScale: 1.0, ringOpacity: 0.12, glowOpacity: 0.16 },
] as const;

type PhaseKey = typeof PHASES[number]['key'];

const LABEL_KEYS: Record<PhaseKey, string> = {
  inhale: 'breatheIn',
  hold:   'hold',
  exhale: 'breatheOut',
};

const SIZE = 180;

export function BreathingGuide() {
  const t = useTranslations('guidance');
  const [phaseIdx, setPhaseIdx] = useState(0);
  const [countdown, setCountdown] = useState<number>(PHASES[0].sec);
  const idxRef = useRef(0);
  const elapsedRef = useRef(0);

  useEffect(() => {
    const iv = setInterval(() => {
      elapsedRef.current += 1;
      const cur = PHASES[idxRef.current];
      const remaining = cur.sec - elapsedRef.current;

      if (remaining <= 0) {
        const next = (idxRef.current + 1) % PHASES.length;
        idxRef.current = next;
        elapsedRef.current = 0;
        setPhaseIdx(next);
        setCountdown(PHASES[next].sec);
      } else {
        setCountdown(remaining);
      }
    }, 1000);

    return () => clearInterval(iv);
  }, []);

  const ph = PHASES[phaseIdx];
  const label = t(LABEL_KEYS[ph.key] as Parameters<typeof t>[0]);
  const transitionTiming = `${ph.sec}s ease-in-out`;

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '2rem 0 1rem',
    }}>
      {/* Ring container */}
      <div style={{
        position: 'relative',
        width: `${SIZE}px`,
        height: `${SIZE}px`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        {/* Outer breathing ring */}
        <div style={{
          position: 'absolute',
          width: '78%',
          height: '78%',
          borderRadius: '50%',
          border: '1.5px solid var(--sage)',
          transform: `scale(${ph.scale})`,
          opacity: ph.ringOpacity,
          transition: `transform ${transitionTiming}, opacity ${transitionTiming}`,
          willChange: 'transform, opacity',
        }} />

        {/* Inner glow */}
        <div style={{
          position: 'absolute',
          width: '56%',
          height: '56%',
          borderRadius: '50%',
          background: 'var(--sage-l)',
          transform: `scale(${ph.innerScale})`,
          opacity: ph.glowOpacity,
          transition: `transform ${transitionTiming}, opacity ${transitionTiming}`,
          willChange: 'transform, opacity',
        }} />

        {/* Tiny center dot (anchor point) */}
        <div style={{
          position: 'absolute',
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          background: 'var(--sage)',
          opacity: 0.35,
        }} />

        {/* Countdown number */}
        <span style={{
          position: 'relative',
          fontSize: '2.2rem',
          fontWeight: 300,
          color: 'var(--sage-d)',
          fontFamily: 'var(--font-cormorant), Georgia, serif',
          letterSpacing: '0.02em',
          lineHeight: 1,
        }}>
          {countdown}
        </span>
      </div>

      {/* Phase label — re-mounts on phase change for fade-in */}
      <p
        key={ph.key}
        className="animate-fade-in"
        style={{
          marginTop: '1.4rem',
          fontSize: '0.92rem',
          fontWeight: 300,
          color: 'var(--ink-mid)',
          letterSpacing: '0.14em',
          textTransform: 'uppercase',
        }}
      >
        {label}
      </p>
    </div>
  );
}
