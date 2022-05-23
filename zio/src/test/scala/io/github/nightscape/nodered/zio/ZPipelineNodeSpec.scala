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

package io.github.nightscape.nodered.zio

import _root_.zio.*
import _root_.zio.*
import _root_.zio.stream.*
import _root_.zio.test.*
import _root_.zio.test.Assertion.*
import _root_.zio.test.TestAspect.*
import io.github.nightscape.nodered.NodeRedTestHelper
import NodeRedTestHelper.*
import typings.nodeRed.mod.*
import typings.nodeRedNodeTestHelper.anon.TestFlowsItemNodeDef
import typings.nodeRedRegistry.mod.NodeMessage
import typings.nodeRedRegistry.nodeRedRegistryStrings.{input, NodeCredentials}

import scala.scalajs.js

object ZPipelineNodeSpec extends ZIOSpecDefault {
  println("In Spec")
  val nodeId = "node-id"
  val outNodeId = "out-node-id"
  val nodeName = "generator-name"
  val nodeType = "lower-case"

  def createFlow(): js.Array[TestFlowsItemNodeDef] =
    js.Array(
      js
        .Dictionary[Any](
          "id" -> nodeId,
          "type" -> nodeType,
          "name" -> nodeName,
          "wires" -> js.Array(js.Array(outNodeId))
        ),
      js.Dictionary[Any]("id" -> outNodeId, "type" -> "helper")
    ).map(_.asInstanceOf[TestFlowsItemNodeDef])
  val functionNode = createZPipelineNode[NodeDef, String, String, Throwable](nodeType, ZPipeline.splitOn(","))
  override def spec = suite("Node-RED ZIO function")(
    test("should be loaded") {
      ZIO.scoped(for {
        _ <- loadNodesAndFlow(
          js.Array(functionNode).asInstanceOf[typings.nodeRedNodeTestHelper.mod.TestNodeInitializer],
          createFlow()
        )
        node <- getNode(nodeId)
        outNode <- getNode(outNodeId)
      } yield assertTrue(node.name == nodeName && node.id == nodeId && outNode.id == outNodeId))
    },
    test("should be applied to messages") {
      ZIO.scoped(for {
        _ <- loadNodesAndFlow(
          js.Array(functionNode).asInstanceOf[typings.nodeRedNodeTestHelper.mod.TestNodeInitializer],
          createFlow()
        )
        node <- getNode(nodeId)
        outNode <- getNode(outNodeId)
        outMsgFork <- outNode.inputStream[String].take(3).runCollect.fork
        _ = node.receive(NodeMessage().setPayload("a,b,c,"))
        payload <- outMsgFork.join
      } yield assertTrue(payload == Chunk("a", "b", "c")))
    } @@ timeout(1.seconds)
  )
    .provideSomeLayer[NodeRedTestHelper & _root_.zio.test.Live](NodeRedTestHelper.testServer)
    .provideSomeLayerShared[zio.test.Live](NodeRedTestHelper.scalaTestHelper) @@ TestAspect.sequential
}
