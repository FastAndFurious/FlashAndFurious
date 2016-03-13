package ff.laps

import akka.actor.{Actor, ActorLogging, Props}
import ff.Main
import ff.messages._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class LearnedLaps extends Actor with ActorLogging {

  import LearnedLaps._

  final override def receive: Receive = waitOnStart

  final def waitOnStart: Receive = {
    case RaceStart =>
      context.become(constructCircuit)
      log.info(s"race started")
      Main.emitPower(cruse)

    case x =>
      log.warning(s"race start expected, got: $x")
  }

  val firstThreshold = 0.3
  val secondThreshold = 0.6
  val delayedBuffer = 5
  val smoothing = 5

  var min = Int.MaxValue
  var max = Int.MinValue
  var count = 0
  val factor = 1.0 + count / 4

  var smooth = smoothing

  val laps = mutable.ListBuffer.empty[mutable.ListBuffer[Double]]
  laps += mutable.ListBuffer.empty[Double]
  val buffer = mutable.Queue.fill(delayedBuffer)(0.0)

  val cruse = 255 / 2
  val plan = mutable.ListBuffer.empty[Double]
  val limitings = mutable.ListBuffer.empty[Boolean]
  var speedPortal = 0
  def constructCircuit: Receive = {

    case RoundTime(_, _, duration, ts) =>
      val mean = plan.sum / plan.size.toDouble
      val stdev = math.sqrt(plan.map(x => x * x).sum / plan.size.toDouble - mean * mean)
      val smoothed = plan.grouped(smoothing).map(_.sum / smoothing).toList
      val limits = limitings.grouped(smoothing).map(_.reduce(_ || _)).toArray
      context.become(waitOnStop(smoothed, limits, stdev))
      plan.clear()
      log.info(s"mean $mean")
      log.info(s"stdev $stdev")
      log.info(s"length ${smoothed.size}")

    case Sensor(_, ts, _, (_, _, dev), _, _) =>
      min = math.min(min, dev)
      max = math.max(max, dev)
      plan += dev
      limitings += false

    case Pow(pow) =>
      Main.emitPower(pow)

    case Velocity(_, ts, speed, _) =>
      limitings += true

  }

  def accelerate(p1: Int, p2: Int): Unit = {
    Main.emitPower(p1)
    context.system.scheduler.scheduleOnce(0.5 second, self, Pow(p2))
  }

  var power = cruse
  def toPower(p: Int) = {
    Main.emitPower(p)
    power = p
  }

  def waitOnStop(ref: List[Double], limits: Array[Boolean], stdev: Double): Receive = {

    case Penalty(_, actualSpeed, speedLimit, _, barrier) =>
      context.system.scheduler.scheduleOnce(2.5 second, self, Pow(cruse))

    case RoundTime(_, _, duration, ts) =>
      val mean = plan.sum / plan.size.toDouble
      val stdev = math.sqrt(plan.map(x => x * x).sum / plan.size.toDouble - mean * mean)
      val smoothed = plan.grouped(smoothing).map(_.sum / smoothing).toList
      val limits = limitings.grouped(smoothing).map(_.reduce(_ || _)).toArray
      context.become(waitOnStop(smoothed, limits, stdev))
      plan.clear()
      log.info(s"mean $mean")
      log.info(s"stdev $stdev")
      log.info(s"length ${smoothed.size}")

    case Velocity(_, ts, speed, _) =>
      limitings += true
      val pos = plan.size / smoothing
      log.info(ref.mkString(", "))
      log.info(limits.mkString(", "))
      log.info(List.fill(pos)("  ").mkString(", "))

      if (pos < limits.length && limits(pos)) {
        accelerate(255, cruse)
      }

    case Sensor(_, ts, _, (_, _, dev), _, _) =>
      plan += dev
      limitings += false

    case Pow(pow) =>
      Main.emitPower(pow)

    case RaceStop =>
      context.become(waitOnStart)
      log.info(s"race stopped")

    case x =>
      log.warning(s"race stop expected, got: $x")
  }

}

object LearnedLaps {

  case class Pow(pow: Int)

  def props: Props = Props(classOf[LearnedLaps])

}
