//package scala
import scala.collection.mutable.ArrayBuffer

import scala.Helpers._
import org.mongodb.scala._

case class User(var id: String, userName:String, password: String, name: String,
                favouriteThemes: ArrayBuffer[Themes] = ArrayBuffer(),
                friends: ArrayBuffer[String]= ArrayBuffer(), subscribers:ArrayBuffer[String] = ArrayBuffer(),
                messages: ArrayBuffer[Messages]= ArrayBuffer(), favouriteMessages:
                ArrayBuffer[Path]= ArrayBuffer(),
                timeline: ArrayBuffer[Path]=ArrayBuffer()) {

  def createMessage (text: String, theme: Themes,
                     references: ArrayBuffer[User] = ArrayBuffer()): Unit = {

    val message =  Messages(text, userName, theme, ArrayBuffer(), references)
    messages += message
    MongoInteractor.writeMessageToDatabase(message, userName, messages.size-1, subscribers)

  }

  def printTimeline():Unit={
    val timelineToPrint = MongoInteractor.decodeTimeline(timeline)
    timelineToPrint.foreach(println)
  }

  def repost(text:String, message: Messages, references: ArrayBuffer[User]): Unit ={
    val repostMessage = Messages( text,userName, message.theme,ArrayBuffer[Messages](), references)
    messages+=repostMessage
  }
  def like(path: String, ownerOfMessage:String): Unit ={
    if(!favouriteMessages.exists(x=> x.path == path && x.ownerOfMessage == ownerOfMessage )){
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

object Test extends App{

   // var user1 = new User("user1","fda","a", ArrayBuffer[Themes](), ArrayBuffer[User](), ArrayBuffer[Messages](), ArrayBuffer[Messages]())
    var theme = new Themes("fasdf")

//    user1.createMessage(user1., "ccccc", theme ,ArrayBuffer())
//    user1.createMessage(user1, "bbbbb", theme ,ArrayBuffer())
//    user1.createMessage(user1, "aaaaa", theme ,ArrayBuffer())
//    user1.messages.foreach(x=> println(x.string))

  val mongoClient: MongoClient = MongoClient()



  val database: MongoDatabase = mongoClient.getDatabase("mydb")

  val collection: MongoCollection[Document] = database.getCollection("test")

  collection.drop().results()

  // make a document and insert it
  val doc1: Document = Document( "name" -> "MongoDB", "type" -> "database",
    "count" -> 1, "info" -> Document("x" -> 203, "y" -> 102))

  collection.insertOne(doc1).results()
 //collection.deleteMany(Document())


}