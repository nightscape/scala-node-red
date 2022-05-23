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

import scala.scalajs.js
import typings.nodeRed.mod.Node
import typings.nodeRedNodeTestHelper.mod
import typings.nodeRedNodeTestHelper.mod.TestCredentials
import typings.nodeRedRegistry.nodeRedRegistryStrings.NodeCredentials
import _root_.zio.*

trait RunningNodeRedServer {
  def load(testNode: mod.TestNodeInitializer, testFlows: mod.TestFlows): URIO[Scope, Unit]
  def unload: Task[Unit]
  def getNode(id: String): UIO[Node[TestCredentials[Any]]]
}
trait NodeRedTestHelper {
  def testHelper: mod.NodeTestHelper
  def init(nodeRedRuntime: String): Task[Unit]
  def startServer: Task[Unit]
  def stopServer: Task[Unit]
}

case class NodeRedCredentials(credentials: TestCredentials[Any])
object NodeRedTestHelper {
  def loadNodesAndFlow(
    testNode: mod.TestNodeInitializer,
    testFlows: mod.TestFlows
  ): URIO[RunningNodeRedServer & Scope, Unit] =
    for {
      server <- ZIO.service[RunningNodeRedServer]
      _ <- server.load(testNode, testFlows)
    } yield ()
  def getNode(id: String): URIO[RunningNodeRedServer, Node[TestCredentials[Any]]] =
    ZIO.service[RunningNodeRedServer].flatMap(_.getNode(id))

  private val credentials: TestCredentials[Any] = NodeCredentials.asInstanceOf[TestCredentials[Any]]
  type NodeTestHelper = mod.NodeTestHelper
  val testServer = ZLayer.fromFunction { (t: NodeRedTestHelper) =>
    val th = t.testHelper
    new RunningNodeRedServer {
      def unload: Task[Unit] = ZIO.fromPromiseJS(th.unload())
      def load(testNode: mod.TestNodeInitializer, testFlows: mod.TestFlows): URIO[Scope, Unit] =
        ZIO
          .acquireRelease(
            ZIO.async[Any, Nothing, Unit](cb => th.load(testNode, testFlows, credentials, () => cb(ZIO.unit)))
          )(_ => unload.ignoreLogged)
          .ignoreLogged
      def getNode(id: String): UIO[Node[TestCredentials[Any]]] =
        ZIO.succeed(th.getNode(id).asInstanceOf[Node[TestCredentials[Any]]])
    }
  }
  val scalaTestHelper: ZLayer[Any, Throwable, NodeRedTestHelper] =
    ZLayer.scoped(for {
      l <- ZIO.attempt(mod.`_to`)
      rnt = new NodeRedTestHelper {
        def testHelper: mod.NodeTestHelper = l
        def init(nodeRedRuntime: String): Task[Unit] = ZIO.attempt(l.init(nodeRedRuntime))
        def startServer: Task[Unit] = ZIO.async(cb => l.startServer(done = () => cb(ZIO.unit)))
        def stopServer: UIO[Unit] = ZIO.async(cb => l.stopServer(done = () => cb(ZIO.unit)))
      }
      _ <- rnt.init(js.Dynamic.global.require.resolve("node-red").toString)
      e <- ZIO.acquireRelease(rnt.startServer.as(rnt))(r => r.stopServer)
    } yield e)
}
