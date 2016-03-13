package ff.laps

import akka.actor.{Actor, ActorLogging, Props}
import ff.Main
import ff.messages._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class SlottedLaps extends Actor with ActorLogging {

  val seekSpeed = 90
  val warmSpeed = 110
  var speedIncr = 16
  val delayBuffer = 5
  val freezedDuration = 55
  val accelarationDelay = 0.0
  val accelerationValue = 180

  var lastTimeDiff = 0.0
  var lastTimestamp = 0
  var freezeCount = -1
  val buffer = mutable.Queue.fill(delayBuffer)(0)
  var segmentLostID = -1
  var segmentID = 0

  override def receive: Receive = {
    case RaceStart =>
      context.become(warming)
      log.info(s"+ race started")
  }

  def warming: Receive = {
    power(warmSpeed)

    {
      case RoundTime(_, _, _, ts) =>
        context.become(lap(0, Array.fill(50)(seekSpeed)))
        lastTimestamp = ts
        log.info(s"- warminh ended")
    }
  }

  def lap(num: Int, currentSpeed: Array[Int]): Receive = {
    segmentLostID = -1
    segmentID = 0
    power(currentSpeed(segmentID))

    {
      case Sensor(_, ts, _, (_, _, dev), _, _) =>
        buffer.enqueue(dev)
        buffer.dequeue()
        freezeCount -= 1
        val thres = buffer.map(math.abs).sum/delayBuffer
        if (thres > 1800 && freezeCount < 0) {
          freezeCount = freezedDuration
          segmentID += 1
          if (num > 0) {
            //powerIn(accelerationValue, accelarationDelay second)
            //powerIn(currentSpeed(segmentID), (accelarationDelay + lastTimeDiff) second)
          }
          lastTimeDiff = (ts - lastTimestamp) / 5000.0
          lastTimestamp = ts
          log.info(s"t = $thres")
          log.info(s"d = $lastTimeDiff")
        }

      case Penalty(_, actualSpeed, speedLimit, _, barrier) => // TODO: use barrier for checking
        powerIn(accelerationValue, 2.1 second)
        powerIn(currentSpeed(segmentID), 3 second)
        if (segmentLostID == -1) {
          segmentLostID = segmentID
          log.warning(s"$segmentID too fast at $barrier: $actualSpeed/$speedLimit")
        }

      case RoundTime(_, _, duration, _) =>
        val targetSpeed =
          if (segmentLostID >= 0) {
            val l = segmentLostID
            speedIncr /= 2
            currentSpeed.zipWithIndex.map {
              case (s, `l`) => s - speedIncr * 3
              case (s, _) => s - speedIncr * 2
            }
          } else {
            currentSpeed.map(_ + speedIncr)
          }

        context.become(lap(num + 1, targetSpeed))
        log.info(currentSpeed.toList.toString())
        log.info(s"- lap $num ended")

      case RaceStop =>
        context.become(receive)
        log.info(s"+ race stopped")

    }
  }

  def power(p: Int): Unit =
    Main.emitPower(p)

  def powerIn(p: Int, d: FiniteDuration): Unit =
    context.system.scheduler.scheduleOnce(d, new Runnable {
      override def run(): Unit =
        power(p)
    })

}

object SlottedLaps {

  def props: Props = Props(classOf[SlottedLaps])

}
