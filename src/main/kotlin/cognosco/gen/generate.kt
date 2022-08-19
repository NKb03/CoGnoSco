package cognosco.gen

import cognosco.*
import cognosco.ContinuousNoise.Type.Breath
import cognosco.InstrumentFamily.Percussion
import cognosco.InstrumentFamily.Timpani
import cognosco.RegularAccidental.Flat
import cognosco.RegularAccidental.Natural
import cognosco.SimplePitchedContinuousElement.Type.*
import cognosco.lily.myOrchestra
import cognosco.lily.staffs
import javafx.beans.property.SimpleObjectProperty
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

fun nearbyPitches(a: List<Pitch>, b: List<Pitch>): List<Pair<Pitch, Pitch>> = TODO()

private val spectra =
    listOf(
        Spectrum(Pitch(2, PitchName.D, Natural)),
        Spectrum(Pitch(1, PitchName.E, Natural)),
        Spectrum(Pitch(0, PitchName.B, Flat)),
        Spectrum(Pitch(0, PitchName.C, Natural)),
        Spectrum(Pitch(-1, PitchName.A, Flat))
    )

data class SpectrumSection(val spectrum: Spectrum, val start: Int, val peak: Int, val end: Int)

fun spectrumOptions(totalDuration: Int, sections: List<SpectrumSection>) =
    sections.map { (spectrum, start, peak, end) ->
        spectrum withProbability sequential(
            constant(0.0) withDuration start,
            linearDevelopment(0.0, 1.0) withDuration peak - start,
            linearDevelopment(1.0, 0.0) withDuration end - peak,
            constant(0.0) withDuration totalDuration - end
        )
    }// + sections.zipWithNext { (from, _, peak1, _), (to, _, peak2, _) ->
//        val p = (sqrt(5.0) * sqrt((peak1 - peak2).toDouble().pow(2) + 3 * peak1 - 1 * peak2)).toInt()
//        SpectralTransition(from, to) withProbability sequential(
//            constant(0.0) withDuration peak1,
//            linearDevelopment(0.0, 1.0) withDuration p - peak1,
//            linearDevelopment(1.0, 1.0) withDuration peak2 - p,
//            constant(0.0) withDuration totalDuration - peak2
//        )
//    }

private const val TOTAL_DURATION = 680

val sections = listOf(
    SpectrumSection(spectra[0], 0, 0, 50),
    SpectrumSection(spectra[1], 0, 50, 130),
    SpectrumSection(spectra[2], 50, 130, 180),
    SpectrumSection(spectra[3], 130, 210 /*180*/, 210),
    SpectrumSection(spectra[4], 180, 210, 340)
)

val random = Random(1000)

val spectrumDevelopment =
    stochasticDevelopment(random, spectrumOptions(TOTAL_DURATION, sections)) withDuration TOTAL_DURATION

val elementTypeDevelopment = stochasticDevelopment(
    random,
    Trill withProbability sequential(
        constant(1.0) withDuration 210,
        constant(0.0) withDuration 130
    ),
    FastRepeat withProbability sequential(
        constant(0.0) withDuration 130,
        linearDevelopment(0.0, 1.0) withDuration 30,
        constant(1.0) withDuration 50,
        constant(0.0) withDuration 130
    ),
    Repeat withProbability sequential(
        constant(0.0) withDuration 180,
        linearDevelopment(0.0, 1.0) withDuration 20,
        constant(1.0) withDuration 10,
        constant(0.0) withDuration 130
    ),
    Regular withProbability sequential(
        constant(0.0) withDuration 210,
        linearDevelopment(1.0, 0.0) withDuration 80,
        constant(0.0) withDuration 50
    ),
    Noisy withProbability sequential(
        constant(0.0) withDuration 210,
        linearDevelopment(0.0, 1.0) withDuration 80,
        linearDevelopment(1.0, 0.0) withDuration 30,
        constant(0.0) withDuration 20
    ),
    Breath withProbability sequential(
        constant(0.0) withDuration 290,
        linearDevelopment(0.0, 1.0) withDuration 50
    )
) withDuration TOTAL_DURATION


val peakRatioDevelopment = sequential(
    linearDevelopment(0.8, 0.5) withDuration 50,
    linearDevelopment(0.7, 0.4) withDuration 80,
    linearDevelopment(0.6, 0.3) withDuration 50,
    linearDevelopment(0.5, 0.2) withDuration 30,
    linearDevelopment(0.7, 0.2) withDuration 130
) withDuration TOTAL_DURATION

private fun dynamicDevelopment(from: Double, to: Double) =
    rangeDevelopment(random, average = linearDevelopment(from, to), maxDerivation = constant(1.0))
        .coerceAtMost(maxOf(from, to))
        .coerceAtLeast(minOf(from, to))

val dynamicDevelopment: FixedDurationDevelopment<Dynamic> = sequential(
    dynamicDevelopment(1.0, 3.0) withDuration 35,
    dynamicDevelopment(3.0, 1.0) withDuration 15,
    dynamicDevelopment(1.0, 4.0) withDuration 55,
    dynamicDevelopment(4.0, 2.0) withDuration 25,
    dynamicDevelopment(2.0, 5.0) withDuration 30,
    dynamicDevelopment(5.0, 3.0) withDuration 20,
    dynamicDevelopment(3.0, 7.0) withDuration 30,
    dynamicDevelopment(1.0, 4.0) withDuration 50,
    dynamicDevelopment(3.0, 0.0) withDuration 80,
).transform("associated dynamic") { v -> Dynamic.values()[v.roundToInt()] }
    .withDuration(TOTAL_DURATION)

private fun rhomboidPitchDevelopment(
    minDerivation: Int,
    maxDerivation: Int,
    toPeak: Int,
    toEnd: Int,
    gravity: Double = 1.0
) = sequential(
    rangeDevelopment(
        random,
        average = constant(62),
        maxDerivation = linearDevelopment(minDerivation, maxDerivation),
        gravity = constant(gravity)
    ) withDuration toPeak,
    rangeDevelopment(
        random,
        average = constant(62),
        maxDerivation = linearDevelopment(maxDerivation, minDerivation),
        gravity = constant(gravity)
    ) withDuration toEnd
)

val pitchRangeDevelopment = sequential(
    constant(62) withDuration 50,
    rhomboidPitchDevelopment(minDerivation = 4, maxDerivation = 13, toPeak = 55, toEnd = 25),
    rhomboidPitchDevelopment(minDerivation = 6, maxDerivation = 20, toPeak = 30, toEnd = 20),
    rhomboidPitchDevelopment(minDerivation = 8, maxDerivation = 31, toPeak = 30, toEnd = 0),
    rhomboidPitchDevelopment(minDerivation = 6, maxDerivation = 20, toPeak = 50, toEnd = 30),
    rhomboidPitchDevelopment(minDerivation = 4, maxDerivation = 13, toPeak = 15, toEnd = 35)
).withDuration(TOTAL_DURATION)

private fun densityDevelopment(from: Double, to: Double) =
    rangeDevelopment(
        random,
        average = linearDevelopment(from, to),
        maxDerivation = constant(0.1)
    ).smoothIn().coerceAtMost(1.0).coerceAtLeast(0.05)

val densityDevelopment = sequential(
    densityDevelopment(0.1, 0.4) withDuration 35,
    densityDevelopment(0.4, 0.15) withDuration 15,
    densityDevelopment(0.15, 0.5) withDuration 55,
    densityDevelopment(0.7, 0.25) withDuration 25,
    densityDevelopment(0.22, 0.6) withDuration 30,
    densityDevelopment(0.6, 0.25) withDuration 20,
    densityDevelopment(0.25, 0.7) withDuration 30,
    densityDevelopment(0.15, 0.4) withDuration 50,
    densityDevelopment(0.4, 0.15) withDuration 60,
    densityDevelopment(0.15, 0.0) withDuration 20
) withDuration TOTAL_DURATION

fun elementPhase(end: Int, pitch: Pitch?, dynamic: Dynamic) =
    ElementPhase(SimpleObjectProperty(end), SimpleObjectProperty(pitch), SimpleObjectProperty(dynamic))

val developments = listOf(
    spectrumDevelopment,
    elementTypeDevelopment,
    peakRatioDevelopment,
    dynamicDevelopment,
    densityDevelopment,
    pitchRangeDevelopment
)

fun main(vararg args: String) {
    val staffs = myOrchestra.staffs.filter { it.instrument.family !in setOf(Percussion, Timpani) }
    val ends = IntArray(staffs.size)
    for (dev in developments) check(dev.duration == TOTAL_DURATION)
    val elements = mutableListOf<Element>()
    var t = 0
    while (t + 15 < TOTAL_DURATION) {
        t += (1 / densityDevelopment.at(t)).roundToInt()
        val elementType = elementTypeDevelopment.at(t)
        val pitch = pitchRangeDevelopment.at(t)
        val staffIdx = ends.withIndex()
            .filter { (i, _) -> elementType in staffs[i].instrument.supportedElementTypes }
            .filter { (i, _) -> elementType !is PitchedElement || pitch in staffs[i].instrument.range }
            .minByOrNull { (_, v) -> v }!!
            .index
        val element = elementType.createElement() as ContinuousElement
        val instr = staffs[staffIdx].instrument
        element.instrument.value = instr
        if (element is PitchedElement) {
            val spectrum = spectrumDevelopment.at(t)
            val cent = 100 * pitch
            element.pitch.value = spectrum.minByOrNull { abs(cent - it.cent) }!!
            element.pitch.value = element.pitch.value.copy(register = element.pitch.value.register - 1)
        }
        if (element is Trill) {
            element.secondaryPitch.value = Pitch.fromFrequency(element.pitch.value.frequency * 1.05)
            if (element.secondaryPitch.value.diatonicStep == element.pitch.value.diatonicStep) {
                element.secondaryPitch.value = element.secondaryPitch.value.enharmonicEquivalent
            }
        }
        element.startDynamic.value = Dynamic.PPP
        val totalDuration = random.nextInt(10, 20)
        val peakRatio = peakRatioDevelopment.at(t)
        val toPeak = (peakRatio * totalDuration).toInt()
        val peakDynamic = dynamicDevelopment.at(t)
        element.start.value = t
        element.phases.value = listOf(
            elementPhase(t + toPeak, element.pitch.value, peakDynamic),
            elementPhase(t + totalDuration, element.pitch.value, Dynamic.PPP)
        )
        elements.add(element)
        ends[staffIdx] += totalDuration + 2
    }
    val score = GraphicalScore(elements)
    val dest = Files.getDirectory("scores").resolve(args[0] + ".json")
    dest.writeText(score.encodeToString())
//    workingDirectory.run("java", "-jar dist/cognosco.jar", dest.absolutePath)
}


/*
* Constants:
* - pool of elements for the individual spectra and their transitions
* + list of instruments and their ranges and capabilities
* Time-varying parameters:
* + probabilities for the individual spectra and their transitions
* + probabilities of the element types
* + position of the peak dynamik (~ ratio between rise and decline)
* (+) pitch range
* + dynamic range
* - density (range of rest-durations, range of distance between two cues)
* - instrumentation (is it possible? how to implement?)
* Open questions:
* - what about the middle d (maybe fix it beforehand to consider it in instrumentation)
* - what about the beginning and the end, can it be implemented?
* + good API for the time-varying parameters
* */
