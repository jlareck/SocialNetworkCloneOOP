package scala
import scala.collection.mutable.ArrayBuffer

import scala.Helpers._
import org.mongodb.scala._
import org.mongodb.scala.model.Updates._

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.bson
object MongoInteractor {
   val mongoClient: MongoClient = MongoClient()



   val database: MongoDatabase = mongoClient.getDatabase("mydb")

   val collection: MongoCollection[Document] = database.getCollection("test")

  def authorization(userName: String, password: String):Unit ={
      val a = collection.find(and(equal("userName",userName),equal("password", password))).printHeadResult()
      print(a)
  }

  def writeUserToDatabase(user: User): Unit ={
      val newUserDocument = Document("userName"->user.userName, "password" -> user.password, "name"-> user.name,"favouriteThemes"->Document(),
        "friends"-> Document(), "messages" -> Document(),"favouriteMessages" ->Document() )
      collection.insertOne(newUserDocument).results()
  }
  def writeMessageToDatabase(user: User,message: Messages): Unit ={

    if(user.messages.isEmpty){
        val doc = Document("text"->message.text, "owner"->message.owner,"theme"->message.theme.theme, "comments"->Document(), "references"->Document())

        collection.updateOne(equal("userName", user.userName),  push("messages", doc) )
    }


    //collection.insertOne(doc).results()
  }


}
