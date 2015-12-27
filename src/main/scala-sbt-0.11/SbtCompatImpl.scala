package sbtappengine

import sbt._
import Keys._
import cc.spray.revolver.RevolverKeys

object SbtCompatImpl extends SbtCompat with RevolverKeys {
  import Plugin.{AppengineKeys => gae}

  def changeJavaOptions(f: (File, File, String, File, Boolean, Int) => Seq[String]) =
    javaOptions in gae.devServer <++= (gae.overridesJarPath, gae.agentJarPath, gae.reJRebelJar,
      gae.localDbPath in gae.devServer,
      gae.debug in gae.devServer, gae.debugPort in gae.devServer) apply f
}
