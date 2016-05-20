import org.scalatest._
import org.http4s._, org.http4s.dsl._
import Server._

class ServerSpec extends FlatSpec with Matchers {

  "A service" should "have a default route which returns OK" in {
    val request = Request(method=GET, uri=uri("/"))
    service(request).run.status.code shouldEqual 200
  }
}
