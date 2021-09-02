package wittgenstein.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.image.ImageView
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
    private val typeSelector: ElementTypeSelector,
    private val accidentalSelector: AccidentalSelector,
    private val instrumentSelector: InstrumentSelector,
    private val dynamicsSelector: DynamicSelector
) : Pane() {
    private val noteHeads = mutableMapOf<Element, NoteHead>()
    private val nodes = mutableMapOf<Element, List<Node>>()
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
        typeSelector.selected.addListener { _, _, new ->
            if (new == null) disposition = Pointer()
            else if (disposition !is CreateElement) disposition = CreateElement()
        }
        accidentalSelector.selected.addListener { _, _, acc -> disposition.changeAccidental(acc) }
        instrumentSelector.selected.addListener { _, _, instr ->
            if (instr != null) {
                disposition.changeInstrument(instr)
            }
        }
        dynamicsSelector.selected.addListener { _, _, dyn -> disposition.changeDynamic(dyn) }
    }

    private fun trackMouse() {
        setOnMouseMoved { ev -> disposition.mouseMoved(ev) }
        setOnMouseClicked { ev -> disposition.mouseClicked(ev) }
        setOnMouseEntered { ev -> disposition.mouseEntered(ev) }
        setOnMouseExited { ev -> disposition.mouseExited(ev) }
    }

    private fun normalizeCoords(ev: MouseEvent) = Pair(ev.x.toInt() / 20 * 20.0, (ev.y.toInt() + 12) / 25 * 25.0)

    private fun setupClefs() {
        val bass = loadImage("clefs/bass.png")
        val bassView = ImageView(bass)
        val violin = loadImage("clefs/violin.png")
        val violinView = ImageView(violin)
        bassView.isPreserveRatio = true
        violinView.isPreserveRatio = true
        bassView.fitHeight = bass.height / 5
        violinView.fitHeight = violin.height / 5
        children.add(bassView)
        bassView.x = 20.0
        bassView.y = 550.0
        violinView.x = 20.0
        violinView.y = 160.0
        children.add(violinView)
    }

    private fun setupLines() {
        for (i in 10..1000) {
            val l = Line(i * 20.0, 0.0, i * 20.0, 1000.0)
            val g = if (i % 10 == 0) 0.3 else 0.7
            if (i % 2 != 0) l.strokeDashArray.addAll(2.0, 2.0)
            l.stroke = Color.gray(g)
            children.add(l)
        }
        for (i in 5..15) {
            val l = Line(0.0, i * 50.0, 10000.0, i * 50.0)
            l.strokeWidth = if (i == 10) 5.0 else 3.0
            children.add(l)
        }
    }

    fun handleShortcut(shortcut: KeyCombination) {
        disposition.handleShortcut(shortcut)
    }

    private abstract inner class Disposition {
        open fun init(old: Disposition) {}

        open fun replaced(new: Disposition) {}

        open fun handleShortcut(ev: KeyCombination) {}

        open fun mouseClicked(ev: MouseEvent) {}

        open fun mouseMoved(ev: MouseEvent) {}

        open fun mouseEntered(ev: MouseEvent) {}

        open fun mouseExited(ev: MouseEvent) {}

        open fun changeInstrument(instr: Instrument) {}

        open fun changeAccidental(acc: Accidental) {}

        open fun changeDynamic(dynamic: Dynamic) {}
    }

    private abstract inner class EditElement : Disposition() {
        protected abstract fun withElement(block: (element: Element, head: NoteHead) -> Unit)

        protected abstract fun withDynamic(block: (element: DynamicViewElement) -> Unit)

        override fun changeInstrument(instr: Instrument) = withElement { element, _ ->
            element.instrument = instr
        }

        override fun changeAccidental(acc: Accidental) = withElement { element, _ ->
            if (element is PitchedElement) {
                element.pitch = element.pitch.copy(accidental = acc)
            }
        }

        override fun changeDynamic(dynamic: Dynamic) = withDynamic { element ->
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
                head.y -= 25.0
            }
        }

        private fun moveDown() = withElement { element, head ->
            if (element is PitchedElement) {
                element.pitch = element.pitch.down()
                head.y += 25.0
            }
        }

        protected open fun deleteElement() = withElement { element, _ ->
            nodes.remove(element)!!.forEach { children.remove(it) }
            noteHeads.remove(element)!!
            elements.remove(element)
        }
    }

    private inner class Pointer(var selected: ViewElement? = null) : EditElement() {
        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            val el = selected
            if (el is NoteHead && el.element != null) {
                block(el.element, el)
            }
        }

        override fun withDynamic(block: (element: DynamicViewElement) -> Unit) {
            val el = selected
            if (el is DynamicViewElement) {
                block(el)
            }
        }

        override fun replaced(new: Disposition) {
            selected?.isSelected = false
        }

        override fun mouseClicked(ev: MouseEvent) {
            selected?.isSelected = false
            val element = ev.target as? ViewElement
            selected = element
            element?.isSelected = true
            when (element) {
                is DynamicViewElement -> element.dynamic?.let { dynamicsSelector.select(it) }
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
        private var lastCreatedDynamic: DynamicViewElement? = null
    ) : EditElement() {
        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            val el = lastCreated
            if (el?.element != null) {
                block(el.element, el)
            }
        }

        override fun withDynamic(block: (element: DynamicViewElement) -> Unit) {
            lastCreatedDynamic?.let(block)
        }

        private val phantomHead = NoteHead(NoteHead.State.Phantom)

        override fun replaced(new: Disposition) {
            children.remove(phantomHead)
            if (new is Pointer) new.selected = lastCreated
        }

        override fun mouseEntered(ev: MouseEvent) {
            children.add(phantomHead)
        }

        override fun mouseMoved(ev: MouseEvent) {
            val (x, y) = normalizeCoords(ev)
            phantomHead.x = x.coerceAtLeast(200.0)
            phantomHead.y = (y - 8).coerceIn(17.0..967.0)
        }

        override fun mouseExited(ev: MouseEvent) {
            children.remove(phantomHead)
        }

        override fun mouseClicked(ev: MouseEvent) {
            val (x, y) = normalizeCoords(ev)
            val moment = getMoment(x)
            when (val t = typeSelector.selected.value) {
                Trill -> {
                    val pitch = getPitch(y)
                    val secondaryPitch = getPitch(0.0)
                    val element = Trill(pitch, secondaryPitch)
                    startElementCreation(element, moment, x, y)
                }
                is PitchedContinuousElement.Type -> {
                    val pitch = getPitch(y)
                    val element = PitchedContinuousElement(t, pitch)
                    startElementCreation(element, moment, x, y)
                }
                is ContinuousNoise.Type -> {
                    val element = ContinuousNoise(t)
                    startElementCreation(element, moment, x, y)
                }
                is DiscretePitchedElement.Type -> {
                    val pitch = getPitch(y)
                    val element = DiscretePitchedElement(t, pitch)
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
            val head = NoteHead(x, y - 8, element)
            val dynamic = DynamicViewElement(head.xProperty(), head.yProperty(), element::startDynamic, element::start)
            children.add(dynamic)
            children.add(head)
            noteHeads[element] = head
            nodes[element] = listOf(head, dynamic)
            elements.add(element)
            lastCreated?.isSelected = false
            lastCreated = head
            head.isSelected = true
            lastCreatedDynamic = dynamic
        }

        private fun startElementCreation(element: ContinuousElement, moment: Moment, x: Double, y: Double) {
            element.start = moment
            element.startDynamic = dynamicsSelector.selected.value
            val head = NoteHead(x, y - 8, element, NoteHead.State.InCreation)
            lastCreated?.isSelected = false
            disposition = ElementInCreation(element, head)
        }

        private fun getPitch(y: Double) = Pitch(4, PitchName.C, accidentalSelector.selected.value)
    }

    private inner class ElementInCreation(
        private val element: ContinuousElement,
        private val head: NoteHead
    ) : EditElement() {
        private val line = Line()
        private val removableNodes = mutableListOf<Node>()

        init {
            line.startXProperty().bind(Bindings.add(head.xProperty(), 20))
            line.startYProperty().bind(Bindings.add(head.yProperty(), 8))
            line.endX = head.x + 20
            line.endYProperty().bind(Bindings.add(head.yProperty(), 8))
            line.strokeWidth = 5.0
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            block(element, head)
        }

        override fun withDynamic(block: (element: DynamicViewElement) -> Unit) {}

        override fun init(old: Disposition) {
            add(head)
            add(line)
            add(DynamicViewElement(head.xProperty(), head.yProperty(), element::startDynamic, element::start))
        }

        override fun replaced(new: Disposition) {
            for (node in removableNodes) children.remove(node)
            if (new is Pointer) new.selected = head
        }

        override fun mouseClicked(ev: MouseEvent) {
            val (x, _) = normalizeCoords(ev)
            if (element.climax == null) {
                element.climax = getMoment(x)
                element.climaxDynamic = dynamicsSelector.selected.value
                add(
                    DynamicViewElement(
                        SimpleDoubleProperty(x),
                        head.yProperty(),
                        element::climaxDynamic,
                        element::climax
                    )
                )
            } else {
                element.end = getMoment(x)
                element.endDynamic = dynamicsSelector.selected.value
                val dynamic = DynamicViewElement(SimpleDoubleProperty(x), head.yProperty(), element::endDynamic, element::end)
                line.endXProperty().bind(Bindings.add(dynamic.xProperty(), 10))
                add(dynamic)
                element.instrument = instrumentSelector.selected.value
                elements.add(element)
                noteHeads[element] = head
                head.state = NoteHead.State.Selected
                nodes[element] = removableNodes.toList()
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
            line.endX = ev.x
        }

        override fun changeAccidental(acc: Accidental) {
            if (element is PitchedContinuousElement) {
                element.pitch = element.pitch.copy(accidental = acc)
            }
        }
    }

    companion object {
        private fun getMoment(x: Double) = Moment(x.toInt() / 40 - 10, (x.toInt() / 20) % 2)
    }
}