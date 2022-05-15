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

package io.github.nightscape.nodered

import typings.nodeRed.mod.{Node, NodeAPI, NodeAPISettingsWithData, NodeDef, NodeMessageInFlow, NodeRedApp}
import typings.nodeRedRegistry.mod
import typings.nodeRedRegistry.nodeRedRegistryStrings.input

import scala.scalajs.js
import _root_.zio.*
import _root_.zio.stream.*

package object zio {
  extension [TCreds](node: Node[TCreds])
    def inputStream[I] = ZStream
      .async[Any, Throwable, I] { cb =>
        node
          .on_input(
            input,
            listener = (msg: NodeMessageInFlow, _, _) => cb(ZIO.succeed(Chunk(msg.payload.asInstanceOf[I])))
          )
      }

  def createZioNode[C <: NodeDef : Tag, I, O, E](nodeName: String, f: ZPipeline[C, E, I, O]) =
    val creator: js.Function1[NodeAPI[NodeAPISettingsWithData], Unit] = (red: NodeAPI[NodeAPISettingsWithData]) =>
      class ZioNode(config: C) extends js.Object:
        red.nodes.asInstanceOf[js.Dynamic].createNode(this, config)
        val resultStream = f(this.asInstanceOf[Node[Any]].inputStream[I])
        val sending = resultStream.foreach(payload =>
          ZIO.attempt(this.asInstanceOf[Node[Any]].send(mod.NodeMessage().setPayload(payload)))
        )
        _root_.zio.Runtime.default.unsafeRunAsync(sending.provideLayer(ZLayer.succeed(config)))

      red.nodes.asInstanceOf[js.Dynamic].registerType(nodeName, js.constructorOf[ZioNode])
    creator
}
