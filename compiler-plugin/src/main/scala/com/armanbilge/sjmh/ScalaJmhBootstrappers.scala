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

import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.core.Constants.*
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Flags.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.Scopes.*
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Types.*
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

    val constr = genConstructor(classSym)

    val testMethods = annotatedMethods(testClass, jmhdefn.BenchmarkAnnotClass)

    val defs = List()

    sbt.APIUtils.registerDummyClass(classSym)

    ClassDef(classSym, constr, defs)
  }

  private def genConstructor(owner: ClassSymbol)(using Context): DefDef = {
    val sym = newDefaultConstructor(owner).entered
    DefDef(
      sym,
      Block(
        Super(This(owner), tpnme.EMPTY)
          .select(defn.ObjectClass.primaryConstructor)
          .appliedToNone :: Nil,
        unitLiteral,
      ),
    )
  }

  private def genInvokeBenchmark(
      owner: ClassSymbol,
      benchmarkClass: ClassSymbol,
      benchmarks: List[Symbol],
  )(using Context): DefDef = {
    val jmhdefn = JmhDefinitions.defnJmh

    val sym = newSymbol(
      owner,
      jmhNme.invokeBenchmark,
      Synthetic | Method,
      MethodType(
        List(jmhNme.instance, jmhNme.name),
        List(defn.ObjectType, defn.StringType),
        jmhdefn.FutureType,
      ),
    ).entered

    DefDef(
      sym,
      { (paramRefss: List[List[Tree]]) =>
        val List(List(instanceParamRef, nameParamRef)) = paramRefss
        val castInstanceSym = newSymbol(
          sym,
          jmhNme.castInstance,
          Synthetic,
          benchmarkClass.typeRef,
          coord = owner.span,
        )
        Block(
          ValDef(
            castInstanceSym,
            instanceParamRef.cast(benchmarkClass.typeRef),
          ) :: Nil,
          benchmarks.foldRight[Tree] {
            val tp = jmhdefn.NoSuchMethodExceptionType
            Throw(resolveConstructor(tp, nameParamRef :: Nil))
          } { (benchmark, next) =>
            If(
              Literal(Constant(benchmark.name.mangledString))
                .select(defn.Any_equals)
                .appliedTo(nameParamRef),
              genBenchmarkInvocation(benchmarkClass, benchmark, ref(castInstanceSym)),
              next,
            )
          },
        )
      },
    )
  }

  private def genBenchmarkInvocation(
      benchmarkClass: ClassSymbol,
      benchmarkMethod: Symbol,
      instance: Tree,
  )(using Context): Tree = {
    val jmhdefn = JmhDefinitions.defnJmh

    val resultType = benchmarkMethod.info.resultType
    def returnsFuture = resultType.isRef(jmhdefn.FutureClass)

    val future = if (returnsFuture) {
      instance.select(benchmarkMethod).appliedToNone
    } else {
      Block(
        instance.select(benchmarkMethod).appliedToNone :: Nil,
        ref(jmhdefn.FutureModule_unit),
      )
    }

    Lambda(MethodType(Nil, jmhdefn.FutureType), _ => future)
  }

  private def annotatedMethods(owner: ClassSymbol, annot: Symbol)(using
      Context,
  ): List[Symbol] =
    owner.info
      .membersBasedOnFlags(Method, EmptyFlags)
      .filter(_.symbol.hasAnnotation(annot))
      .map(_.symbol)
      .toList

  private def annotatedVars(owner: ClassSymbol, annot: Symbol)(using
      Context,
  ): List[Symbol] =
    owner.info
      .membersBasedOnFlags(Mutable, EmptyFlags)
      .filter(_.symbol.hasAnnotation(annot))
      .map(_.symbol)
      .toList

  private object jmhNme {
    val invokeBenchmark: TermName = termName("invokeBenchmark")

    val instance: TermName = termName("instance")
    val name: TermName = termName("name")
    val castInstance: TermName = termName("castInstance")
  }

}
