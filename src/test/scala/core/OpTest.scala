package core

import org.junit.{Assert, Test}

import scala.collection.mutable

class OpTest {

  @Test
  def test_insert(): Unit = {
    assert(st(1, 2, 3), InsertOp(0, -1, 4), st(1, 2, 3))//insert negative
    assert(st(1, 2, 3), InsertOp(0, 0, 4), st(4, 1, 2, 3))//insert first
    assert(st(1, 2, 3), InsertOp(0, 1, 4), st(1, 4, 2, 3))//insert non-first
    assert(st(1, 2, 3), InsertOp(0, 2, 4), st(1, 2, 4, 3))//insert last
    assert(st(1, 2, 3), InsertOp(0, 3, 4), st(1, 2, 3, 4))//append
    assert(st(), InsertOp(0, 0, 4), st(4))//append to empty
    assert(st(), InsertOp(0, 1, 4), st()) //insert outside bounds - noop
  }

  @Test
  def test_update(): Unit = {
    assert(st(1, 2, 3), UpdateOp(0, -1, 4), st(1, 2, 3))//update negative
    assert(st(1, 2, 3), UpdateOp(0, 0, 4), st(4, 2, 3))//update first
    assert(st(1, 2, 3), UpdateOp(0, 1, 4), st(1, 4, 3))//update non-first
    assert(st(1, 2, 3), UpdateOp(0, 2, 4), st(1, 2, 4))//update last
    assert(st(1, 2, 3), UpdateOp(0, 3, 4), st(1, 2, 3))//outside bounds
    assert(st(), UpdateOp(0, 0, 4), st())//update empty
  }

  @Test
  def test_delete(): Unit = {
    assert(st(1, 2, 3), DeleteOp(0, -1), st(1, 2, 3))//delete negative
    assert(st(1, 2, 3), DeleteOp(0, 0), st(2, 3))//delete first
    assert(st(1, 2, 3), DeleteOp(0, 1), st(1, 3))//delete non-first
    assert(st(1, 2, 3), DeleteOp(0, 2), st(1, 2))//delete last
    assert(st(1, 2, 3), DeleteOp(0, 3), st(1, 2, 3))//outside bounds
    assert(st(), DeleteOp(0, 0), st())//delete empty
  }

  def assert(state: mutable.Buffer[Int], o: Op, expectedState: mutable.Buffer[Int]): Unit = {
    o(state)
    Assert.assertEquals(s"$state, $o, $expectedState", state, expectedState)
  }

  def st(v: Int*): mutable.Buffer[Int] = mutable.Buffer(v :_*)
}
