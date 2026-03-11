import { createHash } from 'crypto';
import path from 'path';
import fs from 'fs';

const CACHE_DIR = path.join(process.cwd(), 'data', 'tts-cache');

// ── Cache helpers ──────────────────────────────────────────────────────────────

function getCacheKey(text: string): string {
  return createHash('sha256').update(text).digest('hex');
}

function getCachePath(key: string): string {
  return path.join(CACHE_DIR, `${key}.mp3`);
}

function readCache(key: string): Buffer | null {
  const p = getCachePath(key);
  if (fs.existsSync(p)) return fs.readFileSync(p);
  return null;
}

function writeCache(key: string, data: Buffer): void {
  if (!fs.existsSync(CACHE_DIR)) fs.mkdirSync(CACHE_DIR, { recursive: true });
  fs.writeFileSync(getCachePath(key), data);
}

// ── Configuration ──────────────────────────────────────────────────────────────

export function isTTSConfigured(): boolean {
  return Boolean(process.env.ELEVENLABS_API_KEY);
}

// ── Main function ──────────────────────────────────────────────────────────────

export async function textToSpeech(text: string): Promise<Buffer> {
  const apiKey = process.env.ELEVENLABS_API_KEY;
  if (!apiKey) throw new Error('ELEVENLABS_API_KEY not configured');

  const voiceId = process.env.ELEVENLABS_VOICE_ID ?? 'EXAVITQu4vr4xnSDxMaL';

  // Check cache
  const key = getCacheKey(text + voiceId);
  const cached = readCache(key);
  if (cached) return cached;

  // Call ElevenLabs API
  const response = await fetch(
    `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}`,
    {
      method: 'POST',
      headers: {
        'xi-api-key': apiKey,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        text,
        model_id: 'eleven_multilingual_v2',
        voice_settings: {
          stability: 0.80,
          similarity_boost: 0.75,
          style: 0.0,
          use_speaker_boost: false,
        },
      }),
    }
  );

  if (!response.ok) {
    const err = await response.text().catch(() => response.statusText);
    throw new Error(`ElevenLabs API error ${response.status}: ${err}`);
  }

  const buffer = Buffer.from(await response.arrayBuffer());
  writeCache(key, buffer);
  return buffer;
}
