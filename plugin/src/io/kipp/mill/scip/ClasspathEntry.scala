package io.kipp.mill.scip

import com.sourcegraph.scip_java.buildtools

import java.nio.file.Files
import java.nio.file.Path

/** Helpers to calculate the classpath entries needed.
  */
object ClasspathEntry {

  /** Tries to parse a ClasspathEntry from the POM file that lies next to the
    * given jar file.
    *
    * Taken from:
    * https://github.com/sourcegraph/scip-java/blob/9648fa9b2f0fc676aafa54c84342375666ce682b/scip-java/src/main/scala/com/sourcegraph/scip_java/buildtools/ClasspathEntry.scala#L102-L131
    */
  private[scip] def fromPom(jar: Path): Option[buildtools.ClasspathEntry] = {
    val pom = jar
      .resolveSibling(jar.getFileName.toString.stripSuffix(".jar") + ".pom")
    val sources = Option(
      jar.resolveSibling(
        jar.getFileName.toString.stripSuffix(".jar") + ".sources"
      )
    ).filter(Files.isRegularFile(_))
    if (Files.isRegularFile(pom)) {
      val xml = scala.xml.XML.loadFile(pom.toFile)
      def xmlValue(key: String): String = {
        val node = xml \ key
        if (node.isEmpty)
          (xml \ "parent" \ key).text
        else
          node.text
      }.trim
      val groupId = xmlValue("groupId")
      val artifactId = xmlValue("artifactId")
      val version = xmlValue("version")
      Some(
        buildtools.ClasspathEntry(jar, sources, groupId, artifactId, version)
      )
    } else {
      None
    }
  }
}
