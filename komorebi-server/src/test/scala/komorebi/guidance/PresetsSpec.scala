package komorebi.guidance

import munit.FunSuite
import komorebi.domain.*

class PresetsSpec extends FunSuite:

  test("getPreset returns non-empty for all mode/duration/locale combinations"):
    for
      mode     <- GuidanceMode.values
      duration <- GuidanceDuration.all
      locale   <- List("en", "ja")
    do
      val text = Presets.getPreset(mode, duration, locale)
      assert(text.nonEmpty, s"Empty preset for mode=$mode, duration=$duration, locale=$locale")

  test("abort is always preset"):
    assert(Presets.isAlwaysPreset(GuidanceMode.Abort))

  test("reset is always preset"):
    assert(Presets.isAlwaysPreset(GuidanceMode.Reset))

  test("breath is not always preset"):
    assert(!Presets.isAlwaysPreset(GuidanceMode.Breath))

  test("sound is not always preset"):
    assert(!Presets.isAlwaysPreset(GuidanceMode.Sound))

  test("unknown locale falls back to English"):
    val en = Presets.getPreset(GuidanceMode.Breath, GuidanceDuration.Thirty, "en")
    val unknown = Presets.getPreset(GuidanceMode.Breath, GuidanceDuration.Thirty, "fr")
    assertEquals(unknown, en)

  test("Japanese presets are different from English"):
    val en = Presets.getPreset(GuidanceMode.Breath, GuidanceDuration.Sixty, "en")
    val ja = Presets.getPreset(GuidanceMode.Breath, GuidanceDuration.Sixty, "ja")
    assertNotEquals(en, ja)
