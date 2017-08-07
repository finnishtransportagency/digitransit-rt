import sbt._
import sbt.Keys.{baseDirectory, managedSourceDirectories, _}
import org.scalatra.sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin.MergeStrategy
import org.scalatra.sbt.PluginKeys._
import sbtprotoc.ProtocPlugin.autoImport.PB

object LiviRealTime extends Build {
  val Organization = "fi.liikennevirasto"
  val ProjectName = "message-processor"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.7"
  val ScalatraVersion = "2.3.1"
  val env = if (System.getProperty("realtime.env") != null) System.getProperty("realtime.env") else "dev"
  val testEnv = if (System.getProperty("realtime.env") != null) System.getProperty("realtime.env") else "test"

  lazy val commonJar = Project (
    "realtime-common",
    file("realtime-common"),
    settings = Defaults.defaultSettings ++
      Seq(
        organization := Organization,
        name := "realtime-common",
        version := Version,
        scalaVersion := ScalaVersion,
        resolvers ++= Seq(Resolver.sonatypeRepo("public"), Classpaths.typesafeReleases,
          "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"),
        scalacOptions ++= Seq("-unchecked", "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatra" %% "scalatra" % ScalatraVersion,
          "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0",
          "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
          "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.6" % "test",
          "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,
          "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
          "org.apache.httpcomponents" % "httpclient" % "4.5.3",
          "com.typesafe.akka" %% "akka-actor" % "2.4.17",
          "com.typesafe.akka" %% "akka-http" % "10.0.3",
          "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.6"
        ),
        unmanagedResourceDirectories in Compile += baseDirectory.value / "conf" /  env,
        unmanagedResourceDirectories in Test += baseDirectory.value / "conf" /  testEnv,
        PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value),
        PB.protoSources in Compile := Seq(file("realtime-common/src/main/protobuf")),
        unmanagedSourceDirectories in Compile += baseDirectory.value / "target/scala-2.11/src_managed/main/"
      )
  )

  lazy val processorJar = Project (
    ProjectName,
    file(ProjectName),
    settings = Defaults.defaultSettings ++
      Seq(
        organization := Organization,
        name := ProjectName,
        version := Version,
        scalaVersion := ScalaVersion,
        resolvers ++= Seq(Resolver.sonatypeRepo("public"), Classpaths.typesafeReleases,
          "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"),
        scalacOptions ++= Seq("-unchecked", "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatra" %% "scalatra" % ScalatraVersion,
          "org.scalatra" %% "scalatra-json" % ScalatraVersion,
          "org.apache.commons" % "commons-lang3" % "3.2",
          "commons-codec" % "commons-codec" % "1.9",
          "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
          "org.json4s"   %% "json4s-jackson" % "3.2.11",
          "org.mockito" % "mockito-core" % "1.9.5" % "test",
          "com.googlecode.flyway" % "flyway-core" % "2.3" % "test",
          "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0",
          "com.typesafe.slick" % "slick_2.11" % "3.1.1",
          "com.github.tminglei" %% "slick-pg" % "0.15.0-M3",
          "com.github.tminglei" %% "slick-pg_joda-time" % "0.15.0-M3",
          "com.github.tminglei" %% "slick-pg_jts" % "0.15.0-M3",
          "com.github.tminglei" %% "slick-pg_json4s" % "0.15.0-M3",
          "com.github.tminglei" %% "slick-pg_play-json" % "0.15.0-M3"
        ),
        unmanagedResourceDirectories in Compile += baseDirectory.value / "conf" /  env,
        unmanagedResourceDirectories in Test += baseDirectory.value / "conf" /  testEnv
      )
  ) dependsOn(commonJar)
  lazy val gtfsRtConnectorJar = Project (
    "gtfs-rt-connector",
    file("gtfs-rt-connector"),
    settings = Defaults.defaultSettings ++
      Seq(
        organization := Organization,
        name := "gtfs-rt-connector",
        version := Version,
        scalaVersion := ScalaVersion,
        resolvers ++= Seq(Resolver.sonatypeRepo("public"), Classpaths.typesafeReleases,
          "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"),
        scalacOptions ++= Seq("-unchecked", "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatra" %% "scalatra" % ScalatraVersion,
          "org.scalatra" %% "scalatra-json" % ScalatraVersion,
          "org.apache.commons" % "commons-lang3" % "3.2",
          "commons-codec" % "commons-codec" % "1.9",
          "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
          "org.json4s"   %% "json4s-jackson" % "3.2.11",
          "org.mockito" % "mockito-core" % "1.9.5" % "test",
          "com.googlecode.flyway" % "flyway-core" % "2.3" % "test",
          "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0",
          "org.slf4j" % "slf4j-simple" % "1.6.1"
        ),
        unmanagedResourceDirectories in Compile += baseDirectory.value / "conf" /  env,
        unmanagedResourceDirectories in Test += baseDirectory.value / "conf" /  testEnv
      )
  ) dependsOn(commonJar)
  lazy val gtfsBinaryConnectorJar = Project (
    "gtfs-binary-connector",
    file("gtfs-binary-connector"),
    settings = Defaults.defaultSettings ++
      Seq(
        organization := Organization,
        name := "gtfs-binary-connector",
        version := Version,
        scalaVersion := ScalaVersion,
        resolvers ++= Seq(Resolver.sonatypeRepo("public"), Classpaths.typesafeReleases,
          "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"),
        scalacOptions ++= Seq("-unchecked", "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatra" %% "scalatra" % ScalatraVersion,
          "org.scalatra" %% "scalatra-json" % ScalatraVersion,
          "org.apache.commons" % "commons-lang3" % "3.2",
          "commons-codec" % "commons-codec" % "1.9",
          "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
          "org.json4s"   %% "json4s-jackson" % "3.2.11",
          "org.mockito" % "mockito-all" % "1.9.5" % "test",
          "com.googlecode.flyway" % "flyway-core" % "2.3" % "test",
          "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0",
          "org.slf4j" % "slf4j-simple" % "1.6.1",
          "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.6",
          "com.typesafe.akka" %% "akka-actor" % "2.4.17"
        ),
        unmanagedResourceDirectories in Compile += baseDirectory.value / "conf" /  env,
        unmanagedResourceDirectories in Test += baseDirectory.value / "conf" /  testEnv
      )
  ) dependsOn(commonJar)
  lazy val railsConnectorJar = Project (
    "vr-polling-connector",
    file("vr-polling-connector"),
    settings = Defaults.defaultSettings ++
      Seq(
        organization := Organization,
        name := "vr-polling-connector",
        version := Version,
        scalaVersion := ScalaVersion,
        resolvers ++= Seq(Resolver.sonatypeRepo("public"), Classpaths.typesafeReleases,
          "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"),
        scalacOptions ++= Seq("-unchecked", "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatra" %% "scalatra" % ScalatraVersion,
          "org.scalatra" %% "scalatra-json" % ScalatraVersion,
          "org.apache.commons" % "commons-lang3" % "3.2",
          "commons-codec" % "commons-codec" % "1.9",
          "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
          "org.json4s"   %% "json4s-jackson" % "3.2.11",
          "org.mockito" % "mockito-all" % "1.9.5" % "test",
          "com.googlecode.flyway" % "flyway-core" % "2.3" % "test",
          "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0",
          "org.slf4j" % "slf4j-simple" % "1.6.1",
          "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.6",
          "com.typesafe.akka" %% "akka-actor" % "2.4.17",
          "com.typesafe.akka" %% "akka-http" % "10.0.3"
        ),
        unmanagedResourceDirectories in Compile += baseDirectory.value / "conf" /  env,
        unmanagedResourceDirectories in Test += baseDirectory.value / "conf" /  testEnv
      )
  ) dependsOn(commonJar)
  lazy val root = Project (
    id = "root",
    base = file("."),
    settings = Project.defaultSettings ++
      assemblySettings ++
      Seq(
        organization := Organization,
        name := "root",
        version := Version,
        scalaVersion := ScalaVersion,
        resolvers ++= Seq(Resolver.sonatypeRepo("public"), Classpaths.typesafeReleases,
          "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"),
        scalacOptions ++= Seq("-unchecked", "-feature")
      )
  ) dependsOn(
    processorJar, gtfsRtConnectorJar, gtfsBinaryConnectorJar, commonJar, railsConnectorJar
  ) aggregate(
    processorJar, gtfsRtConnectorJar, gtfsBinaryConnectorJar, commonJar, railsConnectorJar)

  val assemblySettings = sbtassembly.Plugin.assemblySettings ++ Seq(
    mainClass in assembly := Some("fi.liikennevirasto.realtime.MessageProcessor"),
    test in assembly := {}
  )
}
