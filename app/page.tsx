import Link from "next/link";
import { isLLMConfigured } from "@/lib/llm/client";
import { isTTSConfigured } from "@/lib/tts/client";

export default function Home() {
  const llmOk = isLLMConfigured();
  const ttsOk = isTTSConfigured();

  return (
    <div className="space-y-12">
      <div className="space-y-4">
        <h1 className="text-2xl font-light text-stone-800">Mindfulness Supervisor</h1>
        <p className="text-stone-500 leading-relaxed max-w-md">
          Not a mindfulness teacher. A monitor that checks whether your practice
          is working for you — and suggests stopping or switching when it isn&apos;t.
        </p>
      </div>

      <div className="space-y-3">
        <p className="text-sm font-medium text-stone-600">What it watches for</p>
        <ul className="space-y-1.5 text-sm text-stone-500">
          <li>Trying too hard</li>
          <li>Turning acceptance into an obligation</li>
          <li>Monitoring yourself instead of resting</li>
          <li>Treating the session as a performance</li>
          <li>Continuing when it is making things worse</li>
        </ul>
      </div>

      <div className="space-y-3">
        <p className="text-sm font-medium text-stone-600">What it does not do</p>
        <ul className="space-y-1.5 text-sm text-stone-500">
          <li>No streaks. No scores. No achievements.</li>
          <li>No promises about how you will feel.</li>
          <li>No spiritual language.</li>
          <li>No pushing you to meditate longer.</li>
        </ul>
      </div>

      <div>
        <Link
          href="/session"
          className="inline-block px-6 py-3 text-sm text-white bg-stone-700 rounded hover:bg-stone-800 transition-colors"
        >
          Start a session
        </Link>
      </div>

      <div className="border-t border-stone-100 pt-6 space-y-1">
        <p className="text-xs text-stone-400">
          AI supervision: {llmOk ? "active (Anthropic)" : "rule-based only — add ANTHROPIC_API_KEY to .env.local"}
        </p>
        <p className="text-xs text-stone-400">
          Voice: {ttsOk ? "available (ElevenLabs)" : "text only — add ELEVENLABS_API_KEY to .env.local"}
        </p>
      </div>
    </div>
  );
}
