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

    private var disposition: Disposition by Delegates.observable(Pointer()) { _, old, new ->
        old.replaced(new)
        new.init(old)
    }

    init {
        setupLines()
        setupClefs()
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
        val x = ev.x.toInt() / 20 * 20.0
        val y = (ev.y.toInt() + 12) / 25 * 25.0
        return Pair(x.coerceAtLeast(200.0), y.coerceIn(25.0, 1075.0))
    }

    private fun setupClefs() {
        val bass = loadImage("clefs/bass.png")
        val bassView = bass.view().fitHeight(bass.height / 5)
        val violin = loadImage("clefs/violin.png")
        val violinView = violin.view().fitHeight(violin.height / 5)
        bassView.x = 20.0
        bassView.y = 650.0
        violinView.x = 20.0
        violinView.y = 260.0
        children.addAll(violinView, bassView)
    }

    private fun setupLines() {
        for (i in 10..1000) {
            val l = Line(i * 20.0, 0.0, i * 20.0, 1100.0)
            val g = if (i % 10 == 0) 0.3 else 0.7
            if (i % 2 != 0) l.strokeDashArray.addAll(2.0, 2.0)
            l.stroke = Color.gray(g)
            children.add(l)
        }
        for (i in 7..17) {
            val l = Line(0.0, i * 50.0, 10000.0, i * 50.0)
            l.strokeWidth = if (i == 12) 5.0 else 3.0
            children.add(l)
        }
    }

    fun handleShortcut(shortcut: KeyCombination) {
        disposition.handleShortcut(shortcut)
    }

    private fun add(element: Element, vararg nodes: Node) {
        associatedNodes.getOrPut(element) { mutableListOf() }.addAll(nodes)
        children.addAll(*nodes)
    }

    private fun deleteElement(element: Element) {
        associatedNodes.remove(element)!!.forEach { children.remove(it) }
        noteHeads.remove(element)!!
        elements.remove(element)
    }

    private fun clearScore() {
        for (el in elements) {
            deleteElement(el)
        }
        elements.clear()
    }

    private fun addElement(element: Element) {
        val head = NoteHead(element)
        head.x = element.start!!.getX()
        head.setNoteHeadType(element.type.noteHeadType)
        noteHeads[element] = head
        val dynamic = DynamicView(head.xProperty(), head.yProperty(), element::startDynamic, element::start)
        add(element, head, dynamic)
        if (element is PitchedElement) {
            head.y = element.pitch.getY()
            val accidental = AccidentalView(element.pitch.accidental, head)
            accidentalViews[element] = accidental
            add(element, accidental)
        } else {
            head.y = element.customY!!
        }
        if (element is ContinuousElement && element.climax != null && element.end != null) {
            val line = createDurationLine(element, head)
            val climaxX = SimpleDoubleProperty(element.climax!!.getX())
            val climax = DynamicView(climaxX, head.yProperty(), element::climaxDynamic, element::climax)
            val endX = SimpleDoubleProperty(element.end!!.getX())
            val end = DynamicView(endX, head.yProperty(), element::endDynamic, element::end)
            line.endXProperty().bind(Bindings.add(end.xProperty(), 10))
            add(element, line, climax, end)
        }
        if (element is Trill && element.secondaryPitch != null) {
            val littleHead = NoteHead(element).scale(0.6)
            val littleAccidental = AccidentalView(element.secondaryPitch!!.accidental, littleHead)
            layoutSecondaryPitch(element, head, littleHead, littleAccidental)
        }
        elements.add(element)
    }

    private fun createDurationLine(element: ContinuousElement, head: NoteHead): Line {
        val line = Line()
        line.startXProperty().bind(Bindings.add(head.xProperty(), 15))
        line.startYProperty().bind(Bindings.add(head.yProperty(), 8))
        line.endYProperty().bind(Bindings.add(head.yProperty(), 8))
        line.strokeWidth = 5.0
        line.strokeDashArray.addAll(element.type.strokeDashArray.orEmpty())
        return line
    }

    private fun layoutSecondaryPitch(trill: Trill, head: NoteHead, littleHead: NoteHead, accidental: AccidentalView) {
        val lp = NoteHead.leftParentheses().scale(0.6)
        val rp = NoteHead.rightParentheses().scale(0.6)
        lp.strokeProperty().bind(littleHead.root.fillProperty())
        rp.strokeProperty().bind(littleHead.root.fillProperty())
        littleHead.xProperty().bind(binding(littleHead.yProperty(), head.yProperty(), head.xProperty()) {
            if (littleHead.y == head.y + 1) head.x + 35 else head.x + 15
        })
        littleHead.y = head.y + 1
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


    fun openScore(score: Score) {
        clearScore()
        for (el in score.elements) {
            addElement(el)
        }
    }

    fun getScore() = Score(elements)

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
            if (element is Trill && head.scaleX == 0.6) {
                element.secondaryPitch = element.secondaryPitch
                trillAccidentalViews[element]!!.setAccidental(acc)
            } else if (element is PitchedElement) {
                element.pitch = element.pitch.copy(accidental = acc)
                accidentalViews[element]!!.setAccidental(acc)
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
                    element.start = element.start?.prev()
                    head.x -= 20
                }
            }
            withDynamic { element ->
                element.moment = element.moment?.prev()
                element.x -= 20
            }
        }

        private fun moveRight() {
            withElement { element, head ->
                if (head.scaleX == 1.0) {
                    element.start = element.start?.next()
                    head.x += 20
                }
            }
            withDynamic { element ->
                element.moment = element.moment?.next()
                element.x += 20
            }
        }

        private fun moveUp() = withElement { element, head ->
            when {
                element is Trill && head.scaleX == 0.6 -> element.secondaryPitch = element.secondaryPitch!!.up()
                element is PitchedElement -> element.pitch = element.pitch.up()
                else -> element.customY = element.customY!! - 25
            }
            head.y -= 25.0
        }

        private fun moveDown() = withElement { element, head ->
            when {
                element is Trill && head.scaleX == 0.0 -> element.secondaryPitch = element.secondaryPitch!!.down()
                element is PitchedElement -> element.pitch = element.pitch.down()
                else -> element.customY = element.customY!! + 25
            }
            head.y += 25.0
        }

        protected open fun deleteElement() = withElement { element, head ->
            if (element is Trill && head.scaleX == 0.6) {
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
            phantomHead.setNoteHeadType(elementTypeSelector.selected.value!!.noteHeadType)
            children.addAll(phantomHead, phantomAccidental)
            phantomHead.isVisible = false
        }

        override fun replaced(new: Disposition) {
            children.removeAll(phantomHead, phantomAccidental)
            if (new is Pointer) new.selected = lastCreated
        }

        override fun elementTypeChanged(type: Element.Type<*>) {
            phantomHead.setNoteHeadType(type.noteHeadType)
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
            element.start = getMoment(x)
            element.startDynamic = dynamicsSelector.selected.value
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

        private fun getPitch(y: Double): Pitch {
            val step = 52 - y.toInt() / 25
            val register = step / 7
            val pitchName = PitchName.values()[step % 7]
            return Pitch(register, pitchName, accidentalSelector.selected.value)
        }

        override fun accidentalChanged(acc: Accidental) {
            phantomAccidental.setAccidental(acc)
        }
    }

    private inner class ElementInCreation(private val element: ContinuousElement) : EditElement() {
        private val head = noteHeads.getValue(element)
        private val line = createDurationLine(element, head)
        private var minX = head.x + 20
        private var finished = false

        override fun init(old: Disposition) {
            head.inCreation()
            add(element, line)
            line.endX = minX
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
            if (element.climax == null) {
                element.climax = getMoment(x)
                element.climaxDynamic = dynamicsSelector.selected.value
                val dynamic = DynamicView(xProp, head.yProperty(), element::climaxDynamic, element::climax)
                add(element, dynamic)
                minX = x + 20
            } else {
                element.end = getMoment(x)
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
            line.endX = x.coerceAtLeast(minX)
        }

        override fun accidentalChanged(acc: Accidental) {
            if (element is PitchedContinuousElement) {
                element.pitch = element.pitch.copy(accidental = acc)
            }
        }
    }

    private inner class TrillInCreation(private val trill: Trill) : EditElement() {
        private val head = noteHeads.getValue(trill)
        private val littleHead = NoteHead(trill).phantom().scale(0.6)
        private val littleAccidental =
            AccidentalView(accidentalSelector.selected.value, littleHead).phantom().scale(0.6)
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
                    littleHead.y -= 25
                    trill.secondaryPitch = trill.secondaryPitch!!.up()
                }
                DOWN -> {
                    littleHead.y += 25
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
            littleAccidental.setAccidental(acc)
        }

        override fun deleteElement() {
            disposition = CreateElement()
        }
    }

    companion object {
        private fun getMoment(x: Double) = Moment(x.toInt() / 40 - 10, (x.toInt() / 20) % 2)

        private fun Pitch.getY(): Double {
            val step = register * 7 + name.ordinal
            return (52 - step) * 25.0 - 8
        }

        private fun Moment.getX(): Double = bar * 40.0 + beat * 20.0 + 400.0

        private fun findSelectableElement(target: EventTarget?): SelectableElement? = when (target) {
            is SelectableElement -> target
            is Node -> findSelectableElement(target.parent)
            else -> null
        }
    }
}