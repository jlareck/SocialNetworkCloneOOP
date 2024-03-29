//TODO: make feed as private instance
import Helpers._
import MongoInteractor._
import org.mongodb.scala.model.Filters.{and, equal}

import scala.collection.mutable.ArrayBuffer
case class User(var id: String, userName:String, password: String, name: String,
                favouriteThemes: ArrayBuffer[Themes] = ArrayBuffer(),
                friends: ArrayBuffer[String]= ArrayBuffer(), subscribers:ArrayBuffer[String] = ArrayBuffer(),
                messages: ArrayBuffer[Messages]= ArrayBuffer(), favouriteMessages:
                ArrayBuffer[Path]=ArrayBuffer(),
                timeline: ArrayBuffer[Path]=ArrayBuffer()){

  def createMessage (text: String, theme: Themes,
                     references: ArrayBuffer[String] = ArrayBuffer()): Unit = {//add post to timeline???

    val message =  Messages(text, userName, theme, ArrayBuffer(), references)
    messages += message

    MongoInteractor.writeMessageToDatabase(message, userName, messages.size-1, subscribers)

  }

  def printTimeline():Unit={
    if (timeline.nonEmpty){
      val timelineToPrint = MongoInteractor.decodeTimeline(timeline)
      timelineToPrint.reverse.foreach(println)
    }

  }

  def repost(path:Path, references: ArrayBuffer[String]): Unit ={
    val repostedMessage = decodePost(path)
    repostedMessage.references.appendAll(references)
    messages+=repostedMessage
    MongoInteractor.writeMessageToDatabase(repostedMessage, userName,messages.size-1, subscribers)


  }
  def comment(text: String, path:String, commentedUser: String, theme: Themes, references: ArrayBuffer[String]=ArrayBuffer() ): Unit ={
    val commentMessage =  Messages(text, userName, theme, ArrayBuffer(), references)


      if (commentedUser == userName){
      val splitPath = path.split("\\.").toBuffer
      splitPath -= Fields.messages
      def getToMessage( messages:ArrayBuffer[Messages],listPath: List[String]): ArrayBuffer[Messages] ={
        listPath.head  match{
          //case "messages" => getToMessage(index, messages, listPath.tail)
          case Int(i) => if(listPath.tail.isEmpty)messages else getToMessage( messages(i).comments, listPath.tail)
          case Fields.comments =>if(listPath.tail.isEmpty) messages else getToMessage(messages,listPath.tail)

        }
      }
      getToMessage(messages,splitPath.toList) += commentMessage
    }
    writeCommentToDatabase(commentMessage, path,commentedUser)
  }
  def like(pathToMessage: Path): Unit ={
    val a = collectionTest.find(and(equal(Fields.userName, pathToMessage.ownerOfMessage),
      equal(pathToMessage.path+s".${Fields.usersWhoReacted}", userName))).results()

    if(a.isEmpty){
      if (pathToMessage.ownerOfMessage == userName){
        val splitPath = pathToMessage.path.split("\\.").toBuffer
        splitPath -= Fields.messages
        def getToMessage( messages:ArrayBuffer[Messages],listPath: List[String]): Messages ={
          listPath.head  match{
            //case "messages" => getToMessage(index, messages, listPath.tail)
            case Int(i) => if(listPath.tail.isEmpty)messages(i) else getToMessage (messages(i).comments, listPath.tail)
            case Fields.comments => getToMessage(messages,listPath.tail)

          }
        }
        getToMessage(messages, splitPath.toList).likes.likes+=1
        getToMessage(messages, splitPath.toList).likes.rating+=1

      }
      MongoInteractor.reactOnMessage(userName, pathToMessage, "like")
    }
    else{
      println("You have already liked this post")
    }

  }
  def dislike(pathToMessage: Path): Unit ={
    val a = collectionTest.find(and(equal(Fields.userName, pathToMessage.ownerOfMessage),
      equal(pathToMessage.path+s".${Fields.usersWhoReacted}", userName))).results()

    if(a.isEmpty){
      if (pathToMessage.ownerOfMessage == userName){
        val splitPath = pathToMessage.path.split("\\.").toBuffer
        splitPath -= Fields.messages
        def getToMessage( messages:ArrayBuffer[Messages],listPath: List[String]): Messages ={
          listPath.head  match{

            case Int(i) => if(listPath.tail.isEmpty)messages(i) else getToMessage (messages(i).comments, listPath.tail)
            case Fields.comments => getToMessage(messages,listPath.tail)

          }
        }
        getToMessage(messages, splitPath.toList).likes.dislikes-=1
        getToMessage(messages, splitPath.toList).likes.rating-=1

      }
      MongoInteractor.reactOnMessage(userName, pathToMessage, "dislike")
    }
    else {
      println("You have already disliked this post")
    }

  }
  def subscribeOnUser(friendsName: String): Unit ={
      if (MongoInteractor.findUser(friendsName) && !friends.contains(friendsName) && userName!=friendsName){

        friends+=userName
        MongoInteractor.subscribeOnUser(userName,friendsName)

        println("You subscribed successfully")
      }
      else println("You cannot subscribe on " + friendsName)
  }
}


object Int {
  def unapply(s: String): Option[Int] = util.Try(s.toInt).toOption
}