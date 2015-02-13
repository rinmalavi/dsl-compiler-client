import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

object ClientApi extends Build {

  lazy val scalaSettings = Seq(
    javacOptions in doc := Seq(
      "-encoding", "UTF-8"

    )
    , javacOptions := Seq(
      "-deprecation"
      , "-Xlint"
    ) ++ (javacOptions in doc).value
    , crossScalaVersions := Seq("2.11.2")
    , scalaVersion := crossScalaVersions.value.head
    , scalacOptions := Seq(
      "-unchecked"
      , "-deprecation"
      , "-optimise"
      , "-encoding", "UTF-8"
      , "-Xcheckinit"
      , "-Yclosure-elim"
      , "-Ydead-code"
      , "-Yinline"
      , "-Xmax-classfile-name", "72"
      , "-Yrepl-sync"
      , "-Xlint"
      , "-Xverify"
      //  , "-Ywarn-all"
      , "-feature"
      , "-language:postfixOps"
      , "-language:implicitConversions"
      , "-language:existentials"
      , "-Yinline-warnings"
    )
    , unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value)
    , unmanagedSourceDirectories in Test := Seq((scalaSource in Test).value)
  )

  lazy val javaSettings = scalaSettings ++ Seq(
    autoScalaLibrary := false
    , crossPaths := false
    , unmanagedSourceDirectories in Compile := Seq((javaSource in Compile).value)
    , unmanagedSourceDirectories in Test := Seq((javaSource in Test).value)
  )


  protected def clientApiProject(id: String) = Project(
    id.toLowerCase
    , file(id.toLowerCase)
    , settings = javaSettings ++ Seq(
      name := "DSL-Compiler-Client-" + id
      , initialCommands := "import com.dslplatform.compiler.client._"
    )
  )

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.7"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.7"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"

  val jodaTime = "joda-time" % "joda-time" % "2.3"
  val jodaConvert = "org.joda" % "joda-convert" % "1.5"

  val jUnit = "junit" % "junit" % "4.11"
  val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3"

  val postgresql = "org.postgresql" % "postgresql" % "9.3-1101-jdbc41"

  val commonsCodec = "commons-codec" % "commons-codec" % "1.9"
  val commonsIo = "commons-io" % "commons-io" % "2.4"

  val config = "com.typesafe" % "config" % "1.2.1"

  val jGit = "org.eclipse.jgit" % "org.eclipse.jgit" % "3.4.0.201405281120-rc2"

  lazy val util = clientApiProject("Util") settings (
    libraryDependencies ++= Seq(slf4j, commonsIo)
    )

  lazy val params = clientApiProject("Params") dependsOn(util)

  lazy val cmdLineClient = clientApiProject("CmdLineClient") dependsOn(api % "test->test;compile->compile", params) settings (
    assemblySettings: _*) settings(
    libraryDependencies += logback /*slf4jSimple*/ ,
    artifact in(Compile, assembly) ~= (_.copy(`classifier` = Some("assembly")))
    , test in assembly := {}
    , mainClass in assembly := Some("com.dslplatform.compiler.client.cmdline.Main")
    , jarName in assembly := s"dsl-clc-${System.currentTimeMillis() / 100000}.jar"
    , test in assembly := {})

  lazy val core = clientApiProject("Core") settings(
    libraryDependencies ++= Seq(jodaTime
      , postgresql //% "provided"
      , slf4j
      , commonsCodec
      , logback % "test"
      , jUnit % "test"),
    unmanagedSourceDirectories in Compile := Seq(
      sourceDirectory.value / "main" / "java"
    )
    , unmanagedResourceDirectories in Compile := Seq(
    sourceDirectory.value / "main" / "resources"
  )
    , unmanagedResourceDirectories in Test := Seq(sourceDirectory.value / "test" / "resources")
    ) dependsOn (util)

  lazy val api = clientApiProject("Api") settings (
    libraryDependencies ++= Seq(
      commonsIo
      , jGit
      , logback % "test"
      , postgresql % "test"
      , jUnit % "test"
      , hamcrest % "test")
    ) dependsOn(core % "test->test;compile->compile", params)

  lazy val dslCompilerSBT = Project(
    "sbt"
    , file("sbt")
  ) settings (ScriptedPlugin.scriptedSettings: _*) settings(
    name := "DSL-Compiler-Client-SBT"
    , libraryDependencies ++= Seq(postgresql, config, logback, jUnit % "test")
    , ScriptedPlugin.scriptedLaunchOpts := {
    ScriptedPlugin.scriptedLaunchOpts.value ++
      Seq("-Xmx1024M"
        , "-XX:MaxPermSize=256M"
        , "-Dplugin.version=" + version.value
      )
  }
    , unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value)
    , unmanagedSourceDirectories in Test <++= unmanagedSourceDirectories in Test in core
    , unmanagedResourceDirectories in Test <++= unmanagedResourceDirectories in Test in core
    , publishArtifact in(Test, packageBin) := true
    , publishLocal <<= publishLocal dependsOn(publishLocal in core, publishLocal in params, publishLocal in api, publishLocal in util)
    , sbtPlugin := true
    ) dependsOn (api)
}
