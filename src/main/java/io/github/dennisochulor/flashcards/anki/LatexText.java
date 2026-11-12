package io.github.dennisochulor.flashcards.anki;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Turns the LaTeX inside Anki's MathJax delimiters ({@code \(...\)} and {@code \[...\]}) into plain Unicode text
 * that Minecraft's font can draw, e.g. {@code \hat\beta_1 \le \sum_{i=1}^{n} x_i^2} becomes {@code βˆ₁ ≤ ∑ᵢ₌₁ⁿ xᵢ²}.
 * <p>
 * Minecraft can't typeset real math, so this is an approximation: fractions become {@code a/b}, roots {@code √x},
 * sub/superscripts use Unicode sub/superscript letters where they exist and {@code _(..)}/{@code ^(..)} otherwise.
 * Bold/italic are emitted as marker characters that {@link AnkiHtml} turns back into formatting.
 */
public final class LatexText {
    private LatexText() {}

    // Marker characters (private use area) understood by AnkiHtml.
    public static final char BOLD_ON = '\uE000', BOLD_OFF = '\uE001', ITALIC_ON = '\uE002', ITALIC_OFF = '\uE003', NEWLINE = '\uE004';

    private static final Map<String, String> SYMBOLS = new HashMap<>();
    private static final Map<Character, Character> SUPERSCRIPTS = new HashMap<>();
    private static final Map<Character, Character> SUBSCRIPTS = new HashMap<>();
    private static final Map<Character, Character> BLACKBOARD = new HashMap<>();
    private static final Map<Character, Character> CALLIGRAPHIC = new HashMap<>();
    /** Commands written as upright words, like \log or \max. */
    private static final Set<String> FUNCTION_NAMES = Set.of(
            "sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh", "coth",
            "log", "ln", "lg", "exp", "lim", "liminf", "limsup", "max", "min", "sup", "inf", "det", "dim", "ker", "deg",
            "gcd", "arg", "Pr", "hom", "mod", "bmod", "var", "Var", "Cov", "cov", "tr", "rank", "diag", "sgn", "argmax", "argmin");
    /** Commands that are dropped entirely (sizing, spacing hints, styles that don't apply). */
    private static final Set<String> IGNORED = Set.of(
            "left", "right", "big", "Big", "bigg", "Bigg", "bigl", "bigr", "Bigl", "Bigr", "biggl", "biggr", "Biggl", "Biggr",
            "middle", "displaystyle", "textstyle", "scriptstyle", "scriptscriptstyle", "limits", "nolimits", "nonumber",
            "notag", "quad", "qquad", "centering", "small", "large", "Large", "tiny", "normalsize", "rm", "it", "bf");

    static {
        String[][] symbols = {
                // lowercase greek
                {"alpha", "α"}, {"beta", "β"}, {"gamma", "γ"}, {"delta", "δ"}, {"epsilon", "ε"}, {"varepsilon", "ε"},
                {"zeta", "ζ"}, {"eta", "η"}, {"theta", "θ"}, {"vartheta", "ϑ"}, {"iota", "ι"}, {"kappa", "κ"},
                {"lambda", "λ"}, {"mu", "μ"}, {"nu", "ν"}, {"xi", "ξ"}, {"omicron", "ο"}, {"pi", "π"}, {"varpi", "ϖ"},
                {"rho", "ρ"}, {"varrho", "ϱ"}, {"sigma", "σ"}, {"varsigma", "ς"}, {"tau", "τ"}, {"upsilon", "υ"},
                {"phi", "φ"}, {"varphi", "φ"}, {"chi", "χ"}, {"psi", "ψ"}, {"omega", "ω"},
                // uppercase greek
                {"Gamma", "Γ"}, {"Delta", "Δ"}, {"Theta", "Θ"}, {"Lambda", "Λ"}, {"Xi", "Ξ"}, {"Pi", "Π"},
                {"Sigma", "Σ"}, {"Upsilon", "Υ"}, {"Phi", "Φ"}, {"Psi", "Ψ"}, {"Omega", "Ω"},
                // relations
                {"le", "≤"}, {"leq", "≤"}, {"leqslant", "≤"}, {"ge", "≥"}, {"geq", "≥"}, {"geqslant", "≥"},
                {"ne", "≠"}, {"neq", "≠"}, {"approx", "≈"}, {"sim", "∼"}, {"simeq", "≃"}, {"cong", "≅"}, {"equiv", "≡"},
                {"propto", "∝"}, {"ll", "≪"}, {"gg", "≫"}, {"prec", "≺"}, {"succ", "≻"}, {"doteq", "≐"},
                {"in", "∈"}, {"notin", "∉"}, {"ni", "∋"}, {"subset", "⊂"}, {"subseteq", "⊆"}, {"supset", "⊃"},
                {"supseteq", "⊇"}, {"perp", "⊥"}, {"parallel", "∥"}, {"mid", "|"}, {"nmid", "∤"}, {"vdash", "⊢"}, {"models", "⊨"},
                // operators
                {"pm", "±"}, {"mp", "∓"}, {"times", "×"}, {"cdot", "·"}, {"div", "÷"}, {"ast", "∗"}, {"star", "⋆"},
                {"circ", "∘"}, {"bullet", "•"}, {"oplus", "⊕"}, {"ominus", "⊖"}, {"otimes", "⊗"}, {"odot", "⊙"},
                {"cup", "∪"}, {"cap", "∩"}, {"setminus", "∖"}, {"wedge", "∧"}, {"land", "∧"}, {"vee", "∨"}, {"lor", "∨"},
                {"neg", "¬"}, {"lnot", "¬"}, {"sum", "∑"}, {"prod", "∏"}, {"coprod", "∐"}, {"int", "∫"}, {"iint", "∬"},
                {"iiint", "∭"}, {"oint", "∮"}, {"bigcup", "⋃"}, {"bigcap", "⋂"}, {"bigoplus", "⊕"}, {"bigotimes", "⊗"},
                {"wr", "≀"}, {"dagger", "†"}, {"ddagger", "‡"}, {"intercal", "ᵀ"}, {"top", "ᵀ"}, {"bot", "⊥"},
                // arrows
                {"to", "→"}, {"rightarrow", "→"}, {"longrightarrow", "⟶"}, {"leftarrow", "←"}, {"gets", "←"},
                {"longleftarrow", "⟵"}, {"leftrightarrow", "↔"}, {"Rightarrow", "⇒"}, {"implies", "⇒"},
                {"Longrightarrow", "⟹"}, {"Leftarrow", "⇐"}, {"Leftrightarrow", "⇔"}, {"iff", "⇔"},
                {"Longleftrightarrow", "⟺"}, {"mapsto", "↦"}, {"uparrow", "↑"}, {"downarrow", "↓"}, {"nearrow", "↗"},
                {"searrow", "↘"}, {"rightleftharpoons", "⇌"},
                // misc
                {"infty", "∞"}, {"partial", "∂"}, {"nabla", "∇"}, {"forall", "∀"}, {"exists", "∃"}, {"nexists", "∄"},
                {"emptyset", "∅"}, {"varnothing", "∅"}, {"ell", "ℓ"}, {"hbar", "ℏ"}, {"Re", "ℜ"}, {"Im", "ℑ"},
                {"aleph", "ℵ"}, {"prime", "′"}, {"degree", "°"}, {"angle", "∠"}, {"triangle", "△"}, {"square", "□"},
                {"therefore", "∴"}, {"because", "∵"}, {"cdots", "⋯"}, {"ldots", "…"}, {"dots", "…"}, {"dotsc", "…"},
                {"dotsb", "⋯"}, {"vdots", "⋮"}, {"ddots", "⋱"}, {"langle", "⟨"}, {"rangle", "⟩"}, {"lfloor", "⌊"},
                {"rfloor", "⌋"}, {"lceil", "⌈"}, {"rceil", "⌉"}, {"lvert", "|"}, {"rvert", "|"}, {"vert", "|"},
                {"lVert", "‖"}, {"rVert", "‖"}, {"Vert", "‖"}, {"|", "‖"}, {"backslash", "\\"}, {"checkmark", "✓"},
                {"surd", "√"}, {"lbrace", "{"}, {"rbrace", "}"}, {"lbrack", "["}, {"rbrack", "]"},
                // escaped characters and spacing
                {"{", "{"}, {"}", "}"}, {"%", "%"}, {"$", "$"}, {"_", "_"}, {"#", "#"}, {"&", "&"},
                {",", " "}, {":", " "}, {";", " "}, {">", " "}, {" ", " "}, {"!", ""}, {"enspace", " "}, {"thinspace", " "},
        };
        for (String[] s : symbols) SYMBOLS.put(s[0], s[1]);

        mapChars(SUPERSCRIPTS, "0123456789+-−=()niabcdefghjklmoprstuvwxyzABDEGHIJKLMNOPRTUVWβγδθφχ⊤ᵀ′*'",
                "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁻⁼⁽⁾ⁿⁱᵃᵇᶜᵈᵉᶠᵍʰʲᵏˡᵐᵒᵖʳˢᵗᵘᵛʷˣʸᶻᴬᴮᴰᴱᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾᴿᵀᵁⱽᵂᵝᵞᵟᶿᵠᵡᵀᵀ′*′");
        mapChars(SUBSCRIPTS, "0123456789+-−=()aehijklmnoprstuvxβγρφχ,.",
                "₀₁₂₃₄₅₆₇₈₉₊₋₋₌₍₎ₐₑₕᵢⱼₖₗₘₙₒₚᵣₛₜᵤᵥₓᵦᵧᵨᵩᵪ,.");
        mapChars(BLACKBOARD, "CHNPQRZ", "ℂℍℕℙℚℝℤ");
        mapChars(CALLIGRAPHIC, "BEFHILMR", "ℬℰℱℋℐℒℳℛ");
    }

    private static void mapChars(Map<Character, Character> map, String from, String to) {
        if (from.length() != to.length()) throw new IllegalStateException("bad char map " + from);
        for (int i = 0; i < from.length(); i++) map.put(from.charAt(i), to.charAt(i));
    }

    /** @param latex the source between the math delimiters, with HTML already removed and entities decoded */
    public static String convert(String latex) {
        Parser parser = new Parser(latex);
        String out = parser.parseUntil(null);
        return out.replaceAll(" {2,}", " ").strip();
    }

    private static final class Parser {
        private final String s;
        private int pos = 0;

        Parser(String s) {
            this.s = s;
        }

        private boolean atEnd() {
            return pos >= s.length();
        }

        /** Parses until the closing brace (consumed) or until {@code \end{...}}/end of input when {@code stop} is null. */
        String parseUntil(Character stop) {
            StringBuilder out = new StringBuilder();
            while (!atEnd()) {
                char c = s.charAt(pos);
                if (stop != null && c == stop) {
                    pos++;
                    return out.toString();
                }
                if (c == '}' ) { // stray closing brace
                    pos++;
                    continue;
                }
                if (c == '^' || c == '_') {
                    pos++;
                    String arg = argument();
                    out.append(c == '^' ? superscript(arg) : subscript(arg));
                    continue;
                }
                out.append(atom());
            }
            return out.toString();
        }

        /** Reads one "thing": a group, a command (with its arguments) or a single character. */
        private String atom() {
            char c = s.charAt(pos);
            if (c == '{') {
                pos++;
                return parseUntil('}');
            }
            if (c == '\\') return command();
            pos++;
            return switch (c) {
                case '~' -> " ";
                case '&' -> "  "; // alignment column
                case '\n', '\r', '\t' -> " ";
                case '-' -> "−";
                case '\'' -> "′";
                default -> String.valueOf(c);
            };
        }

        /** The argument of a command or ^/_ : a {group}, a command or one character (whitespace skipped). */
        private String argument() {
            skipSpaces();
            if (atEnd()) return "";
            return atom();
        }

        /** Raw text of an argument without converting it, e.g. for \begin{pmatrix}. */
        private String rawArgument() {
            skipSpaces();
            if (atEnd()) return "";
            if (s.charAt(pos) != '{') return String.valueOf(s.charAt(pos++));
            int depth = 0;
            int start = pos + 1;
            while (!atEnd()) {
                char c = s.charAt(pos++);
                if (c == '{') depth++;
                else if (c == '}' && --depth == 0) return s.substring(start, pos - 1);
            }
            return s.substring(start);
        }

        /** An optional [argument], or null. */
        private String optionalArgument() {
            skipSpaces();
            if (atEnd() || s.charAt(pos) != '[') return null;
            int end = s.indexOf(']', pos);
            if (end < 0) return null;
            String inner = s.substring(pos + 1, end);
            pos = end + 1;
            return convert(inner);
        }

        private boolean nextIsLetter() {
            int i = pos;
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            return i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '\\');
        }

        private void skipSpaces() {
            while (!atEnd() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        private String command() {
            pos++; // backslash
            if (atEnd()) return "\\";
            char first = s.charAt(pos);
            String name;
            if (Character.isLetter(first)) {
                int start = pos;
                while (!atEnd() && Character.isLetter(s.charAt(pos))) pos++;
                name = s.substring(start, pos);
                if (!atEnd() && s.charAt(pos) == '*') pos++; // \operatorname*{...}
            }
            else {
                pos++;
                name = String.valueOf(first);
            }
            if (name.equals("\\")) return String.valueOf(NEWLINE); // line break in align/matrix

            String symbol = SYMBOLS.get(name);
            if (symbol != null) return symbol;
            if (IGNORED.contains(name)) {
                if (name.equals("left") || name.equals("right") || name.equals("middle")) {
                    skipSpaces();
                    if (!atEnd() && s.charAt(pos) == '.') pos++; // invisible delimiter
                }
                if (name.equals("quad")) return "  ";
                if (name.equals("qquad")) return "    ";
                return "";
            }
            if (FUNCTION_NAMES.contains(name)) return " " + name + (nextIsLetter() ? " " : "");

            return switch (name) {
                case "frac", "dfrac", "tfrac", "cfrac" -> fraction(argument(), argument());
                case "binom", "dbinom", "tbinom" -> "C(" + argument() + ", " + argument() + ")";
                case "sqrt" -> {
                    String n = optionalArgument();
                    String body = wrapIfComplex(argument());
                    if (n == null || n.isBlank() || n.equals("2")) yield "√" + body;
                    if (n.equals("3")) yield "∛" + body;
                    if (n.equals("4")) yield "∜" + body;
                    yield superscript(n) + "√" + body;
                }
                case "hat", "widehat" -> accent(argument(), '\u0302', "ˆ");
                case "bar", "overline" -> accent(argument(), '\u0304', "¯");
                case "tilde", "widetilde" -> accent(argument(), '\u0303', "˜");
                case "dot" -> accent(argument(), '\u0307', "˙");
                case "ddot" -> accent(argument(), '\u0308', "¨");
                case "check" -> accent(argument(), '\u030C', "ˇ");
                case "acute" -> accent(argument(), '\u0301', "´");
                case "grave" -> accent(argument(), '\u0300', "`");
                case "vec", "overrightarrow", "mathbf", "boldsymbol", "bm", "pmb", "textbf", "mathbfit" ->
                        BOLD_ON + argument() + BOLD_OFF;
                case "mathit", "textit", "emph", "textsl" -> ITALIC_ON + argument() + ITALIC_OFF;
                case "text", "textrm", "mathrm", "mbox", "textnormal", "operatorname", "mathsf", "textsf", "mathtt", "texttt", "hbox" -> {
                    String arg = rawArgument();
                    yield name.startsWith("text") || name.equals("mbox") || name.equals("hbox") ? textMode(arg) : convert(arg);
                }
                case "mathbb", "mathds", "mathbbm" -> mapLetters(argument(), BLACKBOARD, true);
                case "mathcal", "mathscr", "mathfrak" -> mapLetters(argument(), CALLIGRAPHIC, false);
                case "underbrace", "overbrace", "underline", "boxed", "phantom", "cancel", "bcancel", "xcancel" -> {
                    String arg = argument();
                    yield name.equals("phantom") ? " " : arg;
                }
                case "overset", "stackrel" -> {
                    String over = argument();
                    yield argument() + superscript(over);
                }
                case "underset" -> {
                    String under = argument();
                    yield argument() + subscript(under);
                }
                case "color", "textcolor" -> {
                    rawArgument(); // the colour itself
                    yield name.equals("textcolor") ? argument() : "";
                }
                case "tag" -> "   (" + convert(rawArgument()) + ")";
                case "label", "ref", "eqref", "hspace", "vspace", "kern", "mkern", "hskip" -> {
                    String arg = rawArgument();
                    yield name.equals("ref") || name.equals("eqref") ? "(" + arg + ")" : " ";
                }
                case "begin" -> environment(rawArgument(), true);
                case "end" -> environment(rawArgument(), false);
                case "not" -> {
                    String arg = argument().strip();
                    String negated = Normalizer.normalize(arg + '\u0338', Normalizer.Form.NFC); // e.g. = -> ≠, ∈ -> ∉
                    yield negated.length() == arg.length() ? negated : "¬" + arg;
                }
                case "pmod" -> " (mod " + argument() + ")";
                default -> name; // unknown command: show its name rather than nothing
            };
        }

        private static String environment(String name, boolean begin) {
            String env = name.replace("*", "");
            return switch (env) {
                case "pmatrix" -> begin ? "(" : ")";
                case "bmatrix" -> begin ? "[" : "]";
                case "Bmatrix" -> begin ? "{" : "}";
                case "vmatrix" -> "|";
                case "Vmatrix" -> "‖";
                case "cases" -> begin ? "{ " : "";
                default -> begin ? "" : String.valueOf(NEWLINE); // align, aligned, gather, array, matrix...
            };
        }
    }

    /** Text inside \text{...}: kept as written, but nested $...$ math is still converted. */
    private static String textMode(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int dollar = text.indexOf('$', i);
            if (dollar < 0) {
                out.append(text, i, text.length());
                break;
            }
            int close = text.indexOf('$', dollar + 1);
            if (close < 0) {
                out.append(text, i, text.length());
                break;
            }
            out.append(text, i, dollar).append(convert(text.substring(dollar + 1, close)));
            i = close + 1;
        }
        return out.toString().replace("\\", "");
    }

    private static String fraction(String numerator, String denominator) {
        return wrapIfComplex(numerator.strip()) + "/" + wrapIfComplex(denominator.strip());
    }

    /** Wraps in parentheses unless it's a single "word" (e.g. x, 12, β², xᵢ). */
    private static String wrapIfComplex(String s) {
        String t = s.strip();
        if (t.isEmpty()) return t;
        boolean simple = t.codePoints().allMatch(cp -> Character.isLetterOrDigit(cp) || isScript(cp) || cp == '.' || cp == '′'
                || cp == BOLD_ON || cp == BOLD_OFF || cp == ITALIC_ON || cp == ITALIC_OFF);
        boolean alreadyWrapped = (t.startsWith("(") && t.endsWith(")")) || (t.startsWith("[") && t.endsWith("]"));
        return simple || alreadyWrapped ? t : "(" + t + ")";
    }

    private static boolean isScript(int cp) {
        return SUPERSCRIPTS.containsValue((char) cp) || SUBSCRIPTS.containsValue((char) cp);
    }

    private static String superscript(String arg) {
        return script(arg, SUPERSCRIPTS, "^");
    }

    private static String subscript(String arg) {
        return script(arg, SUBSCRIPTS, "_");
    }

    private static String script(String arg, Map<Character, Character> map, String fallback) {
        String t = stripMarkers(arg).strip().replace(" ", "");
        if (t.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            Character mapped = map.get(t.charAt(i));
            if (mapped == null) return fallback + (t.length() == 1 ? t : "(" + t + ")");
            out.append(mapped.charValue());
        }
        return out.toString();
    }

    private static String stripMarkers(String s) {
        return s.replace(String.valueOf(BOLD_ON), "").replace(String.valueOf(BOLD_OFF), "")
                .replace(String.valueOf(ITALIC_ON), "").replace(String.valueOf(ITALIC_OFF), "");
    }

    /**
     * Uses a precomposed character when Unicode has one (e.g. â, ā, ñ). Otherwise appends a spacing mark (e.g. βˆ),
     * because Minecraft draws combining accents beside the letter instead of above it.
     */
    private static String accent(String base, char combining, String spacingFallback) {
        String t = base.strip();
        String plain = stripMarkers(t);
        if (plain.length() == 1) {
            String composed = Normalizer.normalize(plain + combining, Normalizer.Form.NFC);
            if (composed.length() == 1) return t.replace(plain, composed);
        }
        return wrapIfComplex(t) + spacingFallback;
    }

    private static String mapLetters(String arg, Map<Character, Character> map, boolean boldFallback) {
        StringBuilder out = new StringBuilder();
        for (char c : arg.toCharArray()) {
            Character mapped = map.get(c);
            if (mapped != null) out.append(mapped.charValue());
            else if (boldFallback && Character.isLetter(c)) out.append(BOLD_ON).append(c).append(BOLD_OFF);
            else out.append(c);
        }
        return out.toString();
    }
}
