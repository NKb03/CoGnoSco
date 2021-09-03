@file: OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package wittgenstein

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import wittgenstein.InstrumentFamily.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val json = Json {
    serializersModule = SerializersModule {
        contextual(Accidental.Serializer)
    }
}

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

@Serializable(with = Accidental.Serializer::class)
sealed interface Accidental {
    object Serializer : KSerializer<Accidental> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("accidental", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Accidental) = encoder.encodeString(value.toString())

        override fun deserialize(decoder: Decoder): Accidental {
            val str = decoder.decodeString()
            val u = str.count { it == 'u' }
            val d = str.count { it == 'd' }
            val reg = RegularAccidental.map[str.take(1)]
            return if (reg != null) if (u - d == 0) reg else BendedAccidental(reg, u - d)
            else QuarterToneAccidental.map[str]!!
        }
    }
}

enum class RegularAccidental : Accidental {
    Natural, Flat, Sharp;

    override fun toString(): String = when (this) {
        Natural -> "n"
        Flat -> "f"
        Sharp -> "s"
    }

    companion object {
        val map = values().associateBy { it.toString() }
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

    companion object {
        val map = values().associateBy { it.toString() }
    }
}

@Serializable
data class BendedAccidental(val reference: RegularAccidental, val adjust: Int) : Accidental {
    override fun toString(): String {
        val suff = "u".repeat(adjust.coerceAtLeast(0)) + "d".repeat((-adjust).coerceAtLeast(0))
        return "$reference$suff"
    }
}

@Serializable
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

@Serializable
data class Moment(val bar: Int, val beat: Int) {
    fun next(): Moment = if (beat == 0) copy(beat = 1) else Moment(bar + 1, 0)
    fun prev(): Moment = if (beat == 1) copy(beat = 0) else Moment(bar - 1, 1)
}

enum class NoteHeadType {
    Regular, Triangle, Rectangle, Rhombus, Cross;
}

sealed interface Element {
    val type: Type<Element>
    var instrument: Instrument?
    var start: Moment?
    var startDynamic: Dynamic?
    var customY: Double?

    fun copyFrom(original: Element) {
        instrument = original.instrument
        start = original.start
        startDynamic = original.startDynamic
        customY = original.customY
    }

    fun serialize(): JsonElement = buildJsonObject {
        val element = this@Element
        put("type", JsonPrimitive(type.abbreviation))
        @Suppress("UNCHECKED_CAST")
        for (prop in element::class.memberProperties as List<KProperty1<Element, Any?>>) {
            if (prop !is KMutableProperty1) continue
            val value = prop.get(element) ?: continue
            val serializer = serializer(prop.returnType)
            val encoded = if (value is Enum<*>) JsonPrimitive(value.name) else json.encodeToJsonElement(serializer, value)
            put(prop.name, encoded)
        }
    }

    fun deserialize(obj: JsonObject) {
        @Suppress("UNCHECKED_CAST")
        for (prop in this::class.memberProperties as List<KProperty1<Element, Any?>>) {
            if (prop !is KMutableProperty1) continue
            val value = obj[prop.name] ?: continue
            val serializer = serializer(prop.returnType)
            prop.set(this, json.decodeFromJsonElement(serializer, value))
        }
    }

    sealed interface Type<out E : Element> {
        val abbreviation: String
        val description: String
        val noteHeadType: NoteHeadType get() = NoteHeadType.Regular
        val properties get() = listOf("instrument", "start", "startDynamic", "customY")

        fun createElement(): E
    }

    companion object {
        val ALL_TYPES = listOf(
            SimplePitchedContinuousElement.Regular,
            Trill,
            SimplePitchedContinuousElement.FlutterTongue,
            SimplePitchedContinuousElement.Tremolo,
            SimplePitchedContinuousElement.Repeat,
            SimplePitchedContinuousElement.ColLegnoTratto,
            SimplePitchedContinuousElement.Noisy,
            ContinuousNoise.DrumRoll,
            ContinuousNoise.Breath,
            DiscretePitchedElement.Staccato,
            DiscretePitchedElement.Pizzicato,
            DiscretePitchedElement.Slap,
            DiscreteNoise.Bang
        )

        private val map = ALL_TYPES.associateBy { it.abbreviation }

        fun deserialize(json: JsonElement): Element {
            require(json is JsonObject)
            val abbr = json.getValue("type").jsonPrimitive.content
            val type = map.getValue(abbr)
            val el = type.createElement()
            el.deserialize(json)
            return el
        }
    }
}

interface PitchedElement : Element {
    override val type: Type<PitchedElement>
    var pitch: Pitch

    override fun copyFrom(original: Element) {
        super.copyFrom(original)
        if (original is PitchedElement) pitch = original.pitch
    }

    sealed interface Type<out E : PitchedElement> : Element.Type<E>
}

sealed class AbstractElement : Element {
    override var start: Moment? = null
    override var startDynamic: Dynamic? = null
    override var instrument: Instrument? = null
    override var customY: Double? = null
}

sealed class ContinuousElement : AbstractElement() {
    abstract override val type: Type<ContinuousElement>

    var climax: Moment? = null
    var end: Moment? = null
    var climaxDynamic: Dynamic? = null
    var endDynamic: Dynamic? = null

    override fun copyFrom(original: Element) {
        super.copyFrom(original)
        if (original is ContinuousElement) {
            climax = original.climax
            end = original.end
            climaxDynamic = original.climaxDynamic
            endDynamic = original.endDynamic
        }
    }

    sealed interface Type<out E : ContinuousElement> : Element.Type<E> {
        val strokeDashArray: List<Double>? get() = null
    }
}

sealed class PitchedContinuousElement : ContinuousElement(), PitchedElement {
    abstract override val type: Type<PitchedContinuousElement>
    override lateinit var pitch: Pitch

    override fun copyFrom(original: Element) {
        super<ContinuousElement>.copyFrom(original)
        super<PitchedElement>.copyFrom(original)
    }

    sealed class Type<out E : PitchedContinuousElement>(
        override val abbreviation: String,
        override val description: String
    ) : PitchedElement.Type<E>, ContinuousElement.Type<E>
}

class SimplePitchedContinuousElement(
    override val type: Type,
) : PitchedContinuousElement() {
    sealed class Type(abbreviation: String, description: String) :
        PitchedContinuousElement.Type<SimplePitchedContinuousElement>(abbreviation, description) {
        override fun createElement(): SimplePitchedContinuousElement = SimplePitchedContinuousElement(this)
    }

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

    object Noisy : Type("noisy", "geräuschhaft (bei Bläsern mit viel Luft)") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Triangle
    }
}

class Trill : PitchedContinuousElement() {
    var secondaryPitch: Pitch? = null

    override fun copyFrom(original: Element) {
        super.copyFrom(original)
        if (original is Trill) {
            secondaryPitch = original.secondaryPitch
        }
    }

    override val type: Type<Trill>
        get() = Trill

    companion object : Type<Trill>("tr", "Triller") {
        override fun createElement(): Trill = Trill()
    }
}

open class ContinuousNoise(override val type: Type) : ContinuousElement() {
    sealed class Type(override val abbreviation: String, override val description: String) :
        ContinuousElement.Type<ContinuousNoise> {
        override fun createElement(): ContinuousNoise = ContinuousNoise(this)
    }

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

class DiscretePitchedElement(override val type: Type) : AbstractElement(), PitchedElement {
    override lateinit var pitch: Pitch

    sealed class Type(
        override val abbreviation: String,
        override val description: String
    ) : PitchedElement.Type<DiscretePitchedElement> {
        override fun createElement(): DiscretePitchedElement = DiscretePitchedElement(this)
    }

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
    sealed class Type(override val abbreviation: String, override val description: String) :
        Element.Type<DiscreteNoise> {
        override fun createElement(): DiscreteNoise = DiscreteNoise(this)
    }

    object Bang : Type("bang", "Schlag") {
        override val noteHeadType: NoteHeadType
            get() = NoteHeadType.Rectangle
    }
}

data class Score(val elements: List<Element>) {
    fun encodeToString(): String {
        val array = JsonArray(elements.map { it.serialize() })
        return array.toString()
    }

    companion object {
        fun decodeFromString(str: String): Score {
            val array = Json.parseToJsonElement(str).jsonArray
            return Score(array.map { Element.deserialize(it) })
        }
    }
}

fun main() {
    val n = ContinuousNoise(ContinuousNoise.Breath).apply {
        start = Moment(0, 0)
        climaxDynamic = Dynamic.F
        startDynamic = Dynamic.P
        instrument = Instrument.Flute
    }
    val t = Trill().apply {
        pitch = Pitch(4, PitchName.C, QuarterToneAccidental.QuarterFlat)
        secondaryPitch = Pitch(4, PitchName.C, QuarterToneAccidental.QuarterSharp)
        start = Moment(4, 1)
        climaxDynamic = Dynamic.FFF
        startDynamic = Dynamic.PPP
        instrument = Instrument.Trombone
    }
    val score = Score(listOf(n, t))
    val str = score.encodeToString()
    println(str)
    println(Score.decodeFromString(str))
}