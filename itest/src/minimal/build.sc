import mill._, scalalib._
import $exec.plugins
import io.kipp.mill.scip.Scip
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "2.13.10"

  object test extends Tests
}

object minimalThree extends ScalaModule {
  def scalaVersion = "3.1.3"
}

object minimalJava extends JavaModule

def generate(ev: Evaluator) = T.command {
  val scipFile = Scip.generate(ev)()

  val classpathFile = (scipFile / os.up / "javacopts.txt")

  // We make sure that the javacopts.txt file was also created
  assertEquals(os.exists(classpathFile), true)

  val firstLine = os.read(classpathFile).takeWhile(_ != '\n')

  // Ensure that the contents of this file were actually created correctly starting with the first line being -classpath
  assertEquals(firstLine, "-classpath")

  val semanticdbFiles = os.walk(os.pwd / "out").filter(_.ext == "semanticdb")
  // A little hacky but we want to check if the user is on 0.10.7 or not because
  // that's when Java support was added, so if we are on that version we expect
  // 4 semanticDB documents since it includes Java, if not 3 which accounts for
  // the Scala module, the test module, and the Scala 3 module.
  val desiredSize = if (BuildInfo.millVersion == "0.10.7") 4 else 3
  assertEquals(semanticdbFiles.size, desiredSize)
  // Then we ensure that the index.scip file was actually created
  assertEquals(os.exists(scipFile), true)
}

def lsif(ev: Evaluator) = T.command {
  val scipFile = Scip.generate(ev, "dump.lsif")()
  assertEquals(os.exists(scipFile), true)
}

def fail(ev: Evaluator) = T.command {
  Scip.generate(ev, "wrong.format")
}
