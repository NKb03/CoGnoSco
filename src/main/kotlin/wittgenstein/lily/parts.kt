package wittgenstein.lily

import wittgenstein.Element

fun makeParts(elements: List<Element>, staffs: List<Staff>): Map<Staff, Part> {
    val parts = staffs.map { mutableListOf<Element>() }
    val partsByInstrument = staffs
        .withIndex().groupBy { (_, staff) -> staff.instrument }
        .mapValues { (_, staffs) -> staffs.map { (idx, _) -> parts[idx] } }
    for (element in elements.sortedBy { it.start }) {
        val instr = element.instrument!!
        val stave = partsByInstrument.getValue(instr).find { s -> s.canBeAdded(element) }
            ?: throw TypesettingException("${instr.fullName} hat bei Takt ${element.start / 2 + 1} zu viele Noten.")
        stave.add(element)
    }
    return parts.withIndex().associate { (idx, elems) -> staffs[idx] to Part(elems) }
}

private fun MutableList<Element>.canBeAdded(element: Element): Boolean = isEmpty() || last().end <= element.start