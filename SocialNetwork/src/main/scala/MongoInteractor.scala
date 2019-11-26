
//import org.mongodb.scala.ChangedStreamsTest.collection
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.Helpers._
import org.mongodb.scala.{ChangedStreamsTest, _}
import org.mongodb.scala.model.Updates._

import org.mongodb.scala.model.Projections._

import collection.mutable._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.changestream.ChangeStreamDocument
import org.mongodb.scala.bson._
import java.util.concurrent.LinkedBlockingDeque

import scala.reflect.ClassTag
object MongoInteractor {
  private val mongoClient: MongoClient = MongoClient()
  private val database: MongoDatabase = mongoClient.getDatabase("mydb")

  val collection: MongoCollection[Document] = database.getCollection("test")
  implicit def toBuffer[A: ClassTag](a: Array[A]) = ArrayBuffer(a: _*)

  implicit val userDecoder: Decoder[User] =
    (hCursor: HCursor) => {
      for {
        id <- hCursor.get[String]("_id")
        userName <- hCursor.get[String]("userName")
        password <- hCursor.get[String]("password")
        name <- hCursor.get[String]("name")
        favouriteThemes <- hCursor.get[Array[Themes]]("favouriteThemes")
        messages <- hCursor.get[Array[Messages]]("messages")
        friends <- hCursor.get[Array[String]]("friends")
        subscribers <- hCursor.get[ArrayBuffer[String]]("subscribers")
        favouriteMessages <- hCursor.get[Array[Path]]("favouriteMessages")
        timeline <- hCursor.get[ArrayBuffer[Path]]("timeline")
      } yield User(id,userName,password, name, favouriteThemes, friends,subscribers, messages, favouriteMessages, timeline)
    }
  implicit val messageDecoder: Decoder[Messages] =
    (hCursor: HCursor) => {
      for {

        text <- hCursor.get[String]("text")
        owner <- hCursor.get[String]("owner")
        theme <- hCursor.get[String]("theme")
        comments <- hCursor.get[Array[Messages]]("comments")
        references <- hCursor.get[Array[String]]("references")
        likes <- hCursor.get[Likes]("likes")
        usersWhoReacted <-hCursor.get[ArrayBuffer[String]]("userWhoReacted")

      } yield Messages(text,owner, Themes(theme), comments,references,likes, usersWhoReacted)
    }


  implicit val pathDecoder: Decoder[Path] =
    (hCursor: HCursor) => {
      for {
        path <- hCursor.get[String]("path")
        ownerOfMessage <- hCursor.get[String]("ownerOfMessage")

      } yield Path(ownerOfMessage, path)
    }
  def authorization(userName: String, password: String):User ={ // TODO: implement smth when user is not decoded
    val foundUser = collection.find(and(equal("userName",userName),
      equal("password", password))).convertToJsonString().stripMargin
    val decodedUser = parser.decode[User](foundUser).toOption.get
    decodedUser
  }

  def addFriendToDataBase(userName: String, friendsName:String): Unit ={
    collection.updateOne(equal("userName", userName),push("friends",friendsName)).results()
    collection.updateOne(equal("userName", friendsName), push("subscribers", userName)).results()
  }

  def writeUserToDatabase(user: User): Unit ={
    val newUserDocument = BsonDocument("_id"->user.id, "userName"->user.userName,
      "password" -> user.password, "name"-> user.name,"favouriteThemes"-> BsonArray(),
      "friends"-> BsonArray(),"subscribers" -> BsonArray(), "messages" -> BsonArray(),"favouriteMessages" ->BsonArray(), "timeline"->BsonArray() )
    collection.insertOne(newUserDocument).results()

  }

  def writeMessageToDatabase(message: Messages, ownerOfMessage: String, lastPositionInMessages: Int,
                             subscribers:ArrayBuffer[String]): Unit ={
    val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme,
      "comments"->BsonArray(), "references"->BsonArray(),"likes"->BsonDocument("likes"->message.likes.likes,
        "dislikes"->message.likes.dislikes,"rating"->message.likes.rating),
      "userWhoReacted"->BsonArray())

    ChangedStreamsTest.collection.updateOne(equal("userName", ownerOfMessage), push("messages", doc)).subscribeAndAwait()
    val path = "messages."+lastPositionInMessages
    val doc2 = BsonDocument("ownerOfMessage"->ownerOfMessage, "path"-> path)
    ChangedStreamsTest.collection.updateOne(equal("userName", ownerOfMessage), push("timeline", doc2)).subscribeAndAwait()

    addPostInSubscribersTimeline(subscribers,doc2)

  }
  def writeCommentToDatabase(message: Messages, path:String, commentedUser: String): Unit ={
    val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme,
      "comments"->BsonArray(), "references"->BsonArray(),"likes"->BsonDocument("likes"->message.likes.likes,
        "dislikes"->message.likes.dislikes,"rating"->message.likes.rating),
      "userWhoReacted"->BsonArray())

    collection.updateOne(equal("userName", commentedUser), push(path, doc)).results()

  }


  def decodeTimeline(timeline: ArrayBuffer[Path]): ArrayBuffer[Messages]={
    val feed: ArrayBuffer[Messages] = ArrayBuffer()
    for(t <- timeline){
      val splitPath:Array[String] = t.path.split("\\.")
      println(t.path)
      println(splitPath.size)
      val message = collection.find(equal("userName",t.ownerOfMessage)).projection(fields(include("text"), slice(splitPath(0),splitPath(1).toInt, 1),excludeId())).convertToJsonString().stripMargin
      var m = message.dropRight(2)
      m = m.drop(14)

      val decodedMessage = parser.decode[Messages](m).toOption.get
      feed+=decodedMessage
    }
    feed
  }



  def addPostInSubscribersTimeline(subscribers: ArrayBuffer[String], doc: BsonDocument):Unit={

    for (s<- subscribers) ChangedStreamsTest.collection.updateOne(equal("userName", s), push("timeline", doc)).subscribeAndAwait()
  }

  def likeMessage(userThatLike:String, ownerOfMessage:String, path:String):Unit={
    val splitPath = path.split("\\.")


    if (!splitPath.contains("comments")){
      collection.updateOne(equal("userName", userThatLike),
        push("favouriteMessages",BsonDocument("ownerOfMessage"->ownerOfMessage, "path"-> path))).results()
    }

    collection.updateOne(equal("userName", ownerOfMessage), inc(path,1)).results()

   // println(splitPath(splitPath.size-1))
    splitPath(splitPath.size-1) = "rating"
    val ratingPath = splitPath.mkString(".")
    collection.updateOne(equal("userName", ownerOfMessage),inc(ratingPath, 1)).results()

    splitPath.remove(splitPath.size-2, 1)
    splitPath(splitPath.size-1) = "usersWhoLiked"
    val usersWhoLikedPath = splitPath.mkString(".")
    collection.updateOne(equal("userName", ownerOfMessage), push(usersWhoLikedPath, userThatLike))

  }
  def dislikeMessage(userThatLike:String, ownerOfMessage:String, path:String):Unit={//TODO: implement adding user to list who disliked message

    collection.updateOne(equal("userName", ownerOfMessage), inc(path,1)).results()
    val splitPath = path.split("\\.")
    splitPath(splitPath.size-1) = "rating"
    val ratingPath = splitPath.mkString(".")
    collection.updateOne(equal("userName", ownerOfMessage),inc(ratingPath, -1)).results()

  }





  def findUser(userName: String):Boolean = {
    val foundUser = collection.find(equal("userName", userName)).results()
    if (foundUser.size == 1) true
    else false

  }
}
