import org.scalatest._
import org.scalatest.BeforeAndAfter
import org.http4s._, org.http4s.dsl._
import org.http4s.circe._
import Server._
import io.circe.Json
import org.mongodb.scala._

class ServerSpec extends FlatSpec with Matchers with BeforeAndAfter {

  val db = MongoClient().getDatabase("comics-test"); //should be comics-test

  before {
  }

  after {
    List("comics","subscriptions","notifications").map(db.getCollection(_).drop().toFuture())
  }

  "A comic" should "get some watchers" in {
    val subscription = Subscription(email="comicfan69@rocketmail.com", author=Some("steve"))
    val r = subscription.save(db)
    val comic = Comic("steve","hatman","1985")
    val w = comic.getWatchers(db)
    w.length should not equal 0
  }
  it should "save" in {
    val comic = Comic("steve","hatman","1985")
    val r = comic.save(db)
    r.isRight should be
  }

  "The default route" should " return OK" in {
    val request = Request(method = GET, uri = uri("/"))
    service(request).run.status.code shouldEqual 200
  }
  // "The add-comic route" should "return OK if the body contains superhero, date and author" in {
  //   val request = Request(method = POST, uri = uri("/add-comic")).withBody(Json.obj("superhero" -> Json.fromString("hatman"), "author" -> Json.fromString("steve"), "date" -> Json.fromString("1905"))).run
  //   service(request).run.status.code shouldEqual 200
  // }
  "The add-comic route" should "return 422 if the fields are wrong" in {
    val request = Request(method = POST, uri = uri("/add-comic")).withBody(Json.obj("normalhero" -> Json.fromString("hatman"), "author" -> Json.fromString("steve"), "date" -> Json.fromString("1905"))).run
    service(request).run.status.code shouldEqual 422
  }
  // "The subscribe route" should " return OK if it contains an email and one field" in {
  //   val request = Request(method = POST, uri = uri("/subscribe")).withBody(Json.obj("email" -> Json.fromString("comicfan69@rocketmail.com"), "author" -> Json.fromString("steve"))).run
  //   service(request).run.status.code shouldEqual 200
  // }
  "The notifications route" should " return 422 if it doesn't contain an email" in {
    val request = Request(method = POST, uri = uri("/subscribe")).withBody(Json.obj("author" -> Json.fromString("steve"))).run
    service(request).run.status.code shouldEqual 422
  }
  // "The notifications route" should "require an email" in {
  //   val request = Request(method = POST, uri = uri("/notifications")).withBody(Json.obj("email" -> Json.fromString("comicfan69@rocketmail.com"))).run
  //   service(request).run.status.code shouldEqual 200}
  "The notificaitons route" should "return 400 if no data is provided" in {
    val badRequest = Request(method = POST, uri = uri("/notifications"))
    service(badRequest).run.status.code shouldEqual 400}
}
