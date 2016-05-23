import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.circe._
import scala.concurrent.{ExecutionContext, Future}
import io.circe.Json
import io.circe.generic.auto._

case class Comic (author: String, superhero: String, date: String)
object Server {

  def service = HttpService {
      case GET -> Root =>
        Ok(s"Yup")
    case req @ POST -> Root / "subscribe" =>
      val comic = jsonOf[Comic].decode(req, strict = true).run.run

      if( comic.isRight )  {
        Ok(s"yes")
      }else{
        BadRequest()
      }

    case req @ POST -> Root / "form-encoded" =>
      // EntityDecoders return a Task[A] which is easy to sequence
      req.decode[UrlForm] { m =>
        val s = m.values
        println(s)
        println(s.keys)
        Ok(s"Form Encoded Data\n$s")
      }
      }

  def main(args: Array[String]): Unit = {
    val main = println("here we go")
    val builder = BlazeBuilder.mountService(service)
    val server = builder.run.awaitShutdown()
  }
}
