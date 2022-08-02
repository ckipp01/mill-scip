package io.kipp.mill.scip

import com.sourcegraph.lsif_protocol.LsifToolInfo
import com.sourcegraph.scip_semanticdb.ScipOutputFormat
import com.sourcegraph.scip_semanticdb.ScipSemanticdb
import com.sourcegraph.scip_semanticdb.ScipSemanticdbOptions
import mill._
import mill.api.Logger
import mill.define.ExternalModule
import mill.eval.Evaluator
import mill.main.EvaluatorScopt
import mill.scalalib.JavaModule
import mill.scalalib.ScalaModule
import mill.scalalib.api.ZincWorkerUtil.isScala3
import mill.scalalib.internal.ModuleUtils
import os.Path

import scala.jdk.CollectionConverters._

object Scip extends ExternalModule {

  /** The main entrypoint into the external module. The process here since we
    * can't really "change" the build as an external module is that we basically
    * hijack everything necessary to compile the project, then add the necessary
    * plugin paths and scalacOptions, and then call compile. We only actually
    * compile what is needed from the previous modules and then stop after
    * producing semanticDB. Once we fully have all the semanticDB we call
    * ScipSemanticdb to slurp it all up and create a SCIP index.
    */
  def generate(ev: Evaluator) = T.command {
    val log = T.ctx().log

    val semanticdbVersion = ScipBuildInfo.semanticDBVersion

    val modules = computeModules(ev)

    log.info(s"Found ${modules.size} total modules to process.")

    modules.foreach {
      case sm: ScalaModule =>
        val (
          name,
          scalaVersion,
          scalaOrganization,
          upstreamCompileOutput,
          sources,
          compileClasspath,
          javacOptions,
          scalacOptions,
          compilerClasspath,
          scalacPluginClasspath,
          zincWorker
        ) =
          Evaluator.evalOrThrow(ev) {
            T.task {
              val name = sm.artifactId()
              val scalaVersion = sm.scalaVersion()
              val scalaOrganization = sm.scalaOrganization()
              val upstreamCompileOutput = sm.upstreamCompileOutput()
              val sources = sm.allSourceFiles().map(_.path)
              val compileClasspath = sm.compileClasspath().map(_.path)
              val javacOptions = sm.javacOptions()
              val scalacOptions = sm.allScalacOptions()

              val compilerClasspath = sm
                .scalaCompilerClasspath()
                .map(_.path)

              val scalacPluginClasspath = sm
                .scalacPluginClasspath()
                .map(_.path)

              val zincWorker = sm.zincWorker.worker()
              (
                name,
                scalaVersion,
                scalaOrganization,
                upstreamCompileOutput,
                sources,
                compileClasspath,
                javacOptions,
                scalacOptions,
                compilerClasspath,
                scalacPluginClasspath,
                zincWorker
              )

            }
          }

        val semanticdbDep: Agg[Path] =
          if (isScala3(scalaVersion)) {
            log.info("Scala 3 detected, skipping getting semanticdb plugin")
            Agg.empty
          } else {
            log.info(
              s"Ensuring everything needed for semanticdb version [${semanticdbVersion}] is available"
            )
            SemanticdbFetcher.getSemanticdbPaths(ev, sm, semanticdbVersion)
          }

        val updatedScalacOptions = if (isScala3(scalaVersion)) {
          scalacOptions ++ Seq("-Xsemanticdb", "-Ystop-after:extractSemanticDB")
        } else {
          scalacOptions.filterNot(_ == "-Xfatal-warnings") ++ semanticdbDep
            .collect {
              case jarPath if jarPath.toString.contains("semanticdb") =>
                s"-Xplugin:${jarPath}"
            } ++ Seq(
            "-Yrangepos",
            s"-P:semanticdb:sourceroot:${T.workspace}",
            // Since we've already compiled what is needed upstream, see if we
            // can shave off time at the edge modules and just stop after
            // semanticDB is produced and don't do a full compilation.
            "-Ystop-after:semanticdb-typer"
          )
        }

        val updatedScalacPluginClasspath =
          scalacPluginClasspath ++ semanticdbDep

        log.info(
          s"Generating semanticdb for [${name}]"
        )

        zincWorker.compileMixed(
          upstreamCompileOutput = upstreamCompileOutput,
          sources = sources,
          compileClasspath = compileClasspath,
          javacOptions = javacOptions,
          scalaVersion = scalaVersion,
          scalaOrganization = scalaOrganization,
          scalacOptions = updatedScalacOptions,
          compilerClasspath = compilerClasspath,
          scalacPluginClasspath = updatedScalacPluginClasspath,
          reporter = T.reporter.apply(hashCode)
        )

      case jm: JavaModule => {
        // TODO Right now there seems to be no way to use this on JDK 17. So
        // dig into the issue below before enabling Java support.
        // https://github.com/com-lihaoyi/mill/issues/1983

        val name =
          Evaluator.evalOrThrow(ev) {
            T.task {
              val name = jm.artifactName()
              // val zincWorker = jm.zincWorker.worker()
              // val upstreamCompileOutput = jm.upstreamCompileOutput()
              // val sources = jm.allSourceFiles().map(_.path)
              // val compileClasspath = jm.compileClasspath().map(_.path)
              // val javacOptions = jm.javacOptions()
              name
            }
          }

        // val updatedCompileClasspath =
        //  compileClasspath ++ SemanticdbFetcher.getSemanticdbPaths(ev, jm)

        // lazy val javacModuleOptions =
        //  List(
        //    "-J--add-exports",
        //    "-Jjdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        //    "-J--add-exports",
        //    "-Jjdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        //    "-J--add-exports",
        //    "-Jjdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        //    "-J--add-exports",
        //    "-Jjdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        //    "-J--add-exports",
        //    "-Jjdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        //  )

        // val extracJavacExports =
        //  if (Properties.isJavaAtLeast(17)) javacModuleOptions.mkString(",")
        //  else ""

        //// TODO can I also add -build-tool:mill like they do for sbt? What does this do?
        // val updatedJavacOptions = javacOptions ++ Seq(
        //  s"-Xplugin:semanticdb -sourceroot:${T.workspace} -targetroot:${T.dest}",
        //  extracJavacExports
        // )

        // zincWorker
        //  .compileJava(
        //    upstreamCompileOutput,
        //    sources,
        //    updatedCompileClasspath,
        //    updatedJavacOptions,
        //    T.reporter.apply(hashCode)
        //  )
        log.info(
          s"Skipping Java module [${name}] for now, You can track the progress of this in https://github.com/com-lihaoyi/mill/issues/1983"
        )
      }
    }

    // TODO need to generate a dependencies.txt file
    // https://sourcegraph.github.io/scip-java/docs/manual-configuration.html#step-5-optional-enable-cross-repository-navigation
    //
    //
    //
    val projects: Seq[Path] = modules
      .flatMap { module =>
        Evaluator.evalOrThrow(ev)(T.task(module.resolvedIvyDeps()))
      }
      .map(_.path)

    createScip(log, T.dest, T.workspace, projects)
    T.dest / "index.scip"
  }

  /** After all the semanticDB has been produced we can create the SCIP index
    * from it.
    *
    * @param log
    *   Logger being used in the current Task
    * @param dest
    *   The destination dir that we'll stick the index in
    * @param workspace
    *   The current workspace root
    * @param projects
    *   Full classpath of the project to be used for cross-project navigation.
    */
  private def createScip(
      log: Logger,
      dest: Path,
      workspace: Path,
      projects: Seq[Path]
  ): Unit = {
    val scipFile = dest / "index.scip"
    val reporter = new ScipReporter(log)
    val toolInfo =
      LsifToolInfo
        .newBuilder()
        .setName("mill-scip")
        .setVersion(ScipBuildInfo.version)
        .build()

    log.info(s"Creating a index.scip in ${scipFile}")

    // So this is maybe a bit opinionated here, but we're only writing this to
    // disk for easier debugging. Normally scip-java can read this up to when
    // someone calls tries to manually create SCIP from semanticDB, but since
    // we are fully controlling everything here I think we can just write it
    // where it's easier since it won't be used that way and instead just
    // create the necessary package information and pass it right to
    // ScipSemanticdbOptions.
    os.write(
      dest / "javacopts.txt",
      Seq("-classpath\n", projects.mkString(":")),
      createFolders = true
    )
    val classPathEntries =
      projects.flatMap(project => ClasspathEntry.fromPom(project.toNIO))

    log.info(s"Including ${classPathEntries.size} classpath entries")

    val options = new ScipSemanticdbOptions(
      List(dest).map(_.toNIO).asJava,
      scipFile.toNIO,
      workspace.toNIO,
      reporter,
      toolInfo,
      "java",
      ScipOutputFormat.TYPED_PROTOBUF,
      true, // parallel -- this is fine
      classPathEntries
        .map(_.toPackageInformation)
        .asJava,
      "" // BuildKind here is fine being ""
    )

    ScipSemanticdb.run(options)
  }

  private def computeModules(ev: Evaluator): Seq[JavaModule] = {
    ModuleUtils
      .transitiveModules(ev.rootModule)
      .collect { case jm: JavaModule => jm }
  }

  implicit def millScoptEvaluatorReads[T]: EvaluatorScopt[T] =
    new mill.main.EvaluatorScopt[T]()

  lazy val millDiscover = mill.define.Discover[this.type]
}
