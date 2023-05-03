package io.kipp.mill.scip

import mill._
import mill.eval.Evaluator
import mill.scalalib.Dep
import mill.scalalib.JavaModule
import mill.scalalib.ScalaModule

/** Depending on the type of module we'll need to fetch the semanticDB compiler
  * plugins. This object just provides helpers to fetch the correct ones.
  */
object SemanticdbFetcher {

  /** Fetch the semanticDB paths necessary for ScalaModule.
    */
  private[scip] def getSemanticdbPaths(
      ev: Evaluator,
      sm: ScalaModule
  ) = {

    val version = ScipBuildInfo.semanticDBVersion
    val millDep =
      Dep.parse(s"org.scalameta:::semanticdb-scalac:${version}")

    val semanticdbPaths = Evaluator
      .evalOrThrow(ev)(
        sm.resolveDeps(T.task(Agg(millDep).map(sm.bindDependency())))
      )

    semanticdbPaths
  }

  /** Fetch the semanticDB paths necessary for a given JavaModule.
    */
  private[scip] def getSemanticdbPaths(
      ev: Evaluator,
      jm: JavaModule
  ) = {
    val version = ScipBuildInfo.semanticDBJavaVersion

    val millDep = Dep.parse(s"com.sourcegraph:semanticdb-javac:${version}")

    val semanticdbPaths = Evaluator
      .evalOrThrow(ev)(
        jm.resolveDeps(T.task(Agg(millDep).map(jm.bindDependency())))
      )

    semanticdbPaths
  }
}
