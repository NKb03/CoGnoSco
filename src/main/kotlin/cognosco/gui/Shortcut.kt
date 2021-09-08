package cognosco.gui

import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

sealed class Shortcut(private val str: String) {
    private val keyCombination = KeyCombination.keyCombination(str)!!

    override fun toString(): String = str

    object Escape : Shortcut("ESCAPE")
    object Delete : Shortcut("DELETE")
    object Enter : Shortcut("ENTER")
    object Down : Shortcut("DOWN")
    object Up : Shortcut("UP")
    object Right : Shortcut("RIGHT")
    object Left : Shortcut("LEFT")
    object SelectType : Shortcut("T")
    object SelectInstrument : Shortcut("I")
    object SelectDynamic : Shortcut("D")
    object SelectBend : Shortcut("B")
    object Louder : Shortcut("PLUS")
    object Quieter : Shortcut("MINUS")
    object Sharp : Shortcut("S")
    object Flat : Shortcut("F")
    object Natural : Shortcut("N")
    object Open : Shortcut("Ctrl+O")
    object Save : Shortcut("Ctrl+S")
    object New : Shortcut("Ctrl+N")
    object Play : Shortcut("SPACE")
    object Typeset : Shortcut("Ctrl+P")
    data class Digit(val value: Int) : Shortcut("$value")

    companion object {
        private val shortcuts = listOf(
            Delete, Down, Up, Right, Left, Enter,
            SelectType, SelectInstrument, SelectDynamic, SelectBend,
            Louder, Quieter,
            Sharp, Flat, Natural,
            Save, New, Play, Typeset
        )

        fun getShortcut(ev: KeyEvent): Shortcut? {
            if (ev.code == KeyCode.ESCAPE) return Escape
            else if (ev.code.isDigitKey) {
                val value = ev.code.toString().removePrefix("DIGIT").toInt()
                return Digit(value)
            }
            return shortcuts.find { it.keyCombination.match(ev) }
        }

        fun listen(target: Scene, block: (shortcut: Shortcut) -> Unit) =
            target.addEventFilter(KeyEvent.KEY_RELEASED) { ev ->
                val shortcut = getShortcut(ev) ?: return@addEventFilter
                block(shortcut)
                ev.consume()
            }
    }

}