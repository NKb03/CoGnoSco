package cognosco.gui

import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

sealed class Shortcut(private vararg val shortcuts: String) {
    private val keyCombinations = shortcuts.map { KeyCombination.valueOf(it)!! }

    override fun toString(): String = shortcuts.joinToString(" | ")

    object Escape : Shortcut("ESCAPE")
    object Delete : Shortcut("DELETE")
    object Enter : Shortcut("ENTER")
    object Down : Shortcut("Ctrl+DOWN")
    object Up : Shortcut("Ctrl+UP")
    object Right : Shortcut("Ctrl+RIGHT")
    object Left : Shortcut("Ctrl+LEFT")
    object SelectType : Shortcut("T")
    object SelectInstrument : Shortcut("I")
    object SelectDynamic : Shortcut("D")
    object SelectBend : Shortcut("B")
    object Louder : Shortcut("PLUS")
    object Quieter : Shortcut("MINUS")
    object Sharp : Shortcut("S")
    object Flat : Shortcut("F")
    object Natural : Shortcut("N")
    object Save : Shortcut("Ctrl+S")
    object New : Shortcut("Ctrl+N")
    object Play : Shortcut("SPACE", "PLAY", "PAUSE")
    object Stop : Shortcut("STOP")
    object Typeset : Shortcut("Ctrl+P")
    object Open : Shortcut("Ctrl+O")
    data class Digit(val value: Int) : Shortcut("$value")

    companion object {
        private val shortcuts = listOfNotNull(
            Delete, Down, Up, Right, Left, Enter,
            SelectType, SelectInstrument, SelectDynamic, SelectBend,
            Louder, Quieter,
            Sharp, Flat, Natural,
            Open, Save, New, Play, Typeset
        )

        fun getShortcut(ev: KeyEvent): Shortcut? {
            if (ev.code == KeyCode.ESCAPE) return Escape
            else if (ev.code.isDigitKey) {
                val value = ev.code.toString().removePrefix("DIGIT").toInt()
                return Digit(value)
            }
            return shortcuts.find { s -> s.keyCombinations.any { comb -> comb.match(ev) } }
        }

        fun listen(target: Scene, block: (shortcut: Shortcut) -> Unit) =
            target.addEventFilter(KeyEvent.KEY_RELEASED) { ev ->
                val shortcut = getShortcut(ev) ?: return@addEventFilter
                block(shortcut)
                ev.consume()
            }
    }

}