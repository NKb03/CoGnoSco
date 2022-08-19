package cognosco.gen

import cognosco.Element
import cognosco.Trill

class SpectralTransition(private val from: Spectrum, private val to: Spectrum) : PitchPool {
    override val supportedElementTypes: Set<Element.Type<*>>
        get() = setOf(Trill)

    override fun applyPitch(element: Element, range: IntRange) {
        TODO("Not yet implemented")
    }
}