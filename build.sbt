name := "service-chatbot"

organization := "com.github.eqdata"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "jcenter repo" at "http://jcenter.bintray.com",
  "jitpack repo" at "https://jitpack.io"
)

libraryDependencies ++= Seq(
  "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1",
  "io.socket" % "socket.io-client" % "0.8.3",
  "io.spray" %% "spray-json" % "1.3.3",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.github.austinv11" % "Discord4J" % "2.8.1"
)

enablePlugins(DockerPlugin)

imageNames in docker := Seq(
  ImageName(s"synesso/service-chatbot:${git.gitHeadCommit.value.get}"),
  ImageName(s"synesso/service-chatbot:latest")
)

docker := (docker dependsOn assembly).value

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = "/app/server.jar"
  new Dockerfile {
    from("openjdk:8-jre-alpine")
    maintainer("eqdata", "jem.mawson@gmail.com")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
