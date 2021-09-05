package wittgenstein.lily

import wittgenstein.*
import java.io.File

private fun Accidental.lilypond(): String = when (this) {
    RegularAccidental.Natural -> ""
    RegularAccidental.Flat -> "es"
    RegularAccidental.Sharp -> "is"
    QuarterToneAccidental.QuarterFlat -> "eh"
    QuarterToneAccidental.QuarterSharp -> "ih"
    QuarterToneAccidental.TreeQuarterFlat -> "eseh"
    QuarterToneAccidental.TreeQuarterSharp -> "isih"
    is BendedAccidental -> TODO()
}

private fun PitchName.lilypond(): String = name.lowercase()

fun Pitch.lilypond(): String {
    val apostrophes = "'".repeat((register - 3).coerceAtLeast(0))
    val commas = ",".repeat((3 - register).coerceAtLeast(0))
    return "${name.lilypond()}${accidental.lilypond()}$apostrophes$commas!"
}

fun Dynamic.lilypond(): String = "\\${name.lowercase()}"

private fun Clef.lilypond() = name.lowercase()

private val String.q get() = "\"$this\""

private fun getTechnique(type: Element.Type<Element>, instrument: Instrument) = when (type) {
    DiscretePitchedElement.Percussive ->
        if (instrument.family == InstrumentFamily.Strings) Technique.Pizzicato else Technique.SlapTongue
    DiscretePitchedElement.Staccato -> Technique.Staccato
    DiscretePitchedElement.ColLegnoBattuto -> Technique.ColLegnoBattuto
    Trill -> Technique.Ordinario
    SimplePitchedContinuousElement.Noisy ->
        if (instrument.family == InstrumentFamily.Strings) Technique.ColLegnoTratto else Technique.Noisy
    SimplePitchedContinuousElement.FastRepeat ->
        if (instrument.family == InstrumentFamily.Strings) Technique.Ordinario else Technique.FlutterTongue
    SimplePitchedContinuousElement.Repeat -> Technique.Ordinario
    SimplePitchedContinuousElement.Regular -> Technique.Ordinario
    ContinuousNoise.Breath -> TODO()
    ContinuousNoise.DrumRoll -> Technique.DrumRoll
    DiscreteNoise.Bang -> Technique.Bang
}

private fun ElementTypesetter.typesetElement(element: Element, instrument: Instrument) {
    technique = getTechnique(element.type, instrument)
    val pitch = if (element is PitchedElement) element.pitch else instrument.clef.middleLinePitch!!
     if (element is Trill) {
        addNote(pitch, "16")
        append("\\trill")
        addDynamic(element.startDynamic!!)
        magnifyMusic(0.6)
        +"\\parenthesize"
        addNote(element.secondaryPitch!!, "16", duration = 0)
    } else {
        val type = if (element is SimplePitchedContinuousElement) {
            when (element.type) {
                SimplePitchedContinuousElement.FastRepeat -> {
                    +"\\repeat tremolo 8"
                    "64"
                }
                SimplePitchedContinuousElement.Repeat -> {
                    +"\\repeat tremolo 4"
                    "32"
                }
                else -> "8"
            }
        } else "8"
        addNote(pitch, type)
        addDynamic(element.startDynamic!!)
    }
    if (element is ContinuousElement) {
        addCrescendo()
        addRestTo(element.climax, endDynamic = true, hide = true)
        addDynamic(element.climaxDynamic!!, force = true)
        addDecrescendo()
        addRestTo(element.end, endDynamic = true, hide = true)
        addDynamic(element.endDynamic!!, force = true)
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

private fun LilypondWriter.typesetGroup(group: InstrumentGroup) {
    new("StaffGroup") {
        for ((instr, staffs) in group.staffs.groupBy { it.instrument }) {
            new("StaffGroup \\with { systemStartDelimiter = #'SystemStartSquare }") {
                for (staff in staffs) {
                    val staffType = if (instr.family == InstrumentFamily.Percussion) "RhythmicStaff" else "Staff"
                    val staffInfo = "instrumentName = ${staff.fullName.q} shortInstrumentName=${staff.shortName.q}"
                    new("$staffType \\with { $staffInfo }") {
                        +"\\${staff.partName()}"
                    }
                }
            }
        }
    }
}

private fun LilypondWriter.typesetPart(staff: Staff, part: Part, score: Score) = (ElementTypesetter(this)) {
    +"\\clef ${staff.instrument.clef.lilypond()}"
    for (element in part.elements) {
        val start = element.start
        addRestTo(start)
        typesetElement(element, staff.instrument)
    }
    addRestTo(score.duration)
}

private fun LilypondWriter.typesetScore(score: Score) {
    include("preamble.ily")
    for (staff in score.orchestra.staffs) {
        val part = score.parts.getValue(staff)
        val name = staff.partName()
        append("$name = ")
        typesetPart(staff, part, score)
    }
    "\\score" {
        new("GrandStaff") {
            +"\\time 1/4"
            for (group in score.orchestra.groups) {
                typesetGroup(group)
            }
        }
    }
}

private fun Appendable.typeset(score: GraphicalScore, orchestra: Orchestra) {
    val writer = LilypondWriterImpl(this)
    val parts = makeParts(score.elements, orchestra.staffs)
    val duration = score.elements.maxOf { it.end }
    writer.typesetScore(Score(orchestra, parts, duration))
}

fun typeset(score: GraphicalScore, file: File) {
    file.bufferedWriter().use { it.typeset(score, myOrchestra) }
}