@file: OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package cognosco

import cognosco.Element.Companion.p
import cognosco.InstrumentFamily.*
import cognosco.gui.impl.flatMap
import cognosco.gui.impl.map
import cognosco.gui.impl.zipWithBy
import cognosco.lily.lilypond
import javafx.beans.property.DoubleProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
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
import kotlin.reflect.KProperty0
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
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
    val program: Int = 0,
    val bank: Int = 0,
    val percussionKey: Int = -1
) {
    Flute("Flöte", "Fl.", Woodwinds, Clef.Violin, "c'", 74, 128),
    Oboe("Oboe", "Ob.", Woodwinds, Clef.Violin, "c'", 69),
    Clarinet("B♭ Klarinette", "B♭ Kl.", Woodwinds, Clef.Violin, "bf", 72),
    Saxophone("Sopransaxophon", "S.sax.", Woodwinds, Clef.Violin, "bf", 65),
    Horn("F Horn", "F Hn.", Woodwinds, Clef.Bass, "f", 61),
    Trumpet("B♭ Trompete", "B♭ Tpt.", Brass, Clef.Violin, "bf", 57),
    Trombone("Posaune", "Pos.", Brass, Clef.Bass, "c'", 58),
    Tuba("Tuba", "Tba.", Brass, Clef.Bass, "c'", 59),
    Timpani("Pauke", "Pk.", InstrumentFamily.Timpani, Clef.Bass, "c'", 48),
    SnareDrum("Snare Drum", "Sn.Dr.", Percussion, Clef.Percussion, "", percussionKey = 38),
    BassDrum("Bass Drum", "B.Dr.", Percussion, Clef.Percussion, "", percussionKey = 36),
    Cymbal("Becken", "Bck.", Percussion, Clef.Percussion, "", percussionKey = 49),
    Violins("Violinen", "Vl.", Strings, Clef.Violin, "c'", 49),
    Violas("Viola", "Vla.", Strings, Clef.Alto, "c'", 49),
    Violoncellos("Violoncello", "Vc.", Strings, Clef.Bass, "c'", 49),
    Contrabasses("Kontrabass", "Kb.", Strings, Clef.Bass, "c", 49)
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
            val bend = when (str.drop(1)) {
                "u" -> +13
                "uu" -> +31
                "d" -> -13
                "dd" -> -31
                else -> +-0
            }
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
    PPP(3), PP(10), P(20), MP(30), MF(55), F(80), FF(104), FFF(127);

    override fun toString(): String = name.lowercase()
}

typealias Time = Int

enum class NoteHeadType {
    Regular, Triangle, Rectangle, Slashed, Rhombus, Cross;
}

sealed interface Element {
    val type: ObservableValue<out Type<Element>>
    val instrument: Property<Instrument?>
    val start: Property<Time>
    val startDynamic: Property<Dynamic>
    val customY: DoubleProperty?
    val end: ObservableValue<out Time>
    val pitch: ObservableValue<out Pitch?> get() = SimpleObjectProperty(null)

    fun setType(type: Type<*>): Boolean

    fun properties(): List<Prop> = listOf(::instrument.p, ::start.p, ::startDynamic.p)

    fun copyFrom(original: Element) {
        for ((p1, p2) in properties().zipWithBy(original.properties()) { it.name }) {
            p1.reactive.value = p2.reactive.value
        }
    }

    fun serialize(): JsonElement = buildJsonObject {
        val element = this@Element
        put("type", JsonPrimitive(element.type.value.id))
        for (prop in properties()) {
            val v = prop.reactive.value
            if (v == null || v == 0 || v == 0.0) continue
            val serializer = serializer(prop.type)
            val encoded = if (v is Enum<*>) JsonPrimitive(v.name) else json.encodeToJsonElement(serializer, v)
            put(prop.name, encoded)
        }
    }

    fun deserialize(obj: JsonObject) {
        for (prop in properties()) {
            val value = obj[prop.name] ?: continue
            val serializer = serializer(prop.type)
            val decoded = json.decodeFromJsonElement(serializer, value)
            if (decoded == null || decoded == 0 || decoded == 0.0) continue
            prop.reactive.value = decoded
        }
    }

    data class Prop(val name: String, val type: KType, val reactive: Property<Any?>)

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

        @Suppress("UNCHECKED_CAST")
        @OptIn(ExperimentalStdlibApi::class)
        inline val <reified T> KProperty0<Property<T>>.p
            get() = Prop(name, typeOf<T>(), get() as Property<Any?>)

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
    override val type: ObservableValue<out Type<PitchedElement>>
    override val pitch: Property<Pitch>

    override fun properties(): List<Element.Prop> = super.properties() + ::pitch.p

    sealed interface Type<out E : PitchedElement> : Element.Type<E>
}

sealed class AbstractElement : Element {
    override val start: Property<Time> = SimpleObjectProperty(0)
    override val startDynamic: Property<Dynamic> = SimpleObjectProperty(Dynamic.MF)
    override val instrument: Property<Instrument?> = SimpleObjectProperty(null)
    override val customY: DoubleProperty? get() = null

    override fun toString(): String = "$type: $start ($startDynamic)"
}

data class PropertySerializer<T : Any>(private val valueSerializer: KSerializer<T>) : KSerializer<Property<T>> {
    override val descriptor: SerialDescriptor
        get() = valueSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Property<T>) {
        encoder.encodeSerializableValue(valueSerializer, value.value)
    }

    override fun deserialize(decoder: Decoder): Property<T> {
        val prop = SimpleObjectProperty<T>()
        prop.value = decoder.decodeSerializableValue(valueSerializer)
        return prop
    }
}

@Serializable
class ElementPhase(
    @Serializable(with = PropertySerializer::class) val end: Property<Time>,
    @Serializable(with = PropertySerializer::class) val targetPitch: Property<Pitch>,
    @Serializable(with = PropertySerializer::class) val targetDynamic: Property<Dynamic>
)

sealed interface ContinuousElement : Element {
    abstract override val type: ObservableValue<out Type<ContinuousElement>>

    val phases: Property<List<ElementPhase>>

    override fun properties(): List<Element.Prop> = super.properties() + ::phases.p

    sealed interface Type<out E : ContinuousElement> : Element.Type<E> {
        val strokeDashArray: List<Double> get() = emptyList()
    }
}

sealed class AbstractContinuousElement : ContinuousElement, AbstractElement() {
    final override val phases: Property<List<ElementPhase>> = SimpleObjectProperty(emptyList())

    override val end: ObservableValue<out Time> =
        phases.flatMap { phases -> phases.lastOrNull()?.end ?: SimpleObjectProperty(0) }

    override fun copyFrom(original: Element) {
        super<ContinuousElement>.copyFrom(original)
    }
}

sealed class PitchedContinuousElement : ContinuousElement, PitchedElement, AbstractContinuousElement() {
    abstract override val type: ObservableValue<out Type<PitchedContinuousElement>>
    override val pitch: Property<Pitch> = SimpleObjectProperty(Pitch(0, PitchName.C, RegularAccidental.Natural))

    override fun properties(): List<Element.Prop> = super<AbstractContinuousElement>.properties() + ::pitch.p

    sealed interface Type<out E : PitchedContinuousElement> : PitchedElement.Type<E>, ContinuousElement.Type<E>
}

class SimplePitchedContinuousElement(
    override val type: Property<Type>,
) : PitchedContinuousElement() {
    override fun toString(): String =
        "${type.value.id} ${pitch.value} (${instrument.value?.shortName}), ${start.value}, (${startDynamic.value})"

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type.value = type
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

        override fun createElement(): SimplePitchedContinuousElement =
            SimplePitchedContinuousElement(SimpleObjectProperty(this))
    }
}

class Trill : PitchedContinuousElement() {
    val secondaryPitch: Property<Pitch> = SimpleObjectProperty(Pitch(0, PitchName.C, RegularAccidental.Natural))

    override fun properties(): List<Element.Prop> = super.properties() + ::secondaryPitch.p

    override fun toString(): String =
        "tr $pitch ($secondaryPitch) gespielt von ${instrument.value?.shortName}, ${start.value}, (${startDynamic.value})"

    override fun setType(type: Element.Type<*>): Boolean = false

    override val type: Property<Type<PitchedContinuousElement>>
        get() = SimpleObjectProperty(Trill)

    companion object : Type<Trill> {
        override val id: String
            get() = "tr"

        override val description: String
            get() = "Triller"

        override fun createElement(): Trill = Trill()

        override fun toString(): String = "Trill"
    }
}

open class ContinuousNoise(override var type: Property<Type>) : ContinuousElement, AbstractContinuousElement() {
    override val customY: DoubleProperty = SimpleDoubleProperty()

    override fun properties(): List<Element.Prop> = super<AbstractContinuousElement>.properties() + ::customY.p

    override fun toString(): String =
        "${type.value.id} (${instrument.value?.shortName}), ${start.value} (${startDynamic.value})"

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type.value = type
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

        override fun createElement(): ContinuousNoise = ContinuousNoise(SimpleObjectProperty(this))
    }
}

class DiscretePitchedElement(override val type: Property<Type>) : AbstractElement(),
    PitchedElement {
    override val pitch: Property<Pitch> = SimpleObjectProperty(Pitch(0, PitchName.C, RegularAccidental.Natural))

    override val end: ObservableValue<out Time> = start.map { t -> t + 1 }

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type.value = type
        return true
    }

    override fun toString(): String =
        "${type.value.id}: $pitch gespielt von ${instrument.value?.shortName}, ${start.value} (${startDynamic.value})"

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

        override fun createElement(): DiscretePitchedElement = DiscretePitchedElement(SimpleObjectProperty(this))
    }
}

class DiscreteNoise(override val type: Property<Type>) : AbstractElement() {
    override val customY: DoubleProperty = SimpleDoubleProperty()

    override fun properties(): List<Element.Prop> = super.properties() + ::customY.p

    override fun setType(type: Element.Type<*>): Boolean {
        if (type !is Type) return false
        this.type.value = type
        return true
    }

    override val end: ObservableValue<out Time> = start.map { t -> t + 1 }

    override fun toString(): String =
        "${type.value.id} gespielt von ${instrument.value?.shortName}, ${start.value} (${startDynamic.value})"

    enum class Type(override val id: String, override val description: String) : Element.Type<DiscreteNoise> {
        Bang("bang", "Schlag") {
            override val noteHeadType: NoteHeadType
                get() = NoteHeadType.Rectangle
        };

        override fun createElement(): DiscreteNoise = DiscreteNoise(SimpleObjectProperty(this))
    }
}

data class GraphicalScore(val elements: List<Element>) {
    fun encodeToString(): String {
        val array = JsonArray(elements.map { it.serialize() })
        return json.encodeToString(array)
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