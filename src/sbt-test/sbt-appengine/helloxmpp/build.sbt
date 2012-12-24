import sbtappengine.Plugin.{AppengineKeys => gae}

name := "taskqueueexamples"

libraryDependencies ++= Seq(
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "7.6.8.v20121106" % "container"
)

seq(appengineSettings: _*)

unmanagedJars in Compile <++= gae.libUserPath in Compile map { libUserPath =>
  val baseDirectories = libUserPath +++ (libUserPath / "orm")
  (baseDirectories * "*.jar").classpath
}

unmanagedJars in Compile <++= gae.libPath in Compile map { libPath =>
  ((libPath / "shared") ** "*.jar").classpath
}
