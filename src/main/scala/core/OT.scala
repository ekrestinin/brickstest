package core


/**
  * Operation transformations
  */
class OT {
  /**
    * Transform operations in such a way that given initial state s and transformed operations o1t, o2t:
    * o1t(o2(s))==o2t(o1(s))
    *
    * @return transformed operations
    */
  def transform(o1: Op, o2: Op): (Op, Op) = {
    if (o1.source == o2.source) (o1, o2)
    else
    (o1, o2) match {
      case (o1: InsertOp, o2: InsertOp) => insertInsert(o1, o2)
      case (o1: InsertOp, o2: UpdateOp) => insertUpdate(o1, o2)
      case (o1: InsertOp, o2: DeleteOp) => insertDelete(o1, o2)
      case (o1: InsertOp, o2: NoOp) => (o1, o2)
      case (o1: NoOp, o2: InsertOp) => (o1, o2)

      case (o1: UpdateOp, o2: InsertOp) => insertUpdate(o2, o1).swap
      case (o1: UpdateOp, o2: UpdateOp) => updateUpdate(o1, o2)
      case (o1: UpdateOp, o2: DeleteOp) => updateDelete(o1, o2)
      case (o1: UpdateOp, o2: NoOp) => (o1, o2)
      case (o1: NoOp, o2: UpdateOp) => (o1, o2)

      case (o1: DeleteOp, o2: InsertOp) => insertDelete(o2, o1).swap
      case (o1: DeleteOp, o2: UpdateOp) => updateDelete(o2, o1).swap
      case (o1: DeleteOp, o2: DeleteOp) => deleteDelete(o1, o2)
      case (o1: DeleteOp, o2: NoOp) => (o1, o2)
      case (o1: NoOp, o2: DeleteOp) => (o1, o2)

      case (o1: NoOp, o2: NoOp) => (o1, o2)
    }
  }

  def insertInsert(o1: InsertOp, o2: InsertOp): (Op, Op) = {
    if (o1.index < o2.index) {
      (o1, o2.copy(index = o2.index + 1))
    } else if (o1.index > o2.index) {
      (o1.copy(index =  o1.index + 1), o2)
    } else {
      if (o1.originalIndex < o2.originalIndex) {
        (o1, o2.copy(index = o2.index + 1))
      } else if (o1.originalIndex > o2.originalIndex) {
        (o1.copy(index = o1.index + 1), o2)
      } else {
        if (o1.source < o2.source) {
          (o1, o2.copy(index = o2.index + 1))
        } else if (o1.source > o2.source) {
          (o1.copy(index = o1.index + 1), o2)
        } else {
          //shouldn't happen - means these came from the same client
          (o1, o2)
        }
      }
    }
  }

  def insertUpdate(o1: InsertOp, o2: UpdateOp): (Op, Op) = {
    if (o1.index < o2.index)
      (o1, o2.copy(index = o2.index + 1))
    else if (o1.index > o2.index)
      (o1, o2)
    else
      (o1, o2.copy(index = o2.index + 1))
  }

  def insertDelete(o1: InsertOp, o2: DeleteOp): (Op, Op) = {
    if (o1.index < o2.index)
      (o1, o2.copy(index = o2.index + 1))
    else if (o1.index > o2.index)
      (o1.copy(index = o1.index - 1), o2)
    else
      (o1, o2.copy(index = o2.index + 1))
  }

  def updateUpdate(o1: UpdateOp, o2: UpdateOp): (Op, Op) = {
    if (o1.index != o2.index) (o1, o2)
    else {
      if (o1.source < o2.source)
        (o1, NoOp(o2.source))
      else (NoOp(o1.source), o2)
    }
  }

  def updateDelete(o1: UpdateOp, o2: DeleteOp): (Op, Op) = {
    if (o1.index < o2.index)
      (o1, o2)
    else if (o1.index > o2.index) {
      (o1.copy(index = o1.index - 1), o2)
    } else {
      (NoOp(o1.source), o2)
    }
  }

  def deleteDelete(o1: DeleteOp, o2: DeleteOp): (Op, Op) = {
    if (o1.index < o2.index)
      (o1, o2.copy(index = o2.index - 1))
    else if (o1.index > o2.index)
      (o1.copy(index = o1.index - 1), o2)
    else {
        (NoOp(o1.source), NoOp(o2.source))
    }
  }
}
