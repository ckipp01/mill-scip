import mill._, scalalib._
import $exec.plugins
import io.kipp.mill.scip.Scip
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "2.13.8"

  object test extends Tests
}

object minimalThree extends ScalaModule {
  def scalaVersion = "3.1.3"
}

def generate(ev: Evaluator) = T.command {
  val scipFile = Scip.generate(ev)()

  val classpathFile = (scipFile / os.up / "javacopts.txt")

  // We make sure that the javacopts.txt file was also created
  assertEquals(os.exists(classpathFile), true)

  val firstLine = os.read(classpathFile).takeWhile(_ != '\n')

  // Ensure that the contents of this file were actually created correctly starting with the first line being -classpath
  assertEquals(firstLine, "-classpath")

  val semanticdbFiles = os.walk(os.pwd / "out").filter(_.ext == "semanticdb")
  // We should find 3 different semanticdb files to show that it's working in a
  // normal Scala 2 module, the test module, and the Scala 3 module.
  assertEquals(semanticdbFiles.size, 3)
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
