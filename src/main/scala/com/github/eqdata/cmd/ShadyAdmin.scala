package com.github.eqdata.cmd

import sx.blah.discord.handle.obj.{IGuild, IUser}

import scala.collection.JavaConverters._

trait ShadyAdmin {

  def isAuthorised(author: IUser, guild: IGuild): Boolean =
    author == guild.getOwner ||
      guild.getRolesByName("shady").asScala.exists(author.getRolesForGuild(guild).asScala.contains)

}
