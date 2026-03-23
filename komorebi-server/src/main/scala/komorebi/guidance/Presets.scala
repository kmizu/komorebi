package komorebi.guidance

import komorebi.domain.*

object Presets:

  private type PresetMap = Map[GuidanceDuration, String]
  private type LocalePresets = Map[GuidanceMode, PresetMap]

  private val EN: LocalePresets = Map(
    GuidanceMode.Abort -> Map(
      GuidanceDuration.Thirty -> "That's enough. You can stop now. There's no reason to continue. Open your eyes and look around the room. Notice where you are. This session is done.",
      GuidanceDuration.Sixty -> "Let's stop here. There's no benefit in pushing through when it doesn't feel right. Open your eyes. Look at something in the room — a wall, a floor, a window. You've done enough.",
      GuidanceDuration.OneEighty -> "Let's stop here. There's no benefit in pushing through when it doesn't feel right. Open your eyes. Look at something in the room — a wall, a floor, a window. You've done enough.",
    ),
    GuidanceMode.Reset -> Map(
      GuidanceDuration.Thirty -> "Look at something in the room. Any surface or shape. Just look at it for a few seconds. That's the whole thing.",
      GuidanceDuration.Sixty -> "Place both feet on the floor. Feel the weight of them. Now look at one thing in front of you — it can be anything. Stay with it for thirty seconds. You're done after that.",
      GuidanceDuration.OneEighty -> "Place both feet on the floor. Feel where they press against the ground. Look at something in the room — a shape, a surface, a color. Stay there for a minute. When your attention moves, just come back to looking. After that, notice any sounds in the room. Whatever is already there. No need to find anything special. That's the session.",
    ),
    GuidanceMode.External -> Map(
      GuidanceDuration.Thirty -> "Look at five things you can see right now. Name them silently. Shape, color, texture. That's all.",
      GuidanceDuration.Sixty -> "Look around the room. Find one object and stay with it. Notice its color, its edges, how light falls on it. When your attention drifts, come back to looking. One minute of this.",
      GuidanceDuration.OneEighty -> "Look around and find something to rest your eyes on. It can be anything — a surface, a corner, a window. Stay with it. Notice color, texture, shadow. After a minute, expand your awareness. What sounds are already in the room? Not searching — just noticing what's already there. When something pulls your attention inward, gently return to looking outward. Three minutes of this. You can stop anytime.",
    ),
    GuidanceMode.Sound -> Map(
      GuidanceDuration.Thirty -> "Close your eyes or lower your gaze. Just listen to what's already in the room. Don't name the sounds. Just notice they're there. Thirty seconds.",
      GuidanceDuration.Sixty -> "Lower your gaze or close your eyes. Let sound come to you — don't search for it. Whatever's already in the room. Cars, air, hum of something. When your mind wanders, return to listening. One minute.",
      GuidanceDuration.OneEighty -> "Lower your gaze or close your eyes. Listen to whatever sound is already present. Near sounds, distant sounds. Don't analyze them. Let them come and go. If a thought appears, notice it and return to listening. Three minutes of this. You can stop earlier if you need to.",
    ),
    GuidanceMode.Body -> Map(
      GuidanceDuration.Thirty -> "Notice where your body is in contact with the chair or floor. Just the pressure and weight. Thirty seconds of that.",
      GuidanceDuration.Sixty -> "Feel where your body meets the surface you're sitting on. The weight of your hands in your lap. The pressure of your feet on the floor. Stay with those sensations. One minute.",
      GuidanceDuration.OneEighty -> "Bring your attention to the weight and pressure of your body. Start with your feet on the floor. Then your seat on the chair. Then your hands — where they rest, what they touch. Move slowly. No need to relax or adjust anything. Just notice. Three minutes.",
    ),
    GuidanceMode.Breath -> Map(
      GuidanceDuration.Thirty -> "Notice your breath without changing it. The air coming in, the air going out. Thirty seconds.",
      GuidanceDuration.Sixty -> "Let your breath happen naturally. Notice the movement — chest, belly, or just the air at the nostrils. No need to deepen or control it. When your mind wanders, come back. One minute.",
      GuidanceDuration.OneEighty -> "Bring your attention to breathing. Let it happen without shaping it. Notice where you feel it most — chest, belly, nose. When thoughts arise, just come back to the breath. No need to push thoughts away. Three minutes. You can stop earlier.",
    ),
  )

  private val JA: LocalePresets = Map(
    GuidanceMode.Abort -> Map(
      GuidanceDuration.Thirty -> "もう十分です。今すぐ止めていいです。続ける理由はありません。目を開けて、部屋を見回してください。自分がどこにいるか確認してください。このセッションは終わりです。",
      GuidanceDuration.Sixty -> "ここで止めましょう。しっくりこないのに無理に続けても意味がありません。目を開けて、部屋の何かを見てください — 壁でも、床でも、窓でも。十分にやりました。",
      GuidanceDuration.OneEighty -> "ここで止めましょう。しっくりこないのに無理に続けても意味がありません。目を開けて、部屋の何かを見てください — 壁でも、床でも、窓でも。十分にやりました。",
    ),
    GuidanceMode.Reset -> Map(
      GuidanceDuration.Thirty -> "部屋の何かを見てください。どんな表面や形でも。ただ数秒間見てください。それだけです。",
      GuidanceDuration.Sixty -> "両足を床につけてください。その重さを感じてください。次に、目の前の何か一つを見てください — 何でも構いません。30秒間そこに留まってください。それで終わりです。",
      GuidanceDuration.OneEighty -> "両足を床につけて、地面に押しつけられている感覚を感じてください。部屋の何か — 形、表面、色 — を見てください。1分間そこに留まってください。注意がそれたら、また見ることに戻ってください。その後、部屋の音に気づいてください。すでにそこにあるものを。特別なものを探す必要はありません。それがこのセッションです。",
    ),
    GuidanceMode.External -> Map(
      GuidanceDuration.Thirty -> "今見えるものを5つ見てください。形、色、質感を心の中で言ってください。それだけです。",
      GuidanceDuration.Sixty -> "部屋を見回して、一つの物を見つけてそこに留まってください。色、輪郭、光の当たり方に気づいてください。注意がそれたら、見ることに戻ってください。1分間。",
      GuidanceDuration.OneEighty -> "目を休ませる場所を見つけてください。何でも構いません — 表面、角、窓。そこに留まってください。色、質感、影に気づいてください。1分後、気づきを広げてください。部屋にはどんな音がありますか？探さなくていい — ただ、すでにそこにあるものに気づくだけ。内側に引っ張られたら、ゆっくりと外を見ることに戻ってください。3分間。いつでも止めていいです。",
    ),
    GuidanceMode.Sound -> Map(
      GuidanceDuration.Thirty -> "目を閉じるか、視線を下げてください。部屋にすでにある音をただ聞いてください。音に名前をつけないでください。ただそこにあることに気づくだけ。30秒。",
      GuidanceDuration.Sixty -> "視線を下げるか目を閉じてください。音が自然に届くのを待ってください — 探さなくていい。部屋にすでにあるもの。車の音、空気の音、何かの低音。心がさまよったら、聞くことに戻ってください。1分。",
      GuidanceDuration.OneEighty -> "視線を下げるか目を閉じてください。すでにある音を聞いてください。近い音、遠い音。分析しなくていい。来ては消えるままにしてください。考えが浮かんだら、それに気づいて聞くことに戻ってください。3分間。必要なら早めに止めていいです。",
    ),
    GuidanceMode.Body -> Map(
      GuidanceDuration.Thirty -> "体が椅子や床に触れている部分に気づいてください。圧力と重さだけ。30秒。",
      GuidanceDuration.Sixty -> "体が座っている表面とどこで接しているか感じてください。膝の上の手の重さ。床に押しつけられている足の圧力。その感覚に留まってください。1分。",
      GuidanceDuration.OneEighty -> "体の重さと圧力に注意を向けてください。床の上の足から始めてください。次に椅子の上のお尻。次に手 — どこに置かれているか、何に触れているか。ゆっくり進んでください。リラックスしたり調整したりする必要はありません。ただ気づくだけ。3分。",
    ),
    GuidanceMode.Breath -> Map(
      GuidanceDuration.Thirty -> "変えずに呼吸に気づいてください。入ってくる空気、出ていく空気。30秒。",
      GuidanceDuration.Sixty -> "呼吸を自然にさせてください。動き — 胸、お腹、または鼻の空気 — に気づいてください。深くしたりコントロールしたりする必要はありません。心がさまよったら戻ってください。1分。",
      GuidanceDuration.OneEighty -> "呼吸に注意を向けてください。形を変えずにそのままにしてください。どこで一番感じるか — 胸、お腹、鼻。考えが浮かんだら、呼吸に戻ってください。追い払う必要はありません。3分。早めに止めていいです。",
    ),
  )

  private val AllPresets: Map[String, LocalePresets] = Map("en" -> EN, "ja" -> JA)

  private val FallbackText: String =
    "Look at five things you can see right now. Name them silently. Shape, color, texture. That's all."

  def getPreset(mode: GuidanceMode, duration: GuidanceDuration, locale: String = "en"): String =
    val localePresets = AllPresets.getOrElse(locale, EN)
    val modeMap = localePresets.getOrElse(mode, EN.getOrElse(mode, Map.empty))
    modeMap.getOrElse(duration,
      modeMap.getOrElse(GuidanceDuration.Sixty,
        modeMap.getOrElse(GuidanceDuration.Thirty, FallbackText)))

  def isAlwaysPreset(mode: GuidanceMode): Boolean =
    mode == GuidanceMode.Abort || mode == GuidanceMode.Reset
