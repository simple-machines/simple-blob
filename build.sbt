name := "simple-blob"

organization := "au.com.simplemachines"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  Seq(
    "com.google.guava" % "guava" % "18.0",
    "com.google.code.findbugs" % "jsr305" % "2.0.0",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
    "au.com.simplemachines" %% "simple-net" % "1.0.0",
    "org.specs2" %% "specs2-core" % "3.6.2" % "test",
    "org.specs2" %% "specs2-mock" % "3.6.2" % "test"
  )
}

publishMavenStyle := true

publishTo := Some {
  "simplemachines repo" at {
    "http://nexus.simplemachines.com.au/content/repositories/" + {
      if (version.value.trim.endsWith("SNAPSHOT")) "snapshots/" else "releases/"
    }
  }
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")