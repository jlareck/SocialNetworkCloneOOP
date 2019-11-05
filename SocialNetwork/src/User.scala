import scala.collection.mutable.ArrayBuffer

class User(var name: String, val favouriteThemes: ArrayBuffer[Themes], val friends: ArrayBuffer[User], val messages: ArrayBuffer[Messages], val favouriteMessages: ArrayBuffer[Messages]) {


  def createMessage (owner: User, string: String, theme: Themes,
                     comments: ArrayBuffer[Messages], references: ArrayBuffer[User] = ArrayBuffer()): ArrayBuffer[Messages] = {
    val message = new Messages(User.this, string, theme, comments, references)
    messages += message
  }


  def repost(text:String, message: Messages, references: ArrayBuffer[User]): Unit = {
    val repostMessage = new Messages(User.this, text, message.theme,ArrayBuffer[Messages](), references)
    messages+=repostMessage
  }

  def like(message: Messages): Unit ={
    message.likes.likes += 1;
    message.likes.rating += 1;

  }

}
object test extends App{
  var user1 = new User("a", ArrayBuffer[Themes](), ArrayBuffer[User](), ArrayBuffer[Messages](), ArrayBuffer[Messages]())
  var theme = new Themes()
  theme.theme = "tema"
//  user1.createMessage(user1, "fdsa", theme , )
}