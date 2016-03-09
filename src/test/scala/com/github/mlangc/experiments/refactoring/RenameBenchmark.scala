package com.github.mlangc.experiments.refactoring

import org.scalameter.picklers.noPickler._
import org.scalameter.api._
import scala.tools.refactoring.tests.util.TestRefactoring
import scala.tools.refactoring.implementations.Rename
import scala.tools.refactoring.common.Change
import scala.tools.refactoring.common.SilentTracing
import scala.tools.refactoring.tests.util.TestHelper.PrepResultWithChanges
import org.scalameter.Gen
import org.scalameter.Key.exec

object RenameBenchmark extends Bench.LocalTime with TestRefactoring {
  private def prepareAndRenameTo(name: String)(pro: FileSet): PrepResultWithChanges = {
    val impl = new TestRefactoringImpl(pro) {
      val refactoring = new Rename with TestProjectIndex
    }
    PrepResultWithChanges(Some(impl.preparationResult()), impl.performRefactoring(name))
  }

  private val renames =
    (new FileSet {
      "class /*(*/Foo/*)*/" -> "Foo.scala" becomes
      "class /*(*/Bazius/*)*/" -> "Bazius.scala"
    }, "Bazius") ::
    (new FileSet {
      """
      class Bug(var /*(*/number/*)*/: Int)
      """ becomes
      """
      class Bug(var /*(*/z/*)*/: Int)
      """;

      """
      object Buggy {
        def x = new Bug(32).number
      }
      """ becomes
      """
      object Buggy {
        def x = new Bug(32).z
      }
      """ -> TaggedAsGlobalRename
    }, "z") ::
    (new FileSet {
      """
      package at.lnet.fp

      import scala.annotation.tailrec
      import scala.util.Random

      object Ex3p2 {
        sealed trait Llist[+A] {
          def /*(*/head/*)*/: A
          def tail: Llist[A]
        }

        case object Nada extends Llist[Nothing] {
          def head = ???
          def tail = ???
        }

        case class Cons[+A](head: A, tail: Llist[A]) extends Llist[A] {

        }

        object Llist {
          def apply[A](as: A*): Llist[A] = {
            if (as.isEmpty) Nada
            else Cons(as.head, apply(as.tail: _*))
          }

          def setHead[A](h: A, list: Llist[A]): Llist[A] = {
            list match {
              case Cons(head, tail) => Cons(h, tail)
              case Nada => ???
            }
          }

          @tailrec
          def drop[A](l: Llist[A], n: Int): Llist[A] = {
            if (n <= 0 || (l eq Nada)) {
              l
            } else {
              drop(l.tail, n - 1)
            }
          }

          @tailrec
          def dropWhile[A](l: Llist[A], f: A => Boolean): Llist[A] = {
            if ((l eq Nada) || !f(l.head)) {
              l
            } else {
              dropWhile(l.tail, f)
            }
          }

          def init[A](l: Llist[A]): Llist[A] = l match {
            case Nada => ???
            case Cons(head, tail) => tail match {
              case Nada => Nada
              case _ => Cons(head, init(tail))
            }
          }

          def foldRight[A, B](as: Llist[A], z: B)(f: (A, B) => B): B = {
            as match {
              case Cons(head, tail) => f(head, foldRight(tail, z)(f))
              case Nada => z
            }
          }

          @tailrec
          def foldLeft[A, B](as: Llist[A], z: B)(f: (B, A) => B): B = {
            as match {
              case Cons(head, tail) => foldLeft(tail, f(z, head))(f)
              case Nada => z
            }
          }

          def append[A](l1: Llist[A], l2: Llist[A]): Llist[A] = {
            foldRight(l1, l2)(Cons(_, _))
          }

          def flatten[A](l: Llist[Llist[A]]): Llist[A] = {
            foldRight(l, Nada: Llist[A])((as, acc) => append(as, acc))
          }

          def tryDisproveClaim = {
            val rnd = new Random

            def rndList(len: Int = rnd.nextInt(1000)) = {
              @tailrec
              def go(acc: Llist[Int], n: Int): Llist[Int] = {
                if (n <= 0) acc else go(Cons(rnd.nextInt, acc), n -1)
              }
              go(Nada, len)
            }

            def isCounterExample(list: Llist[Int]) = {
              def frFun(a: Int, b: Int) = a - b
              def flFun(b: Int, a: Int) = a - b

              val z = rnd.nextInt
              val rr = foldRight(list, z)(frFun)
              val rl = foldLeft(reverse(list), z)(flFun)

              rr != rl
            }

            def filterCounterExample(list: Llist[Int]) = {
              if (isCounterExample(list)) Some(list) else None
            }

            val tries = 1000000
            for {
              _ <- (0 until tries);
              counterExample <- filterCounterExample(rndList())
            } yield counterExample
          }

          def reverse[A](list: Llist[A]): Llist[A] = {
            foldLeft(list, Nada: Llist[A])((tail, head) => Cons(head, tail))
          }

          def product2(ns: Llist[Double]) = foldRight(ns, 1.0)(_ * _)

          def length[A](list: Llist[A]) = foldRight(list, 0)((_, z) => z + 1)

          @tailrec
          def hasSubsequence[A](list: Llist[A], seq: Llist[A]): Boolean = {
            @tailrec
            def startsWith[A](list: Llist[A], seq: Llist[A]): Boolean = {
              if (seq == Nada) {
                true
              } else {
                if (list == Nada) {
                  false
                } else {
                  val lh = list.head
                  val sh = seq.head
                  if (lh == sh) {
                    startsWith(list.tail, seq.tail)
                  } else {
                    false
                  }
                }
              }
            }

            if (startsWith(list, seq)) {
              true
            } else {
              if (list == Nada) {
                false
              } else {
                hasSubsequence(list.tail, seq)
              }
            }
          }
        }
      }
      """ becomes
      """
      package at.lnet.fp

      import scala.annotation.tailrec
      import scala.util.Random

      object Ex3p2 {
        sealed trait Llist[+A] {
          def /*(*/kopf/*)*/: A
          def tail: Llist[A]
        }

        case object Nada extends Llist[Nothing] {
          def kopf = ???
          def tail = ???
        }

        case class Cons[+A](kopf: A, tail: Llist[A]) extends Llist[A] {

        }

        object Llist {
          def apply[A](as: A*): Llist[A] = {
            if (as.isEmpty) Nada
            else Cons(as.head, apply(as.tail: _*))
          }

          def setHead[A](h: A, list: Llist[A]): Llist[A] = {
            list match {
              case Cons(head, tail) => Cons(h, tail)
              case Nada => ???
            }
          }

          @tailrec
          def drop[A](l: Llist[A], n: Int): Llist[A] = {
            if (n <= 0 || (l eq Nada)) {
              l
            } else {
              drop(l.tail, n - 1)
            }
          }

          @tailrec
          def dropWhile[A](l: Llist[A], f: A => Boolean): Llist[A] = {
            if ((l eq Nada) || !f(l.kopf)) {
              l
            } else {
              dropWhile(l.tail, f)
            }
          }

          def init[A](l: Llist[A]): Llist[A] = l match {
            case Nada => ???
            case Cons(head, tail) => tail match {
              case Nada => Nada
              case _ => Cons(head, init(tail))
            }
          }

          def foldRight[A, B](as: Llist[A], z: B)(f: (A, B) => B): B = {
            as match {
              case Cons(head, tail) => f(head, foldRight(tail, z)(f))
              case Nada => z
            }
          }

          @tailrec
          def foldLeft[A, B](as: Llist[A], z: B)(f: (B, A) => B): B = {
            as match {
              case Cons(head, tail) => foldLeft(tail, f(z, head))(f)
              case Nada => z
            }
          }

          def append[A](l1: Llist[A], l2: Llist[A]): Llist[A] = {
            foldRight(l1, l2)(Cons(_, _))
          }

          def flatten[A](l: Llist[Llist[A]]): Llist[A] = {
            foldRight(l, Nada: Llist[A])((as, acc) => append(as, acc))
          }

          def tryDisproveClaim = {
            val rnd = new Random

            def rndList(len: Int = rnd.nextInt(1000)) = {
              @tailrec
              def go(acc: Llist[Int], n: Int): Llist[Int] = {
                if (n <= 0) acc else go(Cons(rnd.nextInt, acc), n -1)
              }
              go(Nada, len)
            }

            def isCounterExample(list: Llist[Int]) = {
              def frFun(a: Int, b: Int) = a - b
              def flFun(b: Int, a: Int) = a - b

              val z = rnd.nextInt
              val rr = foldRight(list, z)(frFun)
              val rl = foldLeft(reverse(list), z)(flFun)

              rr != rl
            }

            def filterCounterExample(list: Llist[Int]) = {
              if (isCounterExample(list)) Some(list) else None
            }

            val tries = 1000000
            for {
              _ <- (0 until tries);
              counterExample <- filterCounterExample(rndList())
            } yield counterExample
          }

          def reverse[A](list: Llist[A]): Llist[A] = {
            foldLeft(list, Nada: Llist[A])((tail, head) => Cons(head, tail))
          }

          def product2(ns: Llist[Double]) = foldRight(ns, 1.0)(_ * _)

          def length[A](list: Llist[A]) = foldRight(list, 0)((_, z) => z + 1)

          @tailrec
          def hasSubsequence[A](list: Llist[A], seq: Llist[A]): Boolean = {
            @tailrec
            def startsWith[A](list: Llist[A], seq: Llist[A]): Boolean = {
              if (seq == Nada) {
                true
              } else {
                if (list == Nada) {
                  false
                } else {
                  val lh = list.kopf
                  val sh = seq.kopf
                  if (lh == sh) {
                    startsWith(list.tail, seq.tail)
                  } else {
                    false
                  }
                }
              }
            }

            if (startsWith(list, seq)) {
              true
            } else {
              if (list == Nada) {
                false
              } else {
                hasSubsequence(list.tail, seq)
              }
            }
          }
        }
      }
      """
    }, "kopf") ::
    (new FileSet {
      """
      trait Base {
        protected def /*(*/x/*)*/: Int
      }

      class Derived1 extends Base {
        override protected val x = 9
      }

      class Derived2 extends Base {
        override protected def x = 9
      }
      """ becomes
      """
      trait Base {
        protected def /*(*/xxx/*)*/: Int
      }

      class Derived1 extends Base {
        override protected val xxx = 9
      }

      class Derived2 extends Base {
        override protected def xxx = 9
      }
      """
    }, "xxx") ::
    (new FileSet {
      """
      package at.lnet.fp

      import scala.annotation.tailrec

      object Ex3p25 {
        sealed trait Tree[+A] {
          def /*(*/size/*)*/: Int = {
            this match {
              case Leaf(_) => 1
              case Branch(left, right) => 1 + left.size + right.size
            }
          }

          def maximum[B >: A](implicit cmp: Ordering[B]): A = {
            def go(t: Tree[A]): A = {
              t match {
                case Leaf(a) => a
                case Branch(a, b) =>
                  val am = go(a)
                  val bm = go(b)
                  if (cmp.lteq(am, bm)) bm else am
              }
            }
            go(this)
          }

          def maximumFold[B >: A](implicit cmp: Ordering[B]): A = {
            fold(identity)((left, right) => if (cmp.lteq(left, right)) right else left)
          }

          def depth: Int = {
            this match {
              case Leaf(_) => 0
              case Branch(left, right) => 1 + (left.depth max right.depth)
            }
          }

          def depthFold: Int = {
            fold(_ => 0)((left, right) => 1 + (if (left >= right) left else right))
          }

          def map[B](f: A => B): Tree[B] = {
            this match {
              case Leaf(a) => Leaf(f(a))
              case Branch(left, right) => Branch(left.map(f), right.map(f))
            }
          }

          def mapFold[B](f: A => B): Tree[B] = {
            fold(a => Leaf(f(a)): Tree[B])((left, right) => Branch(left, right))
          }

          def fold[B](f: A => B)(g: (B, B) => B): B = {
            this match {
              case Leaf(a) => f(a)
              case Branch(left, right) => g(left.fold(f)(g), right.fold(f)(g))
            }
          }
        }

        case class Leaf[A](value: A) extends Tree[A]
        case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

        @inline
        implicit def wrapInLeaf[A](a: A) = Leaf(a)
      }
      """ becomes
      """
      package at.lnet.fp

      import scala.annotation.tailrec

      object Ex3p25 {
        sealed trait Tree[+A] {
          def /*(*/sz/*)*/: Int = {
            this match {
              case Leaf(_) => 1
              case Branch(left, right) => 1 + left.sz + right.sz
            }
          }

          def maximum[B >: A](implicit cmp: Ordering[B]): A = {
            def go(t: Tree[A]): A = {
              t match {
                case Leaf(a) => a
                case Branch(a, b) =>
                  val am = go(a)
                  val bm = go(b)
                  if (cmp.lteq(am, bm)) bm else am
              }
            }
            go(this)
          }

          def maximumFold[B >: A](implicit cmp: Ordering[B]): A = {
            fold(identity)((left, right) => if (cmp.lteq(left, right)) right else left)
          }

          def depth: Int = {
            this match {
              case Leaf(_) => 0
              case Branch(left, right) => 1 + (left.depth max right.depth)
            }
          }

          def depthFold: Int = {
            fold(_ => 0)((left, right) => 1 + (if (left >= right) left else right))
          }

          def map[B](f: A => B): Tree[B] = {
            this match {
              case Leaf(a) => Leaf(f(a))
              case Branch(left, right) => Branch(left.map(f), right.map(f))
            }
          }

          def mapFold[B](f: A => B): Tree[B] = {
            fold(a => Leaf(f(a)): Tree[B])((left, right) => Branch(left, right))
          }

          def fold[B](f: A => B)(g: (B, B) => B): B = {
            this match {
              case Leaf(a) => f(a)
              case Branch(left, right) => g(left.fold(f)(g), right.fold(f)(g))
            }
          }
        }

        case class Leaf[A](value: A) extends Tree[A]
        case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

        @inline
        implicit def wrapInLeaf[A](a: A) = Leaf(a)
      }
      """
    }, "sz") ::
    Nil

  private case class ContainerWithCustomToString(renames: Seq[(FileSet, String)]) {
    override def toString = s"${renames.size} rename ops"
  }

  private val gen = Gen.single("renames")(ContainerWithCustomToString(renames))

  performance of "Rename" config(exec.benchRuns -> 100) in {
    using(gen) in { case ContainerWithCustomToString(renames) =>
      renames.foreach { case (fs, newName) =>
        fs.prepareAndApplyRefactoring(prepareAndRenameTo(newName))
      }
    }
  }
}
