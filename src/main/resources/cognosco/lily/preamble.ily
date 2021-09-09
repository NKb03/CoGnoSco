\language "nederlands"
\version "2.22.0"

#(set-global-staff-size 15)
#(set-default-paper-size "a3landscape")

\paper {
  indent = 2.0\cm  % space for instrumentName
  short-indent = 1.0\cm  % space for shortInstrumentName
}

\header {
  title = "Theseus' Schiff"
  subtitle = "Wittgenstein"
  tagline = ##f
  composer =  "Nikolaus Knop"
}

\layout {
  \context {
    \Voice
    \omit Stem
    \omit Beam
    \omit Flag
    \omit Dots
    \override ParenthesesItem.font-size=#7
 }
 \context {
   \Staff \RemoveEmptyStaves
 }
}

dashPlus = "trill"

