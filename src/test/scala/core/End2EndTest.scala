package core


import org.junit.{Assert, Test}

import scala.collection.mutable

class End2EndTest {

  @Test
  def lateClient(): Unit = {

    val fixture = TestFixture(2, 0, 1, 2, 3)

    fixture.send(UpdateOp(0, 1, 5), DeleteOp(1, 1))

    fixture.pushAll()

    fixture.validate(0, 2, 3)

    //new client connected
    val newClient = fixture.createClient()

    fixture.send(UpdateOp(0, 2, 5), InsertOp(0, 0, 10))

    fixture.send(UpdateOp(newClient, 1, 25), InsertOp(newClient, 1, 20))

    fixture.pushAll()

    fixture.validate(10, 0, 20, 25, 5)
  }

  @Test
  def test(): Unit = {
    val fixture = TestFixture(2, 0, 1, 2, 3)

    fixture.send(UpdateOp(0, 1, 5), DeleteOp(1, 1))

    fixture.pushAll()

    fixture.validate(0, 2, 3)
  }

  @Test
  def test2(): Unit = {

    val fixture = TestFixture(2, 4, 2, 3, 1, 0, 4)

    fixture.send(InsertOp(0, 5, 3385), UpdateOp(1, 5, 7767))

    fixture.pushAll()

    fixture.validate(4, 2, 3, 1, 0, 3385, 7767)
  }

  @Test
  def test3(): Unit = {
    val fixture = TestFixture(4, 0, 3, 3, 1, 1, 3)

    //s: 0, 3, 3, 1, 1, 3 //s0
    //s: 247, 3, 3, 1, 1, 3 //upd 0
    //s: 3, 3, 1, 1, 3 //del 0
    //s: 3, 3, 1, 1, 3 //del 0 - noop
    //s: 3, 3, 1, 8726, 1, 3
    fixture.send(
      UpdateOp(2, 0, 247),
      DeleteOp(1, 0),
      DeleteOp(0, 0),
      InsertOp(3, 4, 8726))
    fixture.pushAll()
    fixture.validate(3, 3, 1, 8726, 1, 3)

    //3, 3, 1, 8726, 1, 3
    //5224, 3, 1, 8726, 3161, 1, 3
    //5936, 8696, 5224, 3, 1, 8726, 3161, 1, 3
    fixture.send(
      UpdateOp(3, 0, 5224),
      InsertOp(2, 0, 8696),
      InsertOp(0, 0, 5936),
      InsertOp(1, 4, 3161)
    )
    fixture.pushAll()
    fixture.validate(5936, 8696, 5224, 3, 1, 8726, 3161, 1, 3)
  }

  @Test
  def test4(): Unit = {
    val fixture = TestFixture(2, 2, 4, 2, 2, 1, 2)
    //2, 4, 2, 2, 1, 2
    //2, 4, 9490, 2, 1, 2
    //2, 9490, 2, 1, 2
    fixture.send(
      UpdateOp(0, 2, 9490),
      DeleteOp(1, 1)
    )
    fixture.push(0)

    //2, 2, 9060, 2, 1, 2
    //2, 9490, 2, 1, 2
    fixture.send(
      InsertOp(1, 2, 9060)
    )

    fixture.push(1)
    fixture.push(0)
    fixture.push(0)
    fixture.push(1)

    fixture.send(
      UpdateOp(0, 3, 2557)
    )

    fixture.push(0)
    fixture.push(1)
    fixture.push(1)


    fixture.validate(2, 9490, 9060, 2557, 1, 2)

  }

  @Test
  def test5(): Unit = {
    val fixture = TestFixture(2, 0, 1, 2, 3, 4, 5, 6)

    //c0: 0, 1, 2, 3, 4, 5, 6
    //c1: 0, 1, 2, 3, 4, 5, 6
    //s: 0, 1, 2, 3, 4, 5, 6
    fixture.send(UpdateOp(0, 6, 11))
    //c0: 0, 1, 2, 3, 4, 5, 11
    //s: 0, 1, 2, 3, 4, 5, 11

    fixture.push(1)
    //c1: 0, 1, 2, 3, 4, 5, 11

    fixture.send(InsertOp(0, 1, 12))
    //c0: 0, 12, 1, 2, 3, 4, 5, 11
    //s: 0, 12, 1, 2, 3, 4, 5, 11

    fixture.send(DeleteOp(1, 0))
    //c1: 1, 2, 3, 4, 5, 11
    //s: 12, 1, 2, 3, 4, 5, 11

    //noop
    fixture.push(0)
    //c0: 0, 12, 1, 2, 3, 4, 5, 11

    fixture.send(InsertOp(1, 0, 13))
    //c1: 13, 1, 2, 3, 4, 5, 11
    //s: 13, 12, 1, 2, 3, 4, 5, 11

    fixture.push(1)
    //c1: 12, 13, 1, 2, 3, 4, 5, 11

    fixture.push(0)
    fixture.send(InsertOp(0, 8, 14))
    fixture.push(1)
    fixture.push(0)
    fixture.push(1)
    fixture.push(0)
    fixture.push(0)
    fixture.push(1)
    fixture.send(DeleteOp(1, 1))
    fixture.push(0)
    fixture.push(1)

    fixture.validate(13, 1, 2, 3, 4, 5, 11, 14)

  }

  @Test
  def test6(): Unit = {
    val fixture = TestFixture(2, 0, 1)

    //0, 1
    fixture.send(UpdateOp(0, 1, 12))
    //c0: 0,12
    //s: 0,12

    fixture.send(UpdateOp(1, 1, 13))
    //c1: 0,13
    //s: 0,12

    fixture.send(InsertOp(1, 1, 14, 1))
    //c1: 0,14,13
    //s: 0,14,12

    //noop
    fixture.push(0)

    fixture.push(0) //InsertOp(1,1,14,1)
    //c0: 0,14,12


    fixture.send(UpdateOp(1, 1, 15))
    //c1: 0,15,13
    //s: 0, 15, 12

    fixture.push(1) //UpdateOp(0,1,12)
    //c1: 0, 15, 12

    fixture.send(DeleteOp(0, 0))
    //c0: 14,12
    //s: 15,12

    fixture.push(0) //UpdateOp(1,1,15)
    //c0: 15,12


    fixture.send(InsertOp(0, 1, 16, 1))
    //c0: 15,16,12
    //s: 15,16,12


    //noop
    fixture.push(0)
    //c0: 15,16,12

    //noop
    fixture.push(1)
    //c1: 0, 15, 12

    fixture.push(1)
    fixture.push(1)
    fixture.push(0)
    fixture.push(1)

    fixture.validate(15, 16, 12)

  }

  @Test
  def test7(): Unit = {
    //collapsed delete
    val fixture = TestFixture(2, 0, 1, 2, 3, 4, 5, 6, 7)

    //s: 0, 1, 2, 3, 4, 5, 6, 7

    fixture.send(InsertOp(0, 1, 11, 1))
    //c0: 0, 11, 1, 2, 3, 4, 5, 6, 7
    //s: 0, 11, 1, 2, 3, 4, 5, 6, 7

    fixture.send(DeleteOp(0, 5))
    //c0: 0, 11, 1, 2, 3, 5, 6, 7
    //s: 0, 11, 1, 2, 3, 5, 6, 7

    fixture.send(UpdateOp(1, 1, 12))
    //c1: 0, 12, 2, 3, 4, 5, 6, 7
    //s: 0, 11, 12, 2, 3, 5, 6, 7

    fixture.send(DeleteOp(0, 4))
    //c0: 0, 11, 1, 2, 5, 6, 7
    //s: 0, 11, 12, 2, 5, 6, 7

    fixture.push(0) //noop

    fixture.send(DeleteOp(1, 3))
    //c1: 0, 12, 2, 4, 5, 6, 7
    //s:  0, 11, 12, 2, 5, 6, 7

    fixture.push(1) //InsertOp(0,1,11,1)
    //c1: 0, 11, 12, 2, 4, 5, 6, 7

    fixture.push(1) //DeleteOp(0,5)
    //c1: 0, 11, 12, 2, 5, 6, 7

    fixture.push(0) //noop

    fixture.send(InsertOp(1, 5, 13, 5))
    //c1: 0, 11, 12, 2, 5, 13, 6, 7
    //s: 0, 11, 12, 2, 13, 5, 6, 7

    fixture.push(0)
    fixture.push(0)
    fixture.push(1)
    fixture.push(1)
    fixture.push(1)
    fixture.push(0)

    fixture.validate(0, 11, 12, 2, 5, 13, 6, 7)

  }

  @Test
  def test8(): Unit = {
    val fixture = TestFixture(3, 0, 1, 2, 3, 4, 5)

    //s: 0, 1, 2, 3, 4, 5

    fixture.send(InsertOp(0, 1, 11, 1))
    //c0: 0, 11, 1, 2, 3, 4, 5
    //s: 0, 11, 1, 2, 3, 4, 5

    fixture.push(2) //InsertOp(0,1,11,1)

    fixture.send(InsertOp(2, 2, 12, 2))
    //c2: 0, 1, 12, 2, 3, 4, 5
    //s: 0, 11, 12, 1, 2, 3, 4, 5

    fixture.send(DeleteOp(1, 4))
    //c1: 0, 1, 2, 3, 5
    //s: 0, 11, 12, 1, 2, 3, 5

    fixture.push(1) //InsertOp(0,1,11,1)
    //c1: 0, 11, 1, 2, 3, 5

    fixture.send(DeleteOp(1, 1))
    //c1: 0, 1, 2, 3, 5
    //s: 0, 12, 1, 2, 3, 5

    fixture.push(0) //noop

    fixture.push(1) //InsertOp(2,2,12,2)
    //c1: 0, 12, 1, 2, 3, 5

    fixture.push(1) //noop

    fixture.send(InsertOp(0, 4, 13, 4))
    //c0: 0, 11, 1, 2, 13, 3, 4, 5
    //s: 0, 12, 1, 2, 13, 3, 5

    fixture.send(DeleteOp(0, 1))
    //c0: 0, 1, 2, 13, 3, 4, 5
    //s: 0, 1, 2, 13, 3, 5

    fixture.push(1) //noop

    fixture.push(2) //noop

    fixture.push(0) //InsertOp(2,2,12,2)
    //c0: 0, 12, 1, 2, 13, 3, 4, 5

    fixture.push(0) //DeleteOp(1,6,Set(1))
    //c0: 0, 12, 1, 2, 13, 3, 5

    fixture.push(2) //DeleteOp(1,6,Set(1))

    fixture.send(DeleteOp(2, 1))
    //s: 0, 1, 2, 13, 3, 5 - noop

    fixture.push(2) //DeleteOp(1,1,Set(1))

    fixture.push(0) //DeleteOp(1,1,Set(1))
    //c0: 0, 12, 1, 2, 13, 3, 5

    fixture.push(1) //InsertOp(0,4,13,4)

    fixture.send(InsertOp(1, 1, 14, 1))
    //s: 0, 14, 1, 2, 13, 3, 5

    fixture.push(0) //noop
    fixture.push(0) //noop

    fixture.push(1) //DeleteOp(0,1,Set(0))

    //fixture.push(1)
    fixture.push(2)
    fixture.send(DeleteOp(2, 4))
    //s: 0, 14, 1, 2, 3, 5

    fixture.push(2)
    fixture.push(0) //InsertOp(1,1,14,1)
    //c0: 0, 14, 12, 1, 2, 13, 3, 5

    //fixture.push(0) //DeleteOp(2,4,Set(2))
    //c0: 0, 14, 12, 1, 13, 3, 5

    fixture.push(1)
    fixture.push(2)
    //fixture.push(2)

    fixture.validate(0, 14, 12, 1, 2, 3, 5)
  }
}

object TestFixture {

  def apply(clientNum: Int, st: Int*): TestFixture = {
    val initialState = mutable.Buffer(st: _*)
    val fixture = new TestFixture(initialState)
    (0 until clientNum).foreach(_ => fixture.createClient())
    fixture
  }
}

class TestFixture(initialState: mutable.Buffer[Int]) {

  private val serverState = new Server(State(initialState))

  def createClient(): Int = {
    val id = clients.size
    val (s, v) = serverState.snapshot(id)
    clients += id -> new Client(id, s, v)
    id
  }

  private val clients = mutable.Map.empty[Int, Client]

  private val queue: mutable.Map[Int, mutable.Buffer[OpMessage]] = mutable.Map.empty[Int, mutable.Buffer[OpMessage]]

  def send(ops: Op*): Unit = {
    ops.foreach(op => {
      val messages = serverState.update(op.source, op, clients(op.source).applyLocal(op))
      messages.foreach(m => queue.getOrElseUpdate(m.id, mutable.Buffer.empty[OpMessage]) += m)
    })
  }

  def push(id: Int): Unit = {
    val m = queue(id).remove(0)
    clients(id).update(m.op, m.version)
  }

  def pushAll(): Unit = {
    var toPush = clients.keySet.flatMap(id => queue(id).headOption.map(_.id))
    while (toPush.nonEmpty) {
      toPush.foreach(push)
      toPush = clients.keySet.flatMap(id => queue(id).headOption.map(_.id))
    }
  }

  def validate(expectedState: Int*): Unit = {
    val exp = mutable.Buffer(expectedState: _*)
    val clientStates = clients.map { case (id, v) => (id, v.state) }
    val allStatesString = clientStates.toSeq.mkString("\n") + "\n" + serverState.state
    val allStates: Seq[mutable.Buffer[Int]] = clientStates.map(_._2.seq) ++: Seq(serverState.state.seq)
    Assert.assertTrue(s"$allStatesString\nexpected: $exp", allStates.forall(v => {
      v == exp
    }))
  }

}

