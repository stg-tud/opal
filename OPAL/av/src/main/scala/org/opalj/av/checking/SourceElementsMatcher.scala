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
package av
package checking

import org.opalj.br._
import org.opalj.br.analyses.SomeProject
import scala.collection.Set

/**
 * A source element matcher determines a set of source elements that matches a given query.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
trait SourceElementsMatcher extends (SomeProject ⇒ Set[VirtualSourceElement]) { left ⇒

    final def apply(project: SomeProject): Set[VirtualSourceElement] = extension(project)

    def extension(implicit project: SomeProject): Set[VirtualSourceElement]

    def and(right: SourceElementsMatcher): SourceElementsMatcher = {
        new SourceElementsMatcher {
            def extension(implicit project: SomeProject) = {
                left.extension ++ right.extension
            }

            override def toString() = s"($left and $right)"
        }
    }

    def except(right: SourceElementsMatcher): SourceElementsMatcher = {
        new SourceElementsMatcher {
            def extension(implicit project: SomeProject) = {
                left.extension -- right.extension
            }

            override def toString() = s"($left except $right)"
        }
    }
}
