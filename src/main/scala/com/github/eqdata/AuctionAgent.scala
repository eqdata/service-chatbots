package com.github.eqdata

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.eqdata.AuctionAgent._
import com.typesafe.scalalogging.LazyLogging
import io.socket.client.{IO, Socket}
import io.socket.emitter.Emitter
import spray.json.DefaultJsonProtocol._
import spray.json.{JsonFormat, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class AuctionAgent(webSocketUrl: String, serverType: String) extends Actor with LazyLogging {

  import JsonProtocol._

  implicit val timeout = Timeout(10 seconds)

  override def receive: Receive = notStarted(Nil)

  private def notStarted(subscribers: List[ActorRef]): Receive = {
    case Subscribe(actor) =>
      context.become(notStarted(actor +: subscribers))
    case Start =>
      logger.trace(s"starting $self")
      Future.sequence(subscribers.map(_ ? Start)).foreach { _ =>
        context.become(started(auctionWebSocket, subscribers))
      }
  }

  private def started(websocket: Socket, subscribers: List[ActorRef]): Receive = {
    case update: AuctionUpdate =>
      logger.trace(s"$update")
      logger.trace(s"Sending update to ${subscribers.size} subscribers")
      val newItems = AuctionCache.post(update)
      for {
        subscriber <- subscribers
        newItem <- newItems
      } {
        subscriber ! newItem
      }
    case Stop =>
      logger.info("disconnecting from websocket & terminating")
      websocket.close()
      context.system.terminate()
  }

  private def auctionWebSocket: Socket = {
    val socket = IO.socket(webSocketUrl)
    socket
      .on(Socket.EVENT_CONNECT, new Emitter.Listener() {
        override def call(args: AnyRef*): Unit = {}
      })
      .on("request-server", new Emitter.Listener {
        override def call(args: AnyRef*): Unit = {
          socket.emit("server-type", serverType)
        }
      })
      .on(
        "join",
        new Emitter.Listener {
          override def call(args: AnyRef*): Unit = {
            val auctionUpdate = args.head.toString.parseJson.convertTo[AuctionUpdate]
            logger.trace(s"Received update on join: $auctionUpdate")
            self ! auctionUpdate
          }
        }
      )
      .on(
        "auctions-updated",
        new Emitter.Listener {
          override def call(args: AnyRef*): Unit = {
            val newAuctions = args.head.toString.parseJson.convertTo[List[Auction]]
            logger.trace(s"Received update: $newAuctions")
            self ! AuctionUpdate(serverType, newAuctions)
          }
        }
      )
      .on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        override def call(args: AnyRef*): Unit = {
          logger.trace(s"disconnection: $args")
        }
      })
    socket.connect()
  }

}

object AuctionAgent {

  object JsonProtocol extends LazyLogging {
    implicit val itemFormat: JsonFormat[Item] = jsonFormat2(Item.apply)
    implicit val auctionFormat: JsonFormat[Auction] = jsonFormat2(Auction.apply)
    implicit val onJoinUpdateReader: JsonReader[AuctionUpdate] = new JsonReader[AuctionUpdate] {
      override def read(json: JsValue): AuctionUpdate = {
        logger.trace(s"received auction update: $json")
        json.asJsObject.getFields("server", "auctions") match {
          case Seq(JsString(server), JsObject(auctionFields)) =>
            auctionFields.get("PreviousAuctions") match {
              case Some(JsArray(auctions)) => AuctionUpdate(server, auctions.toList.map(_.convertTo[Auction]))
              case _                       => throw DeserializationException(s"Could not deserialise to OnJoinUpdate: ${json.compactPrint}")
            }
          case _ => throw DeserializationException(s"Could not deserialise to OnJoinUpdate: ${json.compactPrint}")
        }
      }
    }
  }

  // todo - differentiate between wts and wtb (also "selling", "buying", "wtt", "trading" "mq??")
  case class Item(name: String, uri: String)

  case class Auction(line: String, items: List[Item])

  case class AuctionUpdate(server: String, auctions: List[Auction])

  case class User(name: String) extends AnyVal

  case class Subscribe(actor: ActorRef)

}
