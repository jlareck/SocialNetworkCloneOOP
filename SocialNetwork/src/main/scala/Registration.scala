object Registration extends App {

    println("Hello! Enter your username")
    val userName: String = scala.io.StdIn.readLine()
    println("Enter your password")
    val password: String = scala.io.StdIn.readLine()
    println("Enter your first name and last name")
    val name: String = scala.io.StdIn.readLine()

    val newUser = new User("",userName, password, name)
    MongoInteractor.writeUserToDatabase(newUser)


}
