import sbtappengine.Plugin.{AppengineKeys => gae}

name := "sample"

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-filter" % "0.6.4",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "7.6.8.v20121106" % "container"
)

seq(appengineSettings: _*)
