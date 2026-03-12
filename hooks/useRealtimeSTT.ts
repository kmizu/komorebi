'use client';

import { useState, useRef, useCallback } from 'react';

const TARGET_SAMPLE_RATE = 16000;
const BUFFER_SIZE = 4096;

function float32ToPCM16(input: Float32Array): Int16Array {
  const output = new Int16Array(input.length);
  for (let i = 0; i < input.length; i++) {
    const s = Math.max(-1, Math.min(1, input[i]));
    output[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
  }
  return output;
}

function downsample(pcm: Int16Array, fromRate: number, toRate: number): Int16Array {
  if (fromRate === toRate) return pcm;
  const ratio = fromRate / toRate;
  const result = new Int16Array(Math.floor(pcm.length / ratio));
  for (let i = 0; i < result.length; i++) {
    result[i] = pcm[Math.floor(i * ratio)];
  }
  return result;
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

  const wsRef = useRef<WebSocket | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const processorRef = useRef<ScriptProcessorNode | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const cleanup = useCallback(() => {
    processorRef.current?.disconnect();
    processorRef.current = null;
    audioCtxRef.current?.close();
    audioCtxRef.current = null;
    streamRef.current?.getTracks().forEach(t => t.stop());
    streamRef.current = null;
    wsRef.current?.close();
    wsRef.current = null;
    setStatus('idle');
    setInterimText('');
  }, []);

  const start = useCallback(async (onCommitted: (text: string) => void) => {
    setError('');
    setStatus('connecting');

    try {
      // 1. Get single-use token from our server
      const tokenRes = await fetch('/api/stt-token', { method: 'POST' });
      const tokenJson = await tokenRes.json();
      if (!tokenJson.success) {
        setError(tokenJson.error ?? 'STT not configured');
        setStatus('error');
        return;
      }
      const { token } = tokenJson;

      // 2. Open WebSocket to ElevenLabs
      const lang = locale === 'ja' ? 'ja' : 'en';
      const url = [
        `wss://api.elevenlabs.io/v1/speech-to-text/realtime`,
        `?token=${token}`,
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
              onCommitted(text);
              cleanup();
            }
          } else if (msg.message_type?.includes('error') || msg.message_type === 'auth_error') {
            setError(msg.error ?? 'WebSocket error');
            cleanup();
            setStatus('error');
          }
        } catch { /* ignore parse errors */ }
      };

      ws.onerror = () => {
        setError('Connection failed');
        cleanup();
        setStatus('error');
      };

      ws.onclose = (e) => {
        if (e.code !== 1000) {
          setStatus('idle');
          setInterimText('');
        }
      };

      await new Promise<void>((resolve, reject) => {
        ws.onopen = () => resolve();
        setTimeout(() => reject(new Error('Connection timeout')), 5000);
      });

      // 3. Capture microphone
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      const audioCtx = new AudioContext({ sampleRate: TARGET_SAMPLE_RATE });
      audioCtxRef.current = audioCtx;

      const source = audioCtx.createMediaStreamSource(stream);
      const processor = audioCtx.createScriptProcessor(BUFFER_SIZE, 1, 1);
      processorRef.current = processor;

      processor.onaudioprocess = (e) => {
        if (wsRef.current?.readyState !== WebSocket.OPEN) return;
        const float32 = e.inputBuffer.getChannelData(0);
        const pcm16 = float32ToPCM16(float32);
        const resampled = downsample(pcm16, audioCtx.sampleRate, TARGET_SAMPLE_RATE);
        const audio_base_64 = pcm16ToBase64(resampled);

        wsRef.current.send(JSON.stringify({
          message_type: 'input_audio_chunk',
          audio_base_64,
          commit: false,
          sample_rate: TARGET_SAMPLE_RATE,
        }));
      };

      source.connect(processor);
      processor.connect(audioCtx.destination);
      setStatus('recording');

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      setError(msg);
      cleanup();
      setStatus('error');
    }
  }, [locale, cleanup]);

  const stop = useCallback(() => {
    cleanup();
  }, [cleanup]);

  return { status, interimText, error, start, stop };
}
