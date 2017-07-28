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
package tac

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.bytecode.JRELibraryFolder
import java.io.File

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.Domain
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.analyses.SomeProject

/**
 * Tests that all methods of the JDK can be converted to the ai-based three address representation.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACAIIntegrationTest extends FunSpec with Matchers {

    def checkProject(
        project:       SomeProject,
        domainFactory: (SomeProject, ClassFile, Method) ⇒ Domain with RecordDefUse
    ): Unit = {
        if (Thread.currentThread().isInterrupted) return ;

        var errors: List[(String, Throwable)] = Nil
        val successfullyCompleted = new java.util.concurrent.atomic.AtomicInteger(0)
        val mutex = new Object
        val ch = project.classHierarchy
        for {
            cf ← project.allProjectClassFiles.par
            if !Thread.currentThread().isInterrupted
            m ← cf.methods
            body ← m.body
            aiResult = BaseAI(cf, m, domainFactory(project, cf, m))
        } {
            try {
                val TACode(params, tacAICode, cfg, _, _) = TACAI(m, ch, aiResult)(List.empty)
                ToTxt(params, tacAICode, cfg, false, true, true)
            } catch {
                case e: Throwable ⇒ this.synchronized {
                    val methodSignature = m.toJava(cf)
                    mutex.synchronized {
                        println(methodSignature+" - size: "+body.instructions.length)
                        e.printStackTrace(Console.out)
                        if (e.getCause != null) {
                            println("\tcause:")
                            e.getCause.printStackTrace(Console.out)
                        }
                        val instrWithIndex = body.instructions.zipWithIndex.filter(_._1 != null)
                        println(
                            instrWithIndex.map(_.swap).mkString("Instructions:\n\t", "\n\t", "\n")
                        )
                        println(
                            body.exceptionHandlers.mkString("Exception Handlers:\n\t", "\n\t", "\n")
                        )
                        errors ::= ((project.source(cf)+":"+methodSignature, e))
                    }
                }
            }
            successfullyCompleted.incrementAndGet()
        }
        if (errors.nonEmpty) {
            val message =
                errors.
                    map(_.toString()+"\n").
                    mkString(
                        "Errors thrown:\n",
                        "\n",
                        s"successfully transformed ${successfullyCompleted.get} methods: "+
                            "; failed methods: "+errors.size+"\n"
                    )
            fail(message)
        }
    }

    protected def domainFactories = {
        Seq(
            (
                "DefaultDomainWithCFGAndDefUse",
                (p: SomeProject, cf: ClassFile, m: Method) ⇒ {
                    new DefaultDomainWithCFGAndDefUse(p, cf, m)
                }
            ),
            (
                "PrimitiveTACAIDomain",
                (p: SomeProject, cf: ClassFile, m: Method) ⇒ {
                    new PrimitiveTACAIDomain(p.classHierarchy, cf, m)
                }
            )
        )
    }

    domainFactories foreach { domainInformation ⇒
        val (domainName, domainFactory) = domainInformation
        describe(s"creating the 3-address code using $domainName") {
            allBIProjects() foreach { biProject ⇒
                val (name, projectFactory) = biProject
                it(s"it should be able to create the TAC for $name using $domainName") {
                    time {
                        checkProject(projectFactory(), domainFactory)
                    } { t ⇒ info(s"conversion took ${t.toSeconds}") }
                }
            }

            it(s"it should be able to create the TAC for the JDK using $domainName") {
                time {
                    checkProject(createJREProject(), domainFactory)
                } { t ⇒ info(s"conversion took ${t.toSeconds}") }
            }
        }
    }
}