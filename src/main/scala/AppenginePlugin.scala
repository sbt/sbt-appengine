package sbtappengine

import sbt._
import sbt.Process._

object Plugin extends sbt.Plugin {
  import Keys._
  import Project.Initialize
  import com.github.siasia.PluginKeys._
  import com.github.siasia.WebPlugin
  
  object AppengineKeys {
    lazy val requestLogs    = InputKey[Unit]("appengine-request-logs", "Write request logs in Apache common log format.")
    lazy val rollback       = InputKey[Unit]("appengine-rollback", "Rollback an in-progress update.")
    lazy val deploy         = InputKey[Unit]("appengine-deploy", "Create or update an app version.")
    lazy val deployIndexes  = InputKey[Unit]("appengine-deploy-indexes", "Update application indexes.")
    lazy val deployCron     = InputKey[Unit]("appengine-deploy-cron", "Update application cron jobs.")
    lazy val deployQueues   = InputKey[Unit]("appengine-deploy-queues", "Update application task queue definitions.")
    lazy val deployDos      = InputKey[Unit]("appengine-deploy-dos", "Update application DoS protection configuration.")
    lazy val cronInfo       = InputKey[Unit]("appengine-cron-info", "Displays times for the next several runs of each cron job.")
    lazy val devserver      = InputKey[Unit]("appengine-devserver", "Runs web app through development server with optional args")
    
    lazy val apiToolsJar    = SettingKey[String]("appengine-api-tools-jar", "Name of the development startup executable jar.")
    lazy val apiToolsPath   = SettingKey[File]("appengine-api-tools-path", "Path of the development startup executable jar.")
    lazy val sdkVersion     = SettingKey[String]("appengine-sdk-version")
    lazy val sdkPath        = SettingKey[File]("appengine-sdk-path")
    lazy val classpath      = SettingKey[Classpath]("appengine-classpath")
    lazy val apiJarName     = SettingKey[String]("appengine-api-jar-name")
    lazy val apiLabsJarName = SettingKey[String]("appengine-api-labs-jar-name")
    lazy val jsr107CacheJarName = SettingKey[String]("appengine-jsr107-cache-jar-name")
    lazy val binPath        = SettingKey[File]("appengine-bin-path")
    lazy val libPath        = SettingKey[File]("appengine-lib-path")
    lazy val libUserPath    = SettingKey[File]("appengine-lib-user-path")
    lazy val libImplPath    = SettingKey[File]("appengine-lib-impl-path")
    lazy val apiJarPath     = SettingKey[File]("appengine-api-jar-path")
    lazy val appcfgName     = SettingKey[String]("appengine-appcfg-name")
    lazy val appcfgPath     = SettingKey[File]("appengine-appcfg-path")
    lazy val emptyFile      = TaskKey[File]("appengine-empty-file")
    lazy val temporaryWarPath = SettingKey[File]("appengine-temporary-war-path")    
  }
  private val gae = AppengineKeys
  
  // see https://github.com/jberkel/android-plugin/blob/master/src/main/scala/AndroidHelpers.scala
  private def appcfgTask(action: String, outputFile: Option[String],
                         args: TaskKey[Seq[String]],
                         depends: TaskKey[File] = gae.emptyFile) =
    (args, gae.temporaryWarPath, gae.appcfgPath, streams, depends) map { (args, w, appcfgPath, s, m) =>
      val appcfg: Seq[String] = Seq(appcfgPath.absolutePath.toString) ++ args ++ Seq(action, w.absolutePath) ++ outputFile.toSeq
      s.log.debug(appcfg.mkString(" "))
      val out = new StringBuffer
      val process = appcfg.run(true)
      val exit = process.exitValue()
      if (exit != 0) {
        s.log.error(out.toString)
        sys.error("error executing appcfg")
      }
      else s.log.info(out.toString)
      process.destroy()
      ()
    }
  
  private def buildAppengineSdkPath: File = {
    val sdk = System.getenv("APPENGINE_SDK_HOME")
    if (sdk == null) sys.error("You need to set APPENGINE_SDK_HOME")
    new File(sdk)
  }

  private def buildSdkVersion(libUserPath: File): String = {
    val pat = """appengine-api-1.0-sdk-(\d\.\d\.\d(?:\.\d)*)\.jar""".r
    (libUserPath * "appengine-api-1.0-sdk-*.jar").get.toList match {
      case jar::_ => jar.name match {
        case pat(version) => version
        case _ => sys.error("invalid jar file. " + jar)
      }
      case _ => sys.error("not found appengine api jar.")
    }
  }

  private def isWindows = System.getProperty("os.name").startsWith("Windows")
  private def osBatchSuffix = if (isWindows) ".cmd" else ".sh"

  private def launchDevServer(args: TaskKey[Seq[String]]) =
    (args, gae.temporaryWarPath, gae.apiToolsPath, packageWar, streams) map { (args, w, toolsPath, depends, s) =>
      val devServer = Seq("java", "-cp", toolsPath.absolutePath,
        "com.google.appengine.tools.KickStart",
        "com.google.appengine.tools.development.DevAppServerMain"
      )

      val launch: Seq[String] = devServer ++ args ++ Seq(w.absolutePath)

      s.log.info("Press [Enter] to kill appengine dev server...")

      val process = launch.run()
      scala.Console.readLine()
      process.destroy()

      ()
    }

  lazy val baseAppengineSettings: Seq[Project.Setting[_]] = Seq(
    // webappUnmanaged  <<= (gae.temporaryWarPath) { (dir) => dir / "WEB-INF" / "appengine-generated" *** },
    unmanagedClasspath  <++= (gae.classpath) map { (cp) => cp },

    gae.requestLogs <<= inputTask { (args: TaskKey[Seq[String]])   => appcfgTask("request_logs", Some("request.log"), args) },
    gae.rollback <<= inputTask { (args: TaskKey[Seq[String]])      => appcfgTask("rollback", None, args) },
    gae.deploy <<= inputTask { (args: TaskKey[Seq[String]])        => appcfgTask("update", None, args, packageWar) },
    gae.deployIndexes <<= inputTask { (args: TaskKey[Seq[String]]) => appcfgTask("update_indexes", None, args, packageWar) },
    gae.deployCron <<= inputTask { (args: TaskKey[Seq[String]])    => appcfgTask("update_cron", None, args, packageWar) },
    gae.deployQueues <<= inputTask { (args: TaskKey[Seq[String]])  => appcfgTask("update_queues", None, args, packageWar) },
    gae.deployDos <<= inputTask { (args: TaskKey[Seq[String]])     => appcfgTask("update_dos", None, args, packageWar) },
    gae.cronInfo <<= inputTask { (args: TaskKey[Seq[String]])      => appcfgTask("cron_info", None, args) },
    gae.devserver <<= inputTask { (args: TaskKey[Seq[String]])     => launchDevServer(args) },

    gae.apiToolsJar := "appengine-tools-api.jar",
    gae.sdkVersion <<= (gae.libUserPath) { (dir) => buildSdkVersion(dir) },
    gae.sdkPath := buildAppengineSdkPath,
    gae.classpath <<= (gae.apiJarPath) { (jar: File) => Attributed.blankSeq(Seq(jar)) },
    gae.apiJarName <<= (gae.sdkVersion) { (v) => "appengine-api-1.0-sdk-" + v + ".jar" },
    gae.apiLabsJarName <<= (gae.sdkVersion) { (v) => "appengine-api-labs-" + v + ".jar" },
    gae.jsr107CacheJarName <<= (gae.sdkVersion) { (v) => "appengine-jsr107cache-" + v + ".jar" },
    
    gae.binPath <<= gae.sdkPath(_ / "bin"),
    gae.libPath <<= gae.sdkPath(_ / "lib"),
    gae.libUserPath <<= gae.libPath(_ / "user"),
    gae.libImplPath <<= gae.libPath(_ / "impl"),
    gae.apiJarPath <<= (gae.libUserPath, gae.apiJarName) { (dir, name) => dir / name },
    gae.apiToolsPath <<= (gae.libPath, gae.apiToolsJar) { _ / _ },
    gae.appcfgName := "appcfg" + osBatchSuffix,
    gae.appcfgPath <<= (gae.binPath, gae.appcfgName) { (dir, name) => dir / name },
    gae.emptyFile := file(""),
    gae.temporaryWarPath <<= target / "webapp"  
  )

  lazy val webSettings = appengineSettings
  lazy val appengineSettings: Seq[Project.Setting[_]] = WebPlugin.webSettings ++
    inConfig(Compile)(baseAppengineSettings) ++
    inConfig(Test)(Seq(
      unmanagedClasspath <++= (gae.classpath) map { (cp) => cp },
      gae.classpath <<= (gae.classpath in Compile,
        gae.libImplPath in Compile, gae.libPath in Compile) { (cp, impl, lib) =>
        val impljars = (impl * "*.jar").get
        val testingjars = (lib / "testing" * "*.jar").get
        cp ++ Attributed.blankSeq(impljars ++ testingjars)
      }
    ))

  /*
  def appengineToolsJarPath = (appengineLibPath / "appengine-tools-api.jar")
  */
  /* Overrides jar present as of sdk 1.4.3 */
  /*
  def appengineOverridePath = appengineSdkPath / "lib" / "override"
  def overridesJarName = "appengine-dev-jdk-overrides.jar"
  def appengineOverridesJarPath = appengineOverridePath / overridesJarName

  lazy val javaCmd = (Path.fromFile(new java.io.File(System.getProperty("java.home"))) / "bin" / "java").absolutePath

  def appengineAgentPath = appengineLibPath / "agent" / "appengine-agent.jar"

  def devAppserverJvmOptions:Seq[String] = List()
  lazy val devAppserverInstance = new DevAppserverRun
  lazy val devAppserverStart = devAppserverStartAction
  lazy val devAppserverStop = devAppserverStopAction
  def devAppserverStartAction = task{ args => devAppserverStartTask(args) dependsOn(prepareWebapp) }
  def devAppserverStopAction = devAppserverStopTask
  def devAppserverStartTask(args: Seq[String]) = task {devAppserverInstance(args)}
  def devAppserverStopTask = task {
    devAppserverInstance.stop()
    None
  }

  class DevAppserverRun extends Runnable with ExitHook {
    ExitHooks.register(this)
    def name = "dev_appserver-shutdown"
    def runBeforeExiting() { stop() }

    val jvmOptions =
      List("-ea", "-javaagent:"+appengineAgentPath.absolutePath,
           "-cp", appengineToolsJarPath.absolutePath,
           "-Xbootclasspath/p:"+appengineOverridesJarPath) ++ devAppserverJvmOptions

    private var running: Option[Process] = None

    def run() {
      running.foreach(_.exitValue())
      running = None
    }

    def apply(args: Seq[String]): Option[String] = {
      if (running.isDefined)
        Some("An instance of dev_appserver is already running.")
      else {
        val builder: ProcessBuilder =
          Process(javaCmd :: jvmOptions :::
                  "com.google.appengine.tools.development.DevAppServerMain" ::
                  args.toList ::: temporaryWarPath.absolutePath :: Nil,
                  Some(temporaryWarPath.asFile))
        running = Some(builder.run())
        new Thread(this).start()
        None
      }
    }

    def stop() {
      running.foreach(_.destroy)
      running = None
      log.debug("stop")
    }
  }
  */
}

/*
trait DataNucleus extends AppengineProject {
  override def prepareWebappAction = super.prepareWebappAction dependsOn(enhance)

  val appengineORMJarsPath = AppenginePathFinder(appengineLibUserPath / "orm" * "*.jar")
  def appengineORMEnhancerClasspath = (appengineLibPath / "tools" / "orm" * "datanucleus-enhancer-*.jar")  +++ (appengineLibPath / "tools" / "orm" * "asm-*.jar")

  lazy val enhance = enhanceAction
  lazy val enhanceCheck = enhanceCheckAction
  def enhanceAction = enhanceTask(false) dependsOn(compile) describedAs("Executes ORM enhancement.")
  def enhanceCheckAction = enhanceTask(true) dependsOn(compile) describedAs("Just check the classes for enhancement status.")
  def usePersistentApi = "jdo"
  def enhanceTask(checkonly: Boolean) =
    runTask(Some("org.datanucleus.enhancer.DataNucleusEnhancer"),
      appengineToolsJarPath +++ appengineORMEnhancerClasspath +++ compileClasspath ,
      List("-v",
           "-api", usePersistentApi,
           (if(checkonly) "-checkonly" else "")) ++
      mainClasses.get.map(_.absolutePath))
}

trait JRebel extends AppengineProject {
  override def devAppserverJvmOptions =
    if (jrebelPath.isDefined)
      List("-javaagent:" + jrebelPath.get.absolutePath,
           "-noverify") ++ jrebelJvmOptions ++ super.devAppserverJvmOptions
    else
      super.devAppserverJvmOptions

  def jrebelJvmOptions:Seq[String] = List()
  def jrebelPath = {
    val jrebel = System.getenv("JREBEL_JAR_PATH")
    if (jrebel == null) {
      log.error("You need to set JREBEL_JAR_PATH")
      None
    } else
      Some(Path.fromFile(new File(jrebel)))
  }

}
*/
