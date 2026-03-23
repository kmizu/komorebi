'use client';

import { useState, useEffect, useRef } from 'react';
import { useTranslations } from 'next-intl';

/**
 * Komorebi breathing guide — visual rhythm of light filtering through a canopy.
 *
 * Breath mode:  inhale → hold → exhale with expanding ring + color shift
 *               sage green (shade) → komorebi gold (sunlight) → sage green
 *
 * Other modes:  gentle ambient pulse — dappled light slowly shifting
 */

// ── Breath-mode phases ──────────────────────────────────────────────────────

type PhaseKey = 'inhale' | 'hold' | 'exhale';

interface Phase {
  readonly key: PhaseKey;
  readonly sec: number;
  readonly scale: number;
  readonly innerScale: number;
  // Color blend: 0 = full sage (shade), 1 = full komorebi (sunlight)
  readonly warmth: number;
  readonly glowOpacity: number;
  readonly ringOpacity: number;
  readonly haloOpacity: number;
}

const PHASES: readonly Phase[] = [
  { key: 'inhale',  sec: 4, scale: 1.4,  innerScale: 1.25, warmth: 0.8,  glowOpacity: 0.35, ringOpacity: 0.28, haloOpacity: 0.06 },
  { key: 'hold',    sec: 4, scale: 1.4,  innerScale: 1.25, warmth: 1.0,  glowOpacity: 0.45, ringOpacity: 0.35, haloOpacity: 0.14 },
  { key: 'exhale',  sec: 6, scale: 1.0,  innerScale: 1.0,  warmth: 0.15, glowOpacity: 0.18, ringOpacity: 0.14, haloOpacity: 0.0  },
];

const LABEL_KEYS: Record<PhaseKey, string> = {
  inhale: 'breatheIn',
  hold:   'hold',
  exhale: 'breatheOut',
};

// ── Color helpers ───────────────────────────────────────────────────────────

// Blend between sage and komorebi based on warmth (0–1)
function blendColor(warmth: number): string {
  // sage: #5c7a5e → rgb(92,122,94)
  // komorebi: #c9a84c → rgb(201,168,76)
  const r = Math.round(92 + (201 - 92) * warmth);
  const g = Math.round(122 + (168 - 122) * warmth);
  const b = Math.round(94 + (76 - 94) * warmth);
  return `rgb(${r},${g},${b})`;
}

function glowGradient(warmth: number, opacity: number): string {
  const warm = `rgba(201,168,76,${opacity * warmth})`;
  const cool = `rgba(92,122,94,${opacity * (1 - warmth)})`;
  return `radial-gradient(circle, ${warm} 0%, ${cool} 55%, transparent 80%)`;
}

function haloShadow(warmth: number, opacity: number): string {
  if (opacity <= 0) return 'none';
  return `0 0 40px rgba(201,168,76,${opacity * warmth}), 0 0 80px rgba(201,168,76,${opacity * warmth * 0.5})`;
}

// ── Component size ──────────────────────────────────────────────────────────

const SIZE = 200;

// ── Breath mode ─────────────────────────────────────────────────────────────

function BreathMode() {
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
  const timing = `${ph.sec}s ease-in-out`;
  const ringColor = blendColor(ph.warmth);
  const numberColor = blendColor(ph.warmth * 0.7);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '2.5rem 0 1.2rem',
    }}>
      <div style={{
        position: 'relative',
        width: `${SIZE}px`,
        height: `${SIZE}px`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        {/* Outer halo — golden glow during hold */}
        <div style={{
          position: 'absolute',
          width: '120%',
          height: '120%',
          borderRadius: '50%',
          boxShadow: haloShadow(ph.warmth, ph.haloOpacity),
          transition: `box-shadow ${timing}`,
          willChange: 'box-shadow',
        }} />

        {/* Outer breathing ring — color shifts with warmth */}
        <div style={{
          position: 'absolute',
          width: '80%',
          height: '80%',
          borderRadius: '50%',
          border: `1.5px solid ${ringColor}`,
          transform: `scale(${ph.scale})`,
          opacity: ph.ringOpacity,
          transition: `transform ${timing}, opacity ${timing}, border-color ${timing}`,
          willChange: 'transform, opacity',
        }} />

        {/* Inner glow — radial gradient shifting sage↔komorebi */}
        <div style={{
          position: 'absolute',
          width: '60%',
          height: '60%',
          borderRadius: '50%',
          background: glowGradient(ph.warmth, ph.glowOpacity),
          transform: `scale(${ph.innerScale})`,
          transition: `transform ${timing}, background ${timing}`,
          willChange: 'transform, background',
        }} />

        {/* Center dot — warm pulse */}
        <div style={{
          position: 'absolute',
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          background: blendColor(ph.warmth),
          opacity: 0.5 + ph.warmth * 0.2,
          transition: `background ${timing}, opacity ${timing}`,
        }} />

        {/* Countdown number — color subtly shifts */}
        <span style={{
          position: 'relative',
          fontSize: '2.4rem',
          fontWeight: 300,
          color: numberColor,
          fontFamily: 'var(--font-cormorant), Georgia, serif',
          letterSpacing: '0.02em',
          lineHeight: 1,
          transition: `color ${timing}`,
        }}>
          {countdown}
        </span>
      </div>

      {/* Phase label */}
      <p
        key={ph.key}
        className="animate-fade-in"
        style={{
          marginTop: '1.5rem',
          fontSize: '0.88rem',
          fontWeight: 300,
          color: blendColor(ph.warmth * 0.5),
          letterSpacing: '0.16em',
          textTransform: 'uppercase',
          transition: `color ${timing}`,
        }}
      >
        {label}
      </p>
    </div>
  );
}

// ── Mode hint keys ──────────────────────────────────────────────────────────

const MODE_HINT_KEYS: Record<string, string> = {
  sound: 'soundHint',
  body: 'bodyHint',
  external: 'externalHint',
  reset: 'resetHint',
};

// ── Mode icons (simple SVG) ─────────────────────────────────────────────────

function ModeIcon({ mode, color }: { mode: string; color: string }) {
  const size = 28;
  const stroke = color;
  const sw = 1.5;

  if (mode === 'sound') return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round">
      <path d="M9 18V5l12-2v13" /><circle cx="6" cy="18" r="3" /><circle cx="18" cy="16" r="3" />
    </svg>
  );
  if (mode === 'body') return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round">
      <circle cx="12" cy="5" r="2" /><path d="M12 7v5m0 0l-3 5m3-5l3 5m-6-8h6" />
    </svg>
  );
  if (mode === 'external') return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round">
      <circle cx="12" cy="12" r="3" /><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z" />
    </svg>
  );
  // reset / default
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round">
      <circle cx="12" cy="12" r="9" /><path d="M12 8v4l2 2" />
    </svg>
  );
}

// ── Ambient mode (sound / body / external / reset) ──────────────────────────

function AmbientMode({ mode }: { mode: string }) {
  const t = useTranslations('guidance');
  const [pulse, setPulse] = useState(0);

  useEffect(() => {
    const iv = setInterval(() => {
      setPulse(p => (p + 1) % 80);
    }, 100);
    return () => clearInterval(iv);
  }, []);

  const s = Math.sin((pulse / 80) * Math.PI * 2);
  const norm = (s + 1) / 2;
  const scale = 1.0 + norm * 0.15;
  const warmth = 0.15 + norm * 0.35;
  const opacity = 0.12 + norm * 0.14;

  const hintKey = MODE_HINT_KEYS[mode] ?? MODE_HINT_KEYS['reset'];
  const hintColor = blendColor(warmth * 0.4);
  const iconColor = blendColor(warmth * 0.6);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '2.5rem 0 1.2rem',
    }}>
      <div style={{
        position: 'relative',
        width: `${SIZE}px`,
        height: `${SIZE}px`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        {/* Soft outer ring */}
        <div style={{
          position: 'absolute',
          width: '80%',
          height: '80%',
          borderRadius: '50%',
          border: `1px solid ${blendColor(warmth)}`,
          transform: `scale(${scale})`,
          opacity: opacity,
          transition: 'transform 0.1s linear, opacity 0.1s linear, border-color 0.1s linear',
        }} />

        {/* Inner glow */}
        <div style={{
          position: 'absolute',
          width: '55%',
          height: '55%',
          borderRadius: '50%',
          background: glowGradient(warmth, opacity + 0.08),
          transform: `scale(${scale * 0.95})`,
          transition: 'transform 0.1s linear, background 0.1s linear',
        }} />

        {/* Mode icon in center */}
        <div style={{ position: 'relative', opacity: 0.6, transition: 'opacity 0.3s' }}>
          <ModeIcon mode={mode} color={iconColor} />
        </div>
      </div>

      {/* Mode-specific hint label */}
      <p style={{
        marginTop: '1.5rem',
        fontSize: '0.88rem',
        fontWeight: 300,
        color: hintColor,
        letterSpacing: '0.08em',
        textAlign: 'center',
        transition: 'color 0.3s',
      }}>
        {t(hintKey as Parameters<typeof t>[0])}
      </p>
    </div>
  );
}

// ── Public API ───────────────────────────────────────────────────────────────

interface BreathingGuideProps {
  mode?: 'breath' | 'sound' | 'body' | 'external' | 'reset' | 'abort';
}

export function BreathingGuide({ mode = 'breath' }: BreathingGuideProps) {
  if (mode === 'breath') return <BreathMode />;
  return <AmbientMode mode={mode} />;
}
