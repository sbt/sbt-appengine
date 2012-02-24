sbt-appengine is a sbt 0.10+ port of the awesome [sbt-appengine-plugin][1] by [yasushi][2].

usage
=====
export environment variables (actually, JRebel support is not ported yet).

    export APPENGINE_SDK_HOME=~/appengine-java-sdk-1.5.0
    export JREBEL_JAR_PATH=~/jrebel/jrebel.jar

put the following in the `project/plugins.sbt`:

```scala
resolvers += "spray repo" at "http://repo.spray.cc"

addSbtPlugin("com.eed3si9n" % "sbt-appengine" % "0.3.1")
```

for `build.sbt`:

```scala
libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

seq(appengineSettings: _*)
```

for `build.scala`:

```scala
import sbtappengine.Plugin._
import AppengineKeys._

lazy val example = Project("web", file("web"),
  settings = buildSettings ++ appengineSettings ++
             Seq( // your settings here
             ))
```

you can now deploy your application like this:

    > appengine-deploy

to start the development server:

    > appengine-dev-server

to redeploy development server continuously:

    > ~ appengine-dev-server

sample
======

- [simple sample][3]

  [1]: https://github.com/Yasushi/sbt-appengine-plugin
  [2]: https://github.com/Yasushi
  [3]: https://github.com/sbt/sbt-appengine/tree/master/src/sbt-test/sbt-appengine/simple
  