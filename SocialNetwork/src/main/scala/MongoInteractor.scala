

import scala.reflect.ClassTag
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.Helpers._
import org.mongodb.scala._
import org.mongodb.scala.model.Updates._

import collection.mutable._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.bson._
import org.mongodb.scala.bson.ObjectId
object MongoInteractor {
  private val mongoClient: MongoClient = MongoClient()
  private val database: MongoDatabase = mongoClient.getDatabase("mydb")

  private val collection: MongoCollection[Document] = database.getCollection("test")
  implicit def tobuffer[A: ClassTag](a: Array[A]) = ArrayBuffer(a: _*)
  implicit val userDecoder: Decoder[User] =
    (hCursor: HCursor) => {
      for {
        id <- hCursor.get[ObjectId]("_id")
        userName <- hCursor.get[String]("userName")
        password <- hCursor.get[String]("password")
        name <- hCursor.get[String]("name")
        favourireThemes <- hCursor.get[Array[Themes]]("favouriteThemes")
        messages <- hCursor.get[Array[Messages]]("messages")
        friends <- hCursor.get[Array[User]]("friends")
        favouriteMessages <- hCursor.get[Array[Messages]]("favouriteMessages")
      } yield User(id.toString,userName,password, name, favourireThemes, friends, messages, favouriteMessages)
    }

  def authorization(userName: String, password: String):Unit ={
      val a = collection.find(and(equal("userName",userName),equal("password", password))).convertToJsonString().stripMargin
      val b = parser.decode[User](a)
      println(b)

  }

  def writeUserToDatabase(user: User): Unit ={
      val newUserDocument = BsonDocument("userName"->user.userName, "password" -> user.password, "name"-> user.name,"favouriteThemes"-> BsonArray(),
        "friends"-> BsonArray(), "messages" -> BsonArray(),"favouriteMessages" ->BsonArray() )
      collection.insertOne(newUserDocument).results()
  }
  def writeMessageToDatabase(user: User,message: Messages): Unit ={

    if(user.messages.isEmpty){
        val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme, "comments"->BsonDocument(), "references"->BsonDocument())

        collection.updateOne(equal("userName", user.userName),  push("messages", doc) )
    }


    //collection.insertOne(doc).results()
  }


}
