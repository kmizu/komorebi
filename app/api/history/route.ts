import { NextRequest, NextResponse } from 'next/server';
import { getRecentSessions, getPersonalizationHints } from '@/lib/db/queries';

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const limit = Math.min(parseInt(searchParams.get('limit') ?? '20', 10), 50);

    const [sessions, hints] = await Promise.all([
      getRecentSessions(limit),
      getPersonalizationHints(),
    ]);

    return NextResponse.json({ success: true, data: { sessions, hints } });
  } catch (err) {
    console.error('[history] Error:', err);
    return NextResponse.json({ success: false, error: 'Failed to load history' }, { status: 500 });
  }
}
