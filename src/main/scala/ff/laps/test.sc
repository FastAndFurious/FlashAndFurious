import ff.tokens.Token

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object test {
  def longuestSubSequence(seq: mutable.ListBuffer[Int]): ListBuffer[Int] = {

    def req(nseq: mutable.ListBuffer[Int], tail: mutable.ListBuffer[Int], acc: mutable.ListBuffer[Int]): mutable.ListBuffer[Int] = {
      if (nseq.length > 0) {
        val nseq2 = nseq.take(nseq.length-1)
        val ntail = tail.tail
        val nacc = tail.tail.zip(nseq.take(nseq.length-1)).takeWhile((x) => x._1 == x._2).map(_._1)

        println(nseq.length + "-" + nacc)

        req(nseq2, ntail, if (acc.length < nacc.length) acc else nacc)
      } else {
        acc
      }
    }

    req(seq.clone(), seq, new mutable.ListBuffer[Int]())

  }

  val lb = new ListBuffer[Int]()
  lb += 1
  lb += 2
  lb += 3
  lb += 4
  lb += 1
  lb += 2
  lb += 3
  lb += 4
  lb += 1
  lb += 2

  longuestSubSequence(lb)
}