import com.mongodb.client.model.changestream.FullDocument
import org.mongodb.scala.{ChangeStreamObservable, Document}
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Filters

import org.mongodb.scala.model.Updates

import scala.collection.mutable.ArrayBuffer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.ChangedStreamsTest._
//import org.mongodb.scala.bson.BsonDocument
//import org.mongodb.scala.model.Filters._
//TODO: implement concurrent execution of different users' actions
object Login extends App{

    //val observable: ChangeStreamObservable[Document] = collection.watch.fullDocument(FullDocument.UPDATE_LOOKUP)
    def printInfo(txt: String): Unit = {
        val thread = Thread.currentThread.getName
        println(s"[$thread] $txt")
    }
    def user1Execution():Unit={
      val user1 = MongoInteractor.authorization("user1","p1")
//        user1.subscribe("user2")
//        user1.subscribe("user3")
       // Thread.sleep(2000)
      Thread.sleep(2000)
        for (i<-0 until 2){
            user1.createMessage(i.toString, Themes("111" ))
            printInfo(i.toString + " "+"user1")
            Thread.sleep(1000)
        }

    }
    def user2Execution():Unit={
        val user2 = MongoInteractor.authorization("user2","p2")
      //  user2.subscribe("user1")
        //user2.subscribe("user3")
        val pipeline = ArrayBuffer(Aggregates.filter(Document("{'fullDocument.userName': 'user2'}")))
      val observable: ChangeStreamObservable[Document] = collection.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP)

      observable.subscribe(observer)

        while(true){



          if(observer.results().nonEmpty){

            val token = observer.results()(observer.results().size-1)
            observable.resumeAfter(token.getResumeToken)
            observer.waitForThenCancel()
          }

         // Thread.sleep(1000)
        }

    }
    def user3Execution():Unit={
        val user3 = MongoInteractor.authorization("user3","p3")
//        user3.subscribe("user1")
//        user3.subscribe("user2")
      Thread.sleep(2000)
        for (i<-0 until 2){
            user3.createMessage(i.toString, Themes("333" ))
            printInfo(i.toString + " "+"user3")
            Thread.sleep(2000)
        }

    }
    def runTask: Seq[Future[Unit]] = {
        Seq (
            Future {
                user1Execution()
            },Future {
                user2Execution()
            },Future {
                user3Execution()
            }

        )
    }
    val task = Future.sequence(runTask)
    Await.result(task, Duration.Inf)

}


