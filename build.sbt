name := "FlashAndFurious"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:_"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.2",
  "com.typesafe.akka" %% "akka-stream" % "2.4.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.2",
  "io.scalac" %% "reactive-rabbit" % "1.0.3",
  "com.typesafe.scala-logging" %%  "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-core" % "1.1.6",
  "ch.qos.logback" % "logback-classic" % "1.1.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

cancelable in Global := true
fork := true
