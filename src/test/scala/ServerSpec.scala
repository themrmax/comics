import org.scalatest._
import org.http4s._, org.http4s.dsl._
import org.http4s.circe._
import Server._
import io.circe.Json
import org.mongodb.scala._

class ServerSpec extends FlatSpec with Matchers {

  "The default route" should " return OK" in {
    val request = Request(method = GET, uri = uri("/"))
    service(request).run.status.code shouldEqual 200
  }
  "The add-comic route" should "return OK if the body contains superhero, date and author" in {
    val request = Request(method = POST, uri = uri("/add-comic")).withBody(Json.obj("superhero" -> Json.fromString("hatman"), "author" -> Json.fromString("steve"), "date" -> Json.fromString("1905"))).run
    service(request).run.status.code shouldEqual 200
  }
  it should "return 400 if the fields are wrong" in {
    val request = Request(method = POST, uri = uri("/add-comic")).withBody(Json.obj("normalhero" -> Json.fromString("hatman"), "author" -> Json.fromString("steve"), "date" -> Json.fromString("1905"))).run
    service(request).run.status.code shouldEqual 400
  }
  "The subscribe route" should " return OK if it contains an email and one field" in {
    val request = Request(method = POST, uri = uri("/subscribe")).withBody(Json.obj("email" -> Json.fromString("comicfan69@rocketmail.com"), "author" -> Json.fromString("steve"))).run
    service(request).run.status.code shouldEqual 200
  }
  it should " return 400 if it doesn't contain an email" in {
    val request = Request(method = POST, uri = uri("/subscribe")).withBody(Json.obj("author" -> Json.fromString("steve"))).run
    service(request).run.status.code shouldEqual 400
  }
  "getWatchers" should "get some watchers" in {
    val mongoClient = MongoClient()
    val database = mongoClient.getDatabase("comics");
    val w = getWatchers(database, Comic("steve","hatman","1985"))
    w.length should not equal 0
  }
}
