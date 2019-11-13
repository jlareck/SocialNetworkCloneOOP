

import scala.reflect.ClassTag
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.bson.types.ObjectId
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
        id <- hCursor.get[String]("_id")
        userName <- hCursor.get[String]("userName")
        password <- hCursor.get[String]("password")
        name <- hCursor.get[String]("name")
        favourireThemes <- hCursor.get[Array[Themes]]("favouriteThemes")
        messages <- hCursor.get[Array[Messages]]("messages")
        friends <- hCursor.get[Array[User]]("friends")
        favouriteMessages <- hCursor.get[Array[Messages]]("favouriteMessages")
      } yield User(id,userName,password, name, favourireThemes, friends, messages, favouriteMessages)
    }
  implicit val messageDecoder: Decoder[Messages] =
    (hCursor: HCursor) => {
      for {
        text <- hCursor.get[String]("text")
        owner <- hCursor.get[String]("owner")
        theme <- hCursor.get[String]("theme")
        comments <- hCursor.get[Array[Messages]]("comments")
        references <- hCursor.get[Array[User]]("references")
        likes <- hCursor.get[Likes]("likes")

      } yield Messages(text,owner, Themes(theme), comments,references,likes)
    }
  def authorization(userName: String, password: String):User ={
      val foundUser = collection.find(and(equal("userName",userName),equal("password", password))).convertToJsonString().stripMargin
      val decodedUser = parser.decode[User](foundUser).toOption.get
      decodedUser
  }

  def writeUserToDatabase(user: User): Unit ={
      val newUserDocument = BsonDocument("_id"->user.id, "userName"->user.userName,
        "password" -> user.password, "name"-> user.name,"favouriteThemes"-> BsonArray(),
        "friends"-> BsonArray(), "messages" -> BsonArray(),"favouriteMessages" ->BsonArray() )
      collection.insertOne(newUserDocument).results()
  }
  def writeMessageToDatabase(message: Messages, path:String): Unit ={
    val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme, "comments"->BsonArray(),
      "references"->BsonArray(),"likes"->BsonDocument("likes"->message.likes.likes,"dislikes"->message.likes.dislikes,"rating"->message.likes.rating))

//    if(user.messages.isEmpty){
//
//        collection.updateOne(equal("userName", user.userName),  push("messages", doc) )
//    }
//    else{
    collection.updateOne(equal("userName", message.owner), push(path, doc)).results()


    //collection.insertOne(doc).results()
  }


}
