@file: OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package cognosco

import cognosco.InstrumentFamily.*
import cognosco.lily.lilypond
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val json = Json {
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(Accidental.Serializer)
    }
}

enum class InstrumentFamily {
    Woodwinds, Brass, Percussion, Timpani, Strings;
}

enum class Clef(val middleLinePitch: Pitch?) {
    Violin(Pitch(4, PitchName.B, RegularAccidental.Natural)),
    Alto(Pitch(4, PitchName.C, RegularAccidental.Natural)),
    Bass(Pitch(3, PitchName.D, RegularAccidental.Natural)),
    Percussion(null);
}

enum class Instrument(
    val fullName: String,
    val shortName: String,
    val family: InstrumentFamily,
    val clef: Clef,
    val transposition: String,
    val program: Int,
    val percussionKey: Int? = null
) {
    Flute("Flöte", "Fl.", Woodwinds, Clef.Violin, "c'", 74),
    Oboe("Oboe", "Ob.", Woodwinds, Clef.Violin, "c'", 69),
    Clarinet("B♭ Klarinette", "B♭ Kl.", Woodwinds, Clef.Violin, "bf", 72),
    Saxophone("Sopransaxophon", "S.sax.", Woodwinds, Clef.Violin, "bf", 65),
    Horn("F Horn", "F Hn.", Woodwinds, Clef.Bass, "f", 61),
    Trumpet("B♭ Trompete", "B♭ Tpt.", Brass, Clef.Violin, "bf", 57),
    Trombone("Posaune", "Pos.", Brass, Clef.Bass, "c'", 58),
    Tuba("Tuba", "Tba.", Brass, Clef.Bass, "c'", 59),
    Timpani("Pauke", "Pk.", InstrumentFamily.Timpani, Clef.Bass, "c'", 48),
    SnareDrum("Snare Drum", "Sn.Dr.", Percussion, Clef.Percussion, "", 10, 38),
    BassDrum("Bass Drum", "B.Dr.", Percussion, Clef.Percussion, "", 10, 36),
    Cymbal("Becken", "Bck.", Percussion, Clef.Percussion, "", 10, 49),
    Violins("Violinen", "Vl.", Strings, Clef.Violin, "c'", 41),
    Violas("Viola", "Vla.", Strings, Clef.Alto, "c'", 42),
    Violoncellos("Violoncello", "Vc.", Strings, Clef.Bass, "c'", 43),
    Contrabasses("Kontrabass", "Kb.", Strings, Clef.Bass, "c", 44)
}

enum class PitchName(val chromaticStep: Int) {
    C(0), D(2), E(4), F(5), G(7), A(9), B(11);

    val diatonicStep get() = ordinal
}

@Serializable(with = Accidental.Serializer::class)
sealed interface Accidental {
    val reference: RegularAccidental
    val bend: Int

    object Serializer : KSerializer<Accidental> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("accidental", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Accidental) = encoder.encodeString(value.lilypond())

        override fun deserialize(decoder: Decoder): Accidental {
            val str = decoder.decodeString()
            val bend = when (str.getOrNull(1)) {
                'u' -> +13
                'U' -> +31
                'd' -> -13
                'D' -> -31
                else -> +-0
            }
            println(str.take(1))
            val reg = RegularAccidental.map[str.take(1)]
            return when {
                reg == null -> QuarterToneAccidental.map.getValue(str)
                bend == 0 -> reg
                else -> BendedAccidental(reg, bend)
            }
        }
    }
}

enum class RegularAccidental(val chromaticSteps: Int) : Accidental {
    Natural(0), Flat(-1), Sharp(+1);

    override val reference: RegularAccidental
        get() = this

    override val bend: Int
        get() = 0

    companion object {
        val map = values().associateBy { it.lilypond() }
    }
}

enum class QuarterToneAccidental(override val reference: RegularAccidental, override val bend: Int) : Accidental {
    QuarterFlat(RegularAccidental.Natural, -50),
    QuarterSharp(RegularAccidental.Natural, +50),
    TreeQuarterFlat(RegularAccidental.Flat, -50),
    TreeQuarterSharp(RegularAccidental.Sharp, +50);

    companion object {
        val map = values().associateBy { it.lilypond() }
    }
}

@Serializable
data class BendedAccidental(override val reference: RegularAccidental, override val bend: Int) : Accidental {
    override fun toString(): String = if (bend < 0) "$reference$bend" else "$reference+${bend}ct"
}

@Serializable
data class Pitch(val register: Int, val name: PitchName, val accidental: Accidental) {
    fun up(): Pitch =
        if (name == PitchName.B) Pitch(register + 1, PitchName.C, accidental)
        else copy(name = PitchName.values()[name.diatonicStep + 1])

    fun down(): Pitch =
        if (name == PitchName.C) Pitch(register - 1, PitchName.B, accidental)
        else copy(name = PitchName.values()[name.diatonicStep - 1])

    override fun toString(): String = "$name$register$accidental"

    val diatonicStep get() = register * 7 + name.diatonicStep

    val chromaticStep get() = register * 12 + name.chromaticStep + accidental.reference.chromaticSteps

    val cent get() = chromaticStep * 100 + accidental.bend

    operator fun minus(p: Pitch): Int = diatonicStep - p.diatonicStep

    companion object {
        fun fromDiatonicStep(step: Int, accidental: Accidental): Pitch {
            val register = step / 7
            val pitchName = PitchName.values()[step % 7]
            return Pitch(register, pitchName, accidental)
        }
    }
}

enum class Dynamic(val midiVolume: Int) {
    PPP(8), PP(22), P(36), MP(47), MF(64), F(85), FF(104), FFF(127);

    override fun toString(): String = name.lowercase()
}

typealias Time = Int

enum class NoteHeadType {
    Regular, Triangle, Rectangle, Slashed, Rhombus, Cross;
}

sealed interface Element {
    val type: Type<Element>
    var instrument: Instrument?
    var start: Time
    var startDynamic: Dynamic
    var customY: Double?
    val end: Time
    val pitch: Pitch? get() = null

    fun setType(type: Type<*>): Boolean

    fun copyFrom(original: Element) {
        instrument = original.instrument
        start = original.start
        startDynamic = original.startDynamic
        customY = original.customY
    }

    fun serialize(): JsonElement = buildJsonObject {
        val element = this@Element
        put("type", JsonPrimitive(type.id))
        @Suppress("UNCHECKED_CAST")
        for (prop in element::class.memberProperties as List<KProperty1<Element, Any?>>) {
            if (prop !is KMutableProperty1) continue
            val value = prop.get(element) ?: continue
            if (value == 0 || value == 0.0) continue
            val serializer = serializer(prop.returnType)
            val encoded =
                if (value is Enum<*>) JsonPrimitive(value.name) else json.encodeToJsonElement(serializer, value)
            put(prop.name, encoded)
        }
    }

    fun deserialize(obj: JsonObject) {
        @Suppress("UNCHECKED_CAST")
        for (prop in this::class.memberProperties as List<KProperty1<Element, Any?>>) {
            if (prop !is KMutableProperty1) continue
            val value = obj[prop.name] ?: continue
            val serializer = serializer(prop.returnType)
            val decoded = json.decodeFromJsonElement(serializer, value)
            if (decoded == 0 || decoded == 0.0) continue
            prop.set(this, decoded)
        }
    }

    sealed interface Type<out E : Element> {
        val id: String
        val description: String

        val noteHeadType: NoteHeadType get() = NoteHeadType.Regular

        val resourceName: String get() = "element_types/${toString()}.png"

        fun createElement(): E
    }

    companion object {
        val ALL_TYPES = listOf(
            SimplePitchedContinuousElement.Type.Regular,
            Trill,
            SimplePitchedContinuousElement.Type.FastRepeat,
            SimplePitchedContinuousElement.Type.Repeat,
            SimplePitchedContinuousElement.Type.Noisy,
            ContinuousNoise.Type.DrumRoll,
            ContinuousNoise.Type.Breath,
            DiscretePitchedElement.Type.Staccato,
            DiscretePitchedElement.Type.Percussive,
            DiscretePitchedElement.Type.ColLegnoBattuto,
            DiscreteNoise.Type.Bang
        )

        private val map = ALL_TYPES.associateBy { it.id }

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

sealed interface PitchedElement : Element {
    override val type: Type<PitchedElement>
    override var pitch: Pitch

    override fun copyFrom(original: Element) {
        if (original is PitchedElement) pitch = original.pitch
    }

    sealed interface Type<out E : PitchedElement> : Element.Type<E>
}

sealed class AbstractElement : Element {
    override var start: Time = 0
    override var startDynamic: Dynamic = Dynamic.MF
    override var instrument: Instrument? = null
    override var customY: Double? = null
    override val end: Time
        get() = start + 1

    override fun toString(): String = "$type: $start ($startDynamic)"
}

@Serializable
class ElementPhase(var end: Time, var targetPitch: Pitch?, var targetDynamic: Dynamic)

sealed interface ContinuousElement : Element {
    abstract override val type: Type<ContinuousElement>

    var phases: MutableList<ElementPhase>

    override fun copyFrom(original: Element) {
        super.copyFrom(original)
        if (original is ContinuousElement) {
            phases.clear()
            phases.addAll(original.phases)
        }
    }

    sealed interface Type<out E : ContinuousElement> : Element.Type<E> {
        val strokeDashArray: List<Double> get() = emptyList()
    }
}

sealed class AbstractContinuousElement : ContinuousElement, AbstractElement() {
    override var phases: MutableList<ElementPhase> = mutableListOf()

    override val end: Time
        get() = phases.last().end

    override fun copyFrom(original: Element) {
        super<ContinuousElement>.copyFrom(original)
    }
}

sealed class PitchedContinuousElement : ContinuousElement, PitchedElement, AbstractContinuousElement() {
    abstract override val type: Type<PitchedContinuousElement>
    override lateinit var pitch: Pitch

    override fun copyFrom(original: Element) {
        super<AbstractContinuousElement>.copyFrom(original)
        super<PitchedElement>.copyFrom(original)
    }

    sealed interface Type<out E : PitchedContinuousElement> : PitchedElement.Type<E>, ContinuousElement.Type<E>
}

class SimplePitchedContinuousElement(
    override var type: Type,
) : PitchedContinuousElement() {
    override fun toString(): String = "${type.id} $pitch (${instrument?.shortName}), $start, ($startDynamic)"

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type = type
        return true
    }

    enum class Type(
        override val id: String,
        override val description: String
    ) : PitchedContinuousElement.Type<SimplePitchedContinuousElement> {
        Regular("reg", "durchgehaltener Ton"),
        FastRepeat("fastrep", "schnelle Tonwiederholung (bei Bläsern Flatterzunge, bei Streichern Tremolo)") {
            override val strokeDashArray: List<Double>
                get() = listOf(0.0, 7.0)
        },
        Repeat("rep", "Tonwiederholung") {
            override val strokeDashArray: List<Double>
                get() = listOf(4.0, 11.0)
        },
        Noisy("noisy", "geräuschhaft (bei Bläsern mit viel Luft, bei Streichern col legno)") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Rectangle
        };

        override fun createElement(): SimplePitchedContinuousElement = SimplePitchedContinuousElement(this)
    }
}

class Trill : PitchedContinuousElement() {
    var secondaryPitch: Pitch? = null

    override fun toString(): String =
        "tr $pitch ($secondaryPitch) gespielt von ${instrument?.shortName}, $start, ($startDynamic)"

    override fun copyFrom(original: Element) {
        super.copyFrom(original)
        if (original is Trill) {
            secondaryPitch = original.secondaryPitch
        }
    }

    override fun setType(type: Element.Type<*>): Boolean = false

    override val type: Type<Trill>
        get() = Trill

    companion object : Type<Trill> {
        override val id: String
            get() = "tr"

        override val description: String
            get() = "Triller"

        override fun createElement(): Trill = Trill()

        override fun toString(): String = "Trill"
    }
}

open class ContinuousNoise(override var type: Type) : ContinuousElement, AbstractContinuousElement() {
    override fun toString(): String = "${type.id} (${instrument?.shortName}), $start ($startDynamic)"

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type = type
        return true
    }

    enum class Type(override val id: String, override val description: String) :
        ContinuousElement.Type<ContinuousNoise> {
        DrumRoll("d.r.", "Trommelwirbel (Bass Drum, Snare, Pauke, Becken)") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Rectangle

            override val strokeDashArray: List<Double> = listOf(-0.0, 6.0)
        },
        Breath("br.", "Atmen (ganzes Orchester)") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Rhombus
        };

        override fun createElement(): ContinuousNoise = ContinuousNoise(this)
    }
}

class DiscretePitchedElement(override var type: Type) : AbstractElement(), PitchedElement {
    override lateinit var pitch: Pitch

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type = type
        return true
    }

    override fun toString(): String = "${type.id}: $pitch gespielt von ${instrument?.shortName}, $start ($startDynamic)"

    enum class Type(
        override val id: String,
        override val description: String
    ) : PitchedElement.Type<DiscretePitchedElement> {
        Staccato("stacc.", "Staccato"),
        Percussive("perc.", "Perkussiv (Streicher pizz., Bläser slap tongue)") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Cross
        },
        ColLegnoBattuto("c.b.t", "col legno battuto") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Triangle
        };

        override fun createElement(): DiscretePitchedElement = DiscretePitchedElement(this)
    }
}

class DiscreteNoise(override var type: Type) : AbstractElement() {
    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type = type
        return true
    }

    override fun toString(): String = "${type.id} gespielt von ${instrument?.shortName}, $start ($startDynamic)"

    enum class Type(override val id: String, override val description: String) : Element.Type<DiscreteNoise> {
        Bang("bang", "Schlag") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Rectangle
        };

        override fun createElement(): DiscreteNoise = DiscreteNoise(this)
    }
}

data class GraphicalScore(val elements: List<Element>) {
    fun encodeToString(): String {
        val array = JsonArray(elements.map { it.serialize() })
        return array.toString()
    }

    companion object {
        private fun decodeFromString(str: String): GraphicalScore = try {
            val array = Json.parseToJsonElement(str).jsonArray
            GraphicalScore(array.map { Element.deserialize(it) })
        } catch (ex: SerializationException) {
            throw CognoscoException("Invalid input file", ex)
        }

        fun load(file: File) = decodeFromString(file.readText())
    }
}