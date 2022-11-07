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

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Symbols.ClassSymbol
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Types.TypeRef

import scala.annotation.threadUnsafe

object JmhDefinitions {
  private var cached: JmhDefinitions = _
  private var lastContext: Context = _
  def defnJmh(using ctx: Context): JmhDefinitions = {
    if (lastContext != ctx) {
      cached = JmhDefinitions()
      lastContext = ctx
    }
    cached
  }
}

final class JmhDefinitions()(using ctx: Context) {

  @threadUnsafe lazy val BenchmarkAnnotType: TypeRef =
    requiredClassRef("org.openjdk.jmh.annotations.Benchmark")
  def BenchmarkAnnotClass(using Context): ClassSymbol = BenchmarkAnnotType.symbol.asClass

  @threadUnsafe lazy val ParamAnnotType: TypeRef =
    requiredClassRef("org.openjdk.jmh.annotations.Param")
  def ParamAnnotClass(using Context): ClassSymbol = ParamAnnotType.symbol.asClass

}