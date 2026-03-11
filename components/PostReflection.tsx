'use client';

import { useState } from 'react';
import type { PostOutcome } from '@/lib/types';

interface PostReflectionProps {
  sessionId: string;
  onSubmit: (sessionId: string, outcome: PostOutcome) => Promise<void>;
  loading?: boolean;
}

export function PostReflection({ sessionId, onSubmit, loading }: PostReflectionProps) {
  const [feltBetter, setFeltBetter] = useState<boolean | null>(null);
  const [wouldContinue, setWouldContinue] = useState<boolean | null>(null);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (feltBetter === null || wouldContinue === null) {
      setError('Please answer both questions.');
      return;
    }
    setError('');
    try {
      await onSubmit(sessionId, {
        feltBetter,
        wouldContinue,
        notes: notes.trim() || undefined,
      });
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-7 max-w-lg mx-auto">
      <div>
        <p className="text-sm text-stone-600 mb-2">Did the practice reduce pressure or add to it?</p>
        <div className="flex gap-3">
          {([
            { value: true, label: 'Reduced it' },
            { value: false, label: 'Added to it' },
          ] as const).map(({ value, label }) => (
            <button
              key={String(value)}
              type="button"
              onClick={() => setFeltBetter(value)}
              className={`flex-1 py-2 text-sm rounded transition-colors ${
                feltBetter === value
                  ? 'bg-stone-700 text-white'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div>
        <p className="text-sm text-stone-600 mb-2">Would you practice again today?</p>
        <div className="flex gap-3">
          {([
            { value: true, label: 'Yes' },
            { value: false, label: 'No' },
          ] as const).map(({ value, label }) => (
            <button
              key={String(value)}
              type="button"
              onClick={() => setWouldContinue(value)}
              className={`flex-1 py-2 text-sm rounded transition-colors ${
                wouldContinue === value
                  ? 'bg-stone-700 text-white'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div>
        <p className="text-sm text-stone-600 mb-2">
          Anything to remember for next time?{' '}
          <span className="text-stone-400">(optional)</span>
        </p>
        <textarea
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="..."
          rows={2}
          maxLength={400}
          className="w-full px-3 py-2 text-sm text-stone-700 bg-stone-50 border border-stone-200 rounded resize-none focus:outline-none focus:border-stone-400 placeholder:text-stone-300"
        />
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <button
        type="submit"
        disabled={loading}
        className="w-full py-3 text-sm text-white bg-stone-700 rounded hover:bg-stone-800 disabled:opacity-50 transition-colors"
      >
        {loading ? 'Saving...' : 'Done'}
      </button>
    </form>
  );
}
