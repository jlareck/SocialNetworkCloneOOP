import org.bson.types.ObjectId

object Registration {

    def registration(): Unit ={
        println("Hello! Enter your username")
        val userName: String = scala.io.StdIn.readLine()
        println("Enter your password")
        val password: String = scala.io.StdIn.readLine()
        println("Enter your first name and last name")
        val name: String = scala.io.StdIn.readLine()

        val newUser =  User(userName,userName, password, name)

        MongoInteractor.writeUserToDatabase(newUser)
    }

    def registrationTest():Unit={
        val n = 4
        for (i <- 0 until n) {
            MongoInteractor.writeUserToDatabase(User("user"+i, "user"+i, "p"+i, "USER"+i))
        }
    }

}
