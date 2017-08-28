sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.7.1-SNAPSHOT"

description := "sbt plugin to deploy on appengine"

licenses := Seq("MIT License" -> url("https://github.com/sbt/sbt-appengine/blob/master/LICENSE"))

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.4.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.0")

scalacOptions := Seq("-deprecation", "-unchecked")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

resolvers += "Maven.org" at "http://repo1.maven.org/maven2"

resolvers += "spray repo" at "http://repo.spray.cc"

publishMavenStyle := false

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")

bintrayRepository := "sbt-plugin-releases"

bintrayOrganization := Some("sbt")

bintrayPackage := "sbt-appengine"
