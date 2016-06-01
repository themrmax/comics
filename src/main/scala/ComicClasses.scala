import org.http4s.circe._
import org.http4s.EntityDecoder
import io.circe.generic.auto._
import org.mongodb.scala._
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.bson.BsonValue
import scala.concurrent._
import Helpers._

object ComicClasses{

  final case class Comic(
    author: String,
    superhero: String,
    date: String) {

    val bson = Document(
      "author" -> author,
      "superhero" -> superhero,
      "date" -> date
    )

    def save(db: MongoDatabase): Either[Exception, Unit] = {
      try {
        val collection = db.getCollection("comics");
        val o = collection.insertOne(bson)
        val r = o.results()
        return Right(s"OK added the comic")
      }
      catch {case e: Exception =>
        return Left(e)}
    }

    def delete(db: MongoDatabase): Either[Exception, Unit] = {
      try {
        val collection = db.getCollection("comics");
        val o = collection.deleteOne(bson)
        val r = o.results()
        return Right()
      }
      catch {case e: Exception =>
        return Left(e)}
    }

    def getWatchers (db: MongoDatabase): Seq[BsonValue] = {
      val subscriptionsColl = db.getCollection("subscriptions")
      val query =or(
        or(
          equal("author", author),
          equal("date", date),
          equal("superhero", superhero)
        ),
        Document("author" -> None,
          "date" -> None,
          "superhero"-> None
        ))
      val r2 = subscriptionsColl.find(query).projection(include("email")).results()
      val subscribersToNotify = r2.map(res => res.get("email").get).distinct
      return subscribersToNotify
    }
    def notifyWatchers (db: MongoDatabase, message: String): Either[Exception, Unit] = {
      try {
        val notificationsColl = db.getCollection("notifications");
        val watchers = getWatchers(db)
        if(watchers.length != 0){
          val notifications = watchers.map(e => Document("email"-> e, "comic" -> bson, "message" -> message))
          val o2 = notificationsColl.insertMany(notifications)
          val r2 = o2.results()
        }
        return Right()
      }
      catch {case e: Exception =>
        return Left(e)}
    }
  }

  object Comic {
    implicit val comicEntityDecoder: EntityDecoder[Comic] =
      jsonOf[Comic]
  }

  case class Subscription(
    email: String,
    author: Option[String]=None,
    superhero: Option[String]=None,
    date: Option[String]=None)
  {
    def save (db: MongoDatabase): Either[Exception, Unit]= {
      try{
        val subscriptionBson = Document(
          "email" -> email,
          "author" -> author,
          "superhero" -> superhero,
          "date" -> date
        )
        val coll = db.getCollection("subscriptions");
        val o = coll.insertOne(subscriptionBson)
        val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))
        return Right()
      }
      catch {case e: Exception =>
        return Left(e)}
    }
  }

  object Subscription {
    implicit val subscriptionEntityDecoder: EntityDecoder[Subscription] =
      jsonOf[Subscription]
  }

  case class Subscriber(
    email: String)
  {
    def getNotifications(db: MongoDatabase): Either[Exception, Seq[Document]] = {
      try {
        val coll = db.getCollection("notifications");
        val o = coll.find(equal("email", email)).projection(excludeId())
        val r = o.results()
        val o2 = coll.deleteMany(equal("email", email))
        val r2 = o2.results()
        return Right(r)
      }
      catch {case e: Exception =>
        return Left(e)}
    }
  }

  object Subscriber {
    implicit val subscriberEntityDecoder: EntityDecoder[Subscriber] =
      jsonOf[Subscriber]
  }
}
