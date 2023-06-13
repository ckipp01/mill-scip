package io.kipp.mill.scip

import scala.util.Try

object MillUtils {
  private[scip] def canHandleJava(millVersion: String) = {
    Try(millVersion.split('.') match {
      case Array(major, minor, patch)
          if (major.toInt > 0) || (minor.toInt > 10) || (major.toInt > 0 || (minor.toInt >= 10 && patch.toInt >= 6)) =>
        true
      case _ => false
    }).getOrElse(true)
  }

}
