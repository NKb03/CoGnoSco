package wittgenstein.gui

import wittgenstein.Instrument

class InstrumentSelector : SelectorBar<Instrument>(Instrument.values().asList()) {
    override fun extractText(option: Instrument): String = option.shortName

    override fun extractDescription(option: Instrument): String = option.fullName
}