package wittgenstein.gui

import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

object Shortcuts {
    val ESCAPE: KeyCombination = KeyCombination.valueOf("ESCAPE")
    val DELETE: KeyCombination = KeyCombination.valueOf("DELETE")
    val DOWN: KeyCombination = KeyCombination.valueOf("CTRL+DOWN")
    val UP: KeyCombination = KeyCombination.valueOf("CTRL+UP")
    val RIGHT: KeyCombination = KeyCombination.valueOf("CTRL+RIGHT")
    val LEFT: KeyCombination = KeyCombination.valueOf("CTRL+LEFT")
    val SELECT_TYPE: KeyCombination = KeyCombination.valueOf("T")
    val SELECT_INSTRUMENT: KeyCombination = KeyCombination.valueOf("I")
    val LOUDER: KeyCombination = KeyCombination.valueOf("PLUS")
    val QUIETER: KeyCombination = KeyCombination.valueOf("MINUS")
    val SHARP: KeyCombination = KeyCombination.valueOf("S")
    val FLAT: KeyCombination = KeyCombination.valueOf("F")
    val SELECT_BEND: KeyCombination = KeyCombination.valueOf("B")
    val NATURAL: KeyCombination = KeyCombination.valueOf("N")
    val OPEN: KeyCombination = KeyCombination.valueOf("Ctrl+O")
    val SAVE: KeyCombination = KeyCombination.valueOf("Ctrl+S")
    val PLAY: KeyCombination = KeyCombination.valueOf("Ctrl+SPACE")
    val TYPESET: KeyCombination = KeyCombination.valueOf("Ctrl+P")

    private val shortcuts = listOf(
        DELETE, DOWN, UP, RIGHT, LEFT,
        SELECT_TYPE, SELECT_INSTRUMENT,
        LOUDER, QUIETER,
        SHARP, FLAT, NATURAL, SELECT_BEND,
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