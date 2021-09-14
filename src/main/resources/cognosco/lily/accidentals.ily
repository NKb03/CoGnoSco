\version "2.22.0"

ekmTuning = #'(
  (#x1A . 3/4) ;tqs/tqf
  (#x28 . 2/3) ;s+31ct/f-31ct
  (#x36 . 7/12) ;s+13ct/f-13ct
  (#x44 . 1/2) ;s/f
  (#x50 . 5/12) ;s-13ct/f+13ct
  (#xA0 . 1/3) ;s-31ct/f+31ct
  (#xAA . 1/4) ;qs/qf
  (#xAC . 1/6) ;n+31ct/n-31ct
  (#xB6 . 1/12) ;n+13ct/n-13ct
)

ekmLanguages = #'(
(english . (
  (c 0 . 0)
  (d 1 . 0)
  (e 2 . 0)
  (f 3 . 0)
  (g 4 . 0)
  (a 5 . 0)
  (b 6 . 0)

  (cn 0 . 0)
  (dn 1 . 0)
  (en 2 . 0)
  (fn 3 . 0)
  (gn 4 . 0)
  (an 5 . 0)
  (bn 6 . 0)

  (cnu 0 . #xB6)
  (dnu 1 . #xB6)
  (enu 2 . #xB6)
  (fnu 3 . #xB6)
  (gnu 4 . #xB6)
  (anu 5 . #xB6)
  (bnu 6 . #xB6)

  (cnd 0 . #xB7)
  (dnd 1 . #xB7)
  (end 2 . #xB7)
  (fnd 3 . #xB7)
  (gnd 4 . #xB7)
  (and 5 . #xB7)
  (bnd 6 . #xB7)

  (cnuu 0 . #xAC)
  (dnuu 1 . #xAC)
  (enuu 2 . #xAC)
  (fnuu 3 . #xAC)
  (gnuu 4 . #xAC)
  (anuu 5 . #xAC)
  (bnuu 6 . #xAC)

  (cndd 0 . #xAD)
  (dndd 1 . #xAD)
  (endd 2 . #xAD)
  (fndd 3 . #xAD)
  (gndd 4 . #xAD)
  (andd 5 . #xAD)
  (bndd 6 . #xAD)

  (cs 0 . #x44)
  (ds 1 . #x44)
  (es 2 . #x44)
  (fs 3 . #x44)
  (gs 4 . #x44)
  (as 5 . #x44)
  (bs 6 . #x44)

  (cf 0 . #x45)
  (df 1 . #x45)
  (ef 2 . #x45)
  (ff 3 . #x45)
  (gf 4 . #x45)
  (af 5 . #x45)
  (bf 6 . #x45)

  (cqs 0 . #xAA)
  (dqs 1 . #xAA)
  (eqs 2 . #xAA)
  (fqs 3 . #xAA)
  (gqs 4 . #xAA)
  (aqs 5 . #xAA)
  (bqs 6 . #xAA)

  (cqf 0 . #xAB)
  (dqf 1 . #xAB)
  (eqf 2 . #xAB)
  (fqf 3 . #xAB)
  (gqf 4 . #xAB)
  (aqf 5 . #xAB)
  (bqf 6 . #xAB)

  (ctqs 0 . #x1A)
  (dtqs 1 . #x1A)
  (etqs 2 . #x1A)
  (ftqs 3 . #x1A)
  (gtqs 4 . #x1A)
  (atqs 5 . #x1A)
  (btqs 6 . #x1A)

  (ctqf 0 . #x1B)
  (dtqf 1 . #x1B)
  (etqf 2 . #x1B)
  (ftqf 3 . #x1B)
  (gtqf 4 . #x1B)
  (atqf 5 . #x1B)
  (btqf 6 . #x1B)

  (csu 0 . #x36)
  (dsu 1 . #x36)
  (esu 2 . #x36)
  (fsu 3 . #x36)
  (gsu 4 . #x36)
  (asu 5 . #x36)
  (bsu 6 . #x36)

  (cfd 0 . #x37)
  (dfd 1 . #x37)
  (efd 2 . #x37)
  (ffd 3 . #x37)
  (gfd 4 . #x37)
  (afd 5 . #x37)
  (bfd 6 . #x37)

  (csd 0 . #x50)
  (dsd 1 . #x50)
  (esd 2 . #x50)
  (fsd 3 . #x50)
  (gsd 4 . #x50)
  (asd 5 . #x50)
  (bsd 6 . #x50)

  (cfu 0 . #x51)
  (dfu 1 . #x51)
  (efu 2 . #x51)
  (ffu 3 . #x51)
  (gfu 4 . #x51)
  (afu 5 . #x51)
  (bfu 6 . #x51)

  (csuu 0 . #x28)
  (dsuu 1 . #x28)
  (esuu 2 . #x28)
  (fsuu 3 . #x28)
  (gsuu 4 . #x28)
  (asuu 5 . #x28)
  (bsuu 6 . #x28)

  (cfdd 0 . #x29)
  (dfdd 1 . #x29)
  (efdd 2 . #x29)
  (ffdd 3 . #x29)
  (gfdd 4 . #x29)
  (afdd 5 . #x29)
  (bfdd 6 . #x29)

  (csdd 0 . #xA0)
  (dsdd 1 . #xA0)
  (esdd 2 . #xA0)
  (fsdd 3 . #xA0)
  (gsdd 4 . #xA0)
  (asdd 5 . #xA0)
  (bsdd 6 . #xA0)

  (cfuu 0 . #xA1)
  (dfuu 1 . #xA1)
  (efuu 2 . #xA1)
  (ffuu 3 . #xA1)
  (gfuu 4 . #xA1)
  (afuu 5 . #xA1)
  (bfuu 6 . #xA1)
)))

ekmNotations = #'(
(clever . (
  (#x00 #xE261)
  (#xFF #xE261)
  (#x1A #xE283)
  (#x1B #xE281)
  (#x28 #xE2D2)
  (#x29 #xE2CB)
  (#x36 #xE2C8)
  (#x37 #xE2C1)
  (#x44 #xE262)
  (#x45 #xE260)
  (#x50 #xE2C3)
  (#x51 #xE2C6)
  (#xA0 #xE2CD)
  (#xA1 #xE2D0)
  (#xAA #xE282)
  (#xAB #xE280)
  (#xAC #xE2D1)
  (#xAD #xE2CC)
  (#xB6 #xE2C7)
  (#xB7 #xE2C2)
)))

ekmelicFont = Bravura
\include "ekmel-main.ily"
\ekmelicStyle clever