package sbtappengine

import sbt._
import Keys._
import spray.revolver.RevolverKeys

/**
 * Sbt compatibility code concrete instances are declared in sbt version specific folders
 */
trait SbtCompat {
  /**
   * Changes javaOptions by using transformator function
   * (overridesJarPath, agentJarPath, reJRebelJar, localDbPath in devServer,
   *  debug in devServer, debugPort in devServer) => newJavaOptions
   */
  def changeJavaOptions(f: (File, File, String, File, Boolean, Int) => Seq[String]): Setting[_]
}

object SbtCompat {
  def impl: SbtCompat = SbtCompatImpl
}

// ----

object SbtCompatImpl extends SbtCompat with RevolverKeys {
  import Plugin.{AppengineKeys => gae}

  def changeJavaOptions(f: (File, File, String, File, Boolean, Int) => Seq[String]) = {
    
    val additional =  ((gae.overridesJarPath, gae.agentJarPath, gae.reJRebelJar,
      gae.localDbPath in gae.devServer,
      gae.debug in gae.devServer, gae.debugPort in gae.devServer) map f)

    javaOptions in gae.devServer <++=   additional
    
  }
}
