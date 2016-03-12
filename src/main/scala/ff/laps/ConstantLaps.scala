package ff.laps

import akka.actor.{Actor, ActorLogging, Props}
import ff.tokens.{RightTurn, LeftTurn, Straight, Token}
import ff.{Tokenizer, Main}
import ff.messages._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ConstantLaps(var pow: Int) extends Actor with ActorLogging {
  var loop = mutable.ListBuffer[Token]()
  var lastToken: Token = null
  var loopCursor = 0
  val tokenizer = new Tokenizer()
  var max_pow = 140
  var lap = 0
  var gyr_max = 0
  var time_start_lap = 0
  var speed_coef = 0.0

  final override def receive: Receive = waitOnStart

  final def waitOnStart: Receive = {
    case RaceStart =>
      context.become(waitOnStop)
      log.info(s"race started")


    case x =>
      log.warning(s"race start expected, got: $x ${sender()}")
  }

  final def waitOnStop: Receive = {

    case s@Sensor(_,t,_,g,_,_) =>
      if (lap == -1) {
        Main.emitPower(pow)
      }
      if (lap <= 1) {
        if (math.abs(g._3) > gyr_max) {
          gyr_max = math.abs(g._3)
        }
        Main.emitPower(pow)
        tokenizer.tokenize(g._3, t, gyr_max/3)
      } else if (lap == 2) {
        Main.emitPower(pow)
        log.debug(tokenizer.tokenize(g._3, t, gyr_max/3).direction.toString)
      } else {
        val curToken = tokenizer.tokenize(g._3, t, gyr_max/3)
        if (lastToken == null) {
          // penser cas ou c'est pas egal.
          if (curToken.direction == loop(loopCursor).direction)
            lastToken = curToken
          else if (curToken.direction == loop((loopCursor + 1) % loop.length).direction) {
            lastToken = curToken
            loopCursor += 1
          }else
            log.info("on est daans la merde !")
        } else {
          lastToken = curToken
          if (curToken.direction == loop((loopCursor + 1) % loop.length).direction) {
            loopCursor += 1
          }
        }

        loopCursor = loopCursor % loop.length

        curToken match {
          case Straight(dur) => loop(loopCursor) match {
            case Straight(dur2) => loop((loopCursor + 1) % loop.length) match {
              case s:Straight => speed_coef = 1
              case LeftTurn(_,_) if dur < dur2/3 => speed_coef = 1
              case LeftTurn(_,steer) => speed_coef = -steer/gyr_max
              case RightTurn(_,_) if dur < dur2/3 => speed_coef = 1
              case RightTurn(_,steer) => speed_coef = -steer/gyr_max
            }
            case LeftTurn(_,_) => log.debug("merde!")
            case RightTurn(_,_) => log.debug("merde!")
          }
          case LeftTurn(dur,_) => loop(loopCursor) match {
            case LeftTurn(dur2, steering) => loop((loopCursor + 1) % loop.length) match {
              case s:Straight if dur < 2*dur2/3 => speed_coef = -steering/gyr_max
              case Straight(_) => speed_coef = 1
              case LeftTurn(_,_) => speed_coef = -steering/gyr_max
              case RightTurn(_,_) => speed_coef = -steering/gyr_max
            }
            case Straight(_) => log.debug("merde!")
            case RightTurn(_,_) => log.debug("merde!")
          }
          case RightTurn(dur,_) => loop(loopCursor) match {
            case RightTurn(dur2, steering) => loop((loopCursor + 1) % loop.length) match {
              case s:Straight if dur < 2*dur2/3 => speed_coef = -steering/gyr_max
              case Straight(_) => speed_coef = 1
              case LeftTurn(_,_) => speed_coef = -steering/gyr_max
              case RightTurn(_,_) => speed_coef = -steering/gyr_max
            }
            case LeftTurn(_,_) => log.debug("merde!")
            case Straight(_) => log.debug("merde!")
          }
        }

        log.debug("scoef = " + speed_coef)
        log.debug("dir = " + curToken.direction.toString)
        log.debug("prev dur = " + loop(loopCursor).duration.toString)
        log.debug("dur = " + curToken.duration.toString)
        log.debug("next dir = " + loop((loopCursor + 1) % loop.length).direction.toString)
        Main.emitPower(pow + 10 + (40*speed_coef).toInt)

      }

      s match {
        case Sensor(_, ts, (a1, a2, a3), (g1, g2, g3), (m1, m2, m3), t) =>
          //log.info(s"$ts $a1 $a2 $a3 $g1 $g2 $g3 $m1 $m2 $m3 $t")
      }


    case p: Penalty =>
      pow = max_pow - 10
      max_pow = max_pow - 10
      Main.emitPower(pow)

    case r: RoundTime =>
      if (lap == -1) {
        lap = 3
        tokenizer.getLoop()
      } else {
        time_start_lap = r.timestamp
        lap += 1
        log.info("max :" + gyr_max.toString)
        if (lap >= 3){
          log.debug("turns : " + tokenizer.loop)
          loop = tokenizer.getLoop()
        } else {
          pow += 10
          if (pow > max_pow) pow = max_pow
          tokenizer.getLoop()
        }
      }

    case RaceStop =>
      context.become(waitOnStart)
      log.info(s"race stopped")
      log.info(tokenizer.loop.toString())
      log.info("max :" + gyr_max.toString)
      lap = 0
      gyr_max = 0
      max_pow = 140
    case x =>
      //log.warning(s"race stop expected, got: $x")
  }

  def longuestSubSequence(seq: mutable.ListBuffer[Token]): ListBuffer[Token] = {

    def req(nseq: mutable.ListBuffer[Token], tail: mutable.ListBuffer[Token], acc: mutable.ListBuffer[Token]): mutable.ListBuffer[Token] = {
      if (nseq.length > 0) {
        val nseq2 = nseq.take(nseq.length-1)
        val ntail = tail.tail
        val nacc = tail.tail.zip(nseq.take(nseq.length-1)).takeWhile((x) => x._1 == x._2).map(_._1)

        req(nseq2, ntail, if (acc.length < nacc.length) nacc else acc)
      } else {
        acc
      }
    }

    req(seq.clone(), seq, new mutable.ListBuffer[Token]())

  }

}

object ConstantLaps {

  def props(pow: Int): Props = Props(classOf[ConstantLaps], pow)

}


