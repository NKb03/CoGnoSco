package wittgenstein.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.input.KeyCombination
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import wittgenstein.*
import wittgenstein.gui.Shortcuts.DELETE
import wittgenstein.gui.Shortcuts.DOWN
import wittgenstein.gui.Shortcuts.ENTER
import wittgenstein.gui.Shortcuts.LEFT
import wittgenstein.gui.Shortcuts.RIGHT
import wittgenstein.gui.Shortcuts.UP
import wittgenstein.gui.impl.*
import wittgenstein.midi.PULSES_PER_BEAT
import wittgenstein.midi.Pulsator
import kotlin.properties.Delegates

class ScoreView(
    private val elementTypeSelector: ElementTypeSelector,
    private val accidentalSelector: AccidentalSelector,
    private val instrumentSelector: InstrumentSelector,
    private val dynamicsSelector: DynamicSelector
) : Pane() {
    private val elements = mutableListOf<Element>()
    private val associatedNodes = mutableMapOf<Element, MutableList<Node>>()
    private val noteHeads = mutableMapOf<Element, NoteHead>()
    private val accidentalViews = mutableMapOf<Element, AccidentalView>()
    private val trillAccidentalViews = mutableMapOf<Trill, AccidentalView>()
    private val startDynamics = mutableMapOf<Element, DynamicView>()
    private val pulseLine = createPulseLine()

    private var disposition: Disposition by Delegates.observable(Pointer()) { _, old, new ->
        old.replaced(new)
        new.init(old)
    }

    init {
        prefHeight = H
        addVerticalLines()
        addHorizontalLines()
        trackMouse()
        listenForSelectorChanges()
    }

    private fun listenForSelectorChanges() {
        elementTypeSelector.selected.addListener { _, _, new ->
            when {
                new == null -> disposition = Pointer()
                disposition !is CreateElement -> disposition = CreateElement()
                else -> disposition.elementTypeChanged(new)
            }
        }
        accidentalSelector.selected.addListener { _, _, acc -> if (acc != null) disposition.accidentalChanged(acc) }
        instrumentSelector.selected.addListener { _, _, instr ->
            if (instr != null) disposition.instrumentChanged(instr)
        }
        dynamicsSelector.selected.addListener { _, _, dyn -> if (dyn != null) disposition.dynamicChanged(dyn) }
    }

    private fun trackMouse() {
        setOnMouseMoved { ev -> disposition.mouseMoved(ev) }
        setOnMouseClicked { ev -> disposition.mouseClicked(ev) }
        setOnMouseEntered { ev -> disposition.mouseEntered(ev) }
        setOnMouseExited { ev -> disposition.mouseExited(ev) }
    }

    private fun normalizeCoords(ev: MouseEvent): Pair<Double, Double> {
        val x = ev.x.toInt() / BEAT_W * BEAT_W.toDouble()
        val y = (ev.y.toInt() + 12) / PITCH_H * PITCH_H.toDouble()
        return Pair(x, y.coerceIn(PITCH_H.toDouble(), H - PITCH_H))
    }

    private fun addVerticalLines() {
        for (i in 1..(W / BEAT_W)) {
            val l = Line(i * BEAT_W.toDouble(), 0.0, i * BEAT_W.toDouble(), H)
            val g = if (i % 10 == 0) 0.3 else 0.7
            if (i % 2 != 0) l.strokeDashArray.addAll(2.0, 2.0)
            l.stroke = Color.gray(g)
            children.add(l)
        }
    }

    private fun createPulseLine() = Line().apply{
        endY = H
        stroke = Color.DODGERBLUE
        strokeWidth = 4.0
        endXProperty().bind(startXProperty())
        visibleProperty().bind(startXProperty().isNotEqualTo(0))
        children.add(this)
    }

    fun handleShortcut(shortcut: KeyCombination) {
        disposition.handleShortcut(shortcut)
    }

    private fun add(element: Element, vararg nodes: Node) {
        associatedNodes.getOrPut(element) { mutableListOf() }.addAll(nodes)
        children.addAll(*nodes)
    }

    private fun deleteElement(element: Element) {
        check(elements.remove(element)) { "cannot remove non-existent element $element" }
        associatedNodes.remove(element)!!.forEach { children.remove(it) }
        noteHeads.remove(element)!!
        accidentalViews.remove(element)!!
        if (element is Trill) trillAccidentalViews.remove(element)!!
    }

    private fun clearScore() {
        for (el in elements.toList()) {
            deleteElement(el)
        }
        elements.clear()
    }

    private fun addElement(element: Element) {
        val head = NoteHead(element)
        head.x = element.start.toXCoordinate()
        head.noteHeadType = element.type.noteHeadType
        noteHeads[element] = head
        val dynamic = DynamicView(head.xProperty(), head.yProperty(), element::startDynamic, element::start)
        add(element, head, dynamic)
        if (element is PitchedElement) {
            head.y = element.pitch.getY()
            val accidental = AccidentalView(element.pitch.accidental, head)
            accidentalViews[element] = accidental
            add(element, accidental)
        } else {
            head.y = element.customY ?: error("no custom-y provided for '$element'")
        }
        if (element is ContinuousElement && element.climax != 0 && element.end != 0) {
            val line = createDurationLine(element, head)
            val climaxX = SimpleDoubleProperty(element.climax.toXCoordinate())
            val climax = DynamicView(climaxX, head.yProperty(), element::climaxDynamic, element::climax)
            val endX = SimpleDoubleProperty(element.end.toXCoordinate())
            val end = DynamicView(endX, head.yProperty(), element::endDynamic, element::end)
            line.endXProperty().bind(Bindings.add(end.xProperty(), 10))
            add(element, line.shape, climax, end)
        }
        if (element is Trill && element.secondaryPitch != null) {
            val littleHead = NoteHead(element).scale(SECONDARY_PITCH_SCALE)
            val littleAccidental = AccidentalView(element.secondaryPitch!!.accidental, littleHead)
            layoutSecondaryPitch(element, head, littleHead, littleAccidental)
        }
        elements.add(element)
    }

    private fun createDurationLine(element: ContinuousElement, head: NoteHead): ILine {
        val line = if (element.type == Trill) ZigZagLine(3.0) else LineAdapter()
        line.startXProperty().bind(Bindings.add(head.xProperty(), 15))
        line.startYProperty().bind(Bindings.add(head.yProperty(), 8))
        line.endYProperty().bind(Bindings.add(head.yProperty(), 8))
        line.strokeWidth = if (element.type == Trill) 3.0 else 5.0
        line.strokeDashArray.addAll(element.type.strokeDashArray)
        return line
    }

    private fun layoutSecondaryPitch(trill: Trill, head: NoteHead, littleHead: NoteHead, accidental: AccidentalView) {
        val lp = NoteHead.leftParentheses().scale(SECONDARY_PITCH_SCALE)
        val rp = NoteHead.rightParentheses().scale(SECONDARY_PITCH_SCALE)
        lp.strokeProperty().bind(littleHead.root.fillProperty())
        rp.strokeProperty().bind(littleHead.root.fillProperty())
        littleHead.xProperty().bind(binding(littleHead.yProperty(), head.yProperty(), head.xProperty()) {
            if (littleHead.y == head.y + 1) head.x + 35 else head.x + 15
        })
        val steps = trill.secondaryPitch!! - trill.pitch
        littleHead.y = head.y + steps * PITCH_H + 1
        lp.layoutXProperty().bind(accidental.xProperty().subtract(10))
        rp.layoutXProperty().bind(littleHead.xProperty().add(2))
        lp.layoutYProperty().bind(littleHead.yProperty().subtract(4))
        rp.layoutYProperty().bind(littleHead.yProperty().subtract(4))
        val connector = Line()
        connector.startXProperty().bind(head.xProperty().add(10))
        connector.startYProperty().bind(head.yProperty().add(7))
        connector.endXProperty().bind(littleHead.xProperty().add(10))
        connector.endYProperty().bind(littleHead.yProperty().add(7))
        connector.strokeProperty().bind(littleHead.root.fillProperty())
        val diffY = littleHead.yProperty().subtract(head.yProperty())
        connector.visibleProperty().bind(diffY.greaterThan(30).or(diffY.lessThan(-30)))
        add(trill, littleHead, accidental, lp, rp, connector)
    }

    fun openScore(score: GraphicalScore) {
        clearScore()
        for (el in score.elements) {
            addElement(el)
        }
    }

    fun getScore() = GraphicalScore(elements)

    fun setCurrentPulse(pulse: Int) {
        pulseLine.startXProperty().value = (pulse.toDouble() / PULSES_PER_BEAT) * BEAT_W
    }

    private abstract inner class Disposition {
        open fun init(old: Disposition) {}

        open fun replaced(new: Disposition) {}

        open fun handleShortcut(ev: KeyCombination) {}

        open fun mouseClicked(ev: MouseEvent) {}

        open fun mouseMoved(ev: MouseEvent) {}

        open fun mouseEntered(ev: MouseEvent) {}

        open fun mouseExited(ev: MouseEvent) {}

        open fun elementTypeChanged(type: Element.Type<*>) {}

        open fun instrumentChanged(instr: Instrument) {}

        open fun accidentalChanged(acc: Accidental) {}

        open fun dynamicChanged(dynamic: Dynamic) {}
    }

    private abstract inner class EditElement : Disposition() {
        protected abstract fun withElement(block: (element: Element, head: NoteHead) -> Unit)

        protected abstract fun withDynamic(block: (element: DynamicView) -> Unit)

        override fun instrumentChanged(instr: Instrument) = withElement { element, _ ->
            element.instrument = instr
        }

        override fun accidentalChanged(acc: Accidental) = withElement { element, head ->
            if (element is Trill && head.scaleX == SECONDARY_PITCH_SCALE) {
                element.secondaryPitch = element.secondaryPitch
                trillAccidentalViews.getValue(element).accidental = acc
            } else if (element is PitchedElement) {
                element.pitch = element.pitch.copy(accidental = acc)
                accidentalViews.getValue(element).accidental = acc
            }
        }

        override fun dynamicChanged(dynamic: Dynamic) = withDynamic { element ->
            element.dynamic = dynamic
        }

        override fun handleShortcut(ev: KeyCombination) {
            when (ev) {
                DELETE -> deleteElement()
                LEFT -> moveLeft()
                RIGHT -> moveRight()
                UP -> moveUp()
                DOWN -> moveDown()
            }
        }

        private fun moveLeft() {
            withElement { element, head ->
                if (head.scaleX == 1.0) {
                    element.start -= 1
                    head.x -= 20
                }
            }
            withDynamic { element ->
                element.time -= 1
                element.x -= BEAT_W
            }
        }

        private fun moveRight() {
            withElement { element, head ->
                if (head.scaleX == 1.0) {
                    element.start += 1
                    head.x += BEAT_W
                }
            }
            withDynamic { element ->
                element.time += 1
                element.x += BEAT_W
            }
        }

        private fun moveUp() = withElement { element, head ->
            when {
                element is Trill && head.scaleX == SECONDARY_PITCH_SCALE -> element.secondaryPitch =
                    element.secondaryPitch!!.up()
                element is PitchedElement -> element.pitch = element.pitch.up()
                else -> element.customY = element.customY!! - PITCH_H
            }
            head.y -= PITCH_H
        }

        private fun moveDown() = withElement { element, head ->
            when {
                element is Trill && head.scaleX == 0.0 -> element.secondaryPitch = element.secondaryPitch!!.down()
                element is PitchedElement -> element.pitch = element.pitch.down()
                else -> element.customY = element.customY!! + PITCH_H
            }
            head.y += PITCH_H
        }

        protected open fun deleteElement() = withElement { element, head ->
            if (element is Trill && head.scaleX == SECONDARY_PITCH_SCALE) {
                val subst = SimplePitchedContinuousElement(SimplePitchedContinuousElement.Regular)
                subst.copyFrom(element)
                deleteElement(element)
                addElement(subst)
            } else deleteElement(element)
        }
    }

    private inner class Pointer(var selected: SelectableElement? = null) : EditElement() {
        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            val el = selected
            if (el is NoteHead && el.element != null) {
                block(el.element, el)
            }
        }

        override fun withDynamic(block: (element: DynamicView) -> Unit) {
            val el = selected
            if (el is DynamicView) {
                block(el)
            }
        }

        override fun replaced(new: Disposition) {
            selected?.isSelected = false
        }

        override fun mouseClicked(ev: MouseEvent) {
            selected?.isSelected = false
            val element = findSelectableElement(ev.target)
            selected = element
            element?.isSelected = true
            when (element) {
                is DynamicView -> element.dynamic?.let { dynamicsSelector.select(it) }
                is NoteHead -> {
                    val el = element.element ?: return
                    el.instrument?.let { instrumentSelector.select(it) }
                    if (el is PitchedElement) {
                        accidentalSelector.select(el.pitch.accidental)
                    }
                }
            }
        }

        override fun deleteElement() {
            super.deleteElement()
            selected = null
        }
    }

    private inner class CreateElement(
        private var lastCreated: NoteHead? = null,
        private var lastCreatedDynamic: DynamicView? = null
    ) : EditElement() {
        private val phantomHead = NoteHead().phantom()
        private val phantomAccidental = AccidentalView(accidentalSelector.selected.value, phantomHead).phantom()

        init {
            phantomAccidental.visibleProperty().bind(phantomHead.visibleProperty())
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            val el = lastCreated
            if (el?.element != null) {
                block(el.element, el)
            }
        }

        override fun withDynamic(block: (element: DynamicView) -> Unit) {
            lastCreatedDynamic?.let(block)
        }

        override fun init(old: Disposition) {
            val type = elementTypeSelector.selected.value
            if (type != null) phantomHead.noteHeadType = type.noteHeadType
            children.addAll(phantomHead, phantomAccidental)
            phantomHead.isVisible = false
        }

        override fun replaced(new: Disposition) {
            children.removeAll(phantomHead, phantomAccidental)
            if (new is Pointer) new.selected = lastCreated
        }

        override fun elementTypeChanged(type: Element.Type<*>) {
            phantomHead.noteHeadType = type.noteHeadType
        }

        override fun mouseEntered(ev: MouseEvent) {
            phantomHead.isVisible = true
        }

        override fun mouseMoved(ev: MouseEvent) {
            val (x, y) = normalizeCoords(ev)
            phantomHead.isVisible = true
            phantomHead.x = x
            phantomHead.y = y - 8
        }

        override fun mouseExited(ev: MouseEvent) {
            phantomHead.isVisible = false
        }

        override fun mouseClicked(ev: MouseEvent) {
            val (x, y) = normalizeCoords(ev)
            val element = elementTypeSelector.selected.value?.createElement() ?: return
            element.start = getTime(x)
            element.startDynamic = dynamicsSelector.selected.value
            element.instrument = instrumentSelector.selected.value
            if (element is PitchedElement) element.pitch = getPitch(y)
            else element.customY = y - 8
            addElement(element)
            lastCreated?.isSelected = false
            when (element) {
                is Trill -> disposition = TrillInCreation(element)
                is ContinuousElement -> disposition = ElementInCreation(element)
                else -> {
                    val head = noteHeads.getValue(element)
                    lastCreated = head
                    head.isSelected = true
                    lastCreatedDynamic = startDynamics[element]
                }
            }
        }

        private fun getPitch(y: Double): Pitch =
            Pitch.fromDiatonicStep(52 - y.toInt() / PITCH_H, accidentalSelector.selected.value)

        override fun accidentalChanged(acc: Accidental) {
            phantomAccidental.accidental = acc
        }
    }

    private inner class ElementInCreation(private val element: ContinuousElement) : EditElement() {
        private val head = noteHeads.getValue(element)
        private val line = createDurationLine(element, head)
        private var minX = head.x + BEAT_W
        private var finished = false

        override fun init(old: Disposition) {
            head.inCreation()
            add(element, line.shape)
            line.endXProperty().set(minX)
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            block(element, head)
        }

        override fun withDynamic(block: (element: DynamicView) -> Unit) {}

        override fun replaced(new: Disposition) {
            if (!finished) deleteElement(element)
            else {
                head.isSelected = true
                if (new is Pointer) new.selected = head
            }
        }

        override fun mouseClicked(ev: MouseEvent) {
            var (x, _) = normalizeCoords(ev)
            x = x.coerceAtLeast(minX)
            val xProp = SimpleDoubleProperty(x)
            if (element.climax == 0) {
                element.climax = getTime(x)
                element.climaxDynamic = dynamicsSelector.selected.value
                val dynamic = DynamicView(xProp, head.yProperty(), element::climaxDynamic, element::climax)
                add(element, dynamic)
                minX = x + BEAT_W
            } else {
                element.end = getTime(x)
                element.endDynamic = dynamicsSelector.selected.value
                val dynamic = DynamicView(xProp, head.yProperty(), element::endDynamic, element::end)
                line.endXProperty().bind(dynamic.xProperty())
                add(element, dynamic)
                finished = true
                disposition = CreateElement(head, null)
            }
        }

        override fun deleteElement() {
            disposition = CreateElement()
        }

        override fun mouseMoved(ev: MouseEvent) {
            val (x, _) = normalizeCoords(ev)
            line.endXProperty().set(x.coerceAtLeast(minX))
        }

        override fun accidentalChanged(acc: Accidental) {
            if (element is PitchedContinuousElement) {
                element.pitch = element.pitch.copy(accidental = acc)
            }
        }
    }

    private inner class TrillInCreation(private val trill: Trill) : EditElement() {
        private val head = noteHeads.getValue(trill)
        private val littleHead = NoteHead(trill).phantom().scale(SECONDARY_PITCH_SCALE)
        private val littleAccidental =
            AccidentalView(accidentalSelector.selected.value, littleHead).phantom().scale(SECONDARY_PITCH_SCALE)
        private var finished = false

        init {
            trill.secondaryPitch = trill.pitch
            trillAccidentalViews[trill] = littleAccidental
            layoutSecondaryPitch(trill, head, littleHead, littleAccidental)
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) = block(trill, head)

        override fun withDynamic(block: (element: DynamicView) -> Unit) = block(startDynamics.getValue(trill))

        override fun replaced(new: Disposition) {
            if (!finished) deleteElement(trill)
        }

        override fun handleShortcut(ev: KeyCombination) {
            when (ev) {
                UP -> {
                    littleHead.y -= PITCH_H
                    trill.secondaryPitch = trill.secondaryPitch!!.up()
                }
                DOWN -> {
                    littleHead.y += PITCH_H
                    trill.secondaryPitch = trill.secondaryPitch!!.down()
                }
                ENTER -> finish()
            }
        }

        private fun finish() {
            littleHead.regular()
            littleAccidental.regular()
            finished = true
            disposition = ElementInCreation(trill)
        }

        override fun accidentalChanged(acc: Accidental) {
            trill.secondaryPitch = trill.secondaryPitch!!.copy(accidental = acc)
            littleAccidental.accidental = acc
        }

        override fun deleteElement() {
            disposition = CreateElement()
        }
    }

    class Clefs : Pane() {
        init {
            addHorizontalLines()
            addClefs()
            prefHeight = H
            prefWidth = 200.0
            translateY = +1.0
        }

        private fun addClefs() {
            val bass = loadImage("clefs/bass.png")
            val bassView = bass.view().fitHeight(bass.height / 5)
            val violin = loadImage("clefs/violin.png")
            val violinView = violin.view().fitHeight(violin.height / 5)
            bassView.x = BEAT_W.toDouble()
            bassView.y = 650.0
            violinView.x = BEAT_W.toDouble()
            violinView.y = 260.0
            children.addAll(violinView, bassView)
        }
    }

    companion object {
        private const val W = 20000
        private const val H = 1100.0
        private const val BEAT_W = 20
        private const val PITCH_H = 25
        private const val SECONDARY_PITCH_SCALE = 0.6

        private fun getTime(x: Double): Time = x.toInt() / BEAT_W

        private fun Pitch.getY(): Double = (52 - diatonicStep) * PITCH_H.toDouble() - 8

        private fun Time.toXCoordinate(): Double = this * BEAT_W.toDouble()

        private fun findSelectableElement(target: EventTarget?): SelectableElement? = when (target) {
            is SelectableElement -> target
            is Node -> findSelectableElement(target.parent)
            else -> null
        }

        private fun Pane.addHorizontalLines() {
            for (i in 7..17) {
                val l = Line(0.0, i * PITCH_H * 2.0, W.toDouble(), i * PITCH_H * 2.0)
                l.strokeWidth = if (i == 12) 5.0 else 3.0
                children.add(l)
            }
        }
    }
}