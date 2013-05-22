package sbtappengine

import sbt._
import sbt.Process._

object Plugin extends sbt.Plugin {
  import Keys._
  import Project.Initialize
  import com.github.siasia.PluginKeys._
  import com.github.siasia.WebPlugin
  import cc.spray.revolver
  import revolver.Actions._
  import revolver.Utilities._
  
  object AppengineKeys extends revolver.RevolverKeys {
    lazy val requestLogs    = InputKey[Unit]("appengine-request-logs", "Write request logs in Apache common log format.")
    lazy val rollback       = InputKey[Unit]("appengine-rollback", "Rollback an in-progress update.")
    lazy val deploy         = InputKey[Unit]("appengine-deploy", "Create or update an app version.")
    lazy val deployBackends = InputKey[Unit]("appengine-deploy-backends", "Update the specified backend or all backends.")
    lazy val rollbackBackend = InputKey[Unit]("appengine-rollback-backends", "Roll back a previously in-progress update.")
    lazy val configBackends = InputKey[Unit]("appengine-config-backends", "Configure the specified backend.")
    lazy val startBackend   = InputKey[Unit]("appengine-start-backend", "Start the specified backend.")
    lazy val stopBackend    = InputKey[Unit]("appengine-stop-backend", "Stop the specified backend.")
    lazy val deleteBackend  = InputKey[Unit]("appengine-delete-backend", "Delete the specified backend.")
    lazy val deployIndexes  = InputKey[Unit]("appengine-deploy-indexes", "Update application indexes.")
    lazy val deployCron     = InputKey[Unit]("appengine-deploy-cron", "Update application cron jobs.")
    lazy val deployQueues   = InputKey[Unit]("appengine-deploy-queues", "Update application task queue definitions.")
    lazy val deployDos      = InputKey[Unit]("appengine-deploy-dos", "Update application DoS protection configuration.")
    lazy val cronInfo       = InputKey[Unit]("appengine-cron-info", "Displays times for the next several runs of each cron job.")
    lazy val devServer      = InputKey[revolver.AppProcess]("appengine-dev-server", "Run application through development server.")
    lazy val stopDevServer  = TaskKey[Unit]("appengine-stop-dev-server", "Stop development server.")

    lazy val serverOnStarted = SettingKey[Seq[() => Unit]]("appengine-dev-server-onStarted")
    lazy val serverOnStopped = SettingKey[Seq[() => Unit]]("appengine-dev-server-onStopped")

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
    lazy val overridePath   = SettingKey[File]("appengine-override-path")
    lazy val overridesJarPath = SettingKey[File]("appengine-overrides-jar-path")
    lazy val agentJarPath   = SettingKey[File]("appengine-agent-jar-path")
    lazy val emptyFile      = TaskKey[File]("appengine-empty-file")
    lazy val temporaryWarPath = SettingKey[File]("appengine-temporary-war-path")
    lazy val localDbPath    = SettingKey[File]("appengine-local-db-path")
    lazy val debug          = SettingKey[Boolean]("appengine-debug")
    lazy val debugPort      = SettingKey[Int]("appengine-debug-port")
    lazy val includeLibUser = SettingKey[Boolean]("appengine-include-lib-user")
  }
  private val gae = AppengineKeys
  
  // see https://github.com/jberkel/android-plugin/blob/master/src/main/scala/AndroidHelpers.scala
  private def appcfgTask(action: String, opts: TaskKey[Seq[String]],
                         depends: TaskKey[File] = gae.emptyFile, outputFile: Option[String] = None) =
    (gae.appcfgPath, opts, gae.temporaryWarPath, streams, depends) map { (appcfgPath, opts, war, s, m) =>
      appcfgTaskCmd(appcfgPath, opts, Seq(action, war.getAbsolutePath) ++ outputFile.toSeq, s)
    }

  private def appcfgBackendTask(action: String, input: TaskKey[Seq[String]],
                                depends: TaskKey[File] = gae.emptyFile, reqParam: Boolean = false) =
    (gae.appcfgPath, input, gae.temporaryWarPath, streams, depends) map { (appcfgPath, input, war, s, m) =>
      val (opts, args) = input.partition(_.startsWith("--"))
      if (reqParam && args.isEmpty)
        sys.error("error executing appcfg: required parameter missing")
      appcfgTaskCmd(appcfgPath, opts, Seq("backends", war.getAbsolutePath, action) ++ args, s)
    }

  private def appcfgTaskCmd(appcfgPath: sbt.File, args: Seq[String],
                            params: Seq[String], s: TaskStreams) = {
      val appcfg: Seq[String] = Seq(appcfgPath.absolutePath.toString) ++ args ++ params
      s.log.debug(appcfg.mkString(" "))
      val out = new StringBuffer
      val exit = appcfg!<

      if (exit != 0) {
        s.log.error(out.toString)
        sys.error("error executing appcfg")
      }
      else s.log.info(out.toString)
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

  private def restartDevServer(streams: TaskStreams, state: State, fkops: ForkScalaRun, mainClass: Option[String], cp: Classpath,
                               args: Seq[String], startConfig: ExtraCmdLineOptions, war: File,
                               onStart: Seq[() => Unit], onStop: Seq[() => Unit]): revolver.AppProcess = {
    if (state.has(appProcessKey)) {
      colorLogger(streams.log).info("[YELLOW]Stopping dev server ...")
      stopAppWithStreams(streams, state)
      onStop foreach { _.apply() }
    }
    startDevServer(streams, unregisterAppProcess(state, ()), fkops, mainClass, cp, args, startConfig, onStart)
  }

  private def startDevServer(streams: TaskStreams, state: State, fkops: ForkScalaRun, mainClass: Option[String], cp: Classpath,
                             args: Seq[String], startConfig: ExtraCmdLineOptions, onStart: Seq[() => Unit]): revolver.AppProcess = {
    assert(!state.has(appProcessKey))
    colorLogger(streams.log).info("[YELLOW]Starting dev server in the background ...")
    onStart foreach { _.apply() }
    revolver.AppProcess {
      Fork.java.fork(fkops.javaHome,
        Seq("-cp", cp.map(_.data.absolutePath).mkString(System.getProperty("file.separator"))) ++
        fkops.runJVMOptions ++ startConfig.jvmArgs ++ 
        Seq(mainClass.get) ++
        startConfig.startArgs ++ args,
        fkops.workingDirectory, Map(), false, StdoutOutput)
    }
  }

  lazy val baseAppengineSettings: Seq[Project.Setting[_]] = Seq(
    // this is classpath during compile
    unmanagedClasspath <++= (gae.classpath) map { (cp) => cp },
    // this is classpath included into WEB-INF/lib
    // https://developers.google.com/appengine/docs/java/tools/ant
    // "All of these JARs are in the SDK's lib/user/ directory."
    unmanagedClasspath in DefaultClasspathConf <++= (unmanagedClasspath) map { (cp) => cp },
        
    gae.requestLogs <<= inputTask { (input: TaskKey[Seq[String]])   => appcfgTask("request_logs", input, outputFile = Some("request.log")) },
    gae.rollback <<= inputTask { (input: TaskKey[Seq[String]])      => appcfgTask("rollback", input) },
    gae.deploy <<= inputTask { (input: TaskKey[Seq[String]])        => appcfgTask("update", input, packageWar) },
    gae.deployIndexes <<= inputTask { (input: TaskKey[Seq[String]]) => appcfgTask("update_indexes", input, packageWar) },
    gae.deployCron <<= inputTask { (input: TaskKey[Seq[String]])    => appcfgTask("update_cron", input, packageWar) },
    gae.deployQueues <<= inputTask { (input: TaskKey[Seq[String]])  => appcfgTask("update_queues", input, packageWar) },
    gae.deployDos <<= inputTask { (input: TaskKey[Seq[String]])     => appcfgTask("update_dos", input, packageWar) },
    gae.cronInfo <<= inputTask { (input: TaskKey[Seq[String]])      => appcfgTask("cron_info", input) },

    gae.deployBackends <<= inputTask { (input: TaskKey[Seq[String]])  => appcfgBackendTask("update", input, packageWar) },
    gae.configBackends <<= inputTask { (input: TaskKey[Seq[String]])  => appcfgBackendTask("configure", input, packageWar) },
    gae.rollbackBackend <<= inputTask { (input: TaskKey[Seq[String]]) => appcfgBackendTask("rollback", input, packageWar, true) },
    gae.startBackend <<= inputTask { (input: TaskKey[Seq[String]])    => appcfgBackendTask("start", input, packageWar, true) },
    gae.stopBackend <<= inputTask { (input: TaskKey[Seq[String]])     => appcfgBackendTask("stop", input, packageWar, true) },
    gae.deleteBackend <<= inputTask { (input: TaskKey[Seq[String]])   => appcfgBackendTask("delete", input, packageWar, true) },
    
    gae.devServer <<= InputTask(startArgsParser) { args =>
      (streams, state, gae.reForkOptions in gae.devServer, mainClass in gae.devServer, fullClasspath in gae.devServer,
        gae.reStartArgs in gae.devServer, args, packageWar, gae.serverOnStarted in gae.devServer, gae.serverOnStopped in gae.devServer)
        .map(restartDevServer)
        .updateState(registerAppProcess)
        .dependsOn(products in Compile) },
    gae.reForkOptions in gae.devServer <<= (gae.temporaryWarPath,
        javaOptions in gae.devServer, outputStrategy, javaHome) map { (wp, jvmOptions, strategy, javaHomeDir) => ForkOptions(
        scalaJars = Nil,
        javaHome = javaHomeDir,
        connectInput = false,
        outputStrategy = strategy,
        runJVMOptions = jvmOptions,
        workingDirectory = Some(wp)
      )
    },
    mainClass in gae.devServer := Some("com.google.appengine.tools.development.DevAppServerMain"),
    fullClasspath in gae.devServer <<= (gae.apiToolsPath) map { (jar: File) => Seq(jar).classpath },
    gae.localDbPath in gae.devServer <<= target / "local_db.bin",
    gae.reStartArgs in gae.devServer <<= gae.temporaryWarPath { (wp) => Seq(wp.absolutePath) },
    // http://thoughts.inphina.com/2010/06/24/remote-debugging-google-app-engine-application-on-eclipse/
    gae.debug in gae.devServer := true,
    gae.debugPort in gae.devServer := 1044,
    SbtCompat.impl.changeJavaOptions { (o, a, jr, ldb, d, dp) =>
      Seq("-ea" , "-javaagent:" + a.getAbsolutePath, "-Xbootclasspath/p:" + o.getAbsolutePath,
        "-Ddatastore.backing_store=" + ldb.getAbsolutePath) ++
      Seq("-Djava.awt.headless=true") ++
      (if (d) Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=" + dp.toString) else Nil) ++
      createJRebelAgentOption(revolver.SysoutLogger, jr).toSeq },
    gae.stopDevServer <<= gae.reStop map {identity},

    gae.serverOnStarted := Nil,
    gae.serverOnStopped := Nil,

    gae.apiToolsJar := "appengine-tools-api.jar",
    gae.sdkVersion <<= (gae.libUserPath) { (dir) => buildSdkVersion(dir) },
    gae.sdkPath := buildAppengineSdkPath,

    gae.includeLibUser := true,
    // this controls appengine classpath, which is used in unmanagedClasspath
    gae.classpath <<= (gae.includeLibUser, gae.libUserPath) { (b, dir) =>
      if (b) (dir ** "*.jar").classpath
      else Nil
    },
    
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
    gae.overridePath <<= gae.libPath(_ / "override"),
    gae.overridesJarPath <<= (gae.overridePath) { (dir) => dir / "appengine-dev-jdk-overrides.jar" },
    gae.agentJarPath <<= (gae.libPath) { (dir) => dir / "agent" / "appengine-agent.jar" },
    gae.emptyFile := file(""),
    gae.temporaryWarPath <<= target / "webapp"
  )

  lazy val webSettings = appengineSettings
  lazy val appengineSettings: Seq[Project.Setting[_]] = WebPlugin.webSettings ++
    inConfig(Compile)(revolver.RevolverPlugin.Revolver.settings ++ baseAppengineSettings) ++
    inConfig(Test)(Seq(
      unmanagedClasspath <++= (gae.classpath) map { (cp) => cp },
      gae.classpath <<= (gae.classpath in Compile,
        gae.libImplPath in Compile, gae.libPath in Compile) { (cp, impl, lib) =>
        val impljars = (impl * "*.jar").get
        val testingjars = (lib / "testing" * "*.jar").get
        cp ++ Attributed.blankSeq(impljars ++ testingjars)
      }
    )) ++
    Seq(
      watchSources <++= (webappResources in Compile) map { (wr) => (wr ** "*").get })
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

*/
