/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.AllocationSite
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.AllocationSites
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.ConditionalFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.VConditionalFreshReturnValue
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.TACode
import org.opalj.tac.ReturnValue
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.Assignment
import org.opalj.tac.Const
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall

/**
 * An analysis that determines for a given method, whether its the return value is a fresh object,
 * that is created within the method and does not escape by other than [[EscapeViaReturn]].
 *
 * In other words, it aggregates the escape information for all allocation-sites, that might be used
 * as return value.
 *
 * @author Florian Kuebler
 */
class ReturnValueFreshnessAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, DUVar[(Domain with RecordDefUse)#DomainValue]] = project.get(DefaultTACAIKey)
    private[this] val allocationSites: AllocationSites = propertyStore.context[AllocationSites]
    private[this] val declaredMethods: DeclaredMethods = propertyStore.context[DeclaredMethods]

    def doDetermineFreshness(dm: DefinedMethod): PropertyComputationResult = {

        val m = dm.definedMethod
        // todo hardcode clone on array type
        // base types are always fresh
        if (m.returnType.isBaseType) {
            Result(dm, PrimitiveReturnValue)
        } else {
            var dependees: Set[EOptionP[Entity, Property]] = Set.empty
            val code = tacaiProvider(m).stmts

            // for every return-value statement check the def-sites
            for {
                ReturnValue(_, expr) ← code
                defSite ← expr.asVar.definedBy
            } {
                if (defSite >= 0) {
                    val stmt = code(defSite)
                    stmt match {
                        // if the def-site of the return-value statement is a new, we check the escape state
                        case Assignment(pc, _, New(_, _) | NewArray(_, _, _)) ⇒
                            val allocationSite = allocationSites(m)(pc)
                            val resultState = propertyStore(allocationSite, EscapeProperty.key)
                            resultState match {
                                case EP(_, EscapeViaReturn)              ⇒
                                case EP(_, NoEscape | EscapeInCallee)    ⇒ throw new RuntimeException("unexpected result")
                                case EP(_, p) if p.isFinal               ⇒ return Result(dm, NoFreshReturnValue)
                                case EP(_, AtMost(_))                    ⇒ return Result(dm, NoFreshReturnValue)
                                case EP(_, Conditional(AtMost(_)))       ⇒ return Result(dm, NoFreshReturnValue)
                                case EP(_, Conditional(NoEscape))        ⇒ throw new RuntimeException("unexpected result")
                                case EP(_, Conditional(EscapeInCallee))  ⇒ throw new RuntimeException("unexpected result")
                                case EP(_, Conditional(EscapeViaReturn)) ⇒ dependees += resultState
                                case _                                   ⇒ dependees += resultState
                            }
                        // const values are handled as fresh
                        case Assignment(_, _, _: Const) ⇒

                        case Assignment(_, tgt, StaticFunctionCall(_, dc, isI, name, desc, _)) ⇒
                            if (tgt.usedBy.size == 1) {
                                if (dm.name == "transitiveObjectFactory")
                                    println(dm.name)
                                val callee = project.staticCall(dc, isI, name, desc)

                                // unkown method
                                if (callee.isEmpty)
                                    return Result(dm, NoFreshReturnValue)

                                propertyStore(declaredMethods(callee.value), ReturnValueFreshness.key) match {
                                    case EP(_, NoFreshReturnValue) ⇒
                                        return Result(dm, NoFreshReturnValue)
                                    case EP(_, FreshReturnValue) ⇒
                                    case ep @ EP(_, ConditionalFreshReturnValue) ⇒
                                        dependees += ep
                                    case epk ⇒ dependees += epk
                                }
                            } else
                                return Result(dm, NoFreshReturnValue)

                        case Assignment(_, tgt, NonVirtualFunctionCall(_, dc, isI, name, desc, _, _)) ⇒
                            if (tgt.usedBy.size == 1) {
                                val callee = project.specialCall(dc, isI, name, desc)

                                // unkown method
                                if (callee.isEmpty)
                                    return Result(dm, NoFreshReturnValue)

                                propertyStore(declaredMethods(callee.value), ReturnValueFreshness.key) match {
                                    case EP(_, NoFreshReturnValue) ⇒
                                        return Result(dm, NoFreshReturnValue)
                                    case EP(_, FreshReturnValue) ⇒
                                    case ep @ EP(_, ConditionalFreshReturnValue) ⇒
                                        dependees += ep
                                    case epk ⇒ dependees += epk
                                }

                            } else
                                return Result(dm, NoFreshReturnValue)

                        case Assignment(_, tgt, VirtualFunctionCall(_, dc, _, name, desc, _, _)) ⇒
                            if (tgt.usedBy.size == 1) {
                                val callee = project.instanceCall(m.classFile.thisType, dc, name, desc)

                                // unkown method
                                if (callee.isEmpty)
                                    return Result(dm, NoFreshReturnValue)

                                propertyStore(declaredMethods(callee.value), VirtualMethodReturnValueFreshness.key) match {
                                    case EP(_, VNoFreshReturnValue) ⇒
                                        return Result(dm, NoFreshReturnValue)
                                    case EP(_, VFreshReturnValue) ⇒
                                    case ep @ EP(_, VConditionalFreshReturnValue) ⇒
                                        dependees += ep
                                    case epk ⇒ dependees += epk
                                }
                            } else {
                                return Result(dm, NoFreshReturnValue)
                            }
                        // other kinds of assignments came from other methods, fields etc, which we do not track
                        case Assignment(_, _, _) ⇒ return Result(dm, NoFreshReturnValue)
                        case _                   ⇒ throw new RuntimeException("not yet implemented")
                    }
                } else {
                    return Result(dm, NoFreshReturnValue)
                }

            }

            /**
             * A continuation function, that handles updates for the escape state.
             */
            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
                if (dm.name == "transitiveObjectFactory")
                    println(dm.name)
                e match {
                    case _: AllocationSite ⇒ p match {
                        case NoEscape | EscapeInCallee ⇒
                            throw new RuntimeException("unexpected result")
                        case EscapeViaReturn ⇒
                            dependees = dependees.filter { _.e ne e }
                            if (dependees.isEmpty) {
                                Result(dm, FreshReturnValue)
                            } else {
                                IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)
                            }
                        case _: EscapeProperty if p.isFinal ⇒ Result(dm, NoFreshReturnValue)
                        case AtMost(_)                      ⇒ Result(dm, NoFreshReturnValue)
                        case Conditional(AtMost(_))         ⇒ Result(dm, NoFreshReturnValue)

                        case p @ Conditional(EscapeViaReturn) ⇒
                            val newEP = EP(e, p)
                            dependees = dependees.filter(_.e ne e) + newEP
                            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                        case Conditional(NoEscape) | Conditional(EscapeInCallee) ⇒
                            throw new RuntimeException("unexpected result")

                        case PropertyIsLazilyComputed ⇒
                            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                    }
                    case _: DeclaredMethod ⇒ p match {
                        case NoFreshReturnValue | VNoFreshReturnValue ⇒
                            Result(dm, NoFreshReturnValue)
                        case FreshReturnValue | VFreshReturnValue ⇒
                            dependees = dependees filter { _.e ne e }
                            if (dependees.isEmpty)
                                Result(dm, FreshReturnValue)
                            else
                                IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                        case ConditionalFreshReturnValue | VConditionalFreshReturnValue ⇒
                            val newEP = EP(e, p)
                            dependees = dependees.filter(_.e ne e) + newEP
                            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                        case PropertyIsLazilyComputed ⇒
                            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)
                    }
                }

            }

            if (dependees.isEmpty) {
                Result(dm, FreshReturnValue)
            } else {
                IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)
            }
        }
    }
    /**
     * Determines the freshness of the return value.
     */
    def determineFreshness(m: DeclaredMethod): PropertyComputationResult = {
        m match {
            case dm @ DefinedMethod(_, me) if me.body.isDefined ⇒ doDetermineFreshness(dm)
            case dm @ DefinedMethod(_, me) if me.body.isEmpty   ⇒ Result(dm, NoFreshReturnValue)
            case _                                              ⇒ throw new NotImplementedError()
        }
    }
}

object ReturnValueFreshnessAnalysis extends FPCFEagerAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(ReturnValueFreshness)

    override def usedProperties: Set[PropertyKind] = Set(EscapeProperty)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val declaredMethods = propertyStore.context[DeclaredMethods].declaredMethods
        val analysis = new ReturnValueFreshnessAnalysis(project)
        VirtualReturnValueFreshnessAnalysis.startLazily(project, propertyStore)
        propertyStore.scheduleForEntities(declaredMethods)(analysis.determineFreshness)
        analysis
    }
}
