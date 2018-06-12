package org.ergoplatform.nodeView.history.storage.modifierprocessors

import io.iohk.iodb.ByteArrayWrapper
import org.ergoplatform.modifiers.ErgoPersistentModifier
import org.ergoplatform.modifiers.state.UTXOSnapshotChunk
import org.ergoplatform.nodeView.history.storage.HistoryStorage
import scorex.core.consensus.History.ProgressInfo
import scorex.core.utils.ScorexLogging

import scala.util.{Failure, Success, Try}

/**
  * Contains all functions required by History to process UTXOSnapshotChunk
  */
trait UTXOSnapshotChunkProcessor extends ScorexLogging {

  protected val historyStorage: HistoryStorage

  def process(m: UTXOSnapshotChunk): ProgressInfo[ErgoPersistentModifier] = {
    //TODO
    val toInsert = ???
    historyStorage.insert(ByteArrayWrapper(m.id), Seq.empty, toInsert)
    ProgressInfo(None, Seq.empty, Seq(m), Seq.empty)
  }

  def validate(m: UTXOSnapshotChunk): Try[Unit] = if (historyStorage.contains(m.id)) {
    Failure(new Error(s"UTXOSnapshotChunk with id ${m.encodedId} is already in history"))
  } else {
    Success()
  }

}
