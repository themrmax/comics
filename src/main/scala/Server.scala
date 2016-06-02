import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import org.mongodb.scala._
import ComicClasses._
import ComicClasses.Subscription


object Server {
  val mongoUrl = "mongodb://localhost:27017"
  val db = MongoClient(mongoUrl).getDatabase("comics");
  def service = HttpService {
    case GET -> Root =>
      Ok(s"Yup")

    case request @ POST -> Root / "add-comic" =>
      request.decode[Comic]{ c =>
        val r = c.save(db)
        val r2 = c.notifyWatchers(db, message="New comic up for auction...")
        if (r.isRight & r2.isRight){
          Ok(s"OK, added the comic\n")
        }
        else{
          ServiceUnavailable()
        }
      }

    case request @ POST -> Root / "remove-comic" =>
      request.decode[Comic]{ c =>
        val r2 = c.notifyWatchers(db, message="This comic is sold!")
        val r = c.delete(db)
        if (r.isRight & r2.isRight){
          Ok(s"OK, deleted the comic\n")
        }
        else{
          ServiceUnavailable()
        }
      }

    case request @ POST -> Root / "subscribe" =>
      request.decode[Subscription]{ s =>
        println(s)
        val r = s.save(db)
        if (r.isRight){
          Ok(s"Subscription successful\n")
        }
        else{
          ServiceUnavailable()
        }
      }

    case request @ POST -> Root / "notifications" =>
      request.decode[Subscriber]{ s =>
        val r = s.getNotifications(db)
        if (r.isRight){
          Ok(Document( "comics" -> r.right.get).toJson + "\n")
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
