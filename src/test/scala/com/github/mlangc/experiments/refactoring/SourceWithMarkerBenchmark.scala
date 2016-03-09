package com.github.mlangc.experiments.refactoring

import org.scalameter.picklers.noPickler._
import org.scalameter.api._
import scala.tools.refactoring.util.SourceWithMarker.Movement
import scala.tools.refactoring.util.SourceWithMarker.Movements
import scala.tools.refactoring.util.SourceWithMarker

object SourceWithMarkerBenchmark extends Bench.LocalTime with LocalResourceSupport {
  def testSource = {
     SourceWithMarker(localResourceAsString("LongScalaSource.txt"))
  }

  val input = Gen.single("testSource")(testSource)

  def runTest(name: String, mvnt: Movement) = {
    performance of name in {
      measure method "apply" in {
        using(input) in { src =>
          require(mvnt(src).isDefined)
        }
      }
    }
  }


  import Movements._

  val packageDef = "package" ~ space.atLeastOnce ~ id ~ ('.' ~ id).zeroOrMore
  val toEnd = until("/*END*/")
  val toStartOfLastTest = toEnd ~ (until("@Test", skipping = (stringLiteral | space | comment)).backward)

  runTest("any", any)
  runTest("parsePreamble", commentsAndSpaces ~ packageDef ~ commentsAndSpaces ~ packageDef)
  runTest("toEnd", toEnd)
  runTest("parseTestBackwards", toStartOfLastTest)
}
