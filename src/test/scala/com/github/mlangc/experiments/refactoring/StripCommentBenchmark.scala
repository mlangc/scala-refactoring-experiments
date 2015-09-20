package com.github.mlangc.experiments.refactoring

import org.scalameter.PerformanceTest.Quickbenchmark
import com.google.common.io.Resources
import java.nio.file.Files
import org.apache.commons.io.IOUtils
import org.scalameter.Gen
import org.scalameter.Key._
import scala.tools.refactoring.sourcegen.CommentsUtils

object StripCommentBenchmark extends Quickbenchmark {
  private case class Input(source: String) {
    override def toString = {
      "Input(...)"
    }
  }
  
  private def testSource = {
     IOUtils.toString(Resources.getResource(getClass, "StripCommentBenchmarkInput.txt"))
  }
  
  private val input = Gen.single("testSource")(Input(testSource))
  
  
  performance of "stripComment" config(exec.benchRuns -> 500) in {
    using(input) in { input =>
      CommentsUtils.stripComment(input.source)
    }
  }
}
