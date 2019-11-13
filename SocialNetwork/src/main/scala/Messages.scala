//package scala
import scala.collection.mutable.ArrayBuffer

case class Likes(var likes: Int = 0,var dislikes: Int = 0,var rating: Int = 0)


case class Messages( text: String,owner: String, theme: Themes,
                    var comments: ArrayBuffer[Messages] = ArrayBuffer(),
                    var references: ArrayBuffer[User] = ArrayBuffer(), likes: Likes = Likes()){

}

