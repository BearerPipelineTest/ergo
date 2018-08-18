package org.ergoplatform.api

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import org.ergoplatform.modifiers.mempool.ErgoTransaction
import org.ergoplatform.nodeView.ErgoReadersHolder.{GetReaders, Readers}
import org.ergoplatform.nodeView.wallet.{ErgoWalletReader, PaymentRequest, PaymentRequestDecoder}
import org.ergoplatform.settings.ErgoSettings
import scorex.core.NodeViewHolder.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.api.http.ApiError.BadRequest
import scorex.core.api.http.ApiResponse
import scorex.core.settings.RESTApiSettings

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class WalletApiRoute(readersHolder: ActorRef, nodeViewActorRef: ActorRef, ergoSettings: ErgoSettings)
                         (implicit val context: ActorRefFactory) extends ErgoBaseApiRoute with ApiCodecs {

  implicit val paymentRequestDecoder = new PaymentRequestDecoder(ergoSettings)

  val settings: RESTApiSettings = ergoSettings.scorexSettings.restApi

  private def getWallet: Future[ErgoWalletReader] = (readersHolder ? GetReaders).mapTo[Readers].map(_.w)

  override val route: Route = (pathPrefix("wallet") & withCors) {
    balancesRoute ~
      unconfirmedBalanceRoute ~
      sendTransactionRoute ~
      generateTransactionRoute ~
      generateAndSendTransactionRoute
  }

  def sendTransactionRoute: Route = (path("transaction") & post & entity(as[ErgoTransaction])) { tx =>
    // todo validation?
    nodeViewActorRef ! LocallyGeneratedTransaction[ErgoTransaction](tx)
    ApiResponse(tx.id)
  }

  def generateTransactionRoute: Route = (path("transaction" / "generate") & post
    & entity(as[Seq[PaymentRequest]])) { payments =>
    Directives.onSuccess(getWallet.flatMap(_.generateTransaction(payments))) {
      case Failure(e) => BadRequest(s"Bad payment request $payments. ${Option(e.getMessage).getOrElse(e.toString)}")
      case Success(tx) => ApiResponse(tx)
    }
  }

  def generateAndSendTransactionRoute: Route = (path("transaction" / "payment") & post
    & entity(as[Seq[PaymentRequest]])) { payments =>
    Directives.onSuccess(getWallet.flatMap(_.generateTransaction(payments))) {
      case Failure(e) => BadRequest(s"Bad payment request $payments. ${Option(e.getMessage).getOrElse(e.toString)}")
      case Success(tx) =>
        nodeViewActorRef ! LocallyGeneratedTransaction[ErgoTransaction](tx)
        ApiResponse(tx.id)
    }
  }

  def balancesRoute: Route = (path("balances") & get) {
    ApiResponse(getWallet.flatMap(_.confirmedBalances()))
  }

  def unconfirmedBalanceRoute: Route = (path("balances" / "unconfirmed") & get) {
    ApiResponse(getWallet.flatMap(_.unconfirmedBalances()))
  }

}
