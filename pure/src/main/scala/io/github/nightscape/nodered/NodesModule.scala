package io.github.nightscape.nodered

import scala.scalajs.js
import typings.nodeRed.mod.{NodeAPI, NodeAPISettingsWithData}
import typings.nodeRedRegistry.mod.NodeInitializer

trait NodesModule {
  def nodeInitializers: Seq[NodeInitializer[NodeAPISettingsWithData]]

  def main(args: Array[String]): Unit =
    js.Dynamic.global.module.exports = (nodeRed: NodeAPI[NodeAPISettingsWithData]) =>
      nodeInitializers.foreach(_.apply(nodeRed))
}
