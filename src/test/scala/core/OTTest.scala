package core

import org.junit.{Assert, Test}

import scala.collection.mutable

class OTTest {

  private val ot = new OT

  @Test
  def test_insert_insert(): Unit = {
    val state = st(1, 2, 3)
    test(state, InsertOp(1, 1, 4), InsertOp(2, 0, 5), st(5, 1, 4, 2, 3)) //different places
    test(state, InsertOp(1, 0, 5), InsertOp(2, 1, 4), st(5, 1, 4, 2, 3)) //other way around
    test(state, InsertOp(1, 0, 5), InsertOp(2, 0, 4), st(5, 4, 1, 2, 3)) //same index - insert first
    test(state, InsertOp(1, 2, 5), InsertOp(2, 2, 4), st(1, 2, 5, 4, 3)) //same index - insert in the mid
    test(state, InsertOp(1, 3, 5), InsertOp(2, 3, 4), st(1, 2, 3, 5, 4)) //same index - append
    test(state, InsertOp(1, 4, 5), InsertOp(2, 4, 4), st(1, 2, 3)) //same index - outside bounds

    //preserve intent
    //cases when original index is different from current index due to transformations
    test(state, InsertOp(1, 1, 5, originalIndex = 1), InsertOp(2, 1, 4, originalIndex = 1), st(1, 5, 4, 2, 3)) //same original index - source will win
    test(state, InsertOp(1, 1, 5, originalIndex = 2), InsertOp(2, 1, 4, originalIndex = 1), st(1, 4, 5, 2, 3)) //different original index
    test(state, InsertOp(1, 1, 5, originalIndex = 1), InsertOp(2, 1, 4, originalIndex = 2), st(1, 5, 4, 2, 3)) //different original index
  }

  @Test
  def test_insert_update(): Unit = {
    val state = st(1, 2, 3)
    test(state, InsertOp(1, 1, 4), UpdateOp(2, 0, 5), st(5, 4, 2, 3)) //insert > update
    test(state, InsertOp(1, 0, 5), UpdateOp(2, 1, 4), st(5, 1, 4, 3)) //insert < update
    test(state, InsertOp(1, 0, 5), UpdateOp(2, 0, 4), st(5, 4, 2, 3)) //same index
  }

  @Test
  def test_insert_delete(): Unit = {
    val state = st(1, 2, 3)
    test(state, InsertOp(1, 1, 4), DeleteOp(2, 0), st(4, 2, 3)) //insert > delete
    test(state, InsertOp(1, 0, 5), DeleteOp(2, 1), st(5, 1, 3)) //insert < delete
    test(state, InsertOp(1, 0, 5), DeleteOp(2, 0), st(5, 2, 3)) //same index - first element
    test(state, InsertOp(1, 1, 5), DeleteOp(2, 1), st(1, 5, 3)) //same index - mid element
    test(state, InsertOp(1, 2, 5), DeleteOp(2, 2), st(1, 2, 5)) //same index - end element
    test(state, InsertOp(1, 3, 5), DeleteOp(2, 2), st(1, 2, 5)) //append & delete
  }

  @Test
  def test_insert_noop(): Unit = {
    val state = st(1, 2, 3)
    test(state, InsertOp(1, 1, 4), NoOp(2), st(1, 4, 2, 3))
    test(state, InsertOp(1, 0, 5), NoOp(2), st(5, 1, 2, 3))
    test(state, InsertOp(1, 0, 5), NoOp(2), st(5, 1, 2, 3))
  }

  @Test
  def test_update_update(): Unit = {
    val state = st(1, 2, 3)
    test(state, UpdateOp(1, 1, 4), UpdateOp(2, 0, 5), st(5, 4, 3)) //source1>source2
    test(state, UpdateOp(1, 0, 5), UpdateOp(2, 1, 4), st(5, 4, 3)) //source1<source2
    test(state, UpdateOp(1, 0, 5), UpdateOp(2, 0, 4), st(5, 2, 3)) //source1==source2
  }

  @Test
  def test_update_delete(): Unit = {
    val state = st(1, 2, 3)
    test(state, UpdateOp(1, 1, 4), DeleteOp(2, 0), st(4, 3)) //update > delete
    test(state, UpdateOp(1, 0, 5), DeleteOp(2, 1), st(5, 3)) //update < delete
    test(state, UpdateOp(1, 0, 5), DeleteOp(2, 0), st(2, 3)) //same index
    test(state, UpdateOp(1, 2, 5), DeleteOp(2, 0), st(2, 5)) //update last
    test(state, UpdateOp(1, 2, 5), DeleteOp(2, 2), st(1, 2)) //update last - same index
  }

  @Test
  def test_update_noop(): Unit = {
    val state = st(1, 2, 3)
    test(state, UpdateOp(1, 1, 4), NoOp(2), st(1, 4, 3))
    test(state, UpdateOp(1, 0, 5), NoOp(2), st(5, 2, 3))
    test(state, UpdateOp(1, 2, 5), NoOp(2), st(1, 2, 5))
  }

  @Test
  def test_delete_delete(): Unit = {
    val state = st(1, 2, 3)
    test(state, DeleteOp(1, 1), DeleteOp(2, 0), st(3))
    test(state, DeleteOp(1, 0), DeleteOp(2, 1), st(3))
    test(state, DeleteOp(1, 0), DeleteOp(2, 0), st(2, 3)) //same index
    test(state, DeleteOp(1, 2), DeleteOp(2, 1), st(1)) //delete last
    test(state, DeleteOp(1, 1), DeleteOp(2, 2), st(1)) //delete last
    test(state, DeleteOp(1, 2), DeleteOp(2, 2), st(1, 2)) //delete last - same index
  }

  @Test
  def test_delete_noop(): Unit = {
    val state = st(1, 2, 3)
    test(state, DeleteOp(1, 0), NoOp(2), st(2, 3))
    test(state, DeleteOp(1, 1), NoOp(2), st(1, 3))
    test(state, DeleteOp(1, 2), NoOp(2), st(1, 2))
  }


  def test(state: mutable.Buffer[Int], o1: Op, o2: Op, expectedState: mutable.Buffer[Int]): Unit = {
    transformAndTest(state, o1, o2, expectedState)
    transformAndTest(state, o2, o1, expectedState)
  }

  def transformAndTest(state: mutable.Buffer[Int], o1: Op, o2: Op, expectedState: mutable.Buffer[Int]): Unit = {
    val state1 = state.clone()
    val state2 = state.clone()

    val (o1t, o2t) = ot.transform(o1, o2)

    o1(state1)
    o2t(state1)

    o2(state2)
    o1t(state2)

    Assert.assertTrue(s"$state, $o1, $o2, expected: $expectedState, actual: $state1, $state2", state1 == expectedState && state2 == expectedState)
  }

  def st(v: Int*): mutable.Buffer[Int] = mutable.Buffer(v: _*)
}
