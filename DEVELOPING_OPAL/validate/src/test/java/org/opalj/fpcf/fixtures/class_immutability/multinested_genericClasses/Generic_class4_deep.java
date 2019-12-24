package org.opalj.fpcf.fixtures.class_immutability.multinested_genericClasses;

import org.opalj.fpcf.FinalE;
import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public final class Generic_class4_deep {

    @ImmutableReferenceAnnotation("")
    @DeepImmutableFieldAnnotation("")
    private Generic_class3<FinalEmptyClass> gc;

    public Generic_class4_deep(FinalEmptyClass fec1, FinalEmptyClass fec2, FinalEmptyClass fec3, FinalEmptyClass fec4, FinalEmptyClass fec5){
        gc = new Generic_class3<FinalEmptyClass>(fec1, fec2, fec3, fec4, fec5);
    }
}