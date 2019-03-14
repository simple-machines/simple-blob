name := "simple-blob"

organization := "au.com.simplemachines"

scalaVersion := "2.12.8"

libraryDependencies ++= {
  Seq(
    "com.google.guava" % "guava" % "18.0",
    "com.google.code.findbugs" % "jsr305" % "2.0.0",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.517",
    "org.specs2" %% "specs2-core" % "3.8.9" % "test",
    "org.specs2" %% "specs2-mock" % "3.8.9" % "test"
  )
}

publishMavenStyle := true

publishTo := Some {
  "skyfii repo" at {
    "https://nexus.skyfii.com/nexus/content/repositories/" + {
      if (version.value.trim.endsWith("SNAPSHOT")) "snapshots/" else "releases/"
    }
  }
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
