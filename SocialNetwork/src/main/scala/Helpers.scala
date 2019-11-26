
package scala


import org.mongodb.scala.ChangedStreamsTest.LatchedObserver




import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.mongodb.scala._
//TODO: remove duplication of ObservableExecutor
object Helpers {
  val waitDuration = Duration(10, "seconds")

  implicit class ObservableExecutor[T](observable: Observable[T]) {
    def execute(): Seq[T] = Await.result(observable.toFuture(), waitDuration)

    def subscribeAndAwait(): Unit = {
      val observer: LatchedObserver[T] = new LatchedObserver[T](false)
      observable.subscribe(observer)
      observer.await()
    }
  }
  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: (Document) => String = (doc) => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]

    val converter: (C) => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration.Inf)
    def headResult() = Await.result(observable.head(), Duration.Inf)
    def printResults(initial: String = ""): Unit = {
      if (initial.length > 0) print(initial)
      results().foreach(res => println(converter(res)))
    }
    def convertToJsonString(initial: String = ""): String = s"${initial}${converter(headResult())}"
    def printHeadResult(initial: String = ""): Unit = println(s"${initial}${converter(headResult())}")
//    def subscribeAndAwait(): Unit = {
//      val observer: LatchedObserver[C] = new LatchedObserver[C](false)
//      observable.subscribe(observer)
//      observer.await()
//    }
  }

}