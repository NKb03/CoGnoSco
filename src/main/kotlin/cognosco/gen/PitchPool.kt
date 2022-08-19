package cognosco.gen

import cognosco.Element

interface PitchPool {
    val supportedElementTypes: Set<Element.Type<*>>

    fun applyPitch(element: Element, range: IntRange)
}