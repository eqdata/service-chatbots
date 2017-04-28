package com.github.synesso.p99bot

import akka.actor.ActorRef
import io.scalac.slack.common._
import io.socket.client.{IO, Socket}
import io.socket.emitter.Emitter
import org.json.{JSONArray, JSONObject}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsonFormat, _}

import scala.collection.mutable.ListBuffer
import scala.util.Random

object AuctionClient {

  implicit val itemFormat: JsonFormat[Item] = jsonFormat2(Item.apply)
  implicit val auctionFormat: JsonFormat[Auction] = jsonFormat2(Auction.apply)
  implicit val onJoinUpdateReader: JsonReader[AuctionUpdate] = new JsonReader[AuctionUpdate] {
    override def read(json: JsValue): AuctionUpdate = {
      println(json)
      json.asJsObject.getFields("server", "auctions") match {
        case Seq(JsString(server), JsObject(auctionFields)) =>
          auctionFields.get("PreviousAuctions") match {
            case Some(JsArray(auctions)) => AuctionUpdate(server, auctions.toList.map(_.convertTo[Auction]))
            case _ => throw new DeserializationException(s"Could not deserialise to OnJoinUpdate: ${json.compactPrint}")
          }
        case _ => throw new DeserializationException(s"Could not deserialise to OnJoinUpdate: ${json.compactPrint}")
      }
    }
  }

  // todo - make it return Future[OnJoinUpdate] & have OnJoinUpdate (renamed) to have a Stream[Auction]
  def auctionStream(serverType: String, bot: ActorRef): Socket = {
    val socket = IO.socket("http://52.205.204.206:3000")
    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
      override def call(args: AnyRef*): Unit = {}
    }).on("request-server", new Emitter.Listener {
      override def call(args: AnyRef*): Unit = {
        socket.emit("server-type", serverType)
      }
    }).on("join", new Emitter.Listener {
      override def call(args: AnyRef*): Unit = {
        val auctionUpdate = args.head.toString.parseJson.convertTo[AuctionUpdate]
        bot ! auctionUpdate
      }
    }).on("auctions-updated", new Emitter.Listener {
      override def call(args: AnyRef*): Unit = {
        val newAuctions = args.head.toString.parseJson.convertTo[List[Auction]]
        bot ! AuctionUpdate(serverType, newAuctions)
      }
    }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
      override def call(args: AnyRef*): Unit = {
        println(s"disconnection: $args")
      }
    })
    socket.connect()
  }
}


// todo - links don't work
case class Item(name: String, uri: String) {
  def link: String = s"<https://wiki.project1999.com/$uri|$name>"
}

// todo - differentiate between wts and wtb (also "selling", "buying", "wtt", "trading" "mq??")
case class Auction(line: String, items: List[Item])
case class AuctionUpdate(server: String, auctions: List[Auction])

