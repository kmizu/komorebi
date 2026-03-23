package komorebi.agents

import munit.FunSuite

class AlignmentAgentSpec extends FunSuite:

  test("countViolations detects 'just'"):
    assertEquals(AlignmentAgent.countViolations("just breathe"), 1)

  test("countViolations detects multiple"):
    assertEquals(AlignmentAgent.countViolations("just relax and let go"), 3)

  test("countViolations is case-insensitive"):
    assertEquals(AlignmentAgent.countViolations("JUST RELAX"), 2)

  test("countViolations returns 0 for clean text"):
    assertEquals(AlignmentAgent.countViolations("Notice the weight of your hands."), 0)

  test("fixViolations removes 'just'"):
    assertEquals(AlignmentAgent.fixViolations("just notice the sound"), "notice the sound")

  test("fixViolations removes 'you should'"):
    assertEquals(AlignmentAgent.fixViolations("you should sit still"), " sit still")

  test("fixViolations removes 'try to'"):
    assertEquals(AlignmentAgent.fixViolations("try to breathe slowly"), " breathe slowly")

  test("fixViolations is case-insensitive"):
    assertEquals(AlignmentAgent.fixViolations("JUST notice"), "notice")
