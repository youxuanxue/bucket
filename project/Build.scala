import sbt.Keys._
import sbt.{FileFilter, ModuleID, TaskKey, _}

import scala.collection.immutable
import scalariform.formatter.preferences.FormattingPreferences

/**
  * @author xuejiao
  */
object Build extends sbt.Build {
  lazy val root: Project = Project("bucket", file("."))
    .aggregate(bucket_base, bucket_store, bucket_inbound, bucket_pubsub)
    .settings(basicSettings: _*)
    .settings(Formatting.buildFileSettings: _*)
    .settings(noPublishing: _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)

  lazy val bucket_base: Project = Project("bucket-base", file("bucket-base"))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Dependencies.bucket_base)
    .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(Packaging.settings)

  lazy val bucket_store: Project = Project("bucket-store", file("bucket-store"))
    .dependsOn(bucket_base)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Dependencies.bucket_store)
    .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(Packaging.settings)

  lazy val bucket_inbound: Project = Project("bucket-inbound", file("bucket-inbound"))
    .dependsOn(bucket_store)
    .settings(basicSettings: _*)
    .settings(noPublishing: _*)
    .settings(libraryDependencies ++= Dependencies.bucket_inbound)
    .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(Packaging.settings)

  lazy val bucket_pubsub: Project = Project("bucket-pubsub", file("bucket-pubsub"))
    .dependsOn(bucket_base)
    .settings(basicSettings: _*)
    .settings(noPublishing: _*)
    .settings(libraryDependencies ++= Dependencies.bucket_pubsub)
    .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(Packaging.settings)

  lazy val basicSettings: Seq[Def.Setting[_]] = Defaults.coreDefaultSettings ++ Seq(
    organization := "com.yiyiyi.card",
    version := "0.0.1",
    resolvers ++= Seq(
      "Local Maven" at Path.userHome.asURL + ".m2/repository",
      "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    ),
    fork in run := true,
    fork in Test := true,
    parallelExecution in Test := false,
    scalaVersion := "2.12.1",
    scalacOptions ++= Seq("-unchecked", "-deprecation")
  ) ++ Formatting.settings

  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := (),
    publishTo := None
  )

}


object Dependencies {

  private val AKKA_VERSION = "2.5.0-RC1"
  private val AKKA_HTTP_VERSION = "10.0.5"
  private val SLF4J_VERSION = "1.7.24"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-remote" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-cluster-sharding" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-cluster-tools" % AKKA_VERSION,
    //"com.typesafe.akka" %% "akka-contrib" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-persistence" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-stream" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-slf4j" % AKKA_VERSION,
    "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % Test,
    "com.typesafe.akka" %% "akka-multi-node-testkit" % AKKA_VERSION % Test,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.50-M2" % Runtime,
    "org.iq80.leveldb" % "leveldb" % "0.9" % Runtime,
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" % Runtime
  )

  val akka_http = Seq(
    "com.typesafe.akka" %% "akka-http-core" % AKKA_HTTP_VERSION,
    "com.typesafe.akka" %% "akka-http" % AKKA_HTTP_VERSION,
    "com.typesafe.akka" %% "akka-http-spray-json" % AKKA_HTTP_VERSION
  )

  val jackson = Seq("com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.4")

  val log = Seq(
    "org.slf4j" % "slf4j-api" % SLF4J_VERSION,
    "org.slf4j" % "jcl-over-slf4j" % SLF4J_VERSION,
    "org.slf4j" % "log4j-over-slf4j" % SLF4J_VERSION,
    "ch.qos.logback" % "logback-classic" % "1.2.1"
  )

  val test = Seq(
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  )

  private val basic: Seq[ModuleID] = jackson ++ log ++ test

  val bucket_base: Seq[ModuleID] = basic ++ akka_http

  val bucket_store: Seq[ModuleID] = basic

  val bucket_inbound: Seq[ModuleID] = bucket_base ++ akka

  val bucket_pubsub: Seq[ModuleID] = bucket_base ++ akka

}

object Formatting {

  import com.typesafe.sbt.SbtScalariform
  import com.typesafe.sbt.SbtScalariform.ScalariformKeys
  import ScalariformKeys._

  val BuildConfig: Configuration = config("build") extend Compile
  val BuildSbtConfig: Configuration = config("buildsbt") extend Compile

  val formattingPreferences: FormattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, false)
      .setPreference(AlignParameters, false)
      .setPreference(AlignSingleLineCaseStatements, false)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(SpacesAroundMultiImports, true)
      .setPreference(IndentSpaces, 2)
      .setPreference(CompactControlReadability, true)
  }

  // invoke: build:scalariformFormat
  val buildFileSettings: Seq[Setting[_]] =
    SbtScalariform.noConfigScalariformSettings ++
    inConfig(BuildConfig)(SbtScalariform.configScalariformSettings) ++
    inConfig(BuildSbtConfig)(SbtScalariform.configScalariformSettings) ++
    Seq(scalaSource in BuildConfig := baseDirectory.value / "project",
      scalaSource in BuildSbtConfig := baseDirectory.value,
      includeFilter in (BuildConfig, format) := ("*.scala": FileFilter),
      includeFilter in (BuildSbtConfig, format) := ("*.sbt": FileFilter),
      format in BuildConfig := {
        val x = (format in BuildSbtConfig).value
        (format in BuildConfig).value
      },
      ScalariformKeys.preferences in BuildConfig := formattingPreferences,
      ScalariformKeys.preferences in BuildSbtConfig := formattingPreferences)

  val settings: immutable.Seq[Setting[_]] = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences)
}

object Packaging {
  // Good example https://github.com/typesafehub/activator/blob/master/project/Packaging.scala
  import com.typesafe.sbt.SbtNativePackager._

  // This is dirty, but play has stolen our keys, and we must mimc them here.
  val stage: TaskKey[File] = TaskKey[File]("stage")
  val dist: TaskKey[File] = TaskKey[File]("dist")

  val settings: Seq[Setting[_]] = packageArchetype.java_application ++ Seq(
    name in Universal := s"${name.value}",
    dist <<= packageBin in Universal
  )
}