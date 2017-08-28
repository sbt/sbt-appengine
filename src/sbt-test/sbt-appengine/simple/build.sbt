name := "sample"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  "ws.unfiltered" %% "unfiltered-filter" % "0.9.1",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
)

enablePlugins(AppenginePlugin)

(appengineOnStartHooks in appengineDevServer in Compile) += { () =>
  println("hello")
}

(appengineOnStopHooks in appengineDevServer in Compile) += { () =>
  println("bye")
}

appengineDataNucleusSettings

appenginePersistenceApi in appengineEnhance in Compile := "JDO"
