package scala
import scala.collection.mutable.ArrayBuffer

class Likes{
  var likes: Int = 0
  var dislikes: Int = 0
  var rating: Int = 0

}
class Messages(var owner: String, var string: String, var theme: Themes,
               var comments: ArrayBuffer[Messages], var references: ArrayBuffer[User]) {
  var likes = new Likes()
}
