package core

import scala.collection.mutable

/**
  * Interface for operations
  */
trait Op {
  /**
    * Indicates if operation should be replicated to all clients
    *
    * @return
    */
  def shouldFanOut: Boolean = false

  /**
    * infra.Client id
    *
    * @return
    */
  def source: Int

  def apply(state: mutable.Buffer[Int]): Unit = {}

  def sameSource(op: Op): Boolean = source == op.source
}

/**
  * Trait for replicated operations
  */
trait FanOutOp extends Op {
  override val shouldFanOut = true
}

/**
  * Insert new value into a given position.
  * If index is outside of state bounds [0,state.size], the operation is no op.
  *
  * @param source        - client id
  * @param index         - position where to insert new value
  * @param value         - value to insert
  * @param originalIndex - original insert index as defined by source. Used to preserve intent if index has been changed by transformations.
  */
case class InsertOp(source: Int, index: Int, value: Int, originalIndex: Int) extends FanOutOp {
  override def apply(state: mutable.Buffer[Int]): Unit =
    if (index <= state.size && index >= 0)
      state.insert(index, value)
}

object InsertOp {
  def apply(source: Int, index: Int, value: Int): InsertOp = new InsertOp(source, index, value, index)
}

/**
  * Update value at given position
  * If index is outside of state bounds[0, state.size-1], the operation is no op.
  *
  * @param source - client id
  * @param index  - position to update
  * @param value  - new value
  */
case class UpdateOp(source: Int, index: Int, value: Int) extends FanOutOp {
  override def apply(state: mutable.Buffer[Int]): Unit =
    if (index < state.size && index >= 0)
      state.update(index, value)
}

/**
  * Delete value at given position
  * If index is outside of state bounds[0, state.size-1], the operation is no op.
  *
  * @param source - client id
  * @param index  - position to update
  */
case class DeleteOp(source: Int, index: Int) extends FanOutOp {
  override def apply(state: mutable.Buffer[Int]): Unit =
    if (index < state.size && index >= 0)
      state.remove(index)
}

case class NoOp(source: Int) extends Op

case class OpWithVersion(op: Op, version: Version)

case class Version(sentMessages: Long, receivedMessages: Long) {
  def sent(): Version = this.copy(sentMessages = sentMessages + 1)

  def received(): Version = this.copy(receivedMessages = receivedMessages + 1)
}

case class OpMessage(id: Int, op: Op, version: Version)
