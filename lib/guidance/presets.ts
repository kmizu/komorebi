import type { GuidanceMode, GuidanceDuration } from '@/lib/types';

type PresetMap = Partial<Record<GuidanceDuration, string>>;

export const PRESETS: Record<string, PresetMap> = {
  abort: {
    30: "That's enough. You can stop now. There's no reason to continue. Open your eyes and look around the room. Notice where you are. This session is done.",
    60: "Let's stop here. There's no benefit in pushing through when it doesn't feel right. Open your eyes. Look at something in the room — a wall, a floor, a window. You've done enough.",
    180: "Let's stop here. There's no benefit in pushing through when it doesn't feel right. Open your eyes. Look at something in the room — a wall, a floor, a window. You've done enough.",
  },

  reset: {
    30: "Look at something in the room. Any surface or shape. Just look at it for a few seconds. That's the whole thing.",
    60: "Place both feet on the floor. Feel the weight of them. Now look at one thing in front of you — it can be anything. Stay with it for thirty seconds. You're done after that.",
    180: "Place both feet on the floor. Feel where they press against the ground. Look at something in the room — a shape, a surface, a color. Stay there for a minute. When your attention moves, just come back to looking. After that, notice any sounds in the room. Whatever is already there. No need to find anything special. That's the session.",
  },

  external: {
    30: "Look at five things you can see right now. Name them silently. Shape, color, texture. That's all.",
    60: "Look around the room. Find one object and stay with it. Notice its color, its edges, how light falls on it. When your attention drifts, come back to looking. One minute of this.",
    180: "Look around and find something to rest your eyes on. It can be anything — a surface, a corner, a window. Stay with it. Notice color, texture, shadow. After a minute, expand your awareness. What sounds are already in the room? Not searching — just noticing what's already there. When something pulls your attention inward, gently return to looking outward. Three minutes of this. You can stop anytime.",
  },

  sound: {
    30: "Close your eyes or lower your gaze. Just listen to what's already in the room. Don't name the sounds. Just notice they're there. Thirty seconds.",
    60: "Lower your gaze or close your eyes. Let sound come to you — don't search for it. Whatever's already in the room. Cars, air, hum of something. When your mind wanders, return to listening. One minute.",
    180: "Lower your gaze or close your eyes. Listen to whatever sound is already present. Near sounds, distant sounds. Don't analyze them. Let them come and go. If a thought appears, notice it and return to listening. Three minutes of this. You can stop earlier if you need to.",
  },

  body: {
    30: "Notice where your body is in contact with the chair or floor. Just the pressure and weight. Thirty seconds of that.",
    60: "Feel where your body meets the surface you're sitting on. The weight of your hands in your lap. The pressure of your feet on the floor. Stay with those sensations. One minute.",
    180: "Bring your attention to the weight and pressure of your body. Start with your feet on the floor. Then your seat on the chair. Then your hands — where they rest, what they touch. Move slowly. No need to relax or adjust anything. Just notice. Three minutes.",
  },

  breath: {
    30: "Notice your breath without changing it. The air coming in, the air going out. Thirty seconds.",
    60: "Let your breath happen naturally. Notice the movement — chest, belly, or just the air at the nostrils. No need to deepen or control it. When your mind wanders, come back. One minute.",
    180: "Bring your attention to breathing. Let it happen without shaping it. Notice where you feel it most — chest, belly, nose. When thoughts arise, just come back to the breath. No need to push thoughts away. Three minutes. You can stop earlier.",
  },
};

export function getPreset(mode: GuidanceMode, duration: GuidanceDuration): string {
  const map = PRESETS[mode];
  if (!map) return PRESETS['external'][30]!;
  return map[duration] ?? map[60] ?? map[30] ?? PRESETS['external'][30]!;
}

export function isAlwaysPreset(mode: GuidanceMode): boolean {
  return mode === 'abort' || mode === 'reset';
}
