import mill._
import scalalib._
import publish._
import scalafmt._
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import mill.contrib.buildinfo.BuildInfo
import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.6.1`
import de.tobiasroeser.mill.integrationtest._
import mill.scalalib.api.ZincWorkerUtil
import scala.util.Try

val millVersion = "0.10.3"
val artifactBase = "mill-scip"
val scala213 = "2.13.8"
// TODO figure out where to put these 2 so that Steward can update them
val semanticdb = "4.5.11"
val semanticdbJava = "0.8.2"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

object plugin
    extends ScalaModule
    with PublishModule
    with BuildInfo
    with ScalafixModule
    with ScalafmtModule {

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

  override def scalacOptions =
    super.scalacOptions() ++ Seq("-Ywarn-unused", "-deprecation")

  override def scalafixIvyDeps = Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0"
  )

  override def buildInfoMembers = Map(
    "semanticDBVersion" -> semanticdb,
    "semanticDBJavaVersion" -> semanticdbJava,
    "version" -> publishVersion()
  )

  override def buildInfoObjectName = "ScipBuildInfo"

  override def buildInfoPackageName = Some(
    "io.kipp.mill.scip"
  )

  override def publishVersion = VcsVersion
    .vcsState()
    .format(tagModifier = {
      case t if t.startsWith("v") && Try(t.substring(1, 2).toInt).isSuccess =>
        t.substring(1)
      case t => t
    })

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
}

object itest extends MillIntegrationTestModule {

  def millTestVersion = millVersion

  def pluginsUnderTest = Seq(plugin)

  def testBase = millSourcePath / "src"

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    T {
      Seq(
        PathRef(testBase / "minimal") -> Seq(
          TestInvocation.Targets(Seq("generate"), noServer = true)
        )
      )
    }
}
