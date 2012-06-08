sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.4.1-SNAPSHOT"

description := "sbt plugin to deploy on appengine"

licenses := Seq("MIT License" -> url("https://github.com/sbt/sbt-appengine/blob/master/LICENSE"))

libraryDependencies <++= (scalaVersion, sbtVersion) { (scalaV, sv) => Seq(
  "com.github.siasia" %% "xsbt-web-plugin" % (sv + "-0.2.11.1"),
  "cc.spray" % "sbt-revolver" % "0.6.1" extra("scalaVersion" -> scalaV, "sbtVersion" -> sv)
)}

scalacOptions := Seq("-deprecation", "-unchecked")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

resolvers += "Maven.org" at "http://repo1.maven.org/maven2"

resolvers += "spray repo" at "http://repo.spray.cc"

seq(lsSettings :_*)

LsKeys.tags in LsKeys.lsync := Seq("sbt", "appengine", "gae", "web")

publishMavenStyle := false

publishTo <<= (version) { version: String =>
   val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
   val (name, u) = if (version.contains("-SNAPSHOT")) ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                   else ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, url(u))(Resolver.ivyStylePatterns))
}

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")
