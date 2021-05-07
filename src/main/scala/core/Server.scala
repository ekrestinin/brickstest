package core

import util.Log

import scala.collection.mutable

/**
  * Server
  *
  * @param state - initial state
  */
class Server(val state: State) extends Log {
  private val ot = new OT
  /**
    * infra.Server's version
    */
  private var version = Version(0, 0)

  case class ClientContext(var version: Version, var oplog: mutable.Buffer[OpWithVersion] = mutable.Buffer.empty[OpWithVersion])

  private val clients: mutable.Map[Int, ClientContext] = mutable.Map.empty

  /**
    * Process operation received from a client
    *
    * @param id            - client id
    * @param op            - operation
    * @param clientVersion - client's version
    * @return - messages to fan out to clients
    */
  def update(id: Int, op: Op, clientVersion: Version): Seq[OpMessage] = {
    clients.get(id).fold(
      throw new IllegalArgumentException(s"Unknown client $id")
    )(c => {
      val messages = update(id, op, clientVersion, c)
      messages.foreach(m => clients(m.id).oplog += OpWithVersion(m.op, m.version))
      messages
    })
  }

  /**
    * Process operation received from a client
    *
    * @param id            - client id
    * @param op            - operation
    * @param clientVersion - client's version
    * @param context       - client session context
    * @return - messages to fan out to clients
    */
  //client version contains:
  // sent messages - messages generated by this particular client
  // received messages - total number of server messages received by client as part of state reset and later
  private def update(id: Int, op: Op, clientVersion: Version, context: ClientContext): Seq[OpMessage] = {
    if (log.isDebugEnabled)
      log.debug(s"Processing update from client: $id, $op, $clientVersion. Current state: $state, $version")
    context.oplog = context.oplog.dropWhile(_.version.sentMessages < clientVersion.receivedMessages)
    var transformed = op
    val oplog = context.oplog
    for (i <- oplog.indices) {
      //skip messages that have already been processed by client before the client message was generated
      //also ignore messages from the same client
      if (oplog(i).version.sentMessages > clientVersion.receivedMessages
        && !oplog(i).op.sameSource(op)
      ) {
        val (to, loggedOp) = ot.transform(transformed, oplog(i).op)
        transformed = to
        context.oplog(i) = oplog(i).copy(op = loggedOp)
      }
    }
    context.oplog :+= OpWithVersion(transformed, version)
    state(transformed)
    //generate messages to fan out to clients
    val messages = fanOut(id, transformed)
    //mark message has been received from the client
    context.version = context.version.received()
    if (log.isDebugEnabled)
      log.debug(s"Processed update from client: $id, $op, $clientVersion. New state: $state.")
    messages
  }


  /**
    * Generate messages to replicate to all clients
    *
    * @param source - origin client's id
    * @param op     - operation to fan out
    * @return - messages to fan out
    */
  private def fanOut(source: Int, op: Op): Seq[OpMessage] = {
    if (op.shouldFanOut) {
      //mark messages have been sent to clients
      version = version.sent()
      clients.map { case (id, cc) =>
        //report to client total sent messages
        //keep number of messages received from the client
        val clientVersion = cc.version.copy(sentMessages = version.sentMessages)
        if (id == source) {
          //send NoOp to the source as an ack
          OpMessage(id, NoOp(source), clientVersion)
        } else {
          OpMessage(id, op, clientVersion)
        }
      }.toSeq
    } else Seq.empty
  }

  /**
    * Get a current state snapshot along with server's version for new client.
    *
    * @param id - client's id
    * @return - snapshot and version
    */
  def snapshot(id: Int): (State, Version) = {
    //along with snapshot report to client total number of messages generated by server so far
    val v = Version(version.sentMessages, 0)
    clients += id -> ClientContext(v)
    (state.snapshot, v)
  }
}