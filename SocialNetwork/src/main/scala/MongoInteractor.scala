
import Helpers._
import io.circe._
import io.circe.generic.auto._
import org.mongodb.scala._
import org.mongodb.scala.bson._
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Projections.{excludeId, fields, include, slice}
import org.mongodb.scala.model.Updates._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
object MongoInteractor {

  implicit def toBuffer[A: ClassTag](a: Array[A]) = ArrayBuffer(a: _*)

  implicit val userDecoder: Decoder[User] =
    (hCursor: HCursor) => {
      for {
        id <- hCursor.get[String](Fields.id)
        userName <- hCursor.get[String](Fields.userName)
        password <- hCursor.get[String](Fields.password)
        name <- hCursor.get[String](Fields.name)
        favouriteThemes <- hCursor.get[Array[Themes]](Fields.favouriteThemes)
        messages <- hCursor.get[Array[Messages]](Fields.messages)
        friends <- hCursor.get[Array[String]](Fields.friends)
        subscribers <- hCursor.get[Array[String]](Fields.subscribers)
        favouriteMessages <- hCursor.get[Array[Path]](Fields.favouriteMessages)
        timeline <- hCursor.get[Array[Path]](Fields.timeline)
      } yield User(id,userName,password, name, favouriteThemes, friends,subscribers, messages, favouriteMessages, timeline)
    }
  implicit val messageDecoder: Decoder[Messages] =
    (hCursor: HCursor) => {
      for {

        text <- hCursor.get[String](Fields.text)
        owner <- hCursor.get[String](Fields.owner)
        theme <- hCursor.get[String](Fields.theme)
        comments <- hCursor.get[Array[Messages]](Fields.comments)
        references <- hCursor.get[Array[String]](Fields.references)
        likes <- hCursor.get[Likes](Fields.likes)
        usersWhoReacted <-hCursor.get[Array[String]](Fields.usersWhoReacted)

      } yield Messages(text,owner, Themes(theme), comments,references,likes, usersWhoReacted)
    }


  implicit val pathDecoder: Decoder[Path] =
    (hCursor: HCursor) => {
      for {
        path <- hCursor.get[String](Fields.path)
        ownerOfMessage <- hCursor.get[String](Fields.ownerOfMessage)

      } yield Path(ownerOfMessage, path)
    }

  def authorization(userName: String, password: String):User ={ // TODO: implement smth when user is not decoded

      val foundUser = collectionTest.find(and(equal(Fields.userName,userName),
        equal(Fields.password, password))).convertToJsonString().stripMargin

      val decodedUser = parser.decode[User](foundUser).toOption.get
      decodedUser




  }

  def subscribeOnUser(userName: String, friendsName:String): Unit ={
    collectionTest.updateOne(equal(Fields.userName, userName),push(Fields.friends,friendsName)).results()
    collectionTest.updateOne(equal(Fields.userName, friendsName), push(Fields.subscribers, userName)).results()
  }

  def writeUserToDatabase(user: User): Unit ={
    val newUserDocument = BsonDocument(Fields.id -> user.id, Fields.userName -> user.userName,
      Fields.password -> user.password, Fields.name -> user.name, Fields.favouriteThemes -> BsonArray(),
      Fields.friends -> BsonArray(),Fields.subscribers -> BsonArray(), Fields.messages -> BsonArray(), Fields.favouriteMessages -> BsonArray(), Fields.timeline -> BsonArray() )
    collectionTest.insertOne(newUserDocument).results()

  }

  def writeMessageToDatabase(message: Messages, ownerOfMessage: String, lastPositionInMessages: Int,
                             subscribers:ArrayBuffer[String]): Unit ={
    val doc = BsonDocument(Fields.text->message.text, Fields.owner->message.owner, Fields.theme->message.theme.theme,
      Fields.comments->BsonArray(), Fields.references->message.references.toList, Fields.likes->BsonDocument(Fields.likes->message.likes.likes,
        Fields.dislikes->message.likes.dislikes, Fields.rating->message.likes.rating),
      Fields.usersWhoReacted-> message.usersWhoReacted.toList)

    collectionTest.updateOne(equal(Fields.userName, ownerOfMessage), push(Fields.messages, doc)).subscribeAndAwait()
    val path = s"${Fields.messages}."+lastPositionInMessages
    val pathDocument = BsonDocument(Fields.ownerOfMessage->ownerOfMessage, Fields.path-> path)
    collectionTest.updateOne(equal(Fields.userName, ownerOfMessage), push(Fields.timeline, pathDocument)).subscribeAndAwait()

    addPostInSubscribersTimeline(subscribers,pathDocument)

  }
  def writeCommentToDatabase(message: Messages, path:String, commentedUser: String): Unit ={
    val doc = BsonDocument(Fields.text->message.text, Fields.owner->message.owner,Fields.theme->message.theme.theme,
      Fields.comments->BsonArray(), Fields.references->message.references.toList, Fields.likes->BsonDocument(Fields.likes->message.likes.likes,
        Fields.dislikes->message.likes.dislikes, Fields.rating->message.likes.rating),
      Fields.usersWhoReacted->message.usersWhoReacted.toList)

    collectionTest.updateOne(equal(Fields.userName, commentedUser), push(path+s".${Fields.comments}", doc)).subscribeAndAwait()

  }


  def decodeTimeline(timeline: ArrayBuffer[Path]): ArrayBuffer[Messages]={
    val feed: ArrayBuffer[Messages] = ArrayBuffer()
    for(t <- timeline){
      val splitPath:Array[String] = t.path.split("\\.")

      val message = collectionTest.find(equal(Fields.userName,t.ownerOfMessage)).projection(fields(include(Fields.text), slice(splitPath(0),splitPath(1).toInt, 1),excludeId())).convertToJsonString().stripMargin
      var m = message.dropRight(2)
      m = m.drop(14)

      val decodedMessage = parser.decode[Messages](m).toOption.get
      feed+=decodedMessage
    }
    feed
  }
  def decodePost(referenceToPost: Path): Messages={


      val splitPath:Array[String] = referenceToPost.path.split("\\.")

      val message = collectionTest.find(equal(Fields.userName,referenceToPost.ownerOfMessage)).projection(fields(include(Fields.text), slice(splitPath(0),splitPath(1).toInt, 1),excludeId())).convertToJsonString().stripMargin

      var m = message.dropRight(2)
      m = m.drop(14)

      val decodedMessage = parser.decode[Messages](m).toOption.get

      decodedMessage

  }


  def addPostInSubscribersTimeline(subscribers: ArrayBuffer[String], doc: BsonDocument):Unit={

    for (s<- subscribers) collectionTest.updateOne(equal(Fields.userName, s), push(Fields.timeline, doc)).subscribeAndAwait()
  }


  def reactOnMessage(userThatReact:String, pathToMessage: Path, reaction:String):Unit={

    reaction match{
        case "like" => {
          val splitPath = pathToMessage.path.split("\\.")

          if (!splitPath.contains(Fields.comments)) {
            collectionTest.updateOne(equal(Fields.userName, userThatReact),
              push(Fields.favouriteMessages, BsonDocument(Fields.ownerOfMessage-> pathToMessage.ownerOfMessage, Fields.path -> pathToMessage.path))).subscribeAndAwait()

          }
          val pathToLike = pathToMessage.path+s".${Fields.likes}.${Fields.likes}"
          collectionTest.updateOne(equal(Fields.userName, pathToMessage.ownerOfMessage), inc(pathToLike,1)).subscribeAndAwait()

          val pathToRating = pathToMessage.path+s".${Fields.likes}.${Fields.rating}"
          collectionTest.updateOne(equal(Fields.userName, pathToMessage.ownerOfMessage), inc(pathToRating,1)).subscribeAndAwait()

          val pathToUserWhoReacted = pathToMessage.path+s".${Fields.usersWhoReacted}"
          collectionTest.updateOne(equal(Fields.userName, pathToMessage.ownerOfMessage),push(pathToUserWhoReacted, userThatReact)).subscribeAndAwait()

        }
        case "dislike" => {


          val pathToDislike = pathToMessage.path+s".${Fields.likes}.${Fields.dislikes}"
          collectionTest.updateOne(equal(Fields.userName, pathToMessage.ownerOfMessage), inc(pathToDislike,1)).subscribeAndAwait()

          val pathToRating = pathToMessage.path+s".${Fields.likes}.${Fields.rating}"
          collectionTest.updateOne(equal(Fields.userName, pathToMessage.ownerOfMessage), inc(pathToRating,-1)).subscribeAndAwait()

          val pathToUserWhoReacted = pathToMessage.path+s".${Fields.usersWhoReacted}"
          collectionTest.updateOne(equal(Fields.userName, pathToMessage.ownerOfMessage),push(pathToUserWhoReacted, userThatReact)).subscribeAndAwait()

        }
      }


  }


  def findUser(userName: String):Boolean = {
    val foundUser = collectionTest.find(equal(Fields.userName, userName)).results()
    if (foundUser.size == 1) true
    else false

  }
}
