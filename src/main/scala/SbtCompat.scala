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
  import AppenginePlugin.autoImport._

  def changeJavaOptions(f: (File, File, String, File, Boolean, Int) => Seq[String]) =
    javaOptions in appengineDevServer := f(
      appengineOverridesJarPath.value,
      appengineAgentJarPath.value,
      spray.revolver.RevolverPlugin.autoImport.reJRebelJar.value,
      (appengineLocalDbPath in appengineDevServer).value,
      (appengineDebug in appengineDevServer).value,
      (appengineDebugPort in appengineDevServer).value
    )
}
