package io.kipp.mill.scip

import munit.FunSuite

class MillUtilsSpec extends FunSuite {

  val versions = Map(
    "nonsense" -> false,
    "0.9.3-SNAPSHOT" -> false,
    "0.9.7" -> false,
    "0.10.5" -> false,
    "0.10.6" -> true,
    "0.11.8" -> true,
    "1.3.8" -> true
  )

  for ((version, expected) <- versions)
    test(version) {
      assertEquals(MillUtils.canHandleJava(version), expected)
    }
}
