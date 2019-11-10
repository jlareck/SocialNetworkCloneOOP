package scala
import scala.collection.mutable.ArrayBuffer

import scala.Helpers._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
object MongoInteractor {
  val mongoClient: MongoClient = MongoClient()



  val database: MongoDatabase = mongoClient.getDatabase("mydb")

  val collection: MongoCollection[Document] = database.getCollection("test")

  def writeUserToDatabase(user: User): Unit ={
      val newUserDocument = Document("userName"->user.userName, "password" -> user.password, "name"-> user.name,"favouriteThemes"->Document(),
        "friends"-> Document(), "messages" -> Document(),"favouriteMessages" ->Document() )
      collection.insertOne(newUserDocument).results()
  }
  def writeMessageToDatabase(messages: Messages): Unit ={

    collection.updateOne(equal("userName", messages.owner),
      combine(set("size.uom", "cm"), set("status", "P"), currentDate("lastModified"))
    )

    collection.insertOne(doc).results()
  }


}
