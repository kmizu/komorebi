'use client';

import { useState, useRef, useCallback } from 'react';
import type { GuidanceScript, CheckinData, SupervisorDecision } from '@/lib/types';

interface GuidancePlayerProps {
  guidance: GuidanceScript;
  decision: SupervisorDecision;
  checkin: CheckinData;
  onEnd: () => void;
  onWorse: (report: string) => Promise<void>;
}

export function GuidancePlayer({
  guidance,
  decision,
  checkin,
  onEnd,
  onWorse,
}: GuidancePlayerProps) {
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

      // Auto-play
      setTimeout(() => {
        audioRef.current?.play().catch(() => {});
      }, 100);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Audio unavailable';
      if (msg.includes('not configured')) {
        setAudioError('Voice playback not configured. Read the text above.');
      } else {
        setAudioError('Could not load audio. Read the text above.');
      }
    } finally {
      setLoadingAudio(false);
    }
  }, [guidance.text]);

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

  return (
    <div className="max-w-lg mx-auto space-y-6">
      {/* Guidance text */}
      <div className="bg-stone-50 border border-stone-200 rounded-lg p-6">
        <p className="text-stone-700 leading-relaxed text-sm whitespace-pre-wrap">{guidance.text}</p>
        {guidance.isPreset && (
          <p className="text-xs text-stone-400 mt-4">
            {guidance.duration === 30 ? '~30 sec' : guidance.duration === 60 ? '~1 min' : '~3 min'}
          </p>
        )}
      </div>

      {/* Audio controls */}
      <div className="space-y-2">
        {!audioUrl ? (
          <button
            onClick={loadAudio}
            disabled={loadingAudio}
            className="w-full py-2.5 text-sm text-stone-600 bg-stone-100 rounded hover:bg-stone-200 disabled:opacity-50 transition-colors"
          >
            {loadingAudio ? 'Loading audio...' : 'Play audio'}
          </button>
        ) : (
          <audio
            ref={audioRef}
            src={audioUrl}
            controls
            className="w-full"
            onEnded={() => {}}
          />
        )}
        {audioError && <p className="text-xs text-stone-400">{audioError}</p>}
      </div>

      {/* This is making it worse */}
      {!showWorseForm ? (
        <button
          onClick={() => setShowWorseForm(true)}
          className="w-full py-2.5 text-sm text-stone-500 border border-stone-200 rounded hover:border-stone-400 hover:text-stone-700 transition-colors"
        >
          This is making it worse
        </button>
      ) : (
        <div className="border border-stone-200 rounded-lg p-4 space-y-3">
          <p className="text-sm text-stone-600">What&apos;s happening?</p>
          <textarea
            value={worseText}
            onChange={e => setWorseText(e.target.value)}
            placeholder="You can say anything, or just leave this blank."
            rows={2}
            maxLength={300}
            className="w-full px-3 py-2 text-sm text-stone-700 bg-stone-50 border border-stone-200 rounded resize-none focus:outline-none focus:border-stone-400 placeholder:text-stone-300"
          />
          <div className="flex gap-2">
            <button
              onClick={handleWorse}
              disabled={escalating}
              className="flex-1 py-2 text-sm text-white bg-stone-700 rounded hover:bg-stone-800 disabled:opacity-50 transition-colors"
            >
              {escalating ? 'Adjusting...' : 'Adjust or stop'}
            </button>
            <button
              onClick={() => setShowWorseForm(false)}
              className="px-4 py-2 text-sm text-stone-500 hover:text-stone-700 transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* End session */}
      <button
        onClick={onEnd}
        className="w-full py-3 text-sm text-stone-600 hover:text-stone-800 transition-colors"
      >
        I&apos;m done
      </button>
    </div>
  );
}
