package cognosco

typealias Pit = Int
typealias Row = List<Int>

fun normalize(pitch: Pit) = (pitch + 24) % 12

fun Row.normalize(): Row = map(::normalize)

fun Row.transform(f: (Pit) -> Pit): Row = map(f).normalize()

fun Row.transpose(d: Int): Row = transform(d::plus)

fun Int.pow(x: Int): Int = (0 until x).fold(1) { acc, _ -> acc * this }

fun samePitDistances(a: Row, b: Row): List<Int> = (0..11).map { p: Pit ->
    (b.indexOf(p) + a.size - a.lastIndexOf(p) - 1)
}

data class Transposition(val row: Row, val position: Int, val delta: Int)

fun Row.allTranspositions(): List<Transposition> = indices.flatMap { pos ->
    (1..11).map { delta ->
        Transposition(this, pos, delta)
    }
}

fun Transposition.evaluate(): List<Int> {
    val a = row + row.take(position)
    val b = row.drop(position).transpose(delta) + row.transpose(delta)
    return samePitDistances(a, b)
}

fun <E> lexicalListComparator(elementComparator: Comparator<E>) = Comparator { l1: List<E>, l2: List<E> ->
    for ((a, b) in l1.zip(l2)) {
        val compare = elementComparator.compare(a, b)
        if (compare != 0) return@Comparator compare
    }
    l1.size.compareTo(l2.size)
}

fun Row.bestTranspositions(): List<Transposition> = allTranspositions()
    .sortedWith { t1, t2 ->
        lexicalListComparator(Comparator.naturalOrder<Int>()).compare(
            t2.evaluate().sorted(),
            t1.evaluate().sorted()
        )
    }

val r = listOf(11, 8, 5, 4, 0, 7, 6, 3, 8, 10, 1, 2, 9, 6, 0, 10, 11, 5, 2, 7, 1, 3, 4, 9)

fun main() {
    r.bestTranspositions().joinTo(System.out, "\n") { t ->
        val evaluation = t.evaluate().sorted()
        "transpose at ${t.position} by ${t.delta} -> evaluation: $evaluation"
    }
}
