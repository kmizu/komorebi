import { getTranslations } from 'next-intl/server';
import { Link } from '@/i18n/navigation';
import { isLLMConfigured } from '@/lib/llm/client';
import { isTTSConfigured } from '@/lib/tts/client';

/* ── Bokeh circle definitions ─────────────────────────────────────────────── */
const BOKEH = [
  { size: 200, x: '8%',  y: '-8%',  delay: '0s',  dur: '19s' },
  { size: 130, x: '72%', y: '5%',   delay: '4s',  dur: '23s' },
  { size: 90,  x: '50%', y: '30%',  delay: '8s',  dur: '17s' },
  { size: 160, x: '82%', y: '50%',  delay: '2s',  dur: '26s' },
  { size: 70,  x: '18%', y: '62%',  delay: '6s',  dur: '15s' },
  { size: 110, x: '58%', y: '78%',  delay: '11s', dur: '21s' },
  { size: 50,  x: '35%', y: '18%',  delay: '9s',  dur: '14s' },
] as const;

export default async function Home() {
  const t = await getTranslations('home');
  const llmOk = isLLMConfigured();
  const ttsOk = isTTSConfigured();

  const watchItems = t.raw('watchItems') as string[];

  return (
    <div style={{ maxWidth: '34rem', margin: '0 auto', position: 'relative' }}>

      {/* ── Bokeh: dappled light circles ──────────────────────────────────── */}
      <div
        aria-hidden
        style={{
          position: 'absolute',
          inset: '-40px -60px',
          overflow: 'hidden',
          pointerEvents: 'none',
          zIndex: 0,
        }}
      >
        {BOKEH.map((b, i) => (
          <div
            key={i}
            style={{
              position: 'absolute',
              left: b.x,
              top: b.y,
              width: `${b.size}px`,
              height: `${b.size}px`,
              borderRadius: '50%',
              background: 'radial-gradient(circle, var(--komorebi-glow) 0%, rgba(201,168,76,0.03) 50%, transparent 72%)',
              filter: `blur(${Math.round(b.size * 0.2)}px)`,
              animation: `drift ${b.dur} ease-in-out infinite`,
              animationDelay: b.delay,
            }}
          />
        ))}
      </div>

      {/* ── Hero ──────────────────────────────────────────────────────────── */}
      <div
        className="text-center animate-fade-up"
        style={{ paddingTop: '2.5rem', paddingBottom: '4rem', position: 'relative', zIndex: 1 }}
      >
        {/* Komorebi breathing circle — warm golden light */}
        <div style={{
          position: 'relative',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '2.5rem',
        }}>
          {/* Outer glow ring */}
          <div
            className="animate-komorebi-ring"
            style={{
              position: 'absolute',
              width: '110px',
              height: '110px',
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(201,168,76,0.14) 0%, transparent 70%)',
              border: '1px solid rgba(201,168,76,0.12)',
            }}
          />
          {/* Inner warm glow */}
          <div
            className="animate-komorebi-ring-inner"
            style={{
              position: 'absolute',
              width: '78px',
              height: '78px',
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(201,168,76,0.10) 20%, var(--komorebi-glow) 60%, transparent 100%)',
            }}
          />
          {/* Center light point */}
          <div style={{
            width: '7px',
            height: '7px',
            borderRadius: '50%',
            background: 'var(--komorebi)',
            opacity: 0.5,
            position: 'relative',
            boxShadow: '0 0 12px rgba(201,168,76,0.25)',
          }} />
        </div>

        {/* Title */}
        <h1
          className="font-display animate-fade-up delay-200"
          style={{
            fontSize: '2.8rem',
            fontWeight: 300,
            color: 'var(--ink)',
            lineHeight: 1.15,
            marginBottom: '0.5rem',
            letterSpacing: '0.06em',
          }}
        >
          {t('title')}
        </h1>

        {/* Subtitle in Japanese / English */}
        <p
          className="animate-fade-up delay-300"
          style={{
            color: 'var(--ink-soft)',
            fontSize: '0.72rem',
            letterSpacing: '0.18em',
            textTransform: 'uppercase',
            marginBottom: '2rem',
          }}
        >
          {t('tagline')}
        </p>

        <p
          className="animate-fade-up delay-500"
          style={{
            color: 'var(--ink-mid)',
            lineHeight: 2,
            fontSize: '0.88rem',
            maxWidth: '25rem',
            margin: '0 auto 2.8rem',
          }}
        >
          {t('subtitle')}
        </p>

        <div className="animate-fade-up delay-700">
          <Link href="/session" className="btn-start">
            {t('startSession')}
          </Link>
        </div>
      </div>

      {/* ── Watch list ────────────────────────────────────────────────────── */}
      <div
        className="animate-fade-up delay-900"
        style={{
          borderTop: '1px solid rgba(201,168,76,0.15)',
          paddingTop: '2.5rem',
          position: 'relative',
          zIndex: 1,
        }}
      >
        <p style={{
          fontSize: '0.68rem',
          color: 'var(--ink-soft)',
          letterSpacing: '0.12em',
          textTransform: 'uppercase',
          marginBottom: '1.2rem',
        }}>
          {t('watchesFor')}
        </p>
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {watchItems.map((item, i) => (
            <li
              key={i}
              style={{
                display: 'flex',
                alignItems: 'baseline',
                gap: '0.8rem',
                padding: '0.4rem 0',
                fontSize: '0.84rem',
                color: 'var(--ink-mid)',
                borderBottom: i < watchItems.length - 1 ? '1px solid rgba(201,168,76,0.08)' : 'none',
              }}
            >
              <span style={{
                color: 'var(--komorebi)',
                fontSize: '0.35rem',
                flexShrink: 0,
                opacity: 0.7,
              }}>●</span>
              {item}
            </li>
          ))}
        </ul>
      </div>

      {/* ── Status ────────────────────────────────────────────────────────── */}
      {(!llmOk || !ttsOk) && (
        <div style={{
          marginTop: '2.5rem',
          padding: '0.9rem 1.2rem',
          background: 'var(--warm-l)',
          borderRadius: '8px',
          borderLeft: '2px solid var(--komorebi-soft)',
          position: 'relative',
          zIndex: 1,
        }}>
          {!llmOk && (
            <p style={{ fontSize: '0.7rem', color: 'var(--ink-soft)', margin: '0 0 0.2rem' }}>
              {t('llmInactive')}
            </p>
          )}
          {!ttsOk && (
            <p style={{ fontSize: '0.7rem', color: 'var(--ink-soft)', margin: 0 }}>
              {t('ttsInactive')}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
