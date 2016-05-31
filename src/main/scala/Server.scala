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
      val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))
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
  def notifyWatchers (db: MongoDatabase): Either[Exception, Unit] = {
    try {
      val notificationsColl = db.getCollection("notifications");
      val watchers = getWatchers(db)
      val notifications = watchers.map(e => Document("email"-> e, "comic" -> bson))
      val o2 = notificationsColl.insertMany(notifications)
      val r2 = Await.result(o2.toFuture(), Duration(10, TimeUnit.SECONDS))
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
      val r = Await.result(o.toFuture(), Duration(10, TimeUnit.SECONDS))
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

object Server {
  val db = MongoClient().getDatabase("comics");
  def service = HttpService {
    case GET -> Root =>
      Ok(s"Yup")

    case request @ POST -> Root / "add-comic" =>
      request.decode[Comic]{ c =>
        val r = c.save(db)
        val r2 = c.notifyWatchers(db)
        if (r.isRight & r2.isRight){
          Ok(s"OK, added the comic")
        }
        else{
          ServiceUnavailable()
        }
      }

    case request @ POST -> Root / "subscribe" =>
      request.decode[Subscription]{ s =>
        val r = s.save(db)
        if (r.isRight){
          Ok(s"Subscription successful")
        }
        else{
          ServiceUnavailable()
        }
      }

    case request @ POST -> Root / "notifications" =>
      request.decode[Subscriber]{ s =>
        val r = s.getNotifications(db)
        if (r.isRight){
          Ok(Document( "comics" -> r.right.get).toJson)
        }
        else{
          ServiceUnavailable()
        }
      }
  }

  def main(args: Array[String]): Unit = {
    val main = println("here we go")
    val builder = BlazeBuilder.mountService(service)
    val server = builder.run.awaitShutdown()
  }
}
