package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp

object MathRenderer {

    private val greek = mapOf(
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ",
        "omicron" to "ο", "pi" to "π", "varpi" to "ϖ", "rho" to "ρ",
        "varrho" to "ϱ", "sigma" to "σ", "varsigma" to "ς", "tau" to "τ",
        "upsilon" to "υ", "phi" to "φ", "varphi" to "φ", "chi" to "χ",
        "psi" to "ψ", "omega" to "ω",
        "Alpha" to "Α", "Beta" to "Β", "Gamma" to "Γ", "Delta" to "Δ",
        "Epsilon" to "Ε", "Zeta" to "Ζ", "Eta" to "Η", "Theta" to "Θ",
        "Iota" to "Ι", "Kappa" to "Κ", "Lambda" to "Λ", "Mu" to "Μ",
        "Nu" to "Ν", "Xi" to "Ξ", "Omicron" to "Ο", "Pi" to "Π",
        "Rho" to "Ρ", "Sigma" to "Σ", "Tau" to "Τ", "Upsilon" to "Υ",
        "Phi" to "Φ", "Chi" to "Χ", "Psi" to "Ψ", "Omega" to "Ω"
    )

    private val symbols = mapOf(
        "times" to "×", "div" to "÷", "pm" to "±", "mp" to "∓", "cdot" to "·",
        "ast" to "∗", "star" to "⋆", "circ" to "∘", "bullet" to "•", "dagger" to "†",
        "ddagger" to "‡", "ne" to "≠", "neq" to "≠", "le" to "≤", "leq" to "≤",
        "ge" to "≥", "geq" to "≥", "ll" to "≪", "gg" to "≫", "infty" to "∞",
        "rightarrow" to "→", "to" to "→", "leftarrow" to "←", "leftrightarrow" to "↔",
        "Rightarrow" to "⇒", "Leftarrow" to "⇐", "Leftrightarrow" to "⇔",
        "uparrow" to "↑", "downarrow" to "↓", "updownarrow" to "↕",
        "longleftarrow" to "⟵", "longrightarrow" to "⟶", "longleftrightarrow" to "⟷",
        "mapsto" to "↦", "hookrightarrow" to "↪", "hookleftarrow" to "↩",
        "nearrow" to "↗", "searrow" to "↘", "swarrow" to "↙", "nwarrow" to "↖",
        "leadsto" to "⇝", "implies" to "⟹", "iff" to "⟺",
        "in" to "∈", "notin" to "∉", "ni" to "∋", "owns" to "∋",
        "subset" to "⊂", "supset" to "⊃", "subseteq" to "⊆", "supseteq" to "⊇",
        "sqsubseteq" to "⊑", "sqsupseteq" to "⊒", "prec" to "≺", "succ" to "≻",
        "preceq" to "≼", "succeq" to "≽", "asymp" to "≍", "doteq" to "≐",
        "emptyset" to "∅", "varnothing" to "∅", "forall" to "∀", "exists" to "∃",
        "nexists" to "∄", "nabla" to "∇", "partial" to "∂", "dots" to "…",
        "ldots" to "…", "cdots" to "⋯", "vdots" to "⋮", "ddots" to "⋱",
        "approx" to "≈", "propto" to "∝", "sim" to "∼", "cong" to "≅",
        "equiv" to "≡", "angle" to "∠", "measuredangle" to "∡", "degree" to "°",
        "prime" to "′", "perp" to "⊥", "bot" to "⊥", "parallel" to "∥",
        "nparallel" to "∦", "therefore" to "∴", "because" to "∵", "surd" to "√",
        "oplus" to "⊕", "ominus" to "⊖", "otimes" to "⊗", "oslash" to "⊘",
        "odot" to "⊙", "wedge" to "∧", "land" to "∧", "vee" to "∨", "lor" to "∨",
        "neg" to "¬", "lnot" to "¬", "top" to "⊤", "vdash" to "⊢", "models" to "⊨",
        "setminus" to "∖", "cap" to "∩", "cup" to "∪", "bigcap" to "⋂", "bigcup" to "⋃",
        "hbar" to "ℏ", "ell" to "ℓ", "imath" to "ı", "jmath" to "ȷ", "Re" to "ℜ",
        "Im" to "ℑ", "aleph" to "ℵ", "wp" to "℘", "triangle" to "△",
        "triangleleft" to "◁", "triangleright" to "▷", "square" to "□", "Box" to "□",
        "clubsuit" to "♣", "diamondsuit" to "♦", "heartsuit" to "♥", "spadesuit" to "♠",
        "checkmark" to "✓", "sum" to "∑", "int" to "∫", "prod" to "∏",
        "oint" to "∮", "bigodot" to "⨀", "bigotimes" to "⨂", "bigoplus" to "⨁"
    )

    private val functions = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc",
        "sinh", "cosh", "tanh", "coth", "arcsin", "arccos", "arctan",
        "log", "ln", "lg", "exp", "max", "min", "sup", "inf",
        "lim", "limsup", "liminf", "arg", "deg", "dim", "gcd",
        "det", "Pr", "ker", "mod", "bmod"
    )

    private val accents = mapOf(
        "bar" to "\u0304", "hat" to "\u0302", "vec" to "\u20D7", "tilde" to "\u0303",
        "dot" to "\u0307", "ddot" to "\u0308", "overline" to "\u0305", "breve" to "\u0306",
        "check" to "\u030C", "acute" to "\u0301", "grave" to "\u0300"
    )

    private val spacing = mapOf(
        "quad" to "  ", "qquad" to "    ", "enspace" to " ", "thinspace" to " ",
        "," to " ", ":" to " ", ";" to " ", "!" to " "
    )

    private val supScript = SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Superscript)
    private val subScript = SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Subscript)

    fun render(latex: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var i = 0
        while (i < latex.length) {
            val c = latex[i]
            when {
                c == '\\' -> {
                    val nameStart = i + 1
                    var j = nameStart
                    while (j < latex.length && latex[j].isLetter()) j++
                    val name = latex.substring(nameStart, j)
                    val (rendered, consumed) = resolveCommand(latex, i, name)
                    builder.append(rendered)
                    i = consumed
                }
                c == '^' -> {
                    val (content, consumed) = readScriptContent(latex, i + 1)
                    val scripted = render(content)
                    val start = builder.length
                    builder.append(scripted)
                    builder.addStyle(supScript, start, builder.length)
                    i = consumed
                }
                c == '_' -> {
                    val (content, consumed) = readScriptContent(latex, i + 1)
                    val scripted = render(content)
                    val start = builder.length
                    builder.append(scripted)
                    builder.addStyle(subScript, start, builder.length)
                    i = consumed
                }
                else -> {
                    builder.append(c)
                    i++
                }
            }
        }
        return builder.toAnnotatedString()
    }

    private fun resolveCommand(latex: String, cmdStart: Int, name: String): Pair<AnnotatedString, Int> {
        var idx = cmdStart + 1 + name.length
        when {
            name == "frac" -> {
                val (num, i1) = readGroup(latex, idx)
                val (den, i2) = readGroup(latex, i1)
                val b = AnnotatedString.Builder()
                b.append(render(num))
                b.append('\u2044')
                b.append(render(den))
                return b.toAnnotatedString() to i2
            }
            name == "sqrt" -> {
                val (arg, i1) = readGroup(latex, idx)
                val b = AnnotatedString.Builder()
                b.append('√')
                b.append('(')
                b.append(render(arg))
                b.append(')')
                return b.toAnnotatedString() to i1
            }
            name == "text" -> {
                val (arg, i1) = readGroup(latex, idx)
                return render(arg) to i1
            }
            name == "binom" -> {
                val (num, i1) = readGroup(latex, idx)
                val (den, i2) = readGroup(latex, i1)
                val b = AnnotatedString.Builder()
                b.append('(')
                b.append(render(num))
                b.append(' ')
                b.append(render(den))
                b.append(')')
                return b.toAnnotatedString() to i2
            }
            name == "sum" -> return AnnotatedString("∑") to idx
            name == "int" -> return AnnotatedString("∫") to idx
            name == "prod" -> return AnnotatedString("∏") to idx
            name == "operatorname" -> {
                val (arg, i1) = readGroup(latex, idx)
                return AnnotatedString(arg) to i1
            }
            name == "limits" || name == "nolimits" || name == "displaystyle" ||
                name == "textstyle" || name == "scriptstyle" || name == "scriptscriptstyle" ->
                return AnnotatedString("") to idx
            name in functions -> return AnnotatedString(name) to idx
            name in accents -> {
                val (arg, i1) = readGroup(latex, idx)
                val mark = accents.getValue(name)
                val rendered = render(arg)
                val b = AnnotatedString.Builder()
                b.append(rendered)
                b.append(mark)
                return b.toAnnotatedString() to i1
            }
            name in spacing -> return AnnotatedString(spacing.getValue(name)) to idx
            name in greek -> return AnnotatedString(greek.getValue(name)) to idx
            name in symbols -> return AnnotatedString(symbols.getValue(name)) to idx
            name == "left" || name == "right" -> {
                if (idx < latex.length && latex[idx] == ' ') idx++
                val delim = if (idx < latex.length && latex[idx] != '.') latex[idx].toString() else ""
                if (idx < latex.length) idx++
                return AnnotatedString(delim) to idx
            }
            name == "begin" || name == "end" -> {
                val (_, i1) = readGroup(latex, idx)
                return AnnotatedString("") to i1
            }
            name == " " -> return AnnotatedString("") to idx
            name.isEmpty() && cmdStart + 1 < latex.length ->
                return AnnotatedString(latex[cmdStart + 1].toString()) to (cmdStart + 2)
            else -> return AnnotatedString("\\$name") to idx
        }
    }

    private fun readGroup(latex: String, startIdx: Int): Pair<String, Int> {
        var idx = startIdx
        while (idx < latex.length && latex[idx] == ' ') idx++
        if (idx >= latex.length) return "" to idx
        if (latex[idx] != '{') return latex[idx].toString() to (idx + 1)
        var depth = 0
        var k = idx
        while (k < latex.length) {
            when (latex[k]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return latex.substring(idx + 1, k) to (k + 1)
                    }
                }
            }
            k++
        }
        return latex.substring(idx + 1) to latex.length
    }

    private fun readScriptContent(latex: String, startIdx: Int): Pair<String, Int> {
        var idx = startIdx
        while (idx < latex.length && latex[idx] == ' ') idx++
        if (idx >= latex.length) return "" to idx
        if (latex[idx] == '{') {
            val (content, next) = readGroup(latex, idx)
            return content to next
        }
        if (latex[idx] == '\\') {
            var j = idx + 1
            while (j < latex.length && latex[j].isLetter()) j++
            val (rendered, consumed) = resolveCommand(latex, idx, latex.substring(idx + 1, j))
            return rendered.text to consumed
        }
        return latex[idx].toString() to (idx + 1)
    }
}
