package wittgenstein.gui

import javafx.scene.image.ImageView
import wittgenstein.Element

class NoteHead(val element: Element? = null, state: State = State.Regular) : ImageView(), ViewElement {
    constructor(state: State): this(null, state)

    constructor(x: Double, y: Double, element: Element? = null, state: State = State.Regular) : this(element, state) {
        this.x = x
        this.y = y
    }

    init {
        updateState(state)
        isPreserveRatio = true
        fitWidth = 20.0
    }

    override var isSelected: Boolean = false
        set(value) {
            field = value
            state = if (isSelected) State.Selected else State.Regular
        }

    var state = state
        set(value) {
            field = value
            updateState(value)
        }

    private fun updateState(value: State) {
        val res = when (value) {
            State.Regular -> "notehead.png"
            State.Phantom -> "notehead_gray.png"
            State.InCreation -> "notehead_green.png"
            State.Selected -> "notehead_blue.png"
        }
        image = loadImage(res)
    }

    enum class State {
        Regular,
        Phantom,
        InCreation,
        Selected
    }
}