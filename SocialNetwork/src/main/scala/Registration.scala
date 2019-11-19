import org.bson.types.ObjectId

object Registration extends App {

    println("Hello! Enter your username")
    val userName: String = scala.io.StdIn.readLine()
    println("Enter your password")
    val password: String = scala.io.StdIn.readLine()
    println("Enter your first name and last name")
    val name: String = scala.io.StdIn.readLine()

    val newUser =  User("",userName, password, name)
    newUser.id = userName
    println(newUser.id)
    MongoInteractor.writeUserToDatabase(newUser)


}
object RegistrationTest extends App{
    val n = 10
    for (i <- 0 until n) {
        MongoInteractor.writeUserToDatabase(User("user"+i, "user"+i, "p"+i, "USER"+i)
        )
    }
}