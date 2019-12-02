

import org.mongodb.scala._
import MongoInteractor._
import io.circe._
import io.circe.generic.auto._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

import org.mongodb.scala.model.changestream.ChangeStreamDocument

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import scala.collection.mutable

object Helpers {

  private val mongoClient: MongoClient = MongoClient()
  private val database: MongoDatabase = mongoClient.getDatabase("mydb")

  val collectionTest: MongoCollection[Document] = database.getCollection("test")

  var observer = new LatchedObserver[ChangeStreamDocument[Document]]()

  val waitDuration = Duration(60, "seconds")

  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: Document => String = doc => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: C => String = doc => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]

    val converter: C => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration.Inf)

    def headResult() = Await.result(observable.head(), Duration.Inf)
    def printResults(initial: String = ""): Unit = {
      if (initial.length > 0) print(initial)
      results().foreach(res => println(converter(res)))
    }
    def convertToJsonString(initial: String = ""): String = s"${initial}${converter(headResult())}"

    def subscribeAndAwait(): Unit = {
      val observer: LatchedObserver[C] = new LatchedObserver[C](false)
      observable.subscribe(observer)
      observer.await()

    }
  }
  class LatchedObserver[T](val printResults: Boolean = true, val minimumNumberOfResults: Int = 1) extends Observer[T] {
    private val latch: CountDownLatch = new CountDownLatch(1)
    private val resultsBuffer: mutable.ArrayBuffer[T] = new mutable.ArrayBuffer[T]
    private var subscription: Option[Subscription] = None
    private var error: Option[Throwable] = None

    override def onSubscribe(s: Subscription): Unit = {
      subscription = Some(s)
      s.request(Integer.MAX_VALUE)
    }

    override def onNext(r: T): Unit = { //TODO: Show user who commented/liked message
      resultsBuffer+=r

      if (printResults) r match{
        case r: ChangeStreamDocument[Document] => {
          val userWhoseDocumentWasModified = r.getFullDocument.get("_id").get.asString.getValue
          val updatedFields = r.getUpdateDescription.getUpdatedFields
          val valuesOfUpdatedFields = updatedFields.get(updatedFields.getFirstKey)
          if(updatedFields.getFirstKey.contains("timeline")){
            val decodedObject = parser.decode[Path](valuesOfUpdatedFields.toString).toOption.get

            Login.printInfo(userWhoseDocumentWasModified + "  " + decodePost(decodedObject).toString)
          }
          else if (updatedFields.getFirstKey.contains("comments")){
            Login.printInfo("Someone commented post of " + userWhoseDocumentWasModified)
          }
          else if (updatedFields.getFirstKey.contains("likes")){
            Login.printInfo("Someone reacted on message of" + userWhoseDocumentWasModified)
          }

        }
        case _=> println("Parse Error")
      }
      if (resultsBuffer.size >= minimumNumberOfResults) latch.countDown()
    }

    override def onError(t: Throwable): Unit = {
      error = Some(t)
      println(t.getMessage)
      onComplete()
    }

    override def onComplete(): Unit = {
      latch.countDown()
    }

    def results(): mutable.ArrayBuffer[T] = resultsBuffer

    def await(): Unit = {
      if (!latch.await(60, SECONDS)) throw new MongoTimeoutException("observable timed out")
      if (error.isDefined) throw error.get
    }
    def waitForThenCancel(): Unit = {
      if (minimumNumberOfResults > resultsBuffer.size) await()
      subscription.foreach(_.unsubscribe())
    }
  }

}