organization := "com.github.mlangc"

name := "scala-refactoring-experiments"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions in ThisBuild := Seq("-encoding", "utf8", "-feature", "-deprecation", "-optimise", "-target:jvm-1.8", "-Ywarn-unused", "-Ywarn-dead-code", "-Ywarn-unused-import")

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value

libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.6" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.scala-refactoring" %% "org.scala-refactoring.library" % "0.7.0-SNAPSHOT"

libraryDependencies += "com.google.guava" % "guava" % "18.0"

libraryDependencies += "commons-io" % "commons-io" % "2.4"
