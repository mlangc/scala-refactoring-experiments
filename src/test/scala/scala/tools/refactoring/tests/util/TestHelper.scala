/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package tests.util

import scala.Option.option2Iterable
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.tools.nsc.util.FailedInterrupt
import scala.tools.refactoring.MultiStageRefactoring
import scala.tools.refactoring.Refactoring
import scala.tools.refactoring.common.Change
import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.NewFileChange
import scala.tools.refactoring.common.RenameSourceFileChange
import scala.tools.refactoring.common.Selections
import scala.tools.refactoring.common.TextChange
import scala.tools.refactoring.implementations.Rename
import scala.tools.refactoring.util.CompilerProvider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before

import TestHelper.PrepResultWithChanges
import scala.tools.refactoring.util.UniqueNames

object TestHelper {
  case class PrepResultWithChanges(prepResult: Option[Either[MultiStageRefactoring#PreparationError, Rename#PreparationResult]], changes: List[Change])
}

trait TestHelper extends Refactoring with CompilerProvider with common.InteractiveScalaCompiler {
  import TestHelper._

  @Before
  def cleanup() = resetPresentationCompiler()

  type Test = org.junit.Test
  type Ignore = org.junit.Ignore
  type AbstractFile = tools.nsc.io.AbstractFile

  private case class Source(code: String, filename: String) {
    def toPair = (code, filename)
  }

  private object Source {
    def apply(codeWithFilename: (String, String)) = new Source(codeWithFilename._1, codeWithFilename._2)
  }

  /**
   * A project to test multiple compilation units. Add all
   * sources using "add" before using any of the lazy vals.
   */
  abstract class FileSet(private val baseName: String) {
    def this() = this(UniqueNames.basename())
    private val srcs = ListBuffer[(Source, Source)]()

    object TaggedAsGlobalRename
    object TaggedAsLocalRename
    var expectGlobalRename: Option[Boolean] = None

    override def toString = {
      s"FileSet($baseName)"
    }

    implicit class WrapSource(src: String) {
      def becomes(expected: String) {
        val filename = nextFilename()
        srcs += Source(src, filename) → Source(stripWhitespacePreservers(expected), filename)
      }

      def ->(tag: TaggedAsGlobalRename.type): String = {
        expectGlobalRename = Some(true)
        src
      }

      def ->(tag: TaggedAsLocalRename.type): String = {
        expectGlobalRename = Some(false)
        src
      }

      def ->(filename: String): (String, String) = {
        (src, filename)
      }
    }



    implicit class WrapSourceWithFilename(srcWithName: (String, String)) {
      def becomes(newSrcWithName: (String, String)) {
        srcs += Source(srcWithName) -> Source(stripWhitespacePreservers(newSrcWithName._1), newSrcWithName._2)
      }
    }

    private def nextFilename() = {
      baseName + "_" + srcs.size + ".scala"
    }

    lazy val sources = srcs.unzip._1.map(_.toPair).toList

    val NewFile = ""

    def applyRefactoring(createChanges: FileSet => List[Change]) {
      performRefactoring(createChanges).assertEqualSource
    }

    def prepareAndApplyRefactoring(result: FileSet => PrepResultWithChanges) {
      prepareAndPerformRefactoring(result).assertOk()
    }

    def apply(f: FileSet => List[String]) = assert(f(this), Nil)

    private def assert(res: List[String], sourceRenames: List[RenameSourceFileChange]) = {
      assertEquals(srcs.length, res.length)
      val expected = srcs.unzip._2.toList.map(_.code)
      expected zip res foreach (p => assertEquals(p._1, p._2))

      fileRenameOps.foreach { case (oldName, newName) =>
        assertTrue(s"Missing rename operation $oldName -> $newName", sourceRenames.exists { r =>
          r.sourceFile.name == oldName && r.to == newName
        })
      }

      assertNoDuplicates(sourceRenames.map(_.to), d => s"Destination '$d' appearing multiple times")
      assertNoDuplicates(sourceRenames.map(_.sourceFile.canonicalPath), s => s"Source '$s' appearing multiple times")
    }

    private def assertNoDuplicates[T](values: Seq[T], mkErrMsg: Any => String): Unit = {
      values.groupBy(identity).foreach { case (v, vs) =>
        assertTrue(mkErrMsg(v), vs.size < 2)
      }
    }

    private def fileRenameOps = {
      srcs.collect { case (oldSource, newSource) if oldSource.filename != newSource.filename =>
        (oldSource.filename, newSource.filename)
      }
    }

    def prepareAndPerformRefactoring(createChanges: FileSet => PrepResultWithChanges) = {
      val PrepResultWithChanges(prepResult, changes) = try {
        global.ask { () =>
          createChanges(this)
        }
      } catch {
        case e: FailedInterrupt =>
          throw e.getCause
        case e: InterruptedException =>
          throw e.getCause
      }

      val refactoredCode = sources flatMap {
        case (NewFile, name) =>
          changes collect {
            case nfc: NewFileChange => nfc.text
          }

        case (src, name) =>
          val textFileChanges = changes collect {
            case tfc: TextChange if tfc.sourceFile.file.name == name => tfc
          }
          Change.applyChanges(textFileChanges, src) :: Nil

      } filterNot (_.isEmpty)

      val sourceRenames = changes.collect { case d: RenameSourceFileChange => d }

      new {
        def withResultTree(fn: global.Tree => Unit) = fn(treeFrom(refactoredCode.mkString("\n")))
        def withResultSource(fn: String => Unit) = fn(refactoredCode.mkString("\n"))
        def assertEqualSource() = assert(refactoredCode, sourceRenames)

        def assertOk() = {
          expectGlobalRename.foreach { expectGlobalRename =>
            assertEquals(expectGlobalRename, !prepResult.get.right.get.hasLocalScope)
          }
          assertEqualSource()
        }

        def assertEqualTree() = withResultTree { actualTree =>
          val expectedTree = treeFrom(srcs.head._2.code)
          val (expected, actual) = global.ask { () =>
            (expectedTree.toString(), actualTree.toString())
          }
          assertEquals(expected, actual)
        }
      }
    }


    /**
     * Same as applyRefactoring but does not make the assertion on the
     * refactoring result.
     */
    def performRefactoring(createChanges: FileSet => List[Change]) = {
      prepareAndPerformRefactoring(createChanges.andThen(PrepResultWithChanges(None, _)))
    }

  }

  def selection(refactoring: Selections with InteractiveScalaCompiler, project: FileSet) = {

    val files = project.sources map { case (code, filename) => addToCompiler(filename, code)}
    val trees: List[refactoring.global.Tree] = files map (refactoring.global.unitOfFile(_).body)

    (project.sources zip trees flatMap {
      case (src, tree) =>
        findMarkedNodes(refactoring)(src._1, tree)
    } headOption) getOrElse {
      refactoring.FileSelection(trees.head.pos.source.file, trees.head, 0, 0)
    }
  }

  val startPattern = "/*(*/"
  val endPattern = "/*)*/"
  val emptyPattern = "/*<-*/"

  def findMarkedNodes(r: Selections with InteractiveScalaCompiler)(src: String, tree: r.global.Tree): Option[r.Selection] = {

    val start = commentSelectionStart(src)
    val end = commentSelectionEnd(src)
    val emptySelection = src.indexOf(emptyPattern)

    if (start >= 0 && end >= 0) {
      Some(r.FileSelection(tree.pos.source.file, tree, start, end))
    } else if (emptySelection >= 0) {
      Some(r.FileSelection(tree.pos.source.file, tree, emptySelection, emptySelection))
    } else {
      None
    }
  }

  def cleanTree(t: global.Tree) = {
    global.ask { () =>
      val removeAuxiliaryTrees = ↓(transform {

        case t: global.Tree if (t.pos == global.NoPosition || t.pos.isRange) => t

        case t: global.ValDef => global.noSelfType

        // We want to exclude "extends AnyRef" in the pretty printer tests
        case t: global.Select if t.name.isTypeName && t.name.toString != "AnyRef" => t

        case t => global.EmptyTree
      })

      (removeAuxiliaryTrees &> topdown(setNoPosition))(t).get
    }
  }

  def commentSelectionStart(src: String): Int = {
    src.indexOf(startPattern) + startPattern.length
  }

  def commentSelectionEnd(src: String): Int = {
    src.indexOf(endPattern)
  }

  def stripWhitespacePreservers(s: String) = s.replaceAll("▒", "")

  def findMarkedNodes(src: String, tree: global.Tree) = {
    val start = commentSelectionStart(src)
    val end = commentSelectionEnd(src)
    FileSelection(tree.pos.source.file, tree, start, end)
  }

  def toSelection(src: String) = global.ask{ () =>
    val tree = treeFrom(src)
    findMarkedNodes(src, tree)
  }
}
