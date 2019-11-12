/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.class_immutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.ClassImmutability_new
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

/**
 * @author Tobias Peter Roth
 */
class ClassImmutabilityMatcher(val property: ClassImmutability_new)
    extends AbstractPropertyMatcher {

    final private val PropertyReasonID = 0

    override def isRelevant(
        p:      SomeProject,
        as:     Set[ObjectType],
        entity: Object,
        a:      AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asObjectType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)

        analyses.exists(as.contains)

    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        println(11)
        if (!properties.exists(p ⇒ p == property)) {
            // ... when we reach this point the expected property was not found.
            println(22)
            val r = Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
            println(33)
            r
        } else {
            println(44)
            None
        }
    }

}

class MutableClassMatcher extends ClassImmutabilityMatcher(MutableClass)

class DependentImmutableClassMatcher extends ClassImmutabilityMatcher(DependentImmutableClass)

class ShallowImmutableClassMatcher extends ClassImmutabilityMatcher(ShallowImmutableClass)

class DeepImmutableClassMatcher extends ClassImmutabilityMatcher(DeepImmutableClass)
