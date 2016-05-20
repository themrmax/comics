scalaVersion := "2.11.8" // Also supports 2.10.x

lazy val http4sVersion = "0.14.0-SNAPSHOT"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)

