package scala
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.IndexedSeq
import scala.Helpers._
import org.mongodb.scala._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model._
import collection.JavaConverters._
class User(val login:String, var name: String, val favouriteThemes: ArrayBuffer[Themes], val friends: ArrayBuffer[User], val messages: ArrayBuffer[Messages], val favouriteMessages: ArrayBuffer[Messages]) {


  def createMessage (owner: User, string: String, theme: Themes,
                     comments: ArrayBuffer[Messages], references: ArrayBuffer[User] = ArrayBuffer()): ArrayBuffer[Messages] = {
    val message = new Messages(User.this, string, theme, comments, references)
    messages += message
  }


  def repost(text:String, message: Messages, references: ArrayBuffer[User]): Unit = {
    val repostMessage = new Messages(User.this, text, message.theme,ArrayBuffer[Messages](), references)
    messages+=repostMessage
  }

  def like(message: Messages): Unit ={
    message.likes.likes += 1;
    message.likes.rating += 1;

  }


}

object Test extends App{

    var user1 = new User("user1","a", ArrayBuffer[Themes](), ArrayBuffer[User](), ArrayBuffer[Messages](), ArrayBuffer[Messages]())
    var theme = new Themes()
    theme.theme = "tema"
    user1.createMessage(user1, "ccccc", theme ,ArrayBuffer())
    user1.createMessage(user1, "bbbbb", theme ,ArrayBuffer())
    user1.createMessage(user1, "aaaaa", theme ,ArrayBuffer())
    user1.messages.foreach(x=> println(x.string))

  val mongoClient: MongoClient = MongoClient()



  val database: MongoDatabase = mongoClient.getDatabase("mydb")

  val collection: MongoCollection[Document] = database.getCollection("test")

  collection.drop().results()

  // make a document and insert it
  val doc: Document = Document("_id" -> 0, "name" -> "MongoDB", "type" -> "database",
    "count" -> 1, "info" -> Document("x" -> 203, "y" -> 102))
  collection.insertOne(doc).results()

  collection.find().first().printHeadResult()

}