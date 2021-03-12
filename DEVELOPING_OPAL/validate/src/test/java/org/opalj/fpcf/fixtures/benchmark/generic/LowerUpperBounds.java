/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;

class LowerUpperBounds<T extends ClassWithMutableField> {

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private T t;

    public LowerUpperBounds(T t){
        this.t = t;
    }
}