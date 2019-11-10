package scala
import scala.collection.mutable.ArrayBuffer

import scala.Helpers._
import org.mongodb.scala._

class User(val userName:String,val password: String, var name: String, val favouriteThemes: ArrayBuffer[Themes] = ArrayBuffer(), val friends: ArrayBuffer[User]= ArrayBuffer(), val messages: ArrayBuffer[Messages]= ArrayBuffer(), val favouriteMessages: ArrayBuffer[Messages]= ArrayBuffer()) {




  def createMessage (owner: String, string: String, theme: Themes,
                     comments: ArrayBuffer[Messages], references: ArrayBuffer[User] = ArrayBuffer()): ArrayBuffer[Messages] = {
    val message = new Messages(owner, string, theme, comments, references)
    messages += message
  }


  def repost(text:String, message: Messages, references: ArrayBuffer[User]): Unit = {
    val repostMessage = new Messages(User.this.userName, text, message.theme,ArrayBuffer[Messages](), references)
    messages+=repostMessage
  }

  def like(message: Messages): Unit ={
    if(!favouriteMessages.exists(_.string == message.string)){
      message.likes.likes += 1;
      message.likes.rating += 1;
      favouriteMessages+=message
    }

  }


}

object Test extends App{

    var user1 = new User("user1","fda","a", ArrayBuffer[Themes](), ArrayBuffer[User](), ArrayBuffer[Messages](), ArrayBuffer[Messages]())
    var theme = new Themes()
    theme.theme = "tema"
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