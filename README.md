# Comic book auction notification app
This is the backend REST API to an app for an ongoing comic book auction. It uses `http4s` for the webserver, and MongoDB for the database.


## Installation

You need to have mongodb running somewhere for the app and tests to run; the default is `localhost:27017`, or you can set the url in the value `mongoUrl` in `Server.scala`. Then just run `sbt run` to install the dependencies.

## Running and testing
Run the app with `sbt run`, run the unit tests with `sbt test`.

## API Usage
The server runs on port 8080 and has the following endpoints:
### add-comic, remove-comic
The admin can add or remove a comic, you must specify all of author, superhero and date, e.g.:

    curl -X POST -d '{"author":"brad", "superhero":"catman", "date":"1911"}' http://localhost:8080/add-comic
    curl -X POST -d '{"author":"brad", "superhero":"catman", "date":"1911"}' http://localhost:8080/remove-comic

### subscribe
Subscribers can add searches to watch for comics by a certain author, superhero, or date, by making a `POST` request to the `subscribe/` endpoint, e.g.,

    curl -X POST -d '{"email":"max@flan.net", "author":"brad"}' http://localhost:8080/subscribe

### notifications
Subscribers can check their notifications by providing their email to the `notifications` endpoint; make sure to save the results, as this will also purge their notifications queue:

    curl -X POST -d '{"email":"max@flan.net"}' http://localhost:8080/notifications
    { "comics" : [{ "email" : "max@flan.net", "comic" : { "author" : "brad", "superhero" : "catman", "date" : "1911" }, "message" : "This comic is sold!" }
