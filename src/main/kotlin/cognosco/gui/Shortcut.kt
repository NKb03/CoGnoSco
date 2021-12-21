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
    object Confirm : Shortcut("ENTER")
    object MoveDown : Shortcut("Ctrl+DOWN")
    object MoveUp : Shortcut("Ctrl+UP")
    object MoveRight : Shortcut("Ctrl+RIGHT")
    object MoveLeft : Shortcut("Ctrl+LEFT")
    object MoveElementLeft : Shortcut("Ctrl+Shift+LEFT")
    object MoveElementRight : Shortcut("Ctrl+Shift+RIGHT")
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
    object ZoomIn : Shortcut("Ctrl+PLUS")
    object ZoomOut : Shortcut("Ctrl+MINUS")
    data class Digit(val value: Int) : Shortcut("$value")

    companion object {
        private val shortcuts = listOfNotNull(
            Delete, Confirm,
            MoveDown, MoveUp, MoveRight, MoveLeft, MoveElementRight, MoveElementLeft,
            SelectType, SelectInstrument, SelectDynamic, SelectBend,
            Louder, Quieter,
            Sharp, Flat, Natural,
            Open, Save, New, Play, Typeset,
            ZoomIn, ZoomOut
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