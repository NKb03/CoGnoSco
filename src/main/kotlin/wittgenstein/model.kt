package wittgenstein

import wittgenstein.InstrumentFamily.*

enum class InstrumentFamily {
    Woodwinds, Brass, Percussion, Strings
}

enum class Instrument(
    val fullName: String,
    val shortName: String,
    val family: InstrumentFamily,
    val program: Int,
    val key: Int = -1
) {
    Flute("Flöte", "Fl.", Woodwinds, 74), Oboe("Oboe", "Ob.", Woodwinds, 69),
    Clarinet("B♭ Klarinette", "B♭ Kl.", Woodwinds, 72), Saxophone("Sopransaxophon", "S.sax.", Woodwinds, 65),
    Horn("F Horn", "F Hn.", Woodwinds, 61), Trumpet("Trompete", "Tpt.", Brass, 57),
    Trombone("Posaune", "Pos.", Brass, 58), Tuba("Tuba", "Tba.", Brass, 59),
    Timpani("Pauke", "Pk.", Percussion, 48), SnareDrum("Snare Drum", "Sn.Dr.", Percussion, 10, 38),
    BassDrum("Bass Drum", "B.Dr.", Percussion, 10, 36), Cymbal("Becken", "Bck.", Percussion, 10, 49),
    Violins("Violinen", "Vl.", Strings, 41), Violas("Viola", "Vla.", Strings, 42),
    Violoncellos("Violoncello", "Vc.", Strings, 43), Contrabasses("Kontrabass", "Kb.", Strings, 44)
}

enum class PitchName {
    C, D, E, F, G, A, B
}

sealed interface Accidental {
    val resourceName: String
}

enum class RegularAccidental : Accidental {
    Natural, Flat, Sharp;

    override val resourceName: String
        get() = when (this) {
            Natural -> "n"
            Flat -> "f"
            Sharp -> "s"
        }
}

enum class QuarterToneAccidental : Accidental {
    QuarterFlat, QuarterSharp, TreeQuarterFlat, TreeQuarterSharp;

    override val resourceName: String
        get() = when (this) {
            QuarterFlat -> "qf"
            QuarterSharp -> "qs"
            TreeQuarterFlat -> "tqf"
            TreeQuarterSharp -> "tqs"
        }
}

data class BendedAccidental(val reference: RegularAccidental, val adjust: Int) : Accidental {
    override val resourceName: String
        get() {
            val suff = "u".repeat(adjust.coerceAtLeast(0)) + "d".repeat((-adjust).coerceAtLeast(0))
            return "${reference.resourceName}$suff"
        }
}

data class Pitch(val register: Int, val name: PitchName, val accidental: Accidental) {
    fun up(): Pitch =
        if (name == PitchName.B) Pitch(register + 1, PitchName.C, accidental)
        else copy(name = PitchName.values()[name.ordinal + 1])

    fun down(): Pitch =
        if (name == PitchName.C) Pitch(register - 1, PitchName.B, accidental)
        else copy(name = PitchName.values()[name.ordinal - 1])
}

enum class Dynamic {
    PPP, PP, P, MP, MF, F, FF, FFF;

    override fun toString(): String = name.lowercase()
}

data class Moment(val bar: Int, val beat: Int) {
    fun next(): Moment = if (beat == 0) copy(beat = 1) else Moment(bar + 1, 0)
    fun prev(): Moment = if (beat == 1) copy(beat = 0) else Moment(bar - 1, 1)
}

interface Element {
    val type: Type
    var instrument: Instrument?
    var start: Moment?
    var startDynamic: Dynamic?

    sealed interface Type
}

interface PitchedElement : Element {
    override val type: Type
    var pitch: Pitch

    sealed interface Type : Element.Type
}

abstract class AbstractElement : Element {
    override var start: Moment? = null
    override var startDynamic: Dynamic? = null
    override var instrument: Instrument? = null
}

abstract class ContinuousElement : AbstractElement() {
    abstract override val type: Type

    var climax: Moment? = null
    var end: Moment? = null
    var climaxDynamic: Dynamic? = null
    var endDynamic: Dynamic? = null

    sealed interface Type : Element.Type
}

open class PitchedContinuousElement(
    override val type: Type,
    override var pitch: Pitch,
) : ContinuousElement(), PitchedElement {
    sealed interface Type : PitchedElement.Type, ContinuousElement.Type

    object Regular : Type
    object FlutterTongue : Type
    object Tremolo : Type
    object Repeat : Type
    object Noisy : Type
}

class Trill(pitch: Pitch, val secondaryPitch: Pitch) : PitchedContinuousElement(Trill, pitch) {
    companion object : Type
}

open class ContinuousNoise(override val type: Type) : ContinuousElement() {
    sealed interface Type : ContinuousElement.Type

    object DrumRoll : Type
    object Breath : Type
}

class DiscretePitchedElement(override val type: Type, override var pitch: Pitch) : AbstractElement(), PitchedElement {
    sealed interface Type : PitchedElement.Type

    object Pizzicato : Type
    object Staccato : Type
    object Noisy : Type
}

class DiscreteNoise(override val type: Type) : AbstractElement() {
    sealed interface Type : Element.Type

    object Bang : Type
}