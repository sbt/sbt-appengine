sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.3.2-SNAPSHOT"

description := "sbt plugin to deploy on appengine"

licenses := Seq("MIT License" -> url("https://github.com/sbt/sbt-appengine/blob/master/LICENSE"))

libraryDependencies <++= (scalaVersion, sbtVersion) { (scalaV, sv) => Seq(
  "com.github.siasia" %% "xsbt-web-plugin" % (sv + "-0.2.10"),
  "cc.spray" % "sbt-revolver" % "0.6.0" extra("scalaVersion" -> scalaV, "sbtVersion" -> sv)
    from "http://repo.spray.cc/cc/spray/sbt-revolver_2.9.1_0.11.2/0.6.0/sbt-revolver-0.6.0.jar"
)}

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo <<= version { (v: String) =>
  if(v endsWith "-SNAPSHOT") Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/")
  else Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

resolvers += "Maven.org" at "http://repo1.maven.org/maven2"

resolvers += "spray repo" at "http://repo.spray.cc"

seq(lsSettings :_*)

LsKeys.tags in LsKeys.lsync := Seq("sbt", "appengine", "gae", "web")
