package wittgenstein.gui

import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import wittgenstein.*
import kotlin.properties.Delegates

class ScoreView(
    private val typeSelector: ElementTypeSelector,
    private val accidentalSelector: AccidentalSelector,
    private val instrumentSelector: InstrumentSelector,
    private val dynamicsSelector: DynamicSelector
) : Pane() {
    private val noteHeads = mutableMapOf<Element, NoteHead>()
    private var disposition: Disposition by Delegates.observable(Pointer()) { _, old, new ->
        old.replaced()
        new.init()
    }

    init {
        setupLines()
        setupClefs()
        trackMouse()
        listenForSelectorChanges()
    }

    private fun listenForSelectorChanges() {
        typeSelector.selected.addListener { _, _, new ->
            disposition = if (new == null) Pointer() else CreateElement()
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
        setOnMouseExited { ev -> disposition.mouseExited(ev) }
        setOnKeyTyped { ev -> disposition.keyTyped(ev) }
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
            val l = Line(i * 20.0, 0.0, i * 20.0, 20 * 50.0)
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

    private abstract inner class Disposition {
        open fun init() {}

        open fun replaced() {}

        open fun keyTyped(ev: KeyEvent) {
            when {
                OPEN.match(ev) -> {
                }
                SAVE.match(ev) -> {
                }
                PLAY.match(ev) -> {
                }
                TYPESET.match(ev) -> {
                }
            }
        }

        open fun mouseClicked(ev: MouseEvent) {}

        open fun mouseMoved(ev: MouseEvent) {}

        open fun mouseExited(ev: MouseEvent) {}

        open fun changeInstrument(instr: Instrument) {}

        open fun changeAccidental(acc: Accidental) {}

        open fun changeDynamic(dynamic: Dynamic) {}
    }

    private inner class Pointer : Disposition() {
        private var selected: ViewElement? = null

        override fun replaced() {
            selected?.isSelected = false
        }

        override fun mouseClicked(ev: MouseEvent) {
            selected?.isSelected = false
            val element = ev.target as? ViewElement ?: return
            element.isSelected = true
            when (element) {
                is DynamicViewElement -> element.value?.let { dynamicsSelector.select(it) }
                is NoteHead -> {
                    val el = element.element ?: return
                    el.instrument?.let { instrumentSelector.select(it) }
                    if (el is PitchedElement) {
                        accidentalSelector.select(el.pitch.accidental)
                    }
                }
            }
            selected = element
        }

        override fun changeInstrument(instr: Instrument) {
            val sel = selected
            if (sel is NoteHead) {
                sel.element?.instrument = instr
            }
        }

        override fun changeAccidental(acc: Accidental) {
            val sel = selected
            if (sel is NoteHead && sel.element is PitchedElement) {
                sel.element.pitch = sel.element.pitch.copy(accidental = acc)
            }
        }

        override fun changeDynamic(dynamic: Dynamic) {
            val sel = selected
            if (sel is DynamicViewElement) {
                sel.value = dynamic
            }
        }
    }

    private inner class CreateElement : Disposition() {
        private val head = NoteHead(NoteHead.State.Phantom)

        override fun init() {
            children.add(head)
        }

        override fun replaced() {
            children.remove(head)
        }

        override fun mouseMoved(ev: MouseEvent) {
            head.isVisible = true
            val (x, y) = normalizeCoords(ev)
            head.x = x
            head.y = y - 8
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

        override fun mouseExited(ev: MouseEvent) {
            head.isVisible = false
        }

        private fun addElement(element: Element, x: Double, y: Double) {
            val head = NoteHead(x, y - 8, element)
            children.add(head)
            noteHeads[element] = head
        }

        private fun startElementCreation(element: ContinuousElement, moment: Moment, x: Double, y: Double) {
            element.start = moment
            element.startDynamic = dynamicsSelector.selected.value
            val head = NoteHead(x, y - 8, element, NoteHead.State.InCreation)
            disposition = ElementInCreation(element, head)
        }

        private fun getPitch(y: Double) = Pitch(4, PitchName.C, accidentalSelector.selected.value)
    }

    private inner class ElementInCreation(
        private val element: ContinuousElement,
        private val noteHead: NoteHead
    ) : Disposition() {
        private val line = Line(noteHead.x + 20, noteHead.y + 8, noteHead.x + 20, noteHead.y + 8)
        private val removableNodes = mutableListOf<Node>()

        init {
            line.strokeWidth = 5.0
        }

        override fun init() {
            add(noteHead)
            add(line)
            add(DynamicViewElement(noteHead.x, noteHead.y - 5, element::startDynamic))
        }

        override fun replaced() {
            for (node in removableNodes) children.remove(node)
        }

        override fun keyTyped(ev: KeyEvent) {
            disposition = CreateElement()
            super.keyTyped(ev)
        }

        override fun mouseClicked(ev: MouseEvent) {
            val (x, _) = normalizeCoords(ev)
            if (element.climax == null) {
                element.climax = getMoment(x)
                element.climaxDynamic = dynamicsSelector.selected.value
                add(DynamicViewElement(x, noteHead.y, element::climaxDynamic))
            } else {
                element.end = getMoment(x)
                element.endDynamic = dynamicsSelector.selected.value
                add(DynamicViewElement(x, noteHead.y, element::endDynamic))
                element.instrument = instrumentSelector.selected.value
                removableNodes.clear()
                disposition = CreateElement()
            }
        }

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
        private val OPEN = KeyCodeCombination.valueOf("Ctrl+O")
        private val SAVE = KeyCodeCombination.valueOf("Ctrl+S")
        private val PLAY = KeyCodeCombination.valueOf("Ctrl+SPACE")
        private val TYPESET = KeyCodeCombination.valueOf("Ctrl+P")

        private fun getMoment(x: Double) = Moment(x.toInt() / 40 - 10, (x.toInt() / 20) % 2)
    }
}