package core

import scala.collection.mutable

/**
  * State to replicate
  */
case class State(seq: mutable.Buffer[Int]) {
  def apply(op: Op): Unit = op(seq)

  def snapshot: State = State(mutable.Buffer.empty[Int] ++: seq)

  def size: Int = seq.size
}
