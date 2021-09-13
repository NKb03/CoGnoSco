\score {
  \new GrandStaff <<
    \time 1/4\new StaffGroup <<
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Flöte 1" shortInstrumentName="Fl. 1" } <<
          \fluteOneMusic
        >>
        \new Staff \with { instrumentName = "Flöte 2" shortInstrumentName="Fl. 2" } <<
          \fluteTwoMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Oboe 1" shortInstrumentName="Ob. 1" } <<
          \oboeOneMusic
        >>
        \new Staff \with { instrumentName = "Oboe 2" shortInstrumentName="Ob. 2" } <<
          \oboeTwoMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "B♭ Klarinette 1" shortInstrumentName="B♭ Kl. 1" } <<
          \clarinetOneMusic
        >>
        \new Staff \with { instrumentName = "B♭ Klarinette 2" shortInstrumentName="B♭ Kl. 2" } <<
          \clarinetTwoMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Sopransaxophon" shortInstrumentName="S.sax." } <<
          \saxophoneMusic
        >>
      >>
    >>
    \new StaffGroup <<
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "F Horn 1 & 3" shortInstrumentName="F Hn. 1 & 3" } <<
          \hornOneThreeMusic
        >>
        \new Staff \with { instrumentName = "F Horn 2 & 4" shortInstrumentName="F Hn. 2 & 4" } <<
          \hornTwoFourMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "B♭ Trompete 1" shortInstrumentName="B♭ Tpt. 1" } <<
          \trumpetOneMusic
        >>
        \new Staff \with { instrumentName = "B♭ Trompete 2" shortInstrumentName="B♭ Tpt. 2" } <<
          \trumpetTwoMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Posaune" shortInstrumentName="Pos." } <<
          \tromboneMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Tuba" shortInstrumentName="Tba." } <<
          \tubaMusic
        >>
      >>
    >>
    \new StaffGroup <<
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Pauke" shortInstrumentName="Pk." } <<
          \timpaniMusic
        >>
      >>
    >>
    \new StaffGroup <<
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new RhythmicStaff \with { instrumentName = "Snare Drum" shortInstrumentName="Sn.Dr." } <<
          \snaredrumMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new RhythmicStaff \with { instrumentName = "Bass Drum" shortInstrumentName="B.Dr." } <<
          \bassdrumMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new RhythmicStaff \with { instrumentName = "Becken" shortInstrumentName="Bck." } <<
          \cymbalMusic
        >>
      >>
    >>
    \new StaffGroup <<
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Violinen 1" shortInstrumentName="Vl. 1" } <<
          \violinsOneMusic
        >>
        \new Staff \with { instrumentName = "Violinen 2" shortInstrumentName="Vl. 2" } <<
          \violinsTwoMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Viola" shortInstrumentName="Vla." } <<
          \violasMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Violoncello 1" shortInstrumentName="Vc. 1" } <<
          \violoncellosOneMusic
        >>
        \new Staff \with { instrumentName = "Violoncello 2" shortInstrumentName="Vc. 2" } <<
          \violoncellosTwoMusic
        >>
      >>
      \new StaffGroup \with { systemStartDelimiter = #'SystemStartSquare } <<
        \new Staff \with { instrumentName = "Kontrabass" shortInstrumentName="Kb." } <<
          \contrabassesMusic
        >>
      >>
    >>
  >>
}
