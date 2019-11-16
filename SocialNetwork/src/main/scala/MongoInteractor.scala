

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

import org.mongodb.scala.bson._

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
        friends <- hCursor.get[Array[String]]("friends")
        favouriteMessages <- hCursor.get[Array[PathToFavouriteMessage]]("favouriteMessages")
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

  def authorization(userName: String, password: String):User ={ // TODO: implement smth when user in not decoded
    val foundUser = collection.find(and(equal("userName",userName),
      equal("password", password))).convertToJsonString().stripMargin
    val decodedUser = parser.decode[User](foundUser).toOption.get
    decodedUser
  }

  def addFriendToDataBase(userName: String, friendsName:String): Unit ={
    collection.updateOne(equal("userName", userName),push("friends",friendsName)).results()
  }

  def writeUserToDatabase(user: User): Unit ={
    val newUserDocument = BsonDocument("_id"->user.id, "userName"->user.userName,
      "password" -> user.password, "name"-> user.name,"favouriteThemes"-> BsonArray(),
      "friends"-> BsonArray(), "messages" -> BsonArray(),"favouriteMessages" ->BsonArray() )
    collection.insertOne(newUserDocument).results()
  }

  def writeMessageToDatabase(message: Messages, path:String, commentedUser: String): Unit ={
    val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme,
      "comments"->BsonArray(), "references"->BsonArray(),"likes"->BsonDocument("likes"->message.likes.likes,
        "dislikes"->message.likes.dislikes,"rating"->message.likes.rating))

    collection.updateOne(equal("userName", commentedUser), push(path, doc)).results()
  }

  def likeMessage(userThatLike:String, ownerOfMessageToBeLiked:String, path:String):Unit={
    collection.updateOne(equal("userName", userThatLike),
      push(path,BsonDocument("ownerOfMessage"->ownerOfMessageToBeLiked, "path"-> path)))

    collection.updateOne(equal("userName", ownerOfMessageToBeLiked), inc(path,1))
    val splitedPath = path.split(".")
    splitedPath(splitedPath.size-1) = "rating"
    val ratingPath = splitedPath.mkString(".")
    collection.updateOne(equal("userName", ownerOfMessageToBeLiked),inc(ratingPath, 1))

  }
  def dislikeMessage(userThatLike:String, ownerOfMessageToBeLiked:String, path:String):Unit={

    collection.updateOne(equal("userName", ownerOfMessageToBeLiked), inc(path,1))
    val splitPath = path.split(".")
    splitPath(splitPath.size-1) = "rating"
    val ratingPath = splitPath.mkString(".")
    collection.updateOne(equal("userName", ownerOfMessageToBeLiked),inc(ratingPath, -1))

  }
  


  def findUser(userName: String):Boolean = {
    val foundUser = collection.find(equal("userName", userName)).results()
    if (foundUser.size == 1) true
    else false

  }
}
