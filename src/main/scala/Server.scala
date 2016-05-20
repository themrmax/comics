import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import scala.concurrent.{ExecutionContext, Future}

object Server {
  def service = HttpService {
      case GET -> Root =>
        Ok(s"Yup")
    }

  def main(args: Array[String]): Unit = {
    val main = println("here we go")
    val builder = BlazeBuilder.mountService(service)
    val server = builder.run.awaitShutdown()
  }
}
