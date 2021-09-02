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

sealed interface Accidental

enum class RegularAccidental : Accidental {
    Natural, Flat, Sharp;

    override fun toString(): String = when (this) {
        Natural -> "n"
        Flat -> "f"
        Sharp -> "s"
    }
}

enum class QuarterToneAccidental : Accidental {
    QuarterFlat, QuarterSharp, TreeQuarterFlat, TreeQuarterSharp;

    override fun toString(): String = when (this) {
        QuarterFlat -> "qf"
        QuarterSharp -> "qs"
        TreeQuarterFlat -> "tqf"
        TreeQuarterSharp -> "tqs"
    }
}

data class BendedAccidental(val reference: RegularAccidental, val adjust: Int) : Accidental {
    override fun toString(): String {
        val suff = "u".repeat(adjust.coerceAtLeast(0)) + "d".repeat((-adjust).coerceAtLeast(0))
        return "${reference.toString()}$suff"
    }
}

data class Pitch(val register: Int, val name: PitchName, val accidental: Accidental) {
    fun up(): Pitch =
        if (name == PitchName.B) Pitch(register + 1, PitchName.C, accidental)
        else copy(name = PitchName.values()[name.ordinal + 1])

    fun down(): Pitch =
        if (name == PitchName.C) Pitch(register - 1, PitchName.B, accidental)
        else copy(name = PitchName.values()[name.ordinal - 1])

    override fun toString(): String = "$name$register$accidental"
}

enum class Dynamic {
    PPP, PP, P, MP, MF, F, FF, FFF;

    override fun toString(): String = name.lowercase()
}

data class Moment(val bar: Int, val beat: Int) {
    fun next(): Moment = if (beat == 0) copy(beat = 1) else Moment(bar + 1, 0)
    fun prev(): Moment = if (beat == 1) copy(beat = 0) else Moment(bar - 1, 1)
}

enum class NoteHeadType {
    Regular, Triangle, Rectangle, Rhombus, Cross
}

interface Element {
    val type: Type
    var instrument: Instrument?
    var start: Moment?
    var startDynamic: Dynamic?

    sealed interface Type {
        val abbreviation: String
        val description: String
        val noteHeadType: NoteHeadType get() = NoteHeadType.Regular
    }
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

    sealed interface Type : Element.Type {
        val strokeDashArray: List<Double>? get() = null
    }
}

open class PitchedContinuousElement(
    override val type: Type,
    override var pitch: Pitch,
) : ContinuousElement(), PitchedElement {
    sealed class Type(
        override val abbreviation: String,
        override val description: String
    ) : PitchedElement.Type, ContinuousElement.Type

    object Regular : Type("reg", "durchgehaltener Ton")
    object FlutterTongue : Type("f.t.", "Flatterzunge (Bläser)") {
        override val strokeDashArray: List<Double>
            get() = listOf(0.0, 6.0)
    }

    object Tremolo : Type("trm", "Tremolo (Streicher)") {
        override val strokeDashArray: List<Double>
            get() = listOf(2.0, 9.0)
    }

    object Repeat : Type("rep", "Tonwiederholung") {
        override val strokeDashArray: List<Double>
            get() = listOf(4.0, 11.0)
    }

    object ColLegnoTratto : Type("c.l.t.", "col legno tratto (Streicher)") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Triangle
    }

    object Noisy: Type("noisy", "geräuschhaft (bei Bläsern mit viel Luft)") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Triangle
    }
}

class Trill(pitch: Pitch, val secondaryPitch: Pitch) : PitchedContinuousElement(Trill, pitch) {
    companion object : Type("tr", "Triller")
}

open class ContinuousNoise(override val type: Type) : ContinuousElement() {
    sealed class Type(override val abbreviation: String, override val description: String) : ContinuousElement.Type

    object DrumRoll : Type("d.r.", "Trommelwirbel (Bass Drum, Snare, Pauke, Becken)") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Rectangle

        override val strokeDashArray: List<Double> = listOf(-0.0, 6.0)
    }

    object Breath : Type("br.", "Atem (ganzes Orchester)") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Rhombus
    }
}

class DiscretePitchedElement(override val type: Type, override var pitch: Pitch) : AbstractElement(), PitchedElement {
    sealed class Type(override val abbreviation: String, override val description: String) : PitchedElement.Type

    object Staccato : Type("stacc.", "Staccato")

    object Pizzicato : Type("pizz.", "Pizzicato (Streicher)") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Cross
    }


    object Slap : Type("s.t.", "Slap Tongue") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Triangle
    }
}

class DiscreteNoise(override val type: Type) : AbstractElement() {
    sealed class Type(override val abbreviation: String, override val description: String) : Element.Type

    object Bang : Type("bang", "Schlag") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Rectangle
    }
}