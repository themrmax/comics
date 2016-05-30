import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.circe._
import io.circe.generic.auto._
import org.mongodb.scala._
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.bson.BsonValue
import Helpers._
import scala.concurrent._
import ExecutionContext.Implicits.global


final case class Comic(
  author: String,
  superhero: String,
  date: String) {

  def getWatchers (db: MongoDatabase): Seq[BsonValue] = {
    val subscriptionsColl = db.getCollection("subscriptions")
    val query =
      or(
        equal("author", author),
        equal("date", date),
        equal("superhero", superhero)
      )

    // val query =or(
    //   or(
    //     equal("author", c.author),
    //     equal("date", c.date),
    //     equal("superhero", c.superhero)
    //   ),
    //   Document("author" -> None,
    //     "date" -> None,
    //     "superhero"-> None
    //   ))
    val r2 = subscriptionsColl.find(query).projection(include("email")).results()
    val subscribersToNotify = r2.map(res => res.get("email").get).distinct
    return subscribersToNotify
  }
}

object Comic {
  implicit val comicEntityDecoder: EntityDecoder[Comic] =
    jsonOf[Comic]
}

case class Subscription(
  email: String,
  author: Option[String],
  superhero: Option[String],
  date: Option[String])

object Subscription {
  implicit val subscriptionEntityDecoder: EntityDecoder[Subscription] =
    jsonOf[Subscription]
}

case class Subscriber(
  email: String)

object Subscriber {
  implicit val subscriberEntityDecoder: EntityDecoder[Subscriber] =
    jsonOf[Subscriber]
}

object Server {
  def service = HttpService {
    case GET -> Root =>
      Ok(s"Yup")

    case request @ POST -> Root / "add-comic" =>
      val mongoClient = MongoClient()
      val database = mongoClient.getDatabase("comics");
      val collection = database.getCollection("auctions");
      request.decode[Comic]{ c =>
        val comicBson = Document(
          "author" -> c.author,
          "superhero" -> c.superhero,
          "date" -> c.date
        )
        val o = collection.insertOne(comicBson)
        val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))
        val notificationsColl = database.getCollection("notifications");
        val watchers = c.getWatchers(database)
        val notifications = watchers.map(e => Document("email"-> e, "comic" -> comicBson))
        val o2 = notificationsColl.insertMany(notifications)
        val r2 = Await.result(o2.toFuture(), Duration(10, TimeUnit.SECONDS))
        val count = notificationsColl.count()
        Ok(s"OK, added the comic")
      }

    case request @ POST -> Root / "subscribe" =>
      val mongoClient = MongoClient()
      val database = mongoClient.getDatabase("comics");
      val collection = database.getCollection("subscriptions");
      request.decode[Subscription]{ s =>
        val subscriptionBson = Document(
          "email" -> s.email,
          "author" -> s.author,
          "superhero" -> s.superhero,
          "date" -> s.date
        )
        val o = collection.insertOne(subscriptionBson)
        val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))

        Ok(s"Subscription successful")
      }

    case request @ POST -> Root / "notifications" =>
      val mongoClient: MongoClient = MongoClient()
      val database: MongoDatabase = mongoClient.getDatabase("comics");
      val coll: MongoCollection[Document] = database.getCollection("notifications");
      request.decode[Subscriber]{ s =>
        val o = coll.find(equal("email", s.email))
        val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))
        Ok(Document( "comics" -> r).toString)
      }
  }

  def main(args: Array[String]): Unit = {
    val main = println("here we go")
    val builder = BlazeBuilder.mountService(service)
    val server = builder.run.awaitShutdown()
  }
}
