package com.github.mlangc.experiments.refactoring

import org.scalameter.PerformanceTest.Quickbenchmark
import com.google.common.io.Resources
import java.nio.file.Files
import org.apache.commons.io.IOUtils
import org.scalameter.Gen
import org.scalameter.Key._
import scala.tools.refactoring.sourcegen.SourceUtils

object SourceUtilsBenchmark extends Quickbenchmark {
  private case class Input(source: String) {
    override def toString = {
      "Input(...)"
    }
  }

  private def testSource = {
     IOUtils.toString(Resources.getResource(getClass, "LongScalaSource.txt"))
  }

  private val input = Gen.single("testSource")(Input(testSource))

  performance of "SourceUtils" in {
    measure method "stripComment" config(exec.benchRuns -> 500) in {
      using(input) in { input =>
        SourceUtils.stripComment(input.source)
      }

      measure method "countRelevantBrackets" in {
        using(input) config(exec.benchRuns -> 500) in { input =>
          SourceUtils.countRelevantBrackets(input.source, '(', ')')
        }
      }
    }
  }
}
