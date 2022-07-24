/*
 * Copyright 2022 Martin Mauch (@nightscape)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.nightscape

import scala.scalajs.js
import js.*
import typings.nodeRed.mod.{Node, NodeAPI, NodeAPISettingsWithData, NodeDef, NodeMessage, NodeMessageInFlow}
import typings.nodeRedRegistry.mod.{NodeConstructor, NodeInitializer}
import typings.nodeRedRegistry.nodeRedRegistryStrings.input

package object nodered {
  def createScalaJsFunctionNode[C <: NodeDef, I, O](
    nodeType: String,
    f: C => I => O
  ): NodeInitializer[NodeAPISettingsWithData] = {
    val creator: NodeInitializer[NodeAPISettingsWithData] = { (red: NodeAPI[NodeAPISettingsWithData]) =>
      class ScalaJsFunctionNode(config: C) extends js.Object {
        red.nodes.createNode(this.asInstanceOf[Node[js.Object]], config)
        private val fn = f(config)
        this
          .asInstanceOf[Node[Any]]
          .on_input(
            input,
            listener = (
              msg: NodeMessageInFlow,
              send: js.Function1[NodeMessage | (js.Array[NodeMessage | js.Array[NodeMessage] | Null]), Unit],
              _: js.Function1[ /* err */ js.UndefOr[js.Error], Unit]
            ) => {
              val result = fn(msg.payload.asInstanceOf[I])
              msg.payload = result.asInstanceOf[js.UndefOr[Any]]
              send(msg)
            }
          )
      }
      red.nodes.registerType(
        nodeType,
        js.constructorOf[ScalaJsFunctionNode].asInstanceOf[NodeConstructor[ScalaJsFunctionNode, NodeDef, Any]]
      )
    }
    creator

  }
}
