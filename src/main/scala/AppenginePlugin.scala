package sbtappengine

import java.util.Properties

import sbt._
import spray.revolver.RevolverPlugin

@deprecated("will be removed. use enablePlugins(AppenginePlugin)", "0.7.0")
object Plugin {
  val AppengineKeys = AppenginePlugin.autoImport.AppengineKeys
  val appengineSettings = AppenginePlugin.projectSettings
}

object AppenginePlugin extends AutoPlugin {
  import Keys._
  import Def.Initialize
  import com.earldouglas.xsbtwebplugin.PluginKeys._
  import com.earldouglas.xsbtwebplugin.WebPlugin
  import spray.revolver
  import revolver.Actions._
  import revolver.Utilities._

  override def requires = sbt.plugins.JvmPlugin

  object autoImport {
    @deprecated("", "")
    object AppengineKeys extends revolver.RevolverKeys

    lazy val appengineRequestLogs        = InputKey[Unit]("appengine-request-logs", "Write request logs in Apache common log format.")
    lazy val appengineRollback           = InputKey[Unit]("appengine-rollback", "Rollback an in-progress update.")
    lazy val appengineDeploy             = InputKey[Unit]("appengine-deploy", "Create or update an app version.")
    lazy val appengineDeployBackends     = InputKey[Unit]("appengine-deploy-backends", "Update the specified backend or all backends.")
    lazy val appengineRollbackBackend    = InputKey[Unit]("appengine-rollback-backends", "Roll back a previously in-progress update.")
    lazy val appengineConfigBackends     = InputKey[Unit]("appengine-config-backends", "Configure the specified backend.")
    lazy val appengineStartBackend       = InputKey[Unit]("appengine-start-backend", "Start the specified backend.")
    lazy val appengineStopBackend        = InputKey[Unit]("appengine-stop-backend", "Stop the specified backend.")
    lazy val appengineDeleteBackend      = InputKey[Unit]("appengine-delete-backend", "Delete the specified backend.")
    lazy val appengineDeployIndexes      = InputKey[Unit]("appengine-deploy-indexes", "Update application indexes.")
    lazy val appengineDeployCron         = InputKey[Unit]("appengine-deploy-cron", "Update application cron jobs.")
    lazy val appengineDeployQueues       = InputKey[Unit]("appengine-deploy-queues", "Update application task queue definitions.")
    lazy val appengineDeployDos          = InputKey[Unit]("appengine-deploy-dos", "Update application DoS protection configuration.")
    lazy val appengineCronInfo           = InputKey[Unit]("appengine-cron-info", "Displays times for the next several runs of each cron job.")
    lazy val appengineDevServer          = InputKey[revolver.AppProcess]("appengine-dev-server", "Run application through development server.")
    lazy val appengineStopDevServer      = TaskKey[Unit]("appengine-stop-dev-server", "Stop development server.")
    lazy val appengineEnhance            = TaskKey[Unit]("appengine-enhance", "Execute ORM enhancement.")
    lazy val appengineEnhanceCheck       = TaskKey[Unit]("appengine-enhance-check", "Just check the classes for enhancement status.")

    lazy val appengineOnStartHooks       = SettingKey[Seq[() => Unit]]("appengine-on-start-hooks")
    lazy val appengineOnStopHooks        = SettingKey[Seq[() => Unit]]("appengine-on-stop-hooks")
    lazy val appengineApiToolsJar        = SettingKey[String]("appengine-api-tools-jar", "Name of the development startup executable jar.")
    lazy val appengineApiToolsPath       = SettingKey[File]("appengine-api-tools-path", "Path of the development startup executable jar.")
    lazy val appengineSdkVersion         = SettingKey[String]("appengine-sdk-version")
    lazy val appengineSdkPath            = SettingKey[File]("appengine-sdk-path")
    lazy val appengineClasspath          = SettingKey[Classpath]("appengine-classpath")
    lazy val appengineApiJarName         = SettingKey[String]("appengine-api-jar-name")
    lazy val appengineApiLabsJarName     = SettingKey[String]("appengine-api-labs-jar-name")
    lazy val appengineJsr107CacheJarName = SettingKey[String]("appengine-jsr107-cache-jar-name")
    lazy val appengineBinPath            = SettingKey[File]("appengine-bin-path")
    lazy val appengineLibPath            = SettingKey[File]("appengine-lib-path")
    lazy val appengineLibUserPath        = SettingKey[File]("appengine-lib-user-path")
    lazy val appengineLibImplPath        = SettingKey[File]("appengine-lib-impl-path")
    lazy val appengineApiJarPath         = SettingKey[File]("appengine-api-jar-path")
    lazy val appengineAppcfgName         = SettingKey[String]("appengine-appcfg-name")
    lazy val appengineAppcfgPath         = SettingKey[File]("appengine-appcfg-path")
    lazy val appengineOverridePath       = SettingKey[File]("appengine-override-path")
    lazy val appengineOverridesJarPath   = SettingKey[File]("appengine-overrides-jar-path")
    lazy val appengineAgentJarPath       = SettingKey[File]("appengine-agent-jar-path")
    lazy val appengineEmptyFile          = TaskKey[File]("appengine-empty-file")
    lazy val appengineTemporaryWarPath   = SettingKey[File]("appengine-temporary-war-path")
    lazy val appengineLocalDbPath        = SettingKey[File]("appengine-local-db-path")
    lazy val appengineDebug              = SettingKey[Boolean]("appengine-debug")
    lazy val appengineDebugPort          = SettingKey[Int]("appengine-debug-port")
    lazy val appengineIncludeLibUser     = SettingKey[Boolean]("appengine-include-lib-user")
    lazy val appenginePersistenceApi     = SettingKey[String]("appengine-persistence-api", "Name of the API we are enhancing for: JDO, JPA.")

    @deprecated("will be removed. use enablePlugins(AppenginePlugin)", "0.7.0")
    lazy val appengineSettings = AppenginePlugin.projectSettings

    lazy val appengineDataNucleusSettings: Seq[Def.Setting[_]] =
      inConfig(Compile)(baseAppengineDataNucleusSettings)
  }
  import autoImport._

  object AppEngine {
    // see https://github.com/jberkel/android-plugin/blob/master/src/main/scala/AndroidHelpers.scala
    def appcfgTask(action: String,
                   depends: TaskKey[File] = appengineEmptyFile, outputFile: Option[String] = None): Initialize[InputTask[Unit]] =
      Def.inputTask {
        import complete.DefaultParsers._
        val input: Seq[String] = spaceDelimited("<arg>").parsed
        val x = depends.value
        appcfgTaskCmd(appengineAppcfgPath.value, input, Seq(action, appengineTemporaryWarPath.value.getAbsolutePath) ++ outputFile.toSeq, streams.value)
      }
    def appcfgBackendTask(action: String,
                          depends: TaskKey[File] = appengineEmptyFile, reqParam: Boolean = false): Initialize[InputTask[Unit]] =
      Def.inputTask {
        import complete.DefaultParsers._
        val input: Seq[String] = spaceDelimited("<arg>").parsed
        val (opts, args) = input.partition(_.startsWith("--"))
        if (reqParam && args.isEmpty) {
          sys.error("error executing appcfg: required parameter missing")
        }
        val x = depends.value
        appcfgTaskCmd(appengineAppcfgPath.value, opts, Seq("backends", appengineTemporaryWarPath.value.getAbsolutePath, action) ++ args, streams.value)
      }
    def appcfgTaskCmd(appcfgPath: sbt.File, args: Seq[String],
                              params: Seq[String], s: TaskStreams) = {
        val appcfg: Seq[String] = Seq(appcfgPath.absolutePath.toString) ++ args ++ params
        s.log.debug(appcfg.mkString(" "))
        val out = new StringBuffer
        val exit = Process(appcfg)!<

        if (exit != 0) {
          s.log.error(out.toString)
          sys.error("error executing appcfg")
        }
        else s.log.info(out.toString)
        ()
      }
    
    def buildAppengineSdkPath(baseDir: File): File = {
      var sdk = System.getenv("APPENGINE_SDK_HOME")
      if (sdk == null) {
        val appengineSettings = baseDir / "appengine.properties"
        val prop = new Properties()
        IO.load(prop, appengineSettings)
        sdk = prop.getProperty("sdkHome")
      }
      if (sdk == null) sys.error("You need to set the 'APPENGINE_SDK_HOME' environment variable " +
        "or the 'sdkHome' property in 'appengine.properties'")
      new File(sdk)
    }

    def buildSdkVersion(libUserPath: File): String = {
      val pat = """appengine-api-1.0-sdk-(\d\.\d+\.\d+(?:\.\d+)*)\.jar""".r
      (libUserPath * "appengine-api-1.0-sdk-*.jar").get.toList match {
        case jar::_ => jar.name match {
          case pat(version) => version
          case _ => sys.error("invalid jar file. " + jar)
        }
        case _ => sys.error("not found appengine api jar.")
      }
    }

    def isWindows = System.getProperty("os.name").startsWith("Windows")
    def osBatchSuffix = if (isWindows) ".cmd" else ".sh"

    // see https://github.com/spray/sbt-revolver/blob/master/src/main/scala/spray/revolver/Actions.scala#L26
    def restartDevServer(streams: TaskStreams, logTag: String, project: ProjectRef, options: ForkOptions, mainClass: Option[String],
      cp: Classpath, args: Seq[String], startConfig: ExtraCmdLineOptions, war: File,
      onStart: Seq[() => Unit], onStop: Seq[() => Unit]): revolver.AppProcess = {
      if (revolverState.getProcess(project).exists(_.isRunning)) {
        colorLogger(streams.log).info("[YELLOW]Stopping dev server ...")
        stopAppWithStreams(streams, project)
        onStop foreach { _.apply() }
      }
      startDevServer(streams, logTag, project, options, mainClass, cp, args, startConfig, onStart)
    }
    // see https://github.com/spray/sbt-revolver/blob/master/src/main/scala/spray/revolver/Actions.scala#L32
    def startDevServer(streams: TaskStreams, logTag: String, project: ProjectRef, options: ForkOptions, mainClass: Option[String],
        cp: Classpath, args: Seq[String], startConfig: ExtraCmdLineOptions, onStart: Seq[() => Unit]): revolver.AppProcess = {
      assert(!revolverState.getProcess(project).exists(_.isRunning))

      val color = updateStateAndGet(_.takeColor)
      val logger = new revolver.SysoutLogger(logTag, color, streams.log.ansiCodesSupported)
      colorLogger(streams.log).info("[YELLOW]Starting dev server in the background ...")
      onStart foreach { _.apply() }
      val appProcess = revolver.AppProcess(project, color, logger) {
        Fork.java.fork(options.javaHome,
          Seq("-cp", cp.map(_.data.absolutePath).mkString(System.getProperty("file.separator"))) ++
          options.runJVMOptions ++ startConfig.jvmArgs ++
          Seq(mainClass.get) ++
          startConfig.startArgs ++ args,
          options.workingDirectory, Map(), false, StdoutOutput)
      }
      registerAppProcess(project, appProcess)
      appProcess
    }
  }

  lazy val baseAppengineSettings: Seq[Def.Setting[_]] = Seq(
    // this is classpath during compile
    unmanagedClasspath ++= appengineClasspath.value,
    // this is classpath included into WEB-INF/lib
    // https://developers.google.com/appengine/docs/java/tools/ant
    // "All of these JARs are in the SDK's lib/user/ directory."
    unmanagedClasspath in DefaultClasspathConf ++= unmanagedClasspath.value,

    appengineRequestLogs     := AppEngine.appcfgTask("request_logs", outputFile = Some("request.log")).evaluated,
    appengineRollback        := AppEngine.appcfgTask("rollback").evaluated,
    appengineDeploy          := AppEngine.appcfgTask("update", packageWar).evaluated,
    appengineDeployIndexes   := AppEngine.appcfgTask("update_indexes", packageWar).evaluated,
    appengineDeployCron      := AppEngine.appcfgTask("update_cron", packageWar).evaluated,
    appengineDeployQueues    := AppEngine.appcfgTask("update_queues", packageWar).evaluated,
    appengineDeployDos       := AppEngine.appcfgTask("update_dos", packageWar).evaluated,
    appengineCronInfo        := AppEngine.appcfgTask("cron_info").evaluated,

    appengineDeployBackends  := AppEngine.appcfgBackendTask("update", packageWar).evaluated,
    appengineConfigBackends  := AppEngine.appcfgBackendTask("configure", packageWar).evaluated,
    appengineRollbackBackend := AppEngine.appcfgBackendTask("rollback", packageWar, true).evaluated,
    appengineStartBackend    := AppEngine.appcfgBackendTask("start", packageWar, true).evaluated,
    appengineStopBackend     := AppEngine.appcfgBackendTask("stop", packageWar, true).evaluated,
    appengineDeleteBackend   := AppEngine.appcfgBackendTask("delete", packageWar, true).evaluated,

    appengineDevServer       := {
      val args = startArgsParser.parsed
      val x = (products in Compile).value
      AppEngine.restartDevServer(streams.value, (RevolverPlugin.autoImport.reLogTag in appengineDevServer).value,
        thisProjectRef.value, (RevolverPlugin.autoImport.reForkOptions in appengineDevServer).value,
        (mainClass in appengineDevServer).value, (fullClasspath in appengineDevServer).value,
        (RevolverPlugin.autoImport.reStartArgs in appengineDevServer).value, args,
        packageWar.value,
        (appengineOnStartHooks in appengineDevServer).value, (appengineOnStopHooks in appengineDevServer).value)
    },
    RevolverPlugin.autoImport.reForkOptions in appengineDevServer := {
      ForkOptions(
        javaHome = javaHome.value,
        outputStrategy = outputStrategy.value,
        bootJars = scalaInstance.value.jars,
        workingDirectory = Some(appengineTemporaryWarPath.value),
        runJVMOptions = (javaOptions in appengineDevServer).value,
        connectInput = false,
        envVars = Map.empty
      )
    },
    RevolverPlugin.autoImport.reLogTag in appengineDevServer := "appengineDevServer",
    mainClass in appengineDevServer := Some("com.google.appengine.tools.development.DevAppServerMain"),
    fullClasspath in appengineDevServer := Seq(appengineApiToolsPath.value).classpath,
    appengineLocalDbPath in appengineDevServer := target.value / "local_db.bin",
    RevolverPlugin.autoImport.reStartArgs in appengineDevServer := Seq(appengineTemporaryWarPath.value.absolutePath),
    // http://thoughts.inphina.com/2010/06/24/remote-debugging-google-app-engine-application-on-eclipse/
    appengineDebug in appengineDevServer := true,
    appengineDebugPort in appengineDevServer := 1044,
    appengineOnStartHooks in appengineDevServer := Nil,
    appengineOnStopHooks in appengineDevServer := Nil,
    SbtCompat.impl.changeJavaOptions { (o, a, jr, ldb, d, dp) =>
      Seq("-ea" , "-javaagent:" + a.getAbsolutePath, "-Xbootclasspath/p:" + o.getAbsolutePath,
        "-Ddatastore.backing_store=" + ldb.getAbsolutePath) ++
      Seq("-Djava.awt.headless=true") ++
      (if (d) Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=" + dp.toString) else Nil) ++
      createJRebelAgentOption(revolver.SysoutLogger, jr).toSeq },
    appengineStopDevServer := RevolverPlugin.autoImport.reStop.value,
    
    appengineApiToolsJar := "appengine-tools-api.jar",
    appengineSdkVersion := AppEngine.buildSdkVersion(appengineLibUserPath.value),
    appengineSdkPath := AppEngine.buildAppengineSdkPath(baseDirectory.value),

    appengineIncludeLibUser := true,
    // this controls appengine classpath, which is used in unmanagedClasspath
    appengineClasspath := {
      if (appengineIncludeLibUser.value) (appengineLibUserPath.value ** "*.jar").classpath
      else Nil
    },

    appengineApiJarName := { "appengine-api-1.0-sdk-" + appengineSdkVersion.value + ".jar" },
    appengineApiLabsJarName := { "appengine-api-labs-" + appengineSdkVersion.value + ".jar" },
    appengineJsr107CacheJarName := { "appengine-jsr107cache-" + appengineSdkVersion.value + ".jar" },

    appengineBinPath := appengineSdkPath.value / "bin",
    appengineLibPath := appengineSdkPath.value / "lib",
    appengineLibUserPath := appengineLibPath.value / "user",
    appengineLibImplPath := appengineLibPath.value / "impl",
    appengineApiJarPath := appengineLibUserPath.value / appengineApiJarName.value,
    appengineApiToolsPath := appengineLibPath.value / appengineApiToolsJar.value,
    appengineAppcfgName := "appcfg" + AppEngine.osBatchSuffix,
    appengineAppcfgPath := appengineBinPath.value / appengineAppcfgName.value,
    appengineOverridePath := appengineLibPath.value / "override",
    appengineOverridesJarPath := appengineOverridePath.value / "appengine-dev-jdk-overrides.jar",
    appengineAgentJarPath := appengineLibPath.value / "agent" / "appengine-agent.jar",
    appengineEmptyFile := file(""),
    appengineTemporaryWarPath := target.value / "webapp"
  )

  lazy val baseAppengineDataNucleusSettings: Seq[Def.Setting[_]] = Seq(
    packageWar := packageWar.dependsOn(appengineEnhance).value,
    appengineClasspath := {
      val appengineORMJars = (appengineLibPath.value / "orm" * "*.jar").get
      appengineClasspath.value ++ appengineORMJars.classpath
    },
    appengineEnhance := {
      val r: ScalaRun = (runner in Runtime).value
      val main: String = (mainClass in appengineEnhance).value.get
      val files: Seq[File] = (exportedProducts in Runtime).value flatMap { dir =>
        (dir.data ** "*.class").get ++ (dir.data ** "*.jdo").get
      }
      val args: Seq[String] = (scalacOptions in appengineEnhance).value ++ (files map {_.toString})
      r.run(main, (fullClasspath in appengineEnhance).value map {_.data}, args, streams.value.log)
    },
    appengineEnhanceCheck := {
      val r: ScalaRun = (runner in Runtime).value
      val main: String = (mainClass in appengineEnhance).value.get
      val files: Seq[File] = (exportedProducts in Runtime).value flatMap { dir =>
        (dir.data ** "*.class").get ++ (dir.data ** "*.jdo").get
      }
      val args: Seq[String] = (scalacOptions in appengineEnhance).value ++ Seq("-checkonly") ++ (files map {_.toString})
      r.run(main, (fullClasspath in appengineEnhance).value map {_.data}, args, streams.value.log)
    },
    mainClass in appengineEnhance := Some("org.datanucleus.enhancer.DataNucleusEnhancer"),
    fullClasspath in appengineEnhance := {
      val appengineORMEnhancerJars = (appengineLibPath.value / "tools" / "orm" * "datanucleus-enhancer-*.jar").get ++
        (appengineLibPath.value / "tools" / "orm" * "asm-*.jar").get
      (Seq(appengineApiToolsPath.value) ++ appengineORMEnhancerJars).classpath ++ (fullClasspath in Compile).value
    },
    // http://www.datanucleus.org/products/accessplatform_2_2/enhancer.html
    scalacOptions in appengineEnhance := ((logLevel in appengineEnhance) match {
      case Level.Debug => Seq("-v")
      case _           => Seq()
    } ) ++ Seq("-api", (appenginePersistenceApi in appengineEnhance).value),
    logLevel in appengineEnhance := Level.Debug,
    appenginePersistenceApi in appengineEnhance := "JDO"
  )

  lazy val webSettings = projectSettings

  override lazy val projectSettings: Seq[Def.Setting[_]] = WebPlugin.webSettings ++
    inConfig(Compile)(revolver.RevolverPlugin.settings ++ baseAppengineSettings) ++
    inConfig(Test)(Seq(
      unmanagedClasspath ++= appengineClasspath.value,
      appengineClasspath := {
        val impljars = ((appengineLibImplPath in Compile).value * "*.jar").get
        val testingjars = ((appengineLibPath in Compile).value / "testing" * "*.jar").get
        (appengineClasspath in Compile).value ++ Attributed.blankSeq(impljars ++ testingjars)
      }
    )) ++
    Seq(
      watchSources ++= ((webappResources in Compile).value ** "*").get)
}
