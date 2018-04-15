name := "queueService"
 
version := "1.0"

scalacOptions += "-Ypartial-unification"
      
lazy val `queueservice` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice)

unmanagedResourceDirectories in Test <+=  baseDirectory (_/"target/web/public/test")

lazy val Version = new {
  val doobie = "0.5.2"
}

libraryDependencies ++= Seq(

  "org.tpolecat" %% "doobie-core"      % Version.doobie,

//  "org.tpolecat" %% "doobie-hikari"    % Version.doobie, // HikariCP transactor.
  "org.tpolecat" %% "doobie-postgres"  % Version.doobie, // Postgres driver 42.2.2 + type mappings.
  "org.tpolecat" %% "doobie-specs2"    % Version.doobie, // Specs2 support for typechecking statements.
  "org.tpolecat" %% "doobie-scalatest" % Version.doobie  // ScalaTest support for typechecking statements.
)

libraryDependencies ++= Seq(
  "org.flywaydb" %% "flyway-play" % "5.0.0"
)

      