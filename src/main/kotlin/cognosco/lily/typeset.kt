package cognosco.lily

import cognosco.*
import java.io.File

private fun PitchName.lilypond(): String = name.lowercase()

fun Accidental.lilypond(): String = when (this) {
    RegularAccidental.Natural -> "n"
    RegularAccidental.Flat -> "f"
    RegularAccidental.Sharp -> "s"
    QuarterToneAccidental.QuarterFlat -> "qf"
    QuarterToneAccidental.QuarterSharp -> "qs"
    QuarterToneAccidental.TreeQuarterFlat -> "tqf"
    QuarterToneAccidental.TreeQuarterSharp -> "tqs"
    is BendedAccidental -> {
        val suffix = when (bend) {
            in -39..-25 -> "D"
            in -24..-6 -> "d"
            in -5..+5 -> ""
            in 6..24 -> "u"
            in 25..39 -> "U"
            else -> error("pitch bend out of range: $bend")
        }
        "${reference.lilypond()}$suffix"
    }
}

fun Pitch.lilypond(): String {
    val apostrophes = "'".repeat((register - 3).coerceAtLeast(0))
    val commas = ",".repeat((3 - register).coerceAtLeast(0))
    return "${name.lilypond()}${accidental.lilypond()}$apostrophes$commas!"
}

fun Dynamic.lilypond(): String = "\\${name.lowercase()}"

private fun Clef.lilypond() = name.lowercase()

private val String.q get() = "\"$this\""

private fun getTechnique(type: Element.Type<Element>, instrument: Instrument) = when (type) {
    DiscretePitchedElement.Type.Percussive ->
        if (instrument.family == InstrumentFamily.Strings) Technique.Pizzicato else Technique.SlapTongue
    DiscretePitchedElement.Type.Staccato -> Technique.Staccato
    DiscretePitchedElement.Type.ColLegnoBattuto -> Technique.ColLegnoBattuto
    Trill -> Technique.Ordinario
    SimplePitchedContinuousElement.Type.Noisy ->
        if (instrument.family == InstrumentFamily.Strings) Technique.ColLegnoTratto else Technique.Noisy
    SimplePitchedContinuousElement.Type.FastRepeat ->
        if (instrument.family == InstrumentFamily.Strings) Technique.Ordinario else Technique.FlutterTongue
    SimplePitchedContinuousElement.Type.Repeat -> Technique.Ordinario
    SimplePitchedContinuousElement.Type.Regular -> Technique.Ordinario
    ContinuousNoise.Type.Breath -> TODO()
    ContinuousNoise.Type.DrumRoll -> Technique.DrumRoll
    DiscreteNoise.Type.Bang -> Technique.Bang
}

private fun ElementTypesetter.typesetElement(element: Element, instrument: Instrument) {
    technique = getTechnique(element.type.value, instrument)
    val pitch = if (element is PitchedElement) element.pitch.value else instrument.clef.middleLinePitch!!
    if (element is Trill) {
        addNote(pitch, "16")
        append("\\trill")
        addDynamic(element.startDynamic.value)
        magnifyMusic(0.6)
        +"\\parenthesize"
        addNote(element.secondaryPitch.value, "16", duration = 0)
    } else {
        val type = if (element is SimplePitchedContinuousElement) {
            when (element.type.value) {
                SimplePitchedContinuousElement.Type.FastRepeat -> {
                    +"\\repeat tremolo 8"
                    "64"
                }
                SimplePitchedContinuousElement.Type.Repeat -> {
                    +"\\repeat tremolo 4"
                    "32"
                }
                else -> "8"
            }
        } else "8"
        addNote(pitch, type)
        addDynamic(element.startDynamic.value)
    }
    if (element is ContinuousElement) {
        var lastDynamic = element.startDynamic.value
        for (phase in element.phases.value) {
            if (phase.targetDynamic.value > lastDynamic) addCrescendo() else addDecrescendo()
            addRestTo(phase.end.value, hide = true)
            addRest(1, hide = true)
            addDynamic(phase.targetDynamic.value, force = true)
            lastDynamic = phase.targetDynamic.value
        }
    }
}

private fun Staff.partName(): String {
    val idx = index.replace(" ", "")
        .replace("1", "One")
        .replace("2", "Two")
        .replace("&3", "Three")
        .replace("&4", "Four")
    return "${instrument.name.lowercase()}${idx}Music"
}

private fun LilypondWriter.typesetPart(staff: Staff, part: Part, score: Score) = with(ElementTypesetter(this)) {
    val instr = staff.instrument
    val transpose =
        if (instr.family != InstrumentFamily.Percussion && instr.transposition != "c'")
            "\\transpose ${instr.transposition} c'"
        else ""
    transpose {
        +"\\clef ${instr.clef.lilypond()}"
        for (element in part.elements) {
            val start = element.start
            addRestTo(start.value)
            typesetElement(element, instr)
        }
        addRestTo(score.duration)
    }
}

private fun LilypondWriter.typesetScore(score: Score) {
    +"\\version \"2.22.1\""
    includePath(copyFromResources("preamble.ily"))
    includePath(copyFromResources("accidentals.ily"))
    includePath(copyFromResources("ekmel-main.ily"))
    for (staff in score.orchestra.staffs) {
        val part = score.parts.getValue(staff)
        val name = staff.partName()
        +"$name = "
        typesetPart(staff, part, score)
    }
    includePath(copyFromResources("score.ily"))
}

private fun copyFromResources(name: String): String {
    val file = Files.ily.resolve(name)
    if (!file.exists()) {
        val res = LilypondWriter::class.java.getResource(name) ?: error("Cannot find resource $name")
        file.writeText(res.readText())
    }
    return file.absolutePath
}

private fun Appendable.typeset(score: GraphicalScore, orchestra: Orchestra) {
    val writer = LilypondWriterImpl(this)
    val parts = makeParts(score.elements, orchestra.staffs)
    val duration = score.elements.maxOf { it.end.value + 1 }
    writer.typesetScore(Score(orchestra, parts, duration))
}

fun typeset(score: GraphicalScore, file: File) {
    file.bufferedWriter().use { it.typeset(score, myOrchestra) }
}