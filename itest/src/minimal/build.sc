import mill._, scalalib._
import $file.plugins
import io.kipp.mill.scip.Scip
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "2.13.16"

  object test extends ScalaModuleTests with TestModule.Munit
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
  assertEquals(semanticdbFiles.size, 4)
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
