package wittgenstein.gui

import javafx.scene.image.ImageView
import wittgenstein.Element

class NoteHead(val element: Element? = null, state: State = State.Regular) : ImageView(), ViewElement {
    constructor(state: State) : this(null, state)

    constructor(x: Double, y: Double, element: Element? = null, state: State = State.Regular) : this(element, state) {
        this.x = x
        this.y = y
    }

    init {
        image = loadImage(state.res)
        isFocusTraversable = true
        isPreserveRatio = true
        fitWidth = 20.0
    }

    override var isSelected: Boolean = false
        set(value) {
            field = value
            state = if (isSelected) State.Selected else State.Regular
            if (isSelected) requestFocus()
        }

    var state = state
        set(value) {
            field = value
            image = loadImage(value.res)
        }

    enum class State(val res: String) {
        Regular("notehead.png"),
        Phantom("notehead_gray.png"),
        InCreation("notehead_green.png"),
        Selected("notehead_blue.png")
    }
}