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
case class Comic(
  author: String,
  superhero: String,
  date: String)

case class Subscription(
  email: String,
  author: Option[String],
  superhero: Option[String],
  date: Option[String])

case class Subscriber(
  email: String)

object Server {
  def getWatchers (db: MongoDatabase, c: Comic): Seq[BsonValue] = {
    val subscriptionsColl = db.getCollection("subscriptions")
    val query =
      or(
        equal("author", c.author),
        equal("date", c.date),
        equal("superhero", c.superhero)
      )

    val o2 = subscriptionsColl.find(query).projection(include("email"))
    val r2 = Await.result(o2.toFuture(), Duration(10, TimeUnit.SECONDS))
    val subscribersToNotify = r2.map(res => res.get("email").get).distinct
   return subscribersToNotify
  }

  def service = HttpService {
    case GET -> Root =>
      Ok(s"Yup")

    case request @ POST -> Root / "add-comic" =>
      val mongoClient = MongoClient()
      val database = mongoClient.getDatabase("comics");
      val collection = database.getCollection("auctions");
      val comic = jsonOf[Comic].decode(request, strict = true).run.run
      if (comic.isRight) {
        val c = comic | Comic("none", "none", "none")
        val comicBson = Document(
          "author" -> c.author,
          "superhero" -> c.superhero,
          "date" -> c.date
        )
        val o = collection.insertOne(comicBson)
        val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))
        val notificationsColl = database.getCollection("notifications");

        val query =or(
          or(
            equal("author", c.author),
            equal("date", c.date),
            equal("superhero", c.superhero)
          ),
          Document("author" -> None,
            "date" -> None,
            "superhero"-> None
          ))
        Ok(s"OK, added the comic")
      } else {
        BadRequest()
      }

    case request @ POST -> Root / "subscribe" =>
      val mongoClient = MongoClient()
      val database = mongoClient.getDatabase("comics");
      val collection = database.getCollection("subscriptions");
      val subscription = jsonOf[Subscription].decode(request, strict = true).run.run
      if (subscription.isRight) {
        val s = subscription | Subscription("invalid", None, None, None)
        val subscriptionBson = Document(
          "email" -> s.email,
          "author" -> s.author,
          "superhero" -> s.superhero,
          "date" -> s.date
        )
        val o = collection.insertOne(subscriptionBson)
        val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))

        Ok(s"Subscription successful")
      } else {
        BadRequest()
      }

    case request @ POST -> Root / "notifications" =>
      val mongoClient: MongoClient = MongoClient()
      val database: MongoDatabase = mongoClient.getDatabase("comics");
      val collection: MongoCollection[Document] = database.getCollection("subscriptions");
      val subscriber = jsonOf[Subscriber].decode(request, strict = true).run.run
      if (subscriber.isRight) {
        val s = subscriber | Subscriber("invalid")
        Ok(s"yep ok")
      } else {
        BadRequest()
      }
  }

  def main(args: Array[String]): Unit = {
    val main = println("here we go")
    val builder = BlazeBuilder.mountService(service)
    val server = builder.run.awaitShutdown()
  }
}

