import org.scalatest._
import org.http4s._, org.http4s.dsl._
import org.http4s.circe._
import Server._
import io.circe.Json

class ServerSpec extends FlatSpec with Matchers {

  "The default route" should " return OK" in {
    val request = Request(method=GET, uri=uri("/"))
    service(request).run.status.code shouldEqual 200
  }
  "The subscribe route" should "return OK if the body contains superhero, date and author" in {
    val request = Request(method=POST, uri=uri("/subscribe")).withBody(Json.obj("superhero" -> Json.fromString("hatman"), "author" -> Json.fromString("steve"), "date" -> Json.fromString("1905")))
    service(request.run).run.status.code shouldEqual 200
  }
  it should "return 400 if the fields are wrong"  in {
    val request = Request(method=POST, uri=uri("/subscribe")).withBody(Json.obj("normalhero" -> Json.fromString("hatman"), "author" -> Json.fromString("steve"), "date" -> Json.fromString("1905")))
    service(request.run).run.status.code shouldEqual 400
}
}
