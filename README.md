sbt-appengine is a sbt 0.10+ port of the awesome [Yasushi/sbt-appengine-plugin][1].

# https://github.com/sbt/sbt-appengine/issues/59

requirements
------------

export environment variables (`JREBEL_PATH` is optional).

    export APPENGINE_SDK_HOME=/Applications/appengine-java-sdk-1.6.2.1
    export JREBEL_PATH=/Applications/ZeroTurnaround/JRebel/jrebel.jar

If you have problems setting the APPENGINE_SDK_HOME environment variable (for example if you're using IntelliJ), then you can instead create a `appengine.properties` file with a single line of `sdkHome=/Applications/appengine-java-sdk-1.6.2.1`.

setup for sbt 0.13 and sbt 1.x
------------------

put the following in the `project/appengine.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-appengine" % "0.8.0")
```

and the following in `appengine.sbt`:

```scala
libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

enablePlugins(AppenginePlugin)
```

setup for sbt 0.12
------------------

see <https://github.com/sbt/sbt-appengine/tree/0.6.2#setup-for-sbt-012>

usage
-----

### deploy

you can deploy your application like this:

    > appengineDeploy

### development server

to (re)start the development server in the background:

    > appengineDevServer

to redeploy development server continuously:

    > ~ appengineDevServer

### hot loading!

to hot reload development server continuously, set `JREBEL_PATH` and:

    > appengineDevServer
    > ~ packageWar

by default development server runs in debug mode. IDE can connect to it via port 1044.

### appengineDevServer lifecyle hooks

to run a code on start/stop of dev server:

```scala
(appengineOnStartHooks in appengineDevServer in Compile) += { () =>
  println("hello")
}

(appengineOnStopHooks in appengineDevServer in Compile) += { () =>
  println("bye")
}
```

### backend support

you can deploy your backend application(s) like this:

    > appengineDeployBackends
    
to start a backend instance in the cloud:

    > appengineStartBackend <backend-name>
    
to stop a backend instance:

    > appengineStopBackend <backend-name>

### DataNucleous enhancer support (experimental)

sbt-appengine provides experimental support for DataNucleous enhancer. to use this, include the following in `build.sbt`:

```scala
enablePlugins(AppenginePlugin)

appengineDataNucleusSettings

appenginePersistenceApi in appengineEnhance in Compile := "JDO"
```

this will call the enhancer automatically on `packageWar` task. since DataNucleous expects plain Java fields, the entity class looks a bit ugly in Scala:

```scala
import javax.jdo.annotations._
import com.google.appengine.api.datastore.Key
import scala.annotation.meta.field

@PersistenceCapable
case class Counter(
  @(PrimaryKey @field)
  @(Persistent @field)(valueStrategy = IdGeneratorStrategy.IDENTITY)
  var key: Key,
  @(Persistent @field)
  var count: Int)
```

sample
------

- [simple sample][3]

note
----

When trying to launch the dev server with `appengineDevServer`, you might run
into the following exception: `java.lang.RuntimeException: Unable to restore the previous TimeZone`.
[This issue][4] has been resolved in the latest App Engine SDK.

  [1]: https://github.com/Yasushi/sbt-appengine-plugin
  [2]: https://github.com/Yasushi
  [3]: https://github.com/sbt/sbt-appengine/tree/master/src/sbt-test/sbt-appengine/simple
  [4]: http://code.google.com/p/googleappengine/issues/detail?id=6928
