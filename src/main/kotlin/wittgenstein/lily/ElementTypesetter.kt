package wittgenstein.lily

import wittgenstein.Dynamic
import wittgenstein.Time
import wittgenstein.Pitch

class ElementTypesetter(private val writer: LilypondWriter) : LilypondWriter by writer {
    private var techniqueIsNew = false

    var lastDynamic: Dynamic? = null

    private var currentTime: Time = 0

    var technique: Technique = Technique.Ordinario
        set(value) {
            if (value != field) {
                +field.afterLast
                techniqueIsNew = true
                field = value
            }
        }


    fun magnifyMusic(scale: Double) {
        +"\\magnifyMusic $scale"
    }

    fun override(property: String, value: String) {
        +"\\override $property=$value"
    }

    fun addRest(rest: Int, endDynamic: Boolean = false, hide: Boolean = false) {
        if (rest == 0) return
        var r = rest
        val h = if (hide) "\\hide " else ""
        appendLine()
        if (currentTime % 2 != 0) {
            append("${h}r8")
            r -= 1
        }
        if (currentTime != 0) { append("|") }
        if (r / 2 > 0) {
            if (endDynamic && r % 2 == 0) append(" ${h}R4 * ${r / 2 - 1} | ${h}R4")
            else append("${h}R4 * ${r / 2} ")
            r -= (r - r % 2)
        }
        if (r == 1) {
            append(" | ${h}r8")
            r -= 1
        }
        check(r == 0)
        currentTime += rest
    }

    fun addRestTo(to: Time, endDynamic: Boolean = false, hide: Boolean = false) {
        addRest(to - currentTime, endDynamic, hide)
        currentTime = to
    }

    fun addNote(pitch: Pitch, type: String = "8", duration: Time = 1) {
        appendLine()
        if (duration != 0 && currentTime != 0 && currentTime % 2 == 0) append("| ")
        if (techniqueIsNew || technique.everyNote) append(technique.beforeNote)
        append(pitch.lilypond())
        append(type)
        if (techniqueIsNew || technique.everyNote) append(technique.afterNote)
        currentTime += duration
    }

    fun addDynamic(dynamic: Dynamic, force: Boolean = false) {
        if (dynamic != lastDynamic || force) {
            append(dynamic.lilypond())
            lastDynamic = dynamic
        }
    }

    fun addCrescendo() = +"\\<"

    fun addDecrescendo() = +"\\>"

    operator fun invoke(block: ElementTypesetter.() -> Unit) {
        "" { block() }
    }

    override fun toString(): String = "ElementTypesetter [ currentTime = $currentTime ]"
}