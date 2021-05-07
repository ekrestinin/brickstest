package app

import core._
import org.kohsuke.args4j.{CmdLineException, CmdLineParser, Option}
import util.Log

import scala.collection.mutable
import scala.util.Random

object TestApp extends App with Log {

  val options = getOptions(args)

  val n = options.n
  val clientNum = options.clientNum
  val runs = options.runs
  val runTime = options.runTime

  log.info(s"Running with options: $options")

  val random = new Random()
  val initialState = mutable.Buffer.empty[Int]
  for (i <- 0 to n)
    initialState += Random.nextInt(n)
  val state = State(initialState)

  if (log.isDebugEnabled)
    log.debug(s"Initial state: $initialState")

  val server = new Server(state)

  val clients = (for (i <- 0 until clientNum) yield {
    val (s, v) = server.snapshot(i)
    val client = new Client(i, s, v)
    i -> client
  }).toMap

  var allInSync = true
  var runsCount = 0
  while(allInSync && runsCount < runs) {
    allInSync = runCycle(runTime)
    runsCount += 1
  }

  def runCycle(seconds: Int, frequency: Int = 5): Boolean = {
    val totalTicks = seconds * frequency * clientNum
    log.info(s"Running cycle: $seconds seconds, frequency: $frequency ticks p/client p/second. Total ticks: $totalTicks.")
    val trace = mutable.Buffer.empty[String]
    val responses = mutable.Map.empty[Int, mutable.Buffer[OpMessage]]
    val pendingResponses = mutable.Buffer.empty[Int]

    var pendingUpdates = mutable.Buffer.empty[Int]
    pendingUpdates ++= (0 until frequency*seconds).flatMap(_ => 0 until clientNum)

    def send(id: Int, op: Op): Unit = {
      val messages = server.update(id, op, clients(id).applyLocal(op))
      messages.foreach(m => responses.getOrElseUpdate(m.id, mutable.Buffer.empty[OpMessage]) += m)
      pendingResponses ++= messages.map(_.id)
    }

    def push(id: Int): Unit = {
      val m = responses(id).remove(0)
      clients(id).update(m.op, m.version)
    }
    val start = System.currentTimeMillis()
    var eventSpace = pendingUpdates.size
    while (eventSpace != 0) {
      val event = random.nextInt(eventSpace)
      if (event < pendingUpdates.size) {
        val id = pendingUpdates(event)
        val op = generateOp(id, clients(id).state.size)
        send(id, op)
        trace += s"fixture.send($op)"
        pendingUpdates.remove(event)
      } else {
        val id = pendingResponses(event - pendingUpdates.size)
        push(id)
        trace += s"fixture.push($id)"
        pendingResponses.remove(event - pendingUpdates.size)
      }
      eventSpace = pendingUpdates.size + pendingResponses.size
    }
    val end = System.currentTimeMillis()
    val allInSync = clients.values.forall(_.state == server.state)
    log.info(s"All in sync: $allInSync. Cycle time: ${end - start} ms.")
    if (!allInSync && log.isDebugEnabled) {
      log.debug(s"${server.state}")
      clients.foreach { case (id, s) => log.debug(s"$id, ${s.state}") }
      log.debug(trace.mkString("\n"))
    }
    allInSync
  }

  def generateOp(source: Int, size: Int): Op = {
    if (size > 0) {
      random.nextInt(3) match {
        case 0 => InsertOp(source, random.nextInt(size + 1), random.nextInt(10000))
        case 1 => UpdateOp(source, random.nextInt(size), random.nextInt(10000))
        case 2 => DeleteOp(source, random.nextInt(size))
      }
    } else {
      InsertOp(source, 0, random.nextInt(10000))
    }
  }

  class Options {
    @Option(name = "-help", usage = "Print usage info")
    var help: Boolean = false

    @Option(name = "-n", usage = "Initial state size")
    var n: Int = 100

    @Option(name = "-cn", usage = "Number of clients")
    var clientNum: Int = 20

    @Option(name = "-runs", usage = "Number of test runs")
    var runs: Int = 100

    @Option(name = "-rt", usage = "Test cycle time in seconds")
    var runTime: Int = 10

    override def toString = s"Options(n=$n, clientNum=$clientNum, runs=$runs, runTime=$runTime)"
  }
  

  def getOptions(args: Array[String]): Options = {
    val options = new Options
    val parser = new CmdLineParser(options)
    try {
      parser.parseArgument(args: _*)
    } catch {
      case x: CmdLineException =>
        println(x.getMessage)
        parser.printUsage(System.out)
        System.exit(1)
    }

    if (options.help) {
      parser.printUsage(System.out)
      System.exit(0)
    }
    options
  }

}
