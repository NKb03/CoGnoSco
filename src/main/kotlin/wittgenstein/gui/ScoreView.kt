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
    private val noteHeads = mutableMapOf<Element, NoteHead>()
    private val accidentalViews = mutableMapOf<Element, AccidentalView>()
    private val associatedNodes = mutableMapOf<Element, List<Node>>()
    private val elements = mutableListOf<Element>()
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

    private fun addElement(el: Element) {
        TODO("Not yet implemented")
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

        override fun accidentalChanged(acc: Accidental) = withElement { element, _ ->
            if (element is PitchedElement) {
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
                element.start = element.start?.prev()
                head.x -= 20
            }
            withDynamic { element ->
                element.moment = element.moment?.prev()
                element.x -= 20
            }
        }

        private fun moveRight() {
            withElement { element, head ->
                element.start = element.start?.next()
                head.x += 20
            }
            withDynamic { element ->
                element.moment = element.moment?.next()
                element.x += 20
            }
        }

        private fun moveUp() = withElement { element, head ->
            if (element is PitchedElement) {
                element.pitch = element.pitch.up()
            }
            head.y -= 25.0
        }

        private fun moveDown() = withElement { element, head ->
            if (element is PitchedElement) {
                element.pitch = element.pitch.down()
            }
            head.y += 25.0
        }

        protected open fun deleteElement() = withElement { element, _ ->
            deleteElement(element)
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
            children.add(phantomHead)
            phantomHead.isVisible = false
        }

        override fun replaced(new: Disposition) {
            children.remove(phantomHead)
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
            val moment = getMoment(x)
            val pitch = getPitch(y)
            when (val t = elementTypeSelector.selected.value) {
                Trill -> {
                    val element = Trill()
                    element.pitch = pitch
                    element.secondaryPitch = getPitch(0.0)
                    startElementCreation(element, moment, x, y)
                }
                is SimplePitchedContinuousElement.Type -> {
                    val element = SimplePitchedContinuousElement(t)
                    element.pitch = pitch
                    startElementCreation(element, moment, x, y)
                }
                is ContinuousNoise.Type -> {
                    val element = ContinuousNoise(t)
                    startElementCreation(element, moment, x, y)
                }
                is DiscretePitchedElement.Type -> {
                    val element = DiscretePitchedElement(t)
                    element.pitch = pitch
                    addElement(element, x, y)
                }
                is DiscreteNoise.Type -> {
                    val element = DiscreteNoise(t)
                    addElement(element, x, y)
                }
            }
        }

        private fun addElement(element: Element, x: Double, y: Double) {
            element.start = getMoment(x)
            element.startDynamic = dynamicsSelector.selected.value
            element.instrument = instrumentSelector.selected.value
            val nodes = mutableListOf<Node>()
            val head = NoteHead(x, y - 8, element)
            head.setNoteHeadType(element.type.noteHeadType)
            noteHeads[element] = head
            nodes.add(head)
            val dynamic = DynamicView(head.xProperty(), head.yProperty(), element::startDynamic, element::start)
            nodes.add(dynamic)
            if (element is PitchedElement) {
                val accidental = AccidentalView(element.pitch.accidental, head)
                accidentalViews[element] = accidental
                nodes.add(accidental)
            }
            associatedNodes[element] = nodes
            children.addAll(nodes)
            elements.add(element)
            lastCreated?.isSelected = false
            lastCreated = head
            head.isSelected = true
            lastCreatedDynamic = dynamic
        }

        private fun startElementCreation(element: ContinuousElement, moment: Moment, x: Double, y: Double) {
            element.start = moment
            element.startDynamic = dynamicsSelector.selected.value
            val head = NoteHead(x, y - 8, element).inCreation()
            head.setNoteHeadType(element.type.noteHeadType)
            val removableNodes = mutableListOf<Node>()
            if (element is PitchedElement) {
                val accidental = AccidentalView(element.pitch.accidental, head)
                accidentalViews[element] = accidental
                children.add(accidental)
                removableNodes.add(accidental)
            }
            lastCreated?.isSelected = false
            disposition = ElementInCreation(element, head, removableNodes)
        }

        private fun getPitch(y: Double): Pitch {
            val step = 52 - y.toInt() / 25
            val register = step / 7
            val pitchName = PitchName.values()[step % 7]
            val p = Pitch(register, pitchName, accidentalSelector.selected.value)
            println("$y -> $p")
            return p
        }
    }

    private inner class ElementInCreation(
        private val element: ContinuousElement,
        private val head: NoteHead,
        private val removableNodes: MutableList<Node>
    ) : EditElement() {
        private val line = Line()
        private var minX = head.x + 15

        init {
            line.startXProperty().bind(Bindings.add(head.xProperty(), 15))
            line.startYProperty().bind(Bindings.add(head.yProperty(), 8))
            line.endX = line.startX
            line.endYProperty().bind(Bindings.add(head.yProperty(), 8))
            line.strokeWidth = 5.0
            line.strokeDashArray.addAll(element.type.strokeDashArray.orEmpty())
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            block(element, head)
        }

        override fun withDynamic(block: (element: DynamicView) -> Unit) {}

        override fun init(old: Disposition) {
            add(line)
            add(head)
            add(DynamicView(head.xProperty(), head.yProperty(), element::startDynamic, element::start))
        }

        override fun replaced(new: Disposition) {
            for (node in removableNodes) children.remove(node)
            if (new is Pointer) new.selected = head
        }

        override fun mouseClicked(ev: MouseEvent) {
            var (x, _) = normalizeCoords(ev)
            x = x.coerceAtLeast(minX)
            val xProp = SimpleDoubleProperty(x)
            if (element.climax == null) {
                element.climax = getMoment(x)
                element.climaxDynamic = dynamicsSelector.selected.value
                add(DynamicView(xProp, head.yProperty(), element::climaxDynamic, element::climax))
                minX = x + 20
            } else {
                element.end = getMoment(x)
                element.endDynamic = dynamicsSelector.selected.value
                val dynamic = DynamicView(xProp, head.yProperty(), element::endDynamic, element::end)
                line.endXProperty().bind(Bindings.add(dynamic.xProperty(), 10))
                add(dynamic)
                element.instrument = instrumentSelector.selected.value
                elements.add(element)
                noteHeads[element] = head
                head.isSelected = true
                associatedNodes[element] = removableNodes.toList()
                removableNodes.clear()
                disposition = CreateElement(head, null)
            }
        }

        override fun deleteElement() {}

        private fun add(node: Node) {
            children.add(node)
            removableNodes.add(node)
        }

        override fun mouseMoved(ev: MouseEvent) {
            line.endX = ev.x.coerceAtLeast(minX)
        }

        override fun accidentalChanged(acc: Accidental) {
            if (element is PitchedContinuousElement) {
                element.pitch = element.pitch.copy(accidental = acc)
            }
        }
    }

    companion object {
        private fun getMoment(x: Double) = Moment(x.toInt() / 40 - 10, (x.toInt() / 20) % 2)

        private fun findSelectableElement(target: EventTarget?): SelectableElement? = when (target) {
            is SelectableElement -> target
            is Node -> findSelectableElement(target.parent)
            else -> null
        }
    }
}