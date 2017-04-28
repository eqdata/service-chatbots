package com.github.synesso.p99bot

import java.util
import java.util.{LinkedHashMap => LRUMap}

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common._

import scala.language.postfixOps

case class AuctionCommands(override val bus: MessageEventBus) extends AbstractBot with SlackMessaging {

  private val channels = Map(
    "blue" -> "C4QM0A7NV",
    "test" -> "G4Q22VA67"
  )

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, "p99 bot")

  override def act: Receive = act(new LRUMap(2000, 0.7f, true), new LRUMap(2000, 0.7f, true))

  AuctionClient.auctionStream("blue", self)

  def act(users: LRUMap[User, Map[Item, Int]], items: LRUMap[Item, Map[User, Int]]): Receive = {
    auctionUpdates(users, items) orElse funStuff
  }

  private def auctionUpdates_(users: LRUMap[User, Map[Item, Int]], items: LRUMap[Item, Map[User, Int]]): Receive = {
    case AuctionUpdate(server, auctions) =>
      val messages: List[String] = auctions.flatMap{ a =>
        // todo - aggregate users' items & sort user names
        if (a.items.nonEmpty) {
          val user = User(a.line.takeWhile(_ != ' '))
          val userItems = users.getOrDefault(user, Map.empty)
          val newUserItems = a.items.foldLeft(userItems) { case (map, i) =>
              map.updated(i, 0)
          }
          users.put(user, newUserItems)
          val newItemsSeen = newUserItems.keySet -- userItems.keySet
          if (newItemsSeen.nonEmpty) {
            Some(s"${user.name} is selling ${newItemsSeen.map(_.link).toList.sorted.mkString(", ")}")
          } else None
        } else None
//        publish(OutboundMessage(channels("test"), s"writing ${l.length} character message"))
//        publish(OutboundMessage(channels(server), l))
      }
      aggregate(messages).foreach(l => publish(OutboundMessage(channels("test"), l)))
  }

  private def auctionUpdates(users: LRUMap[User, Map[Item, Int]], items: LRUMap[Item, Map[User, Int]]): Receive = {
    case AuctionUpdate(server, auctions) =>
      val messages: List[Attachment] = auctions.flatMap{ a =>
        // todo - aggregate users' items & sort user names
        if (a.items.nonEmpty) {
          val user = User(a.line.takeWhile(_ != ' '))
          val userItems = users.getOrDefault(user, Map.empty)
          val newUserItems = a.items.foldLeft(userItems) { case (map, i) =>
              map.updated(i, 0)
          }
          users.put(user, newUserItems)
          val newItemsSeen = newUserItems.keySet -- userItems.keySet
          if (newItemsSeen.nonEmpty) {
            Some(Attachment(
              title = Some(s"${user.name} is selling"),
              fields = Some(newItemsSeen.toList.sortBy(_.name).map(i => Field("", i.link, short = true)))
            ))
          } else None
        } else None
      }
      messages.foreach(m => publish(RichOutboundMessage(channels("blue"), List(m))))
  }

  private def funStuff: Receive = {
    case Command("hi" | "hello", _, message) =>
      publish(OutboundMessage(message.channel, s"Hello <@${message.user}>! :dog2:"))
  }
}

case class User(name: String) extends AnyVal
