name := "simple-blob"

organization := "au.com.simplemachines"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7", "2.12.2")

libraryDependencies ++= {
  Seq(
    "com.google.guava" % "guava" % "18.0",
    "com.google.code.findbugs" % "jsr305" % "2.0.0",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    "au.com.simplemachines" %% "simple-net" % "1.0.1",
    "org.specs2" %% "specs2-core" % "3.8.9" % "test",
    "org.specs2" %% "specs2-mock" % "3.8.9" % "test"
  )
}

publishMavenStyle := true

publishTo := Some {
  "simplemachines repo" at {
    "https://nexus.simplemachines.com.au/content/repositories/" + {
      if (version.value.trim.endsWith("SNAPSHOT")) "snapshots/" else "releases/"
    }
  }
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
