package cognosco.gen

fun <E, F> List<E>.zipWithNext(n: Int, f: (List<E>) -> F): List<F> =
    indices.drop(n + 1).map { i -> f(drop(i - n - 1).take(n + 1)) }

fun main() {
    println((0..10).toList().zipWithNext(2) { (a, b, c) -> a + b + c })
}