import { NextResponse } from 'next/server';

export async function POST() {
  const apiKey = process.env.ELEVENLABS_API_KEY;
  if (!apiKey) {
    return NextResponse.json({ success: false, error: 'STT not configured' }, { status: 503 });
  }

  const res = await fetch('https://api.elevenlabs.io/v1/single-use-token/realtime_scribe', {
    method: 'POST',
    headers: { 'xi-api-key': apiKey },
  });

  if (!res.ok) {
    return NextResponse.json({ success: false, error: 'Token generation failed' }, { status: 500 });
  }

  const { token } = await res.json();
  return NextResponse.json({ success: true, token });
}
