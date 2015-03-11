/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package bugpicker
package ui

import scalafx.Includes._
import scalafx.scene.control.ListView
import scalafx.application.Platform
import org.opalj.br.analyses.ProgressManagement
import scalafx.beans.property.ReadOnlyBooleanProperty
import scalafx.beans.property.DoubleProperty
import scalafx.stage.Stage
import org.opalj.br.analyses.EventType

class InitProgressManagement(
        interrupted: ReadOnlyBooleanProperty,
        theProgress: DoubleProperty,
        progressListView: ListView[String],
        progressListItems: scala.collection.mutable.HashMap[String, String],
        stepCount: Double,
        progStage: Stage) extends Function1[Int, ProgressManagement] {

    override def apply(x: Int): ProgressManagement = new ProgressManagement { pm ⇒

        final private[this] val finishedSteps = new java.util.concurrent.atomic.AtomicInteger(0)

        final def progress(stepID: Int, evt: EventType.Value, msg: Option[String]): Unit = evt match {
            case EventType.Start ⇒ {
                Platform.runLater(new Runnable() {
                    override def run(): Unit = {
                        progressListView.items() += stepID.toString+": "+msg.get
                        progressListItems += ((stepID.toString, msg.get))
                        progressListView.scrollTo(progressListView.getItems.size() - 1)
                    }
                })
            }
            case EventType.End ⇒ {
                Platform.runLater(new Runnable() {
                    override def run(): Unit = {
                        progressListView.items() -= stepID.toString+": "+progressListItems.get(stepID.toString).get
                        progressListItems.remove(stepID.toString)
                        val finishedSteps = pm.finishedSteps.incrementAndGet()
                        val prog = finishedSteps / stepCount
                        theProgress.synchronized(if (prog > theProgress()) theProgress() = prog)
                        if (finishedSteps == stepCount)
                            progStage.close
                    }
                })
            }
        }

        final def isInterrupted: Boolean = interrupted()
    }
}
