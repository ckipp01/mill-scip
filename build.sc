import $ivy.`com.goyeau::mill-scalafix::0.3.0`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`

import mill._
import scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.ZincWorkerUtil

import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.integrationtest._
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost

val millVersions = Seq("0.10.12", "0.11.0")
val millBinaryVersions = millVersions.map(scalaNativeBinaryVersion)
val artifactBase = "mill-scip"
val scala213 = "2.13.10"
val semanticdb = "4.5.13"
val semanticdbJava = "0.8.21"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

def millVersion(binaryVersion: String) =
  millVersions.find(v => millBinaryVersion(v) == binaryVersion).get

object plugin extends Cross[Plugin](millBinaryVersions: _*)
class Plugin(millBinaryVersion: String)
    extends ScalaModule
    with CiReleaseModule
    with BuildInfo
    with ScalafixModule
    with ScalafmtModule {

  override def scalaVersion = scala213

  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  override def millSourcePath = super.millSourcePath / os.up

  override def sources = T.sources {
    super.sources() ++ Seq(
      millSourcePath / s"src-mill${millVersion(millBinaryVersion).split('.').take(2).mkString(".")}"
    ).map(PathRef(_))
  }

  override def scalafixScalaBinaryVersion =
    ZincWorkerUtil.scalaBinaryVersion(scala213)

  override def artifactName =
    s"${artifactBase}_mill${millBinaryVersion}"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion(millBinaryVersion)}"
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

  override def sonatypeHost: Option[SonatypeHost] = Some(SonatypeHost.s01)

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(ivy"org.scalameta::munit:1.0.0-M8")
  }
}

object itest extends Cross[ItestCross](millVersions: _*)
class ItestCross(millVersion: String) extends MillIntegrationTestModule {

  override def millSourcePath = super.millSourcePath / os.up

  def millTestVersion = millVersion

  def pluginsUnderTest = Seq(plugin(millBinaryVersion(millVersion)))

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
