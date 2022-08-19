package cognosco.gen

import cognosco.Element
import cognosco.Pitch
import cognosco.PitchName
import cognosco.RegularAccidental

data class Spectrum(val fundamental: Pitch) : AbstractList<Pitch>(), PitchPool {
    override val size: Int
        get() = LIMIT

    override fun get(index: Int): Pitch =
        Pitch.fromFrequency(fundamental.frequency * (index + 1))

    override val supportedElementTypes: Set<Element.Type<*>>
        get() = Element.ALL_TYPES.toSet()

    override fun applyPitch(element: Element, range: IntRange) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val LIMIT = 1024

        @JvmStatic
        fun main(args: Array<String>) {
            val s = Spectrum(Pitch(2, PitchName.D, RegularAccidental.Natural))
            for (i in 0..10) {
                println(s[i])
            }
        }
    }
}