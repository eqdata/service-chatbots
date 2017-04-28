package com.github.synesso.p99bot

trait SlackMessaging {

  private val maxCharSize = 500

  def aggregate(lines: List[String]): List[String] = {
    lines.foldLeft(List.empty[String]) {
      case (h :: t, line) if h.length + line.length > maxCharSize => line :: h :: t
      case (h :: t, line) => s"$h\\n$line" :: t
      case (_, line) => List(line)
    }.reverse
  }

}
