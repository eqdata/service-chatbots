package com.github.eqdata

import akka.actor.{Actor, ActorRef}
import io.socket.client.{IO, Socket}
import io.socket.emitter.Emitter
import spray.json.DefaultJsonProtocol._
import AuctionAgent._
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsonFormat, _}

class AuctionAgent(webSocketUrl: String, serverType: String) extends Actor with LazyLogging {

  import JsonProtocol._

  override def receive: Receive = notStarted(Nil)

  private def notStarted(subscribers: List[ActorRef]): Receive = {
    case Subscribe(actor) => context.become(notStarted(actor +: subscribers))
    case Start            => context.become(started(auctionWebSocket, subscribers))
  }

  private def started(websocket: Socket, subscribers: List[ActorRef]): Receive = {
    case update: AuctionUpdate =>
      logger.trace(s"Sending update to ${subscribers.size} subscribers")
      subscribers.foreach(_ ! update)
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
            logger.trace(s"Recieved update on join: $auctionUpdate")
            self ! auctionUpdate
          }
        }
      )
      .on(
        "auctions-updated",
        new Emitter.Listener {
          override def call(args: AnyRef*): Unit = {
            val newAuctions = args.head.toString.parseJson.convertTo[List[Auction]]
            logger.trace(s"Recieved update: $newAuctions")
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

  // todo - links don't work
  case class Item(name: String, uri: String) {
    def link: String = s"<https://wiki.project1999.com/$uri|$name>"
  }

  // todo - differentiate between wts and wtb (also "selling", "buying", "wtt", "trading" "mq??")
  case class Auction(line: String, items: List[Item])
  case class AuctionUpdate(server: String, auctions: List[Auction])

  case class Subscribe(actor: ActorRef)
  case object Start

}
