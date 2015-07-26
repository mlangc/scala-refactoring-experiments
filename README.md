# scala-refactoring-experiments
*Repository for experimenting with scala-refactoring*

#### Usage
To work with this repository you need
```
"org.scala-refactoring" %% "org.scala-refactoring.library" % "0.6.3-SNAPSHOT"
```
to be available locally (you can use `sbt publishLocal` with [this buildfile](https://github.com/scala-ide/scala-refactoring/blob/master/org.scala-refactoring.library/build.sbt)). After you are done clone this repository, cd into it, run
```
$ sbt eclipse
```
and import the project into Scala-IDE.
