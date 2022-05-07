import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val zioVersion = "2.0.0-RC6"
  val zioCore = Def.setting("dev.zio" %%% "zio" % zioVersion)
  val zioStreams = Def.setting("dev.zio" %%% "zio-streams" % zioVersion)
  val zioTest = Def.setting("dev.zio" %%% "zio-test" % zioVersion)
  val zioTestSbt = Def.setting("dev.zio" %%% "zio-test-sbt" % zioVersion)

  object Npm {
    val nodeRedTestHelper = Seq("node-red-node-test-helper" -> "0.2.7", "@types/node-red-node-test-helper" -> "0.2.2")
  }
}
