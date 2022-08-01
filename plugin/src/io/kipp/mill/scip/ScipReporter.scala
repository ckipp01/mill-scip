package io.kipp.mill.scip

import com.sourcegraph.scip_semanticdb.ScipSemanticdbReporter
import mill.api.Logger

import java.util.concurrent.atomic.AtomicInteger

class ScipReporter(log: Logger) extends ScipSemanticdbReporter {
  private var totalSize = 0
  private val currentSize = new AtomicInteger()

  override def startProcessing(taskSize: Int): Unit = {
    totalSize = taskSize
    log.info(
      s"Staring to create a scip index from ${totalSize} SemanticDB documents."
    )
    log.ticker(s"[${currentSize.get}/${totalSize}] processing semanticdb")
  }

  override def endProcessing(): Unit =
    log.ticker(s"[${totalSize}/${totalSize}] Finished creating your scip index")

  override def error(e: Throwable): Unit =
    // TODO do we want to capture anything specific here like file not found
    log.error(e.getMessage())

  override def processedOneItem(): Unit = {
    currentSize.incrementAndGet()
    log.ticker(s"[${currentSize.get}/${totalSize}] processing semanticdb")
  }
}
