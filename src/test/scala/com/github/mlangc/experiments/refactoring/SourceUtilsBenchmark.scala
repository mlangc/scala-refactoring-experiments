package com.github.mlangc.experiments.refactoring

import scala.tools.refactoring.sourcegen.SourceUtils

import org.scalameter.api._
import org.scalameter.picklers.noPickler._

object SourceUtilsBenchmark extends Bench.LocalTime with LocalResourceSupport {
  private case class Input(source: String) {
    override def toString = {
      "Input(...)"
    }
  }

  private def testSource = {
    localResourceAsString("LongScalaSource.txt")
  }

  private val input = Gen.single("testSource")(Input(testSource))

  private trait SourceUtilsAdapter {
    def countRelevantBrackets(source: String, open: Char, close: Char): (Int, Int)
    def stripComment(source: String): String
    def name: String
  }

  private implicit class LegacySourceUtilsAdapter(underlying: LegacySourceUtils) extends SourceUtilsAdapter {
    override def countRelevantBrackets(source: String, open: Char, close: Char) = underlying.countRelevantBrackets(source, open, close)
    override def stripComment(source: String): String = underlying.stripComment(source)
    override def name = "LegacySourceUtils"
  }

  private implicit class ActualSourceUtilsAdapter(underlying: SourceUtils) extends SourceUtilsAdapter {
    override def countRelevantBrackets(source: String, open: Char, close: Char) = underlying.countRelevantBrackets(source, open, close)
    override def stripComment(source: String): String = underlying.stripComment(source)
    override def name = "SourceUtils"
  }

  private def runTests(adapter: SourceUtilsAdapter, testStripComment: Boolean = false, testCountBrackets: Boolean): Unit = {
    performance of adapter.name in {
      if (testStripComment) {
        measure method "stripComment" config(exec.benchRuns -> 500) in {
          using(input) in { input =>
            adapter.stripComment(input.source)
          }
        }
      }

      if (testCountBrackets) {
        measure method "countRelevantBrackets" in {
          using(input) config(exec.benchRuns -> 500) in { input =>
            adapter.countRelevantBrackets(input.source, '(', ')')
          }
        }
      }
    }
  }

  runTests(LegacySourceUtils, testCountBrackets = true)
  runTests(SourceUtils, testCountBrackets = true)
}
