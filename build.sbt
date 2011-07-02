sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.1-SNAPSHOT"

libraryDependencies += "com.github.siasia" %% "xsbt-web-plugin" % "0.1.0-0.10.0"

scalacOptions := Seq("-deprecation", "-unchecked")

publishMavenStyle := true

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"
