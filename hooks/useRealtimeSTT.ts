'use client';

import { useState, useRef, useCallback } from 'react';

const TARGET_SAMPLE_RATE = 16000;

function float32ToPCM16(input: Float32Array): Int16Array {
  const output = new Int16Array(input.length);
  for (let i = 0; i < input.length; i++) {
    const s = Math.max(-1, Math.min(1, input[i]));
    output[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
  }
  return output;
}

function pcm16ToBase64(pcm: Int16Array): string {
  const bytes = new Uint8Array(pcm.buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

export type STTStatus = 'idle' | 'connecting' | 'recording' | 'error';

export function useRealtimeSTT(locale: string) {
  const [status, setStatus] = useState<STTStatus>('idle');
  const [interimText, setInterimText] = useState('');
  const [error, setError] = useState('');

  // Persistent across turns (kept alive between speech segments)
  const wsRef = useRef<WebSocket | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const audioSourceRef = useRef<MediaStreamAudioSourceNode | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const workletLoadedRef = useRef(false);

  // Disconnected between turns, reconnected each time user should speak
  const workletNodeRef = useRef<AudioWorkletNode | null>(null);

  // Stable ref so WS handler always calls the latest callback
  const onCommittedRef = useRef<((text: string) => void) | null>(null);

  // Ref-backed status for guards inside async callbacks
  const statusRef = useRef<STTStatus>('idle');
  const setStatusBoth = useCallback((s: STTStatus) => {
    statusRef.current = s;
    setStatus(s);
  }, []);

  // ── Full teardown (explicit stop or error) ────────────────────────────────
  const stopAll = useCallback(() => {
    workletNodeRef.current?.disconnect();
    workletNodeRef.current = null;
    audioSourceRef.current?.disconnect();
    audioSourceRef.current = null;
    audioCtxRef.current?.close();
    audioCtxRef.current = null;
    streamRef.current?.getTracks().forEach(t => t.stop());
    streamRef.current = null;
    wsRef.current?.close();
    wsRef.current = null;
    workletLoadedRef.current = false;
    setStatusBoth('idle');
    setInterimText('');
  }, [setStatusBoth]);

  // ── Connect worklet to existing AudioContext + source ─────────────────────
  // Called at the start of each speech turn (fast, no network round-trip)
  const resumeAudio = useCallback(async () => {
    const audioCtx = audioCtxRef.current;
    const source = audioSourceRef.current;
    if (!audioCtx || !source) return;

    if (!workletLoadedRef.current) {
      await audioCtx.audioWorklet.addModule('/audio-processor.js');
      workletLoadedRef.current = true;
    }

    const workletNode = new AudioWorkletNode(audioCtx, 'pcm-processor');
    workletNodeRef.current = workletNode;

    workletNode.port.onmessage = (e) => {
      if (wsRef.current?.readyState !== WebSocket.OPEN) return;
      const pcm16 = float32ToPCM16(e.data as Float32Array);
      wsRef.current.send(JSON.stringify({
        message_type: 'input_audio_chunk',
        audio_base_64: pcm16ToBase64(pcm16),
        commit: false,
        sample_rate: TARGET_SAMPLE_RATE,
      }));
    };

    source.connect(workletNode);
    workletNode.connect(audioCtx.destination);
    setStatusBoth('recording');
  }, [setStatusBoth]);

  // ── Disconnect worklet only (WS + stream stay alive for next turn) ────────
  const pauseAudio = useCallback(() => {
    workletNodeRef.current?.disconnect();
    workletNodeRef.current = null;
  }, []);

  // ── start: establish connection once, then just resumes audio each turn ───
  const start = useCallback(async (onCommitted: (text: string) => void) => {
    // Always update the callback (called even when already recording, to stay fresh)
    onCommittedRef.current = onCommitted;

    const s = statusRef.current;
    if (s === 'recording' || s === 'connecting') return;

    // Fast path: WS is still alive from a previous turn — just restart audio
    if (
      wsRef.current?.readyState === WebSocket.OPEN &&
      audioCtxRef.current &&
      audioSourceRef.current
    ) {
      await resumeAudio();
      return;
    }

    setError('');
    setStatusBoth('connecting');

    try {
      // 1. Single-use auth token
      const tokenRes = await fetch('/api/stt-token', { method: 'POST' });
      const tokenJson = await tokenRes.json();
      if (!tokenJson.success) {
        setError(tokenJson.error ?? 'STT not configured');
        setStatusBoth('error');
        return;
      }

      // 2. WebSocket to ElevenLabs
      const lang = locale === 'ja' ? 'ja' : 'en';
      const url = [
        `wss://api.elevenlabs.io/v1/speech-to-text/realtime`,
        `?token=${tokenJson.token}`,
        `&model_id=scribe_v2_realtime`,
        `&audio_format=pcm_16000`,
        `&commit_strategy=vad`,
        `&language_code=${lang}`,
        `&vad_silence_threshold_secs=1.0`,
      ].join('');

      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data as string);
          if (msg.message_type === 'partial_transcript') {
            setInterimText(msg.text ?? '');
          } else if (msg.message_type === 'committed_transcript') {
            const text = (msg.text ?? '').trim();
            setInterimText('');
            if (text) {
              onCommittedRef.current?.(text);
              // Pause audio capture; WS stays alive for the next turn
              pauseAudio();
              setStatusBoth('idle');
            }
          } else if (msg.message_type?.includes('error') || msg.message_type === 'auth_error') {
            setError(msg.error ?? 'WebSocket error');
            stopAll();
            setStatusBoth('error');
          }
        } catch { /* ignore parse errors */ }
      };

      ws.onerror = () => {
        setError('Connection failed');
        stopAll();
        setStatusBoth('error');
      };

      ws.onclose = (e) => {
        if (e.code !== 1000) {
          stopAll();
        }
      };

      await new Promise<void>((resolve, reject) => {
        ws.onopen = () => resolve();
        setTimeout(() => reject(new Error('Connection timeout')), 5000);
      });

      // 3. Microphone
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      // AudioContext at target rate — browser handles mic resampling
      const audioCtx = new AudioContext({ sampleRate: TARGET_SAMPLE_RATE });
      audioCtxRef.current = audioCtx;

      const source = audioCtx.createMediaStreamSource(stream);
      audioSourceRef.current = source;

      // 4. AudioWorklet (loads module + starts streaming)
      await resumeAudio();

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      setError(msg);
      stopAll();
      setStatusBoth('error');
    }
  }, [locale, pauseAudio, resumeAudio, stopAll, setStatusBoth]);

  const stop = useCallback(() => {
    stopAll();
  }, [stopAll]);

  return { status, interimText, error, start, stop };
}
