//package scala
import scala.collection.mutable.ArrayBuffer


import org.mongodb.scala._
import MongoInteractor._
case class User(var id: String, userName:String, password: String, name: String,
                favouriteThemes: ArrayBuffer[Themes] = ArrayBuffer(),
                friends: ArrayBuffer[String]= ArrayBuffer(), subscribers:ArrayBuffer[String] = ArrayBuffer(),
                messages: ArrayBuffer[Messages]= ArrayBuffer(), favouriteMessages:
                ArrayBuffer[Path]=ArrayBuffer(),
                timeline: ArrayBuffer[Path]=ArrayBuffer()) {

  def createMessage (text: String, theme: Themes,
                     references: ArrayBuffer[String] = ArrayBuffer()): Unit = {

    val message =  Messages(text, userName, theme, ArrayBuffer(), references)
    messages += message
    MongoInteractor.writeMessageToDatabase(message, userName, messages.size-1, subscribers)

  }

  def printTimeline():Unit={
    val timelineToPrint = MongoInteractor.decodeTimeline(timeline)
    timelineToPrint.foreach(println)
  }

  def repost(text:String, message: Messages, references: ArrayBuffer[String]): Unit ={
    val repostMessage = Messages( text,userName, message.theme,ArrayBuffer[Messages](), references)
    messages+=repostMessage
  }
  def comment(text: String, path:String, commentedUser: String, theme: Themes, references: ArrayBuffer[String]=ArrayBuffer() ): Unit ={
    val commentMessage =  Messages(text, commentedUser, theme, ArrayBuffer(), references)


    if (commentedUser == userName){
      val splitPath = path.split("\\.").toBuffer
      splitPath -= "messages"
      def getToMessage( messages:ArrayBuffer[Messages],listPath: List[String]): ArrayBuffer[Messages] ={
        listPath.head  match{
          //case "messages" => getToMessage(index, messages, listPath.tail)
          case Int(i) => if(listPath.tail.isEmpty)messages else getToMessage( messages(i).comments, listPath.tail)
          case "comments" =>if(listPath.tail.isEmpty) messages else getToMessage(messages,listPath.tail)

        }
      }
      getToMessage(messages,splitPath.toList) += commentMessage
    }
    writeCommentToDatabase(commentMessage, path,commentedUser)
  }
  def like(path: String, ownerOfMessage:String): Unit ={

    if(!favouriteMessages.exists(x=> x.path == path && x.ownerOfMessage == ownerOfMessage )){
      if (ownerOfMessage == userName){
        val splitPath = path.split("\\.").toBuffer
        splitPath -= "messages"
        def getToMessage( messages:ArrayBuffer[Messages],listPath: List[String]): Likes ={
          listPath.head  match{
            //case "messages" => getToMessage(index, messages, listPath.tail)
            case Int(i) => if(listPath.tail.size == 2)messages(i).likes else getToMessage (messages(i).comments, listPath.tail)
            case "comments" => getToMessage(messages,listPath.tail)

          }
        }
        getToMessage(messages, splitPath.toList).likes+=1
      }
      MongoInteractor.likeMessage(userName, ownerOfMessage,path)
    }
  }

  def subscribe(friendsName: String): Unit ={
      if (MongoInteractor.findUser(userName)){ // TODO: implement smth when user want to subscribe on self

        friends+=userName
        MongoInteractor.addFriendToDataBase(userName,friendsName)

        println("You subscribed successfully")
      }
      else println("There is no person with name " + friendsName)
  }
}


object Int {
  def unapply(s: String): Option[Int] = util.Try(s.toInt).toOption
}