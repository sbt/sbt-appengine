sbt-appengine is a sbt 0.10+ port of the awesome [sbt-appengine-plugin][1] by [yasushi][2].

requirements
------------

export environment variables (`JREBEL_PATH` is optional).

    export APPENGINE_SDK_HOME=/Applications/appengine-java-sdk-1.6.2.1
    export JREBEL_PATH=/Applications/ZeroTurnaround/JRebel/jrebel.jar

usage
-----

put the following in the `project/plugins.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-appengine" % "0.4.0")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.cc",
  Resolver.url("sbt-plugin-releases",
    url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
)
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

to (re)start the development server in the background:

    > appengine-dev-server

to redeploy development server continuously:

    > ~ appengine-dev-server

to hot reload development server continuously, set `JREBEL_PATH` and:

    > appengine-dev-server
    > ~ package-war

sample
======

- [simple sample][3]

known issues and workarounds
======

When trying to launch the dev server with `appengine-dev-server`, you might run
into the following exception: `java.lang.RuntimeException: Unable to restore the previous TimeZone`.

This bug was introduced in a java 6 update.

The workaround is simple, fortunately. In your build settings where you include
the appengine settings, you must pass in the following jvm args to the dev
server launch `-Dappengine.user.timezone=UTC`.

```
javaOptions in (Compile, gae.devServer) += "-Dappengine.user.timezone=UTC"
```

[appengine disscussion about the issue][4]

  [1]: https://github.com/Yasushi/sbt-appengine-plugin
  [2]: https://github.com/Yasushi
  [3]: https://github.com/sbt/sbt-appengine/tree/master/src/sbt-test/sbt-appengine/simple
  [4]: http://code.google.com/p/googleappengine/issues/detail?id=6928
