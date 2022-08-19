package cognosco.gen

interface Development<out T> {
    fun at(time: Double): T
}