package fr.xebia.http4s

import cats.effect._
import cats.implicits._
import fr.xebia.http4s.config.LibraryConfig
import fr.xebia.http4s.domain.book.{BookService, BookValidationInterpreter}
import fr.xebia.http4s.infrastructure.endpoint.BookEndpoints
import fr.xebia.http4s.infrastructure.repository.inmemory.BookRepositoryInMemoryInterpreter
import io.circe.config.parser
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}

object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.liftF(parser.decodePathF[F, LibraryConfig]("library"))
      bookRepo = BookRepositoryInMemoryInterpreter[F]()
      bookValidation = BookValidationInterpreter[F](bookRepo)
      bookService = BookService[F](bookRepo, bookValidation)
      services = BookEndpoints.endpoints[F](bookService)
      httpApp = Router("/" -> services).orNotFound
      server <- BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)

}
