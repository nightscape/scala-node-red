import Dependencies._
import org.scalajs.jsenv.nodejs.NodeJSEnv.Config

ThisBuild / crossScalaVersions := Seq("2.13.8", "3.1.3")
ThisBuild / scalaVersion := crossScalaVersions.value.head
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.graalvm("17"))
ThisBuild / tlBaseVersion := "0.1" // your current series x.y
ThisBuild / tlFatalWarningsInCi := false

ThisBuild / organization := "io.github.nightscape"
ThisBuild / organizationName := "Martin Mauch (@nightscape)"
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("nightscape", "Martin Mauch"))
ThisBuild / tlSonatypeUseLegacyHost := false
Global / stRemoteCache := RemoteCache.S3Aws(bucket = "scala-node-red", region = "us-east-2", prefix = Some("st-cache"))

val commonSettings = Seq(
  useYarn := false,
  scalaJSLinkerConfig ~= {
    _.withModuleKind(ModuleKind.CommonJSModule)
  },
  stIgnore ++= List("express-serve-static-core"),
  scalacOptions ++= (if (scalaVersion.value.startsWith("2")) Seq("-Yrecursion", "1000", "-Xsource:3") else Seq()),
  Compile / npmDependencies ++= Seq("node-red" -> "2.2.2", "@types/node-red" -> "1.2.1"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)
lazy val testHelper = project
  .in(file("test-helper"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-node-red-test",
    libraryDependencies ++= Seq(zioCore.value, zioTest.value),
    Compile / npmDependencies ++= Npm.nodeRedTestHelper
  )

lazy val pure = project
  .in(file("pure"))
  .dependsOn(testHelper % "test->test", zio % "test->test")
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-node-red-pure",
    libraryDependencies ++= Seq(zioCore.value % "test", zioTest.value % "test", zioTestSbt.value % "test"),
    Test / parallelExecution := false,
    // Enable the next line to break before starting to run code in Node
    // Test / jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(
    //   org.scalajs.jsenv.nodejs.NodeJSEnv.Config().withArgs(List("--inspect-brk"))
    // ),
    Test / npmDependencies ++= Npm.nodeRedTestHelper
  )

lazy val zio = project
  .in(file("zio"))
  .dependsOn(testHelper % "test")
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-node-red-zio",
    libraryDependencies ++= Seq(zioCore.value, zioStreams.value, zioTest.value % "test", zioTestSbt.value % "test"),
    Test / npmDependencies ++= Npm.nodeRedTestHelper
  )
