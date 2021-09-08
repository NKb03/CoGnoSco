package cognosco.lily

enum class Technique(
    val beforeNote: String = "",
    val afterNote: String = "",
    val afterLast: String = "",
    val everyNote: Boolean = false
) {
    Ordinario(afterNote = "^\\markup{ord.}"),
    Staccato(afterNote = "-.", everyNote = true),
    Pizzicato(afterNote = "^\\markup{pizz.}"),
    SlapTongue(afterNote = "^\\markup{s.t}"),
    ColLegnoBattuto(afterNote = "^\\markup{col legno battuto}"),
    ColLegnoTratto(afterNote = "^\\markup{col legno tratto}"),
    FlutterTongue(afterNote = "^\\markup{Fltzg.}"),
    Noisy(afterNote = "^\\markup{rauschig, mit viel Luft}"),
    DrumRoll,
    Bang(beforeNote = "\\improvisationOn ", afterLast = "\\improvisationOff ")
}