import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.6.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.1`
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.1`

import mill._
import scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.ZincWorkerUtil

import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import io.kipp.mill.ci.release.CiReleaseModule

val millVersion = "0.10.0"
val artifactBase = "mill-scip"
val scala213 = "2.13.8"
val semanticdb = "4.5.13"
val semanticdbJava = "0.8.7"

// We temporarily test against 0.10.7 as well before we have a 0.11.x just
// to ensure that on that version we can also get semanticDB produced for
// Java Modules.
val millTestVersions = Seq(millVersion, "0.10.7")

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

object plugin
    extends ScalaModule
    with CiReleaseModule
    with BuildInfo
    with ScalafixModule
    with ScalafmtModule
    with TpolecatModule {

  override def scalaVersion = scala213

  override def scalafixScalaBinaryVersion =
    ZincWorkerUtil.scalaBinaryVersion(scala213)

  override def artifactName =
    s"${artifactBase}_mill${millBinaryVersion(millVersion)}"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:$millVersion"
  )

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.sourcegraph::scip-java:$semanticdbJava"
  )

  override def scalafixIvyDeps = Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0"
  )

  override def buildInfoMembers = Map(
    "semanticDBVersion" -> semanticdb,
    "semanticDBJavaVersion" -> semanticdbJava
  )

  override def buildInfoObjectName = "ScipBuildInfo"

  override def buildInfoPackageName = Some(
    "io.kipp.mill.scip"
  )

  override def pomSettings = PomSettings(
    description = "Generate SCIP for your Mill build.",
    organization = "io.chris-kipp",
    url = "https://github.com/ckipp01/mill-scip",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl
      .github(owner = "ckipp01", repo = "mill-scip"),
    developers =
      Seq(Developer("ckipp01", "Chris Kipp", "https://www.chris-kipp.io"))
  )

  override def sonatypeUri = "https://s01.oss.sonatype.org/service/local"
  override def sonatypeSnapshotUri =
    "https://s01.oss.sonatype.org/content/repositories/snapshots"

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(ivy"org.scalameta::munit:1.0.0-M6")
  }
}

object itest extends Cross[ItestCross](millTestVersions: _*)
class ItestCross(testVersion: String) extends MillIntegrationTestModule {

  def millTestVersion = testVersion

  def pluginsUnderTest = Seq(plugin)

  override def millSourcePath = super.millSourcePath / os.up

  def testBase = millSourcePath / "src"

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    T {
      Seq(
        PathRef(testBase / "minimal") -> Seq(
          TestInvocation.Targets(Seq("generate"), noServer = true),
          TestInvocation.Targets(Seq("lsif"), noServer = true),
          TestInvocation.Targets(
            Seq("fail"),
            noServer = true,
            expectedExitCode = 1
          )
        )
      )
    }
}
