package cognosco.gui

import cognosco.*
import cognosco.gui.Shortcut.*
import cognosco.gui.impl.*
import cognosco.midi.MidiPlayer
import cognosco.midi.PULSES_PER_BEAT
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableDoubleValue
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.QuadCurve
import javafx.scene.shape.Shape
import javafx.scene.text.Text
import kotlin.properties.Delegates

class ScoreView(
    private val elementTypeSelector: ElementTypeSelector,
    private val accidentalSelector: AccidentalSelector,
    private val instrumentSelector: InstrumentSelector,
    private val dynamicsSelector: DynamicSelector,
    private val player: MidiPlayer
) : Pane() {
    private val elements = mutableListOf<Element>()
    private val associatedNodes = mutableMapOf<Element, MutableList<Node>>()
    private val noteHeads = mutableMapOf<Element, NoteHead>()
    private val startDynamics = mutableMapOf<Element, DynamicView>()
    private val phaseDynamics = mutableMapOf<ElementPhase, DynamicView>()
    private val pulseLine = createPulseLine()
    private val zoomFactor = SimpleDoubleProperty(1.0)

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
                !disposition.elementTypeChanged(new) -> disposition = CreateElement()
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
        val x = (ev.x / zoomFactor.value).toInt() / BEAT_W * BEAT_W.toDouble() * zoomFactor.value
        val y = (ev.y.toInt() + 12) / PITCH_H * PITCH_H.toDouble()
        return Pair(x, y.coerceIn(PITCH_H.toDouble(), H - PITCH_H))
    }

    private fun addVerticalLines() {
        for (i in 1..(W / BEAT_W)) {
            val l = Line()
            l.startXProperty().bind(zoomFactor.multiply(i * BEAT_W))
            l.endXProperty().bind(l.startXProperty())
            l.endY = H
            val g = if (i % 10 == 0) 0.3 else 0.7
            if (i % 2 != 0) l.strokeDashArray.addAll(2.0, 2.0)
            l.stroke = Color.gray(g)
            if (i % 10 == 0) {
                l.setOnMouseClicked {
                    if (!player.isPlaying) player.currentPulse = i * PULSES_PER_BEAT
                }
            }
            children.add(l)
        }
    }

    private fun createPulseLine() = Line().apply {
        endY = H
        stroke = Color.DODGERBLUE
        strokeWidth = 4.0
        endXProperty().bind(startXProperty())
        visibleProperty().bind(startXProperty().isNotEqualTo(0))
        children.add(this)
    }

    fun handleShortcut(shortcut: Shortcut) {
        when (shortcut) {
            ZoomIn -> zoomFactor.value *= 1.2
            ZoomOut -> zoomFactor.value /= 1.2
            else -> disposition.handleShortcut(shortcut)
        }
    }

    private fun add(element: Element, vararg nodes: Node) {
        associatedNodes.getOrPut(element) { mutableListOf() }.addAll(nodes)
        children.addAll(*nodes)
    }

    private fun deleteElement(element: Element) {
        check(elements.remove(element)) { "cannot remove non-existent element $element" }
        associatedNodes.remove(element)?.forEach { children.remove(it) }
        noteHeads.remove(element)
        startDynamics.remove(element)
        if (element is ContinuousElement) {
            for (phase in element.phases.value) {
                phaseDynamics.remove(phase)
            }
        }
    }

    fun clearScore() {
        for (el in elements.toList()) {
            deleteElement(el)
        }
        elements.clear()
    }

    private fun addElement(element: Element) {
        val head = NoteHead(element)
        head.xProperty().bind(element.start.toXCoordinate())
        head.noteHeadType = element.type.value.noteHeadType
        element.type.addListener { _, _, new -> head.noteHeadType = new.noteHeadType }
        noteHeads[element] = head
        val dynamic = DynamicView(head.xProperty(), head.yProperty(), element.startDynamic, element.start)
        startDynamics[element] = dynamic
        add(element, head, dynamic)
        if (element is PitchedElement) {
            head.yProperty().bind(element.pitch.getY())
            val accidental = AccidentalView(element.pitch.map { it.accidental }, head)
            add(element, accidental)
        } else {
            val customY = element.customY ?: error("no custom-y provided for '$element'")
            head.yProperty().bind(customY)
        }
        val ledgerLines = createLedgerLines(head)
        for (line in ledgerLines) {
            add(element, line)
        }
        if (element is ContinuousElement && element.phases.value.isNotEmpty()) {
            lateinit var phaseTarget: DynamicView
            for (phase in element.phases.value) {
                phaseTarget =
                    DynamicView(phase.end.toXCoordinate(), head.yProperty(), phase.targetDynamic, phase.end)
                add(element, phaseTarget)
            }
            val line = createDurationLine(element, head)
            line.endXProperty().bind(Bindings.add(phaseTarget.xProperty(), 10))
            add(element, line.shape)
        }
        if (element is Trill && element.secondaryPitch.value != null) {
            val littleHead = NoteHead(element).scale(SECONDARY_PITCH_SCALE)
            val accidental = element.secondaryPitch.map { p -> p!!.accidental }
            val littleAccidental = AccidentalView(accidental, littleHead)
                .scale(SECONDARY_PITCH_SCALE)
            layoutSecondaryPitch(element, head, littleHead, littleAccidental)
        }
        elements.add(element)
    }

    private fun createDurationLine(element: ContinuousElement, head: NoteHead): ILine {
        val line = if (element.type.value == Trill) ZigZagLine(3.0) else LineAdapter()
        line.startXProperty().bind(Bindings.add(head.xProperty(), 15))
        line.startYProperty().bind(Bindings.add(head.yProperty(), 8))
        line.endYProperty().bind(Bindings.add(head.yProperty(), 8))
        line.strokeWidth = if (element.type.value == Trill) 3.0 else 5.0
        line.strokeDashArray.addAll(element.type.value.strokeDashArray)
        element.type.addListener { _, _, new -> line.strokeDashArray.setAll(new.strokeDashArray) }
        return line
    }

    private fun layoutSecondaryPitch(trill: Trill, head: NoteHead, littleHead: NoteHead, accidental: AccidentalView) {
        val lp = NoteHead.leftParentheses().scale(SECONDARY_PITCH_SCALE)
        val rp = NoteHead.rightParentheses().scale(SECONDARY_PITCH_SCALE)
        lp.strokeProperty().bind(littleHead.root.fillProperty())
        rp.strokeProperty().bind(littleHead.root.fillProperty())
        littleHead.xProperty().bind(doubleBinding(littleHead.yProperty(), head.yProperty(), head.xProperty()) {
            if (littleHead.y == head.y) head.x + 35 else head.x + 15
        })
        littleHead.yProperty().bind(trill.secondaryPitch.getY())
        lp.layoutXProperty().bind(accidental.xProperty().subtract(7))
        rp.layoutXProperty().bind(littleHead.xProperty().add(5))
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

    private fun createLedgerLines(head: NoteHead): List<Line> {
        val lines = mutableListOf<Line>()
        for (y in 8 * PITCH_H * 2 downTo head.y.toInt() + 8 step 50) {
            lines.add(createLedgerLine(head, y.toDouble()))
        }
        for (y in 18 * PITCH_H * 2..head.y.toInt() + 8 step 50) {
            lines.add(createLedgerLine(head, y.toDouble()))
        }
        return lines
    }

    private fun createLedgerLine(head: NoteHead, y: Double): Line = Line().apply {
        strokeWidth = 3.0
        startXProperty().bind(head.xProperty().subtract(4))
        endXProperty().bind(startXProperty().add(30))
        startY = y
        endY = y
    }

    fun openScore(score: GraphicalScore) {
        clearScore()
        for (el in score.elements) {
            addElement(el)
        }
    }

    fun getScore() = GraphicalScore(elements)

    fun setCurrentPulse(pulse: Int) {
        pulseLine.startXProperty().value = pulse.toDouble() / PULSES_PER_BEAT * BEAT_W * zoomFactor.value
    }

    private abstract inner class Disposition {
        open fun init(old: Disposition) {}

        open fun replaced(new: Disposition) {}

        open fun handleShortcut(ev: Shortcut) {}

        open fun mouseClicked(ev: MouseEvent) {}

        open fun mouseMoved(ev: MouseEvent) {}

        open fun mouseEntered(ev: MouseEvent) {}

        open fun mouseExited(ev: MouseEvent) {}

        open fun elementTypeChanged(type: Element.Type<*>): Boolean = false

        open fun instrumentChanged(instr: Instrument) {}

        open fun accidentalChanged(acc: Accidental) {}

        open fun dynamicChanged(dynamic: Dynamic) {}
    }

    private abstract inner class EditElement : Disposition() {
        protected open fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            tryWithElement { element, head -> block(element, head); false }
        }

        protected open fun tryWithElement(block: (element: Element, head: NoteHead) -> Boolean): Boolean? {
            withElement { element, head -> block(element, head) }
            return null
        }

        protected abstract fun withDynamic(block: (element: DynamicView) -> Unit)

        override fun instrumentChanged(instr: Instrument): Unit = withElement { element, _ ->
            element.instrument.value = instr
        }

        override fun accidentalChanged(acc: Accidental): Unit = withElement { element, head ->
            if (element is Trill && head.scaleX == SECONDARY_PITCH_SCALE) {
                element.secondaryPitch.value = element.secondaryPitch.value.copy(accidental = acc)
            } else if (element is PitchedElement) {
                element.pitch.value = element.pitch.value.copy(accidental = acc)
            }
        }

        override fun dynamicChanged(dynamic: Dynamic) {
            withDynamic { element ->
                element.dynamic.value = dynamic
            }
            withElement { element, _ ->
                startDynamics[element]?.dynamic?.value = dynamic
            }
        }

        override fun elementTypeChanged(type: Element.Type<*>): Boolean =
            tryWithElement { element, _ -> element.setType(type) } ?: false

        override fun handleShortcut(ev: Shortcut) {
            when (ev) {
                Delete -> deleteElement()
                Left -> moveLeft()
                Right -> moveRight()
                Up -> moveUp()
                Down -> moveDown()
                else -> {
                }
            }
        }

        private fun moveLeft() {
            withElement { element, head ->
                if (head.scaleX == 1.0) {
                    element.start.value -= 1
                }
            }
            withDynamic { element ->
                element.time.value -= 1
            }
        }

        private fun moveRight() {
            withElement { element, head ->
                if (head.scaleX == 1.0) {
                    element.start.value += 1
                }
            }
            withDynamic { element ->
                element.time.value += 1
            }
        }

        private fun moveUp() = withElement { element, head ->
            when {
                element is Trill && head.scaleX == SECONDARY_PITCH_SCALE ->
                    element.secondaryPitch.value = element.secondaryPitch.value.up()
                element is PitchedElement -> element.pitch.value = element.pitch.value.up()
                else -> element.customY!!.value -= PITCH_H
            }
        }

        private fun moveDown() = withElement { element, head ->
            when {
                element is Trill && head.scaleX == SECONDARY_PITCH_SCALE ->
                    element.secondaryPitch.value = element.secondaryPitch.value.down()
                element is PitchedElement -> element.pitch.value = element.pitch.value.down()
                else -> element.customY!!.value += PITCH_H
            }
        }

        protected open fun deleteElement() = withElement { element, head ->
            if (element is Trill && head.scaleX == SECONDARY_PITCH_SCALE) {
                val type = SimpleObjectProperty(SimplePitchedContinuousElement.Type.Regular)
                val subst = SimplePitchedContinuousElement(type)
                subst.copyFrom(element)
                deleteElement(element)
                addElement(subst)
            } else deleteElement(element)
        }
    }

    private inner class Pointer : EditElement() {
        private val selected = mutableSetOf<SelectableElement>()

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            for (el in selected.filterIsInstance<NoteHead>()) {
                if (el.element != null) block(el.element, el)
            }
        }

        override fun tryWithElement(block: (element: Element, head: NoteHead) -> Boolean): Boolean? {
            var ok = true
            val heads = selected.filterIsInstance<NoteHead>().ifEmpty { return null }
            for (head in heads) {
                if (head.element != null && !block(head.element, head)) {
                    ok = false
                }
            }
            return ok
        }

        override fun withDynamic(block: (element: DynamicView) -> Unit) {
            for (el in selected.filterIsInstance<DynamicView>()) {
                block(el)
            }
        }

        override fun replaced(new: Disposition) {
            deselectAll()
        }

        private fun deselectAll() {
            for (element in selected) {
                if (element is NoteHead) {
                    for (node in associatedNodes[element.element].orEmpty()) {
                        when (node) {
                            is QuadCurve -> {
                            }
                            is Shape -> {
                                if (!node.fillProperty().isBound) node.fill = Color.BLACK
                                if (!node.strokeProperty().isBound && node !is Text) node.stroke = Color.BLACK
                            }
                        }
                    }
                }
                element.isSelected = false
            }
            selected.clear()
        }

        override fun mouseClicked(ev: MouseEvent) {
            val element = ev.target?.findParentOfType<SelectableElement>()
            select(element, extendSelection = ev.isShiftDown)
        }

        fun select(element: SelectableElement?, extendSelection: Boolean = false) {
            if (!extendSelection) deselectAll()
            if (element == null) return
            selected.add(element)
            if (!extendSelection) {
                when (element) {
                    is DynamicView -> element.dynamic.let { dynamicsSelector.select(it.value) }
                    is NoteHead -> {
                        val el = element.element ?: return
                        el.instrument.value?.let { instrumentSelector.select(it) }
                        if (el is PitchedElement) {
                            val pitch =
                                if (el is Trill && element.scaleX == SECONDARY_PITCH_SCALE) el.secondaryPitch.value
                                else el.pitch.value
                            accidentalSelector.select(pitch.accidental)
                        }
                        for (node in associatedNodes[el].orEmpty()) {
                            when (node) {
                                is QuadCurve -> {
                                }
                                is Shape -> {
                                    if (!node.fillProperty().isBound) node.fill = Color.CADETBLUE
                                    if (!node.strokeProperty().isBound && node !is Text) node.stroke = Color.CADETBLUE
                                }
                            }
                        }
                    }
                }
            }
            element.isSelected = true
        }

        override fun deleteElement() {
            super.deleteElement()
            selected.clear()
        }
    }

    private inner class CreateElement(
        private var lastCreated: NoteHead? = null,
        private var lastCreatedDynamic: DynamicView? = null
    ) : EditElement() {
        private val phantomHead = NoteHead().phantom()
        private var ledgerLines = emptyList<Line>()
        private val phantomAccidental = AccidentalView(accidentalSelector.selected, phantomHead).phantom()

        init {
            phantomAccidental.visibleProperty().bind(phantomHead.visibleProperty())
        }

        override fun tryWithElement(block: (element: Element, head: NoteHead) -> Boolean): Boolean? {
            val el = lastCreated
            return if (el?.element != null) block(el.element, el) else null
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) {
            if (lastCreated != null) block(lastCreated!!.element!!, lastCreated!!)
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
            children.removeAll(ledgerLines)
            lastCreated?.regular()
            if (new is Pointer) {
                new.select(lastCreated)
            }
        }

        override fun elementTypeChanged(type: Element.Type<*>): Boolean {
            phantomHead.noteHeadType = type.noteHeadType
            return true
        }

        override fun instrumentChanged(instr: Instrument) {}

        override fun dynamicChanged(dynamic: Dynamic) {}

        override fun mouseEntered(ev: MouseEvent) {
            phantomHead.isVisible = true
            ledgerLines.forEach { it.isVisible = true }
        }

        override fun mouseMoved(ev: MouseEvent) {
            val (x, y) = normalizeCoords(ev)
            phantomHead.isVisible = true
            phantomHead.x = x
            phantomHead.y = y - 8
            updateLedgerLines()
        }

        private fun updateLedgerLines() {
            children.removeAll(ledgerLines)
            ledgerLines = createLedgerLines(phantomHead)
            children.addAll(ledgerLines)
        }

        override fun mouseExited(ev: MouseEvent) {
            phantomHead.isVisible = false
            ledgerLines.forEach { it.isVisible = false }
        }

        override fun mouseClicked(ev: MouseEvent) {
            val (x, y) = normalizeCoords(ev)
            val element = elementTypeSelector.selected.value?.createElement() ?: return
            element.start.value = getTime(x)
            element.startDynamic.value = dynamicsSelector.selected.value
            element.instrument.value = instrumentSelector.selected.value
            if (element is PitchedElement) element.pitch.value = getPitch(y)
            else element.customY!!.value = y - 8
            addElement(element)
            lastCreated?.regular()
            when (element) {
                is Trill -> disposition = TrillInCreation(element)
                is ContinuousElement -> disposition = ElementInCreation(element)
                else -> {
                    val head = noteHeads.getValue(element)
                    lastCreated = head
                    head.lastCreated()
                    lastCreatedDynamic = startDynamics[element]
                }
            }
        }

        private fun getPitch(y: Double): Pitch =
            Pitch.fromDiatonicStep(52 - y.toInt() / PITCH_H, accidentalSelector.selected.value)
    }

    private inner class ElementInCreation(private val element: ContinuousElement) : EditElement() {
        private val head = noteHeads.getValue(element)
        private val line = createDurationLine(element, head)
        private var minX = head.x + BEAT_W * zoomFactor.value
        private var finished = false

        override fun init(old: Disposition) {
            head.inCreation()
            add(element, line.shape)
            line.endXProperty().set(minX)
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) = block(element, head)

        override fun withDynamic(block: (element: DynamicView) -> Unit) {}

        override fun tryWithElement(block: (element: Element, head: NoteHead) -> Boolean): Boolean =
            block(element, head)

        override fun replaced(new: Disposition) {
            if (!finished) deleteElement(element)
            else {
                head.isSelected = true
                if (new is Pointer) new.select(head)
            }
        }

        override fun handleShortcut(ev: Shortcut) {
            if (ev == Enter && element.phases.value.isNotEmpty()) {
                finish()
            }
        }

        private fun finish() {
            val endDynamic = phaseDynamics.getValue(element.phases.value.last())
            line.endXProperty().bind(endDynamic.xProperty().add(10))
            finished = true
            disposition = CreateElement(head, null)
        }

        override fun mouseClicked(ev: MouseEvent) {
            var (x, _) = normalizeCoords(ev)
            x = x.coerceAtLeast(minX)
            val time = getTime(x)
            val phase = ElementPhase(
                SimpleObjectProperty(time),
                SimpleObjectProperty(element.pitch.value),
                SimpleObjectProperty(dynamicsSelector.selected.value)
            )
            element.phases.value += phase
            val dynamic = DynamicView(phase.end.toXCoordinate(), head.yProperty(), phase.targetDynamic, phase.end)
            phaseDynamics[phase] = dynamic
            add(element, dynamic)
            minX = x + BEAT_W * zoomFactor.value
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
                element.pitch.value = element.pitch.value.copy(accidental = acc)
            }
        }

        override fun dynamicChanged(dynamic: Dynamic) {}
    }

    private inner class TrillInCreation(private val trill: Trill) : EditElement() {
        private val head = noteHeads.getValue(trill)
        private val littleHead = NoteHead(trill).phantom().scale(SECONDARY_PITCH_SCALE)
        private val littleAccidental = AccidentalView(accidentalSelector.selected, littleHead)
            .phantom().scale(SECONDARY_PITCH_SCALE)
        private var finished = false

        init {
            trill.secondaryPitch.value = trill.pitch.value
            layoutSecondaryPitch(trill, head, littleHead, littleAccidental)
        }

        override fun withElement(block: (element: Element, head: NoteHead) -> Unit) = block(trill, head)

        override fun withDynamic(block: (element: DynamicView) -> Unit) = block(startDynamics.getValue(trill))

        override fun replaced(new: Disposition) {
            if (!finished) deleteElement(trill)
        }

        override fun handleShortcut(ev: Shortcut) {
            when (ev) {
                Up -> trill.secondaryPitch.value = trill.secondaryPitch.value.up()
                Down -> trill.secondaryPitch.value = trill.secondaryPitch.value.down()
                Enter -> finish()
                else -> {
                }
            }
        }

        private fun finish() {
            littleHead.regular()
            littleAccidental.regular()
            finished = true
            disposition = ElementInCreation(trill)
        }

        override fun accidentalChanged(acc: Accidental) {
            trill.secondaryPitch.value = trill.secondaryPitch.value.copy(accidental = acc)
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

    private fun getTime(x: Double): Time = (x / zoomFactor.value).toInt() / BEAT_W

    private fun ObservableValue<Time>.toXCoordinate(): DoubleBinding =
        zoomFactor.multiply(this.asObservableNumberValue()).multiply(BEAT_W)

    companion object {
        private const val W = 20000
        private const val H = 1100.0
        private const val BEAT_W = 20
        private const val PITCH_H = 25
        private const val SECONDARY_PITCH_SCALE = 0.7

        private fun ObservableValue<Pitch>.getY(): ObservableDoubleValue =
            mapDouble { p -> (52 - p.diatonicStep) * PITCH_H.toDouble() - 8 }

        private fun Pane.addHorizontalLines() {
            for (i in 7..17) {
                val l = Line(0.0, i * PITCH_H * 2.0, W.toDouble(), i * PITCH_H * 2.0)
                l.strokeWidth = if (i == 12) 5.0 else 3.0
                children.add(l)
            }
        }
    }
}