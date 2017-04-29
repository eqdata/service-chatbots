name := "p99tunnel-bot"

organization := "com.github.eqdata"

scalaVersion := "2.11.8"

resolvers += "scalac repo" at "https://raw.githubusercontent.com/ScalaConsultants/mvn-repo/master/"

libraryDependencies ++= Seq(
  "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1",
  "io.socket" % "socket.io-client" % "0.8.3",
  "io.spray" %% "spray-json" % "1.3.3",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)

enablePlugins(DockerPlugin)

imageNames in docker := Seq(ImageName(s"synesso/p99tunnel-bot:${git.gitHeadCommit.value.get}"))

docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = "/app/server.jar"
  new Dockerfile {
    from("java:8")
    maintainer("eqdata", "jem.mawson@gmail.com")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

//lazy val root = (project in file("."))
//  .enablePlugins(BuildInfoPlugin, GitVersioning)
//  .settings(
//    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitHeadCommit),
//    buildInfoPackage := "com.tallygo.navigation"
//  )
//
