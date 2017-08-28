name := "taskqueueexamples"

libraryDependencies ++= Seq(
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "7.6.8.v20121106" % "container"
)

enablePlugins(AppenginePlugin)

unmanagedJars in Compile ++= {
  val libUserPath = (appengineLibUserPath in Compile).value
  val baseDirectories = libUserPath +++ (libUserPath / "orm")
  (baseDirectories * "*.jar").classpath
}

unmanagedJars in Compile ++= {
  (((appengineLibPath in Compile).value / "shared") ** "*.jar").classpath
}
