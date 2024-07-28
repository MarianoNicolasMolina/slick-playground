package com.mariano

import play.api.libs.json.Json
import slick.jdbc.GetResult

import java.time.{LocalDate, Year}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PrivateExecutionContext {
  val executor = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
}

object Main {
  import slick.jdbc.PostgresProfile.api._
  import PrivateExecutionContext._

  val shawshankRedemption = Movie(1L, "The Shawshank Redemption", LocalDate.of(1994,9,23), 162)
  val theMatrix = Movie(2L, "The Matrix", LocalDate.of(1999,3,31), 134)
  val phantomMenace = Movie(3L, "Stars Wars: A Phantom Menace", LocalDate.of(1999,5,16), 133)
  val shatner = Actor(1L, "Willian Shatner")
  val nemoy = Actor(2L, "Leonard Nemoy")
  val nichols = Actor(3L, "Nichelle Nichols")
  val freeman = Actor(4L, "Morgan Freeman")
  val liamNeeson = Actor(5L, "Liam Neeson")

  val providers = List(
    //StreamingProviderMapping(0L, 1L, StreamingService.Netflix),
    //StreamingProviderMapping(1L, 2L, StreamingService.Netflix),
    StreamingProviderMapping(2L, 3L, StreamingService.Disney)
  )

  val starWarsLocactions = MovieLocations(1L, 3L, List(
    "England", "Tunisia", "Italy"
  ))

  val starWarsProperties = MovieProperties(1L, 3L, Map(
    "Genre" -> "Sci-Fi",
    "Features" -> "Light-sabers"
  ))

  val liamNeesonDetails = ActorDetails(1L, 5L, Json.parse(
    """
      |{
      |   "born": 1952,
      |   "awesome" : "yes"
      |}
      |""".stripMargin
  ))

  val actors = Seq(shatner, nemoy, nichols, freeman)

  def demoInsertMovie(): Unit = {
    val queryDescription = SlickTables.movieTable += shawshankRedemption

    val futureId: Future[Int] = Connection.db.run(queryDescription)

    futureId.onComplete {
      case Success(newMovieId) => println(s"Query was successful, new id is $newMovieId")
      case Failure(ex) => println(s"Query failed, reason: $ex")
    }

    Thread.sleep(100000)
  }

  def demoInsertActors(): Unit = {


    val queryDescription = SlickTables.actorTable ++= actors

    val futureId = Connection.db.run(queryDescription)

    futureId.onComplete {
      case Success(_) => println(s"Query was successful")
      case Failure(ex) => println(s"Query failed, reason: $ex")
    }

    Thread.sleep(100000)
  }

  def demoReadAllMovies(): Unit = {
    val resultFuture: Future[Seq[Movie]] = Connection.db.run(SlickTables.movieTable.result) // select * from ___

    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched: ${movies.mkString(",")}")
      case Failure(ex) => println(s"Fetching failed: $ex")
    }

    Thread.sleep(10000)
  }

  def demoReadSomeMovies(): Unit = {
    val resultFuture: Future[Seq[Movie]] = Connection.db.run(SlickTables.movieTable.filter(_.name.like("%Matrix%")).result)
    // select * from ___
    // where nane like "matrix"

    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched: ${movies.mkString(",")}")
      case Failure(ex) => println(s"Fetching failed: $ex")
    }

    Thread.sleep(10000)
  }

  def demoUpdate(): Unit = {
    val queryDescriptor = SlickTables.movieTable.filter(_.id === 1L).update(shawshankRedemption.copy(lengthInMin = 150))

    val futureId: Future[Int] = Connection.db.run(queryDescriptor)

    futureId.onComplete {
      case Success(newMovieId) => println(s"Query was successful, new id is $newMovieId")
      case Failure(ex) => println(s"Query failed, reason: $ex")
    }

    Thread.sleep(100000)

  }

  def readMoviesByPlainQuery(): Future[Vector[Movie]] = {
    // [id,name,localDate,lengthInMin]
    implicit val getResultMovie: GetResult[Movie] =
      GetResult(positionedResult => Movie(
        positionedResult.<<,
        positionedResult.<<,
        LocalDate.parse(positionedResult.nextString()),
        positionedResult.<<))

    val query = sql""" select * from movies."Movie" """.as[Movie]

    Connection.db.run(query)
  }

  def demoDelete(): Unit = {
    Connection.db.run(SlickTables.movieTable.filter(_.name like "%Matrix%").delete)
    Thread.sleep(10000)
  }

  def multipleQueriesSingleTransaction(): Unit = {
    val insertMovie = SlickTables.movieTable += phantomMenace
    val insertActor = SlickTables.actorTable += liamNeeson
    val finalQuery = DBIO.seq(insertMovie, insertActor)

    Connection.db.run(finalQuery.transactionally)
  }

  def findAllActorsByMovie(movieId: Long): Future[Seq[Actor]] = {
    val joinQuery = SlickTables.movieActorMappingTable
      .filter(_.movieId === movieId)
      .join(SlickTables.actorTable)
      .on(_.actorId === _.id)
      .map(_._2)


    Connection.db.run(joinQuery.result)
  }


  def addStreamingProviders() = {
    val insertQuery = SlickTables.streamingProviderMappingTable ++= providers

    Connection.db.run(insertQuery)
  }

  def findProvidersForMovie(movieId: Long): Future[Seq[StreamingProviderMapping]] = {
    val query = SlickTables.streamingProviderMappingTable.filter(_.movieId === movieId)

    Connection.db.run(query.result)
  }

  def addMovieLocationsForStarWars() = {
    val query = SpecialTables.movieLocationsTable += starWarsLocactions
    Connection.db.run(query)
  }

  def insertMoviePropertiesDemo() = {
    val query = SpecialTables.moviePropertiesTable += starWarsProperties
    Connection.db.run(query)
  }

  def insertPersonalDetail() = {
    val query = SpecialTables.actorDetailsTable += liamNeesonDetails
    Connection.db.run(query)
  }

  def main(args: Array[String]): Unit = {
    insertPersonalDetail()

    Thread.sleep(5000)
    PrivateExecutionContext.executor.shutdown()


  }


}
