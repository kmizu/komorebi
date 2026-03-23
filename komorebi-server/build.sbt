val scala3Version = "3.3.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "komorebi-server",
    version := "0.1.0",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-deprecation",
      "-feature",
    ),

    // HTTP server
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.11.11",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"    % "1.11.11",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.11.11",
      "org.http4s"                  %% "http4s-ember-server"  % "0.23.30",
    ),

    // JSON
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser"  % "0.14.10",
    ),

    // Database
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"   % "1.0.0-RC6",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC6",
      "org.xerial"    % "sqlite-jdbc"   % "3.47.2.0",
    ),

    // HTTP client
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "cats"  % "3.10.2",
      "com.softwaremill.sttp.client3" %% "circe" % "3.10.2",
    ),

    // Effects
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.7",

    // Config
    libraryDependencies += "com.github.pureconfig" %% "pureconfig-core" % "0.17.8",

    // Logging
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "ch.qos.logback" % "logback-classic" % "1.5.12",
    ),

    // Test
    libraryDependencies ++= Seq(
      "org.scalameta"  %% "munit"               % "1.0.3"  % Test,
      "org.typelevel"  %% "munit-cats-effect"    % "2.0.0"  % Test,
    ),

    // CORS
    libraryDependencies += "org.http4s" %% "http4s-server" % "0.23.30",

    testFrameworks += new TestFramework("munit.Framework"),
  )
