import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';
import { textToSpeech, isTTSConfigured } from '@/lib/tts/client';

const TTSSchema = z.object({
  text: z.string().min(1).max(2000),
});

export async function POST(req: NextRequest) {
  try {
    if (!isTTSConfigured()) {
      return NextResponse.json(
        { success: false, error: 'TTS not configured — add ELEVENLABS_API_KEY to .env.local' },
        { status: 503 }
      );
    }

    const body = await req.json();
    const { text } = TTSSchema.parse(body);

    const audio = await textToSpeech(text);

    return new NextResponse(audio.buffer as ArrayBuffer, {
      headers: {
        'Content-Type': 'audio/mpeg',
        'Content-Length': audio.length.toString(),
        'Cache-Control': 'public, max-age=86400',
      },
    });
  } catch (err) {
    if (err instanceof z.ZodError) {
      return NextResponse.json({ success: false, error: 'Invalid input' }, { status: 400 });
    }
    console.error('[tts] Error:', err);
    return NextResponse.json({ success: false, error: 'TTS generation failed' }, { status: 500 });
  }
}
