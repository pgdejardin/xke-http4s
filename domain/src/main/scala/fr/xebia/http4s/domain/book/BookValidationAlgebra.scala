package fr.xebia.http4s.domain.book
import cats.data.EitherT
import fr.xebia.http4s.domain.{BookAlreadyExistsError, BookNotFoundError}
import io.chrisdavenport.fuuid.FUUID

import scala.language.higherKinds

trait BookValidationAlgebra[F[_]] {
  def doesNotExist(book: Book): EitherT[F, BookAlreadyExistsError, Unit]
  def exists(isbn: Option[FUUID]): EitherT[F, BookNotFoundError.type, Unit]
}
