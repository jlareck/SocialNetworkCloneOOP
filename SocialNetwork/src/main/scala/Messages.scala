//package scala
// TODO: Think about how to delete message
// TODO: Implement adding liked and disliked messages to arrays
import org.mongodb.scala.bson.{BsonArray, BsonDocument}

import scala.collection.mutable.ArrayBuffer
import MongoInteractor._
case class Likes(var likes: Int = 0,var dislikes: Int = 0,var rating: Int = 0)


case class Messages( text: String,owner: String, theme: Themes,
                    var comments: ArrayBuffer[Messages] = ArrayBuffer(),
                    var references: ArrayBuffer[User] = ArrayBuffer(),
                     likes: Likes = Likes(), usersWhoLiked: ArrayBuffer[String]= ArrayBuffer(), usersWhoDisliked: ArrayBuffer[String]= ArrayBuffer()){

  def comment(text: String, path:String,userNameToComment: String, theme: Themes, references: ArrayBuffer[User] ): Unit ={
    val message =  Messages(text, owner, theme, ArrayBuffer(), references)
    comments+=message
    val splitedPath = path.split('.')

    writeCommentToDatabase(message, path, userNameToComment)
  }

  //  def like(path: String, ownerOfMessage:String): Unit ={
//    if(!favouriteMessages.exists(x=> x.path == path && x.ownerOfMessage == ownerOfMessage )){
//      MongoInteractor.likeMessage(userName, ownerOfMessage,path)
//    }
//  }
}

