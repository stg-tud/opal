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
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis;
import org.opalj.fpcf.properties.field_mutability.EffectivelyFinal;
import org.opalj.fpcf.properties.field_mutability.NonFinal;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {

    @EffectivelyFinal(
            value = "only initialized by the constructor",
            analyses = { L1FieldMutabilityAnalysis.class }
            )
    private String name;

    @NonFinal("incremented whenever `this` object is passed to another `NonFinal` object")
    private int i;

    private PrivateFieldUpdater(PrivateFieldUpdater s) {
        if (s != null) {
            s.i += 1;
            this.i = s.i;
            this.name = s.name + s.i;
        }
    }

    public String getName() {
        return name;
    }

    public int getI() {
        return i;
    }

}