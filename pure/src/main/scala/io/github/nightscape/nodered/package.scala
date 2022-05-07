package io.github.nightscape

import scala.scalajs.js

import typings.nodeRed.mod.{Node, NodeAPI, NodeAPISettingsWithData, NodeDef, NodeMessage, NodeMessageInFlow, NodeRedApp}
import typings.nodeRedRegistry.mod.NodeConstructor
import typings.nodeRedRegistry.nodeRedRegistryStrings.input

package object nodered {
  def createScalaJsFunctionNode[C <: NodeDef, I, O](nodeType: String, f: C => I => O) =
    val creator: js.Function1[NodeAPI[NodeAPISettingsWithData], Unit] = (red: NodeAPI[NodeAPISettingsWithData]) =>
      class ScalaJsFunctionNode(config: C) extends js.Object:
        red.nodes.createNode(this.asInstanceOf[Node[js.Object]], config)
        val fn = f(config)
        this
          .asInstanceOf[Node[Any]]
          .on_input(
            input,
            listener = (
              msg: NodeMessageInFlow,
              send: js.Function1[NodeMessage | (js.Array[NodeMessage | js.Array[NodeMessage] | Null]), Unit],
              done: js.Function1[ /* err */ js.UndefOr[js.Error], Unit]
            ) => {
              val result = fn(msg.payload.asInstanceOf[I])
              msg.payload = result
              send(msg)
            }
          )
      red.nodes.registerType(
        nodeType,
        js.constructorOf[ScalaJsFunctionNode].asInstanceOf[NodeConstructor[ScalaJsFunctionNode, NodeDef, Any]]
      )
    creator

}
