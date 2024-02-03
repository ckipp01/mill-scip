import $ivy.`com.goyeau::mill-scalafix::0.3.2`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`

import mill._
import scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.api.ZincWorkerUtil._
import mill.contrib.buildinfo.BuildInfo

import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.integrationtest._
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost

val millVersions = Seq("0.10.15", "0.11.6")
val millBinaryVersions = millVersions.map(scalaNativeBinaryVersion)
val artifactBase = "mill-scip"
val scala213 = "2.13.12"
val semanticdb = "4.8.11"
val semanticdbJava = "0.9.9"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

def millVersion(binaryVersion: String) =
  millVersions.find(v => millBinaryVersion(v) == binaryVersion).get

object plugin extends Cross[Plugin](millBinaryVersions)
trait Plugin
    extends Cross.Module[String]
    with ScalaModule
    with CiReleaseModule
    with BuildInfo
    with ScalafixModule
    with ScalafmtModule {

  override def scalaVersion = scala213

  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  override def sources = T.sources {
    super.sources() ++ Seq(
      millSourcePath / s"src-mill${millVersion(crossValue).split('.').take(2).mkString(".")}"
    ).map(PathRef(_))
  }

  override def scalafixScalaBinaryVersion =
    ZincWorkerUtil.scalaBinaryVersion(scala213)

  override def artifactName =
    s"${artifactBase}_mill${crossValue}"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion(crossValue)}"
  )

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.sourcegraph::scip-java:$semanticdbJava"
  )

  override def buildInfoMembers = Seq(
    BuildInfo.Value("semanticDBVersion", semanticdb),
    BuildInfo.Value("semanticDBJavaVersion", semanticdbJava)
  )

  override def buildInfoObjectName = "ScipBuildInfo"

  override def buildInfoPackageName = "io.kipp.mill.scip"

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

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(ivy"org.scalameta::munit:1.0.0-M11")
  }
}

object itest extends Cross[ItestCross](millVersions)
trait ItestCross extends Cross.Module[String] with MillIntegrationTestModule {

  def millTestVersion = crossValue

  def pluginsUnderTest = Seq(plugin(millBinaryVersion(crossValue)))

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
