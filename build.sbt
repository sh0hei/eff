import org.scalajs.jsenv.nodejs._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val specs2Version      = "4.10.6"
lazy val twitterUtilVersion = "21.2.0"
lazy val catbirdVersion     = "20.10.0"
lazy val doobieVersion      = "0.10.0"

enablePlugins(GhpagesPlugin)
enablePlugins(SitePlugin)
enablePlugins(BuildInfoPlugin)

def hash() = sys.process.Process("git rev-parse HEAD").lineStream_!.head

moduleName := "root"
effSettings
noPublishSettings
commonJvmSettings
libraryDependencies ++= scalameter

dependsOn(
  coreJVM % "test->test;compile->compile",
  doobie,
  catsEffectJVM,
  macros,
  monixJVM,
  scalazJVM,
  twitter,
)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform).in(file("."))
  .settings(moduleName := "eff")
  .jsSettings(commonJsSettings)
  .jvmSettings(commonJvmSettings)
  .jvmSettings(notesSettings)
  .nativeSettings(commonNativeSettings)
  .settings(effSettings)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val doobie = project
  .settings(moduleName := "eff-doobie")
  .dependsOn(coreJVM)
  .settings(libraryDependencies ++= doobieJvm)
  .settings(effSettings ++ commonJvmSettings)

lazy val catsEffect = crossProject(JSPlatform, JVMPlatform).in(file("cats"))
  .settings(moduleName := "eff-cats-effect")
  .dependsOn(core)
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "2.3.1",
  )
  .jsSettings(commonJsSettings)
  .jvmSettings(commonJvmSettings)
  .settings(effSettings)

lazy val catsEffectJVM = catsEffect.jvm
lazy val catsEffectJS = catsEffect.js

lazy val macros = project.in(file("macros"))
  .settings(moduleName := "eff-macros")
  .dependsOn(coreJVM)
  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
  .settings(
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch))
        case _ =>
          // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
          // https://github.com/scala/scala/pull/6606
          Nil
      }
    }
  )
  .settings(commonJvmSettings)
  .settings(effSettings)

lazy val monix = crossProject(JSPlatform, JVMPlatform).in(file("monix"))
  .settings(moduleName := "eff-monix")
  .dependsOn(core)
  .settings(
    libraryDependencies += "io.monix" %%% "monix" % "3.3.0",
  )
  .settings(effSettings)
  .jvmSettings(commonJvmSettings)
  .jsSettings(commonJsSettings)

lazy val monixJVM = monix.jvm
lazy val monixJS =  monix.js

lazy val scalaz = crossProject(JSPlatform, JVMPlatform, NativePlatform).in(file("scalaz"))
  .settings(moduleName := "eff-scalaz")
  .dependsOn(core)
  .settings(
    libraryDependencies += "org.scalaz" %%% "scalaz-core" % "7.3.3",
  )
  .settings(effSettings)
  .jvmSettings(commonJvmSettings)
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)

lazy val scalazJVM = scalaz.jvm
lazy val scalazJS = scalaz.js

lazy val twitter = project
  .settings(moduleName := "eff-twitter")
  .dependsOn(coreJVM)
  .settings(libraryDependencies ++= twitterUtilCore ++ catbird)
  .settings(effSettings ++ commonJvmSettings)

lazy val scoverageSettings = Seq(
  coverageMinimum := 60,
  coverageFailOnMinimum := false,
  coverageHighlighting := true,
  coverageExcludedPackages := "org\\.atnos\\.eff\\.bench\\..*"
)

lazy val buildSettings = Seq(
  organization := "org.atnos",
  scalaVersion := "2.12.13",
  crossScalaVersions := Seq(scalaVersion.value, "2.13.4")
)

lazy val commonSettings = Seq(
  libraryDependencies ++= specs2,
  libraryDependencies += "org.typelevel" %%% "cats-core" % "2.4.1",
  scalacOptions ++= commonScalacOptions.value,
  scalacOptions in (Compile, doc) ++= {
    Seq(
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/atnos-org/eff/tree/${hash()}€{FILE_PATH}.scala"
    )
  },
  scalacOptions in (Compile, doc) := (scalacOptions in (Compile, doc)).value.filter(_ != "-Xfatal-warnings"),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Nil
      case _ =>
        Seq(
          compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)
        )
    }
  }
) ++ warnUnusedImport ++ prompt

lazy val commonJsSettings = Seq(
  scalaJSStage in Global := FastOptStage,
  parallelExecution := false,
  scalacOptions += {
    val a = (baseDirectory in LocalRootProject).value.toURI.toString
    val g = "https://raw.githubusercontent.com/atnos-org/eff/" + hash()
    s"-P:scalajs:mapSourceURI:$a->$g/"
  },
  jsEnv := new NodeJSEnv()
) ++ disableTests

def disableTests = Seq(
  test := {},
  testQuick := {},
  testOnly := {}
)

lazy val commonJvmSettings = Seq(
  fork in Test := true,
  cancelable in Global := true,
  (scalacOptions in Test) ~= (_.filterNot(_ == "-Xfatal-warnings")),
) ++ Seq(scalacOptions in Test ++= Seq("-Yrangepos"))

lazy val commonNativeSettings = Def.settings(
  libraryDependencies --= specs2,
  loadedTestFrameworks := Map.empty,
  Test / sources := Nil,
  test := {},
  Test / testOnly := {},
)

lazy val effSettings =
  buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings

lazy val publishSettings =
  Seq(
  homepage := Some(url("https://github.com/atnos-org/eff")),
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  scmInfo := Some(ScmInfo(url("https://github.com/atnos-org/eff"), "scm:git:git@github.com:atnos-org/eff.git")),
  autoAPIMappings := true,
  apiURL := Some(url("http://atnos.org/eff/api/")),
  pomExtra := (
    <developers>
      <developer>
        <id>etorreborre</id>
        <name>Eric Torreborre</name>
        <url>https://github.com/etorreborre/</url>
      </developer>
    </developers>
    )
) ++ credentialSettings ++ sharedPublishSettings


lazy val noPublishSettings = Seq(
  publish := (()),
  publishLocal := (()),
  publishArtifact := false
)

lazy val commonScalacOptions = Def.setting {
  Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Xlint",
    "-Xlint:-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ) ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v >= 13 =>
        Seq(
          "-Ymacro-annotations"
        )
      case Some((3, _)) =>
        Seq(
          "-Ykind-projector"
        )
      case _ =>
        Seq(
          "-Xfatal-warnings",
          "-Yno-adapted-args",
          "-Ypartial-unification",
          "-Xfuture"
        )
    }
  }
}

lazy val sharedPublishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  publishTo := sonatypePublishToBundle.value,
) ++ userGuideSettings ++ notesSettings ++ buildInfoSettings

lazy val userGuideSettings =
  Seq(
    ghpagesNoJekyll := false,
    siteSourceDirectory in makeSite := target.value / "specs2-reports" / "site",
    includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js",
    git.remoteRepo := "git@github.com:atnos-org/eff.git"
  )

lazy val notesSettings = Seq(
  ghreleaseRepoOrg := "atnos-org",
  ghreleaseRepoName := "eff",
  ghreleaseTitle := { tagName: TagName => s"eff ${tagName.replace("EFF-", "")}" },
  ghreleaseIsPrerelease := { tagName: TagName => false },
  ghreleaseNotes := { tagName: TagName =>
    // find the corresponding release notes
    val notesFilePath = s"notes/${tagName.replace("EFF-", "")}.markdown"
    try scala.io.Source.fromFile(notesFilePath).mkString
    catch { case t: Throwable => throw new Exception(s"$notesFilePath not found", t) }
  },
  // just upload the notes
  ghreleaseAssets := Seq()
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "org.atnos.eff"
)

lazy val warnUnusedImport = Seq(
  scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
)

lazy val credentialSettings = Seq(
  // For Travis CI - see http://www.cakesolutions.net/teamblogs/publishing-artefacts-to-oss-sonatype-nexus-using-sbt-and-travis-ci
  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

lazy val prompt = shellPrompt in ThisBuild := { state =>
  val name = Project.extract(state).currentRef.project
  (if (name == "eff") "" else name) + "> "
}

lazy val doobieJvm = Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-h2"   % doobieVersion % "test")

lazy val specs2 = Seq(
    "org.specs2" %% "specs2-core"
  , "org.specs2" %% "specs2-matcher-extra"
  , "org.specs2" %% "specs2-scalacheck"
  , "org.specs2" %% "specs2-html"
  , "org.specs2" %% "specs2-junit").map(_ % specs2Version % "test")

lazy val scalameter = Seq(
  "com.storm-enroute" %% "scalameter" % "0.19" % "test")

lazy val twitterUtilCore = Seq(
  "com.twitter" %% "util-core" % twitterUtilVersion
)

lazy val catbird = Seq(
  "io.catbird" %% "catbird-util" % catbirdVersion
)
