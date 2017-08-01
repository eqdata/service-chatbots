package com.github.eqdata.cmd

import com.github.eqdata.AuctionAgent.{Item, User}

case class PostAuction(user: User, items: Set[Item])
