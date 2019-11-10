package scala
import scala.collection.mutable.ArrayBuffer

case class Likes(var likes: Int = 0,var dislikes: Int = 0,var rating: Int = 0)


case class Messages( owner: String,  string: String,  theme: Themes,
               var comments: ArrayBuffer[Messages], var references: ArrayBuffer[User],  likes: Likes = Likes())
