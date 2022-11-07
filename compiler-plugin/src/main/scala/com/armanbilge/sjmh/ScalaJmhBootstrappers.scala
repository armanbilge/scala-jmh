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

import dotty.tools.dotc.ast.tpd.ClassDef
import dotty.tools.dotc.ast.tpd.PackageDef
import dotty.tools.dotc.ast.tpd.Tree
import dotty.tools.dotc.ast.tpd.TypeDef
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Flags.*
import dotty.tools.dotc.core.Scopes.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.sbt
import dotty.tools.dotc.transform

import scala.annotation.tailrec

class ScalaJmhBootstrappers extends PluginPhase {

  def phaseName: String = "scala-jmhBootstrappers"

  override val runsAfter = Set(transform.Mixin.name)

  final val bootstrapperSuffix = "$scalajmh$bootstrapper"

  override def transformPackageDef(tree: PackageDef)(using Context): Tree = {
    val jmhdefn = JmhDefinitions.defnJmh

    @tailrec
    def hasBenchmarks(sym: ClassSymbol): Boolean =
      sym.info.decls.exists { m =>
        m.is(Method) && m.hasAnnotation(jmhdefn.BenchmarkAnnotClass)
      } || sym.superClass.exists && hasBenchmarks(sym.superClass.asClass)

    def isBenchmarkClass(sym: Symbol): Boolean =
      sym.isClass &&
        !sym.isOneOf(ModuleClass | Abstract | Trait) &&
        hasBenchmarks(sym.asClass)

    val bootstrappers = tree.stats.collect {
      case clDef: TypeDef if isBenchmarkClass(clDef.symbol) =>
        genBootstrapper(clDef.symbol.asClass)
    }

    if (bootstrappers.isEmpty) tree
    else cpy.PackageDef(tree)(tree.pid, tree.stats ::: bootstrappers)
  }

  private def genBootstrapper(
      testClass: ClassSymbol,
  )(using Context): TypeDef = {
    val jmhdefn = JmhDefinitions.defnJmh

    val bootstrapperName =
      (testClass.name ++ bootstrapperSuffix).toTermName

    val owner = testClass.owner
    val moduleSym = newCompleteModuleSymbol(
      owner,
      bootstrapperName,
      Synthetic,
      Synthetic,
      List(defn.ObjectType, jmhdefn.BootstrapperType),
      newScope,
      coord = testClass.span,
      assocFile = testClass.assocFile,
    ).entered
    val classSym = moduleSym.moduleClass.asClass

    val constr = ???

    val testMethods = ???

    val defs = List()

    sbt.APIUtils.registerDummyClass(classSym)

    ClassDef(classSym, constr, defs)
  }

}
