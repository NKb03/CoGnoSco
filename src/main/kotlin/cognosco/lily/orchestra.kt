package cognosco.lily

import cognosco.Element
import cognosco.Instrument
import cognosco.InstrumentFamily
import cognosco.Time

data class Score(val orchestra: Orchestra, val parts: Map<Staff, Part>, val duration: Time)

val Orchestra.staffs get() = groups.flatMap { it.staffs }

data class Orchestra(val groups: List<InstrumentGroup>)

data class InstrumentGroup(val family: InstrumentFamily, val staffs: List<Staff>)

data class Staff(val instrument: Instrument, val index: String) {
    val fullName get() = "${instrument.fullName}$index"
    val shortName get() = "${instrument.shortName}$index"
}

data class Part(val elements: List<Element>)

private fun orchestra(vararg groups: InstrumentGroup) = Orchestra(groups.asList())

private fun group(family: InstrumentFamily, vararg staffs: Staff) = InstrumentGroup(family, staffs.asList())

private fun first(instrument: Instrument) = Staff(instrument, " 1")

private fun second(instrument: Instrument) = Staff(instrument, " 2")

private fun staff(instrument: Instrument) = Staff(instrument, "")

val myOrchestra = orchestra(
    group(
        InstrumentFamily.Woodwinds,
        first(Instrument.Flute), second(Instrument.Flute),
        first(Instrument.Oboe), second(Instrument.Oboe),
        first(Instrument.Clarinet), second(Instrument.Clarinet),
        staff(Instrument.Saxophone)
    ),
    group(
        InstrumentFamily.Brass,
        Staff(Instrument.Horn, " 1 & 3"), Staff(Instrument.Horn, " 2 & 4"),
        first(Instrument.Trumpet), second(Instrument.Trumpet),
        staff(Instrument.Trombone),
        staff(Instrument.Tuba)
    ),
    group(InstrumentFamily.Timpani, staff(Instrument.Timpani)),
    group(
        InstrumentFamily.Percussion,
        staff(Instrument.SnareDrum),
        staff(Instrument.BassDrum),
        staff(Instrument.Cymbal)
    ),
    group(
        InstrumentFamily.Strings,
        first(Instrument.Violins), second(Instrument.Violins),
        staff(Instrument.Violas),
        first(Instrument.Violoncelli), second(Instrument.Violoncelli),
        staff(Instrument.Contrabasses)
    )
)