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

import io.github.nightscape.nodered.zio.inputStream

import NodeRedTestHelper._
import typings.nodeRed.mod.{Node, NodeDef, NodeMessage, NodeMessageInFlow, NodeRedApp}
import typings.nodeRedNodeTestHelper.anon
import typings.nodeRedNodeTestHelper.mod, mod._
import typings.nodeRedRegistry.mod.NodeMessage
import typings.nodeRedRegistry.nodeRedRegistryStrings.{input, NodeCredentials}

import scala.scalajs.js
import _root_.zio.*
import _root_.zio.test.Assertion.*
import _root_.zio.test.*
import _root_.zio.test.TestAspect.*

object ScalaJsFunctionNodeSpec extends ZIOSpecDefault:

  val nodeId = "node-id"
  val outNodeId = "out-node-id"
  val nodeName = "generator-name"
  val nodeType = "lower-case"

  def createFlow(length: Option[Int] = None, setTo: Option[String] = None): js.Array[anon.TestFlowsItemNodeDef] =
    js.Array(
      js
        .Dictionary[Any](
          "id" -> nodeId,
          "type" -> nodeType,
          "name" -> nodeName,
          "wires" -> js.Array(js.Array(outNodeId)),
          "length" -> length.getOrElse(10),
          "setTo" -> setTo.orNull
        ),
      js.Dictionary[Any]("id" -> outNodeId, "type" -> "helper")
    ).map(_.asInstanceOf[anon.TestFlowsItemNodeDef])
  val functionNode = createScalaJsFunctionNode[NodeDef, String, String](nodeType, _ => s => s.toLowerCase)
  override def spec = suite("Node-RED Scala-JS function")(
    test("should be loaded") {
      ZIO.scoped(for {
        _ <- loadNodesAndFlow(js.Array(functionNode), createFlow())
        node <- getNode(nodeId)
        outNode <- getNode(outNodeId)
      } yield assertTrue(node.name == nodeName && node.id == nodeId && outNode.id == outNodeId))
    },
    test("should be applied to messages") {
      ZIO.scoped(for {
        _ <- loadNodesAndFlow(js.Array(functionNode), createFlow(setTo = Some("payload.value")))
        node <- getNode(nodeId)
        outNode <- getNode(outNodeId)
        outMsgFork <- outNode.inputStream[String].take(1).runCollect.fork
        _ = node.receive(NodeMessage().setPayload("ABC"))
        payload <- outMsgFork.join
      } yield assertTrue(payload.headOption == Some("abc")))
    } @@ timeout(1.seconds)
  )
    .provideSomeLayer[NodeRedTestHelper & _root_.zio.test.Live](NodeRedTestHelper.testServer)
    .provideSomeLayerShared(NodeRedTestHelper.scalaTestHelper) @@ TestAspect.sequential
