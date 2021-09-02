package wittgenstein.gui

import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

object Shortcuts {
    val ESCAPE = KeyCombination.valueOf("ESCAPE")
    val DELETE = KeyCombination.valueOf("DELETE")
    val DOWN = KeyCombination.valueOf("CTRL+DOWN")
    val UP = KeyCombination.valueOf("CTRL+UP")
    val RIGHT = KeyCombination.valueOf("CTRL+RIGHT")
    val LEFT = KeyCombination.valueOf("CTRL+LEFT")
    val SELECT_TYPE = KeyCombination.valueOf("T")
    val SELECT_INSTRUMENT = KeyCombination.valueOf("I")
    val LOUDER = KeyCombination.valueOf("PLUS")
    val QUIETER = KeyCombination.valueOf("MINUS")
    val SHARP = KeyCombination.valueOf("S")
    val FLAT = KeyCombination.valueOf("F")
    val SELECT_BEND = KeyCombination.valueOf("B")
    val SELECT_FINE_TUNING = KeyCombination.valueOf("Ctrl+B")
    val NATURAL = KeyCombination.valueOf("N")
    val OPEN = KeyCombination.valueOf("Ctrl+O")
    val SAVE = KeyCombination.valueOf("Ctrl+S")
    val PLAY = KeyCombination.valueOf("Ctrl+SPACE")
    val TYPESET = KeyCombination.valueOf("Ctrl+P")

    private val shortcuts = listOf(
        DELETE, DOWN, UP, RIGHT, LEFT,
        SELECT_TYPE, SELECT_INSTRUMENT,
        LOUDER, QUIETER,
        SHARP, FLAT, NATURAL, SELECT_BEND, SELECT_FINE_TUNING,
        OPEN, SAVE, PLAY, TYPESET
    )

    fun getShortcut(ev: KeyEvent): KeyCombination? {
        if (ev.code == KeyCode.ESCAPE) return ESCAPE
        return shortcuts.find { it.match(ev) }
    }

    fun listen(target: Node, block: (shortcut: KeyCombination) -> Unit) =
        target.addEventFilter(KeyEvent.KEY_RELEASED) { ev ->
            val shortcut = getShortcut(ev) ?: return@addEventFilter
            block(shortcut)
        }
}