
import io.circe._
import io.circe.generic.auto._

import Helpers._

import org.mongodb.scala._
import org.mongodb.scala.model.Updates._

import org.mongodb.scala.model.Projections.{fields, slice, include, excludeId,exclude}


import org.mongodb.scala.model.Filters.{equal,elemMatch, and}

import org.mongodb.scala.bson._
import scala.collection.mutable.ArrayBuffer

import scala.reflect.ClassTag
object MongoInteractor {

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
        subscribers <- hCursor.get[Array[String]]("subscribers")
        favouriteMessages <- hCursor.get[Array[Path]]("favouriteMessages")
        timeline <- hCursor.get[Array[Path]]("timeline")
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
        usersWhoReacted <-hCursor.get[Array[String]]("userWhoReacted")

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

      val foundUser = collectionTest.find(and(equal("userName",userName),
        equal("password", password))).convertToJsonString().stripMargin

      val decodedUser = parser.decode[User](foundUser).toOption.get
      decodedUser




  }

  def addFriendToDataBase(userName: String, friendsName:String): Unit ={
    collectionTest.updateOne(equal("userName", userName),push("friends",friendsName)).results()
    collectionTest.updateOne(equal("userName", friendsName), push("subscribers", userName)).results()
  }

  def writeUserToDatabase(user: User): Unit ={
    val newUserDocument = BsonDocument("_id"->user.id, "userName"->user.userName,
      "password" -> user.password, "name"-> user.name,"favouriteThemes"-> BsonArray(),
      "friends"-> BsonArray(),"subscribers" -> BsonArray(), "messages" -> BsonArray(),"favouriteMessages" ->BsonArray(), "timeline"->BsonArray() )
    collectionTest.insertOne(newUserDocument).results()

  }

  def writeMessageToDatabase(message: Messages, ownerOfMessage: String, lastPositionInMessages: Int,
                             subscribers:ArrayBuffer[String]): Unit ={
    val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme,
      "comments"->BsonArray(), "references"->BsonArray(),"likes"->BsonDocument("likes"->message.likes.likes,
        "dislikes"->message.likes.dislikes,"rating"->message.likes.rating),
      "userWhoReacted"->BsonArray())

    collectionTest.updateOne(equal("userName", ownerOfMessage), push("messages", doc)).subscribeAndAwait()
    val path = "messages."+lastPositionInMessages
    val pathDocument = BsonDocument("ownerOfMessage"->ownerOfMessage, "path"-> path)
    collectionTest.updateOne(equal("userName", ownerOfMessage), push("timeline", pathDocument)).subscribeAndAwait()

    addPostInSubscribersTimeline(subscribers,pathDocument)

  }
  def writeCommentToDatabase(message: Messages, path:String, commentedUser: String): Unit ={
    val doc = BsonDocument("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme,
      "comments"->BsonArray(), "references"->BsonArray(),"likes"->BsonDocument("likes"->message.likes.likes,
        "dislikes"->message.likes.dislikes,"rating"->message.likes.rating),
      "userWhoReacted"->BsonArray())

    collectionTest.updateOne(equal("userName", commentedUser), push(path+".comments", doc)).results()

  }


  def decodeTimeline(timeline: ArrayBuffer[Path]): ArrayBuffer[Messages]={
    val feed: ArrayBuffer[Messages] = ArrayBuffer()
    for(t <- timeline){
      val splitPath:Array[String] = t.path.split("\\.")

      val message = collectionTest.find(equal("userName",t.ownerOfMessage)).projection(fields(include("text"), slice(splitPath(0),splitPath(1).toInt, 1),excludeId())).convertToJsonString().stripMargin
      var m = message.dropRight(2)
      m = m.drop(14)

      val decodedMessage = parser.decode[Messages](m).toOption.get
      feed+=decodedMessage
    }
    feed
  }
  def decodePost(referenceToPost: Path): Messages={


      val splitPath:Array[String] = referenceToPost.path.split("\\.")

      val message = collectionTest.find(equal("userName",referenceToPost.ownerOfMessage)).projection(fields(include("text"), slice(splitPath(0),splitPath(1).toInt, 1),excludeId())).convertToJsonString().stripMargin

      var m = message.dropRight(2)
      m = m.drop(14)

      val decodedMessage = parser.decode[Messages](m).toOption.get

      decodedMessage

  }


  def addPostInSubscribersTimeline(subscribers: ArrayBuffer[String], doc: BsonDocument):Unit={

    for (s<- subscribers) collectionTest.updateOne(equal("userName", s), push("timeline", doc)).subscribeAndAwait()
  }


  def reactOnMessage(userThatReact:String, pathToMessage: Path, reaction:String):Unit={

    reaction match{
        case "like" => {
          val splitPath = pathToMessage.path.split("\\.")

          if (!splitPath.contains("comments")) {
            collectionTest.updateOne(equal("userName", userThatReact),
              push("favouriteMessages", BsonDocument("ownerOfMessage" -> pathToMessage.ownerOfMessage, "path" -> pathToMessage.path))).results()

          }
          val pathToLike = pathToMessage.path+".likes.likes"
          collectionTest.updateOne(equal("userName", pathToMessage.ownerOfMessage), inc(pathToLike,1)).results()

          val pathToRating = pathToMessage.path+".likes.rating"
          collectionTest.updateOne(equal("userName", pathToMessage.ownerOfMessage), inc(pathToRating,1)).results()

          val pathToUserWhoReacted = pathToMessage.path+".userWhoReacted"
          collectionTest.updateOne(equal("userName", pathToMessage.ownerOfMessage),push(pathToUserWhoReacted, userThatReact)).results()

        }
        case "dislike" => {


          val pathToDislike = pathToMessage.path+".likes.dislikes"
          collectionTest.updateOne(equal("userName", pathToMessage.ownerOfMessage), inc(pathToDislike,1)).results()

          val pathToRating = pathToMessage.path+".likes.rating"
          collectionTest.updateOne(equal("userName", pathToMessage.ownerOfMessage), inc(pathToRating,-1)).results()

          val pathToUserWhoReacted = pathToMessage.path+".userWhoReacted"
          collectionTest.updateOne(equal("userName", pathToMessage.ownerOfMessage),push(pathToUserWhoReacted, userThatReact)).results()

        }
      }


  }


  def findUser(userName: String):Boolean = {
    val foundUser = collectionTest.find(equal("userName", userName)).results()
    if (foundUser.size == 1) true
    else false

  }
}
