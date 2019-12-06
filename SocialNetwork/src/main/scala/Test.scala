
import com.mongodb.client.model.changestream.FullDocument
import org.mongodb.scala.{ChangeStreamObservable, Document}
import org.mongodb.scala.model.Aggregates

import scala.collection.mutable.ArrayBuffer
import Helpers._


import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global


object SimpleTest extends App{
  def user1Execution():Unit={
    val user1 = MongoInteractor.authorization("user1","p1")
    user1.subscribeOnUser("user2")
    Thread.sleep(1000)
    for (i<-0 until 2){
      user1.createMessage("Hello world", Themes("111" ))
      Test.printInfo(i.toString + " "+"user1")
      Thread.sleep(1000)
    }
  }
  def user2Execution():Unit={
    val user2 = MongoInteractor.authorization("user2","p2")
    user2.subscribeOnUser("user1")
    val pipeline = ArrayBuffer(Aggregates.filter(Document("{'fullDocument.userName': 'user2'}")))
    val observable: ChangeStreamObservable[Document] = collectionTest.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP)

    observable.subscribe(observer)
    Thread.sleep(1000)
    user2.repost(Path( "user1","messages.0"), ArrayBuffer("user1"))
    user2.like(Path( "user1","messages.0"))
    for (i<-0 until 1){
      user2.createMessage("AAAAAAAAAAAAA", Themes("111" ))
      Test.printInfo(i.toString + " "+"user2")
      Thread.sleep(1000)
    }
  }
  def user3Execution():Unit={
    val user3 = MongoInteractor.authorization("user3","p3")
    user3.subscribeOnUser("user2")
    val pipeline = ArrayBuffer(Aggregates.filter(Document("{'fullDocument.userName': 'user3'}")))
    val observable: ChangeStreamObservable[Document] = collectionTest.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP)

    observable.subscribe(observer)
    Thread.sleep(1000)
    user3.comment("WOW", "messages.0", "user2",Themes("aaaaaa"))
    user3.createMessage("fasdf",Themes("f111"),ArrayBuffer("user1"))
    for (i<-0 until 2){
      user3.createMessage("Hello "+i, Themes("111" ))
      Test.printInfo(i.toString + " "+"user3")
      Thread.sleep(1000)
    }
  }
  def runTask: Seq[Future[Unit]] = {
    Seq (
      Future {
        user1Execution()
      }, Future {
        user2Execution()
      }, Future{
        user3Execution()
      }




    )
  }
  val task = Future.sequence(runTask)
  Await.result(task, Duration.Inf)
  // val user3 = MongoInteractor.authorization("user3","p3")
  //user3.printTimeline()
}

object Test extends App{

     Registration.registrationTest() //uncomment if you want to write 4 users in data base
    //val user = MongoInteractor.authorization("user2","p2")
 // user.createMessage("Hello world", Themes("hello" ))
    def printInfo(txt: String): Unit = {
        val thread = Thread.currentThread.getName
        println(s"[$thread] $txt")
    }
    def user1Execution():Unit={

        val user1 = MongoInteractor.authorization("user1","p1")
//        user1.subscribe("user2")
        user1.subscribeOnUser("user3")
//        Thread.sleep(1000)
//    // val a = Path("user2", "messages.3")
     //   user1.comment("LALDLSA", "messages.0", "user2", Themes("hhhhh"))
        Thread.sleep(1000)
//
       for (i<-0 until 1){
        user1.createMessage("asfds", Themes("111" ))
            printInfo(i.toString + " "+"user1")
            Thread.sleep(1000)
        }

    }
    def user2Execution():Unit={
      val user2 = MongoInteractor.authorization("user2","p2")
      user2.subscribeOnUser("user1")
      val pipeline = ArrayBuffer(Aggregates.filter(Document("{'fullDocument.userName': 'user2'}")))
      val observable: ChangeStreamObservable[Document] = collectionTest.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP)


      observable.subscribe(observer)

      Thread.sleep(1000)
      for (i<-0 until 2){
        user2.createMessage(i.toString, Themes("222" ))
        //printInfo(i.toString + " "+"user2")
        //Thread.sleep(2000)
      }
//
//          // Thread.sleep(1000)





    }
    def user3Execution():Unit={
      val user3 = MongoInteractor.authorization("user3","p3")
      user3.subscribeOnUser("user2")
      val pipeline = ArrayBuffer(Aggregates.filter(Document("{'fullDocument.userName': 'user3'}")))
      val observable: ChangeStreamObservable[Document] = collectionTest.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP)

    //  observable.subscribe(observer)
//        Thread.sleep(1000)
//       for (i<-0 until 2){
//           user3.createMessage(i.toString, Themes("333" ))
//            printInfo(i.toString + " "+"user3")
////            //Thread.sleep(2000)
//       }
    //  user3.repost(Path( "user2","messages.0"), ArrayBuffer("user1"))
    }
    def runTask: Seq[Future[Unit]] = {
        Seq (
            Future {
                user1Execution()
            }, Future {
                user2Execution()
            }, Future {
             //   user3Execution()
           }

        )
    }

      //val user = MongoInteractor.authorization("user0", "p0")
    //user.comment("aaaaaa", "messages.1", "user1", Themes("kek"))
//    val path = Path("user1","messages.1.comments.0")
//    user.like(path)

    //  val user1 = MongoInteractor.authorization("user1", "p1")
      //user1.printTimeline()
}

