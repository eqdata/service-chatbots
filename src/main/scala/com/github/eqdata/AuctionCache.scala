package com.github.eqdata

import java.util.Collections.synchronizedMap
import java.util.{LinkedHashMap => LRUMap}

import com.github.eqdata.AuctionAgent.{Auction, AuctionUpdate, Item, User}

object AuctionCache {

  private val users = synchronizedMap(new LRUMap[User, Map[Item, Int]](2000, 0.7f, true))

  // private val items = synchronizedMap(new LRUMap[Item, Map[User, Int]](2000, 0.7f, true))

  // currently returns a message // todo - return objects that describe new or changed price.
  def post(update: AuctionUpdate): Map[User, Set[Item]] = {
    update.auctions.filter(_.items.nonEmpty).map { a =>
      val user = User(a.line.takeWhile(_ != ' '))
      val userItems = users.getOrDefault(user, Map.empty)
      val newUserItems = a.items.foldLeft(userItems) {
        case (map, i) =>
          map.updated(i, 0)
      }
      users.put(user, newUserItems)
      val newItemsSeen = newUserItems.keySet -- userItems.keySet
      user -> newItemsSeen
    }
  }.toMap.filter { case (_, items) => items.nonEmpty }

}
