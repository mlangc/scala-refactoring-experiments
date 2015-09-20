# scala-refactoring-experiments
*Repository for experimenting with scala-refactoring*

#### Usage
To work with this repository you need

    "org.scala-refactoring" %% "org.scala-refactoring.library" % "0.7.0-SNAPSHOT"

to be available locally (run `sbt publishLocal` in the refactoring library after eventually adapting the library version in the respective build file). After you are done clone this repository, cd into it, run
```
$ sbt eclipse
```
and import the project into Scala-IDE.
