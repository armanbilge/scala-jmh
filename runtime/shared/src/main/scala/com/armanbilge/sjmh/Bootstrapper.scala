/*
 * Copyright 2022 Arman Bilge
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

package com.armanbilge.sjmh

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

@EnableReflectiveInstantiation
trait Bootstrapper {

  def params(): Array[ParamMetadata]
  def setParam(name: String, value: String): Unit

  def benchmarks(): Array[BenchmarkMetadata]
  def invokeBenchmark(instance: AnyRef, name: String): Function0[Future[Unit]]

  def newInstance(): AnyRef
}

final class BenchmarkMetadata(
    val name: String,
)

final class ParamMetadata(
    val name: String,
    val values: Array[String],
)
