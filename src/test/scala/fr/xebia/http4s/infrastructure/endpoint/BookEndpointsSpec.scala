package fr.xebia.http4s.infrastructure.endpoint

import cats.effect.IO
import fr.xebia.http4s.BookStoreArbitraries
import fr.xebia.http4s.domain.{BookService, BookValidation}
import fr.xebia.http4s.domain.spi.Book
import fr.xebia.http4s.endpoint.BookEndpoints
import fr.xebia.http4s.infrastructure.repository.inmemory.InMemoryBookRepository
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BookEndpointsSpec
    extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with BookStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  implicit val bookEncoder: EntityEncoder[IO, Book]           = jsonEncoderOf
  implicit val bookDecoder: EntityDecoder[IO, Book]           = jsonOf
  implicit val listBookDecoder: EntityDecoder[IO, List[Book]] = jsonOf

  test("Add book to Library") {
    val bookRepo        = InMemoryBookRepository[IO]()
    val bookValidation  = BookValidation[IO](bookRepo)
    val bookService     = BookService[IO](bookRepo, bookValidation)
    val bookHttpService = BookEndpoints.endpoints[IO](bookService).orNotFound

    forAll { book: Book =>
      (for {
        request  <- POST(book, Uri.uri("/books"))
        response <- bookHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }
  }

  test("Get book from Library") {
    val bookRepo        = InMemoryBookRepository[IO]()
    val bookValidation  = BookValidation[IO](bookRepo)
    val bookService     = BookService[IO](bookRepo, bookValidation)
    val bookHttpService = BookEndpoints.endpoints[IO](bookService).orNotFound

    forAll { book: Book =>
      (for {
        createRequest  <- POST(book, Uri.uri("/books"))
        createResponse <- bookHttpService.run(createRequest)
        createdBook    <- createResponse.as[Book]
        request        <- GET(Uri.unsafeFromString(s"/books/${createdBook.isbn.getOrElse("")}"))
        response       <- bookHttpService.run(request)
        responseBook   <- response.as[Book]
      } yield {
        response.status shouldEqual Ok
        responseBook.title shouldEqual book.title
        responseBook.authorId shouldEqual book.authorId
        responseBook.description shouldEqual book.description
      }).unsafeRunSync
    }
  }

  test("Get all books in Library") {
    val bookRepo        = InMemoryBookRepository[IO]()
    val bookValidation  = BookValidation[IO](bookRepo)
    val bookService     = BookService[IO](bookRepo, bookValidation)
    val bookHttpService = BookEndpoints.endpoints[IO](bookService).orNotFound

    forAll { book: Book =>
      (for {
        createRequest <- POST(book, Uri.uri("/books"))
        _             <- bookHttpService.run(createRequest)
        request       <- GET(Uri.uri("/books"))
        response      <- bookHttpService.run(request)
        responseList  <- response.as[List[Book]]
      } yield {
        response.status shouldEqual Ok
        assert(responseList.nonEmpty)
      }).unsafeRunSync

    }
  }
}
