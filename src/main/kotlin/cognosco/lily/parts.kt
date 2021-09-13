package cognosco.lily

import cognosco.CognoscoException
import cognosco.Element

fun makeParts(elements: List<Element>, staffs: List<Staff>): Map<Staff, Part> {
    val parts = staffs.map { mutableListOf<Element>() }
    val partsByInstrument = staffs
        .withIndex().groupBy { (_, staff) -> staff.instrument }
        .mapValues { (_, staffs) -> staffs.map { (idx, _) -> parts[idx] } }
    for (element in elements.sortedBy { it.start.value }) {
        val instr = element.instrument.value!!
        val stave = partsByInstrument.getValue(instr).find { s -> s.canBeAdded(element) }
            ?: throw CognoscoException("${instr.fullName} hat bei Takt ${element.start.value / 2 + 1} zu viele Noten.")
        stave.add(element)
    }
    return parts.withIndex().associate { (idx, elems) -> staffs[idx] to Part(elems) }
}

private fun MutableList<Element>.canBeAdded(element: Element): Boolean =
    isEmpty() || last().end.value <= element.start.value