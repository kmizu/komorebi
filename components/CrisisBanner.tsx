'use client';

interface CrisisBannerProps {
  message: string;
}

export function CrisisBanner({ message }: CrisisBannerProps) {
  return (
    <div className="max-w-lg mx-auto">
      <div className="bg-stone-100 border border-stone-300 rounded-lg p-6 space-y-4">
        <p className="text-stone-800 leading-relaxed">{message}</p>

        <div className="space-y-2">
          <p className="text-sm font-medium text-stone-600">If you need to talk to someone:</p>
          <ul className="space-y-1 text-sm text-stone-600">
            <li>Crisis Text Line: Text HOME to 741741</li>
            <li>988 Suicide &amp; Crisis Lifeline: Call or text 988 (US)</li>
            <li>Samaritans (UK): 116 123</li>
            <li>Befrienders Worldwide: befrienders.org</li>
          </ul>
        </div>

        <p className="text-sm text-stone-500">
          This app is not designed for crisis situations. The people above are.
        </p>
      </div>
    </div>
  );
}
