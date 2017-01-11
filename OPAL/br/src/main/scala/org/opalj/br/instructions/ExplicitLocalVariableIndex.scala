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
package br
package instructions

/**
 * Trait that can be mixed in if the local variable index of a load or store instruction
 * ((a,i,l,...)load/store_X) is not predefined as part of the instruction.
 *
 * @author Michael Eichberg
 */
trait ExplicitLocalVariableIndex extends Instruction {

    def lvIndex: Int

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)

        (this eq other) || (
            other.opcode == this.opcode &&
            other.asInstanceOf[ExplicitLocalVariableIndex].lvIndex == this.lvIndex
        )
    }

    final def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, code.isModifiedByWide(currentPC))
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = {
        if (modifiedByWide)
            currentPC + 3
        else
            currentPC + 2
    }

}

object ExplicitLocalVariableIndex {

    def unapply(i: ExplicitLocalVariableIndex): Some[Int] = Some(i.lvIndex)

}
