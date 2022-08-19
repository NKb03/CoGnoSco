package cognosco.midi

import java.util.*
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.system.exitProcess

fun fail(msg: String): Nothing {
    System.err.println(msg)
    exitProcess(1)
}

fun askInt(msg: String): Int {
    print(msg)
    return try {
        Scanner(System.`in`.reader()).nextInt().takeIf { it != -1 } ?: exitProcess(0)
    } catch (e: InputMismatchException) {
        println("Invalid frequency input")
        return askInt(msg)
    } catch (e: NoSuchElementException) {
        exitProcess(0)
    } catch (e: Exception) {
        fail("Error while gathering frequency input")
    }
}

data class ParameterSlide<T : Any>(val from: T, val to: T, val duration: Int)

fun midiStep(freq: Double) = 12 * log2(freq / 440.0) + 69

fun centToBend(cent: Double) = (cent / 400.0 * 16348).toInt()

class Line(private val channel: MidiChannel) {
    private var lastPitch = -1

    var freq = 0.0
        set(value) {
            field = value
            val stepExact = midiStep(value)
            val step = stepExact.toInt()
            val bendInCent = (stepExact - step) * 100
            channel.pitchBend = centToBend(bendInCent)
            if (step != lastPitch) {
                channel.allNotesOff()
                channel.noteOn(step, 127)
                lastPitch = step
            }
        }

    var volume = 0
        set(value) {
            field = value
            channel.controlChange(7, volume)
        }
    private var freqSlide: ParameterSlide<Double>? = null
    private var volumeSlide: ParameterSlide<Int>? = null

    fun play() {
        channel.noteOn(midiStep(freq).toInt(), 127)
    }

    fun pause() {
        channel.allNotesOff()
    }

    fun slideVolume(target: Int, duration: Int) {
        volumeSlide = ParameterSlide(volume, target, duration)
    }

    fun slideFrequency(target: Double, duration: Int) {
        freqSlide = ParameterSlide(freq, target, duration)
    }

    fun receivePulse(t: Int) {
        if (freqSlide != null) {
            val (from, to, duration) = freqSlide!!
            val prop = t / duration.toDouble()
            freq = (prop * to + (1 - prop) * from)
            if (t == duration) freqSlide = null
        }
        if (volumeSlide != null) {
            val (from, to, duration) = volumeSlide!!
            val prop = t / duration.toDouble()
            volume = (prop * to + (1 - prop) * from).toInt()
            if (t == duration) volumeSlide = null
        }
    }

    fun programChange(program: Int) {
        channel.programChange(program)
        pause()
        play()
    }
}

fun volumeOfPartial(i: Int): Int = (127 - ((i - 1) * 1600.0).pow(0.46)).toInt()

const val GRANULARITY = 100

fun main() {
    val synth = MidiSystem.getSynthesizer()
    synth.open()
    val initialFreq = askInt("Initial frequency: ").toDouble()
    val channels = synth.channels
    Thread.sleep(10)
    val lines = channels.map { ch -> Line(ch) }
    for ((i, line) in lines.withIndex()) {
        line.freq = (i + 1) * initialFreq
        line.volume = volumeOfPartial(i + 1)
        line.programChange(85)
    }
    lines.forEach { l -> l.play() }
    while (true) {
        val next = askInt("next frequency: ").toDouble()
        val duration = askInt("duration: ") * (1000 / GRANULARITY)
        val available = lines.toMutableSet()
        val unmatched = (1..lines.size).toMutableSet()
        for (i in (1..lines.size).shuffled().take(8)) {
            val fr = i * next
            val nearestLine = available.minByOrNull { l -> abs(midiStep(l.freq) - midiStep(fr)) }!!
            available.remove(nearestLine)
            unmatched.remove(i)
            nearestLine.slideFrequency(fr, duration)
            nearestLine.slideVolume(volumeOfPartial(i), duration)
        }
        for ((l, i) in available.shuffled().zip(unmatched)) {
            l.slideFrequency(i * next, duration)
            l.slideVolume(volumeOfPartial(i), duration)
        }
        for (t in 1..duration) {
            Thread.sleep(GRANULARITY.toLong())
            lines.forEach { l -> l.receivePulse(t) }
        }
        for (l in lines) {
            println("${l.freq} Hz at volume ${l.volume}")
        }
    }

} //52, 63, 101, 42, 69, 85,