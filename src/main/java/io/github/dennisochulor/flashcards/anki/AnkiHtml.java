package io.github.dennisochulor.flashcards.anki;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the rendered card HTML that Anki produces into text Minecraft can display,
 * keeping bold/italic/underline as § formatting codes and pulling out image filenames.
 */
public final class AnkiHtml {
    private AnkiHtml() {}

    public record Rendered(String text, List<String> images) {}

    private static final Pattern REMOVE_BLOCKS = Pattern.compile("(?is)<(style|script|head|title)\\b[^>]*>.*?</\\1\\s*>|<!--.*?-->");
    private static final Pattern AV_TAGS = Pattern.compile("(?i)\\[(?:sound:[^\\]]*|anki:play:[^\\]]*)\\]");
    private static final Pattern ANSWER_DIVIDER = Pattern.compile("(?is)<hr[^>]*\\bid\\s*=\\s*[\"']?answer[\"']?[^>]*>");
    private static final Pattern TAG = Pattern.compile("(?s)<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>");
    private static final Pattern IMG_SRC = Pattern.compile("(?is)\\bsrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final Pattern INLINE_MATH = Pattern.compile("(?s)\\\\\\((.+?)\\\\\\)");
    private static final Pattern DISPLAY_MATH = Pattern.compile("(?s)\\\\\\[(.+?)\\\\\\]|\\$\\$(.+?)\\$\\$");
    private static final Pattern MATH_BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern ANY_TAG = Pattern.compile("(?s)<[^>]*>");
    /** Invisible direction marks (Anki wraps interval texts like "<1m" in U+2068..U+2069); Minecraft draws them as boxes. */
    public static final Pattern BIDI_CONTROLS = Pattern.compile("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]");
    private static final Pattern ENTITY = Pattern.compile("&(#[xX]?[0-9a-fA-F]+|[a-zA-Z]+);");
    private static final Map<String, String> NAMED_ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"), Map.entry("quot", "\""),
            Map.entry("apos", "'"), Map.entry("nbsp", " "), Map.entry("ndash", "–"), Map.entry("mdash", "—"),
            Map.entry("hellip", "…"), Map.entry("laquo", "«"), Map.entry("raquo", "»"), Map.entry("lsquo", "‘"),
            Map.entry("rsquo", "’"), Map.entry("ldquo", "“"), Map.entry("rdquo", "”"), Map.entry("middot", "·"),
            Map.entry("times", "×"), Map.entry("divide", "÷"), Map.entry("deg", "°"), Map.entry("rarr", "→"),
            Map.entry("larr", "←"), Map.entry("harr", "↔"), Map.entry("bull", "•"), Map.entry("shy", ""));

    /** @return the answer side only, i.e. what comes after Anki's {@code <hr id=answer>} divider (or everything if there's no divider). */
    public static String answerOnly(String answerHtml) {
        Matcher m = ANSWER_DIVIDER.matcher(answerHtml);
        return m.find() ? answerHtml.substring(m.end()) : answerHtml;
    }

    public static Rendered render(String html) {
        String s = REMOVE_BLOCKS.matcher(html).replaceAll("");
        s = AV_TAGS.matcher(s).replaceAll("");
        s = replaceMath(s, DISPLAY_MATH, true);
        s = replaceMath(s, INLINE_MATH, false);

        List<String> images = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        int bold = 0, italic = 0, underline = 0;
        int last = 0;
        Matcher tag = TAG.matcher(s);
        while (tag.find()) {
            appendText(out, s.substring(last, tag.start()));
            last = tag.end();

            boolean closing = !tag.group(1).isEmpty();
            String name = tag.group(2).toLowerCase(Locale.ROOT);
            int before = bold * 100 + italic * 10 + underline;
            switch (name) {
                case "b", "strong" -> bold = Math.max(0, bold + (closing ? -1 : 1));
                case "i", "em" -> italic = Math.max(0, italic + (closing ? -1 : 1));
                case "u" -> underline = Math.max(0, underline + (closing ? -1 : 1));
                case "br" -> { if (!out.isEmpty() && !out.toString().endsWith("\n\n")) out.append('\n'); } // <br><br> = blank line
                case "div", "p", "tr", "ul", "ol", "table", "blockquote", "h1", "h2", "h3", "h4", "h5", "h6" -> newline(out, 1);
                case "hr" -> newline(out, 2);
                case "li" -> { if (!closing) { newline(out, 1); out.append("• "); } }
                case "td", "th" -> { if (closing) out.append("  "); }
                case "img" -> {
                    Matcher src = IMG_SRC.matcher(tag.group(3));
                    if (src.find()) {
                        String file = src.group(1) != null ? src.group(1) : src.group(2) != null ? src.group(2) : src.group(3);
                        file = decodeEntities(file).trim();
                        if (!file.isEmpty() && !file.contains("://") && !file.startsWith("data:")) images.add(file);
                    }
                }
                default -> {}
            }
            if (before != bold * 100 + italic * 10 + underline) {
                out.append("§r");
                if (bold > 0) out.append("§l");
                if (italic > 0) out.append("§o");
                if (underline > 0) out.append("§n");
            }
        }
        appendText(out, s.substring(last));

        String text = out.toString()
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
        return new Rendered(text, images);
    }

    /** Converts MathJax/LaTeX spans to Unicode text, emitted as escaped HTML so the normal tag handling (bold, line breaks) applies. */
    private static String replaceMath(String html, Pattern pattern, boolean display) {
        Matcher m = pattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String latex = m.group(1) != null ? m.group(1) : m.group(m.groupCount());
            latex = decodeEntities(ANY_TAG.matcher(MATH_BREAK.matcher(latex).replaceAll(" ")).replaceAll(""));
            String converted;
            try {
                converted = LatexText.convert(latex);
            }
            catch (RuntimeException e) { // never lose the card because of odd LaTeX
                converted = latex;
            }
            String escaped = converted.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace(String.valueOf(LatexText.BOLD_ON), "<b>").replace(String.valueOf(LatexText.BOLD_OFF), "</b>")
                    .replace(String.valueOf(LatexText.ITALIC_ON), "<i>").replace(String.valueOf(LatexText.ITALIC_OFF), "</i>")
                    .replace(String.valueOf(LatexText.NEWLINE), "<br>");
            if (display) escaped = "<div>" + escaped + "</div>";
            m.appendReplacement(sb, Matcher.quoteReplacement(escaped));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static void appendText(StringBuilder out, String raw) {
        if (raw.isEmpty()) return;
        String text = BIDI_CONTROLS.matcher(raw).replaceAll("").replaceAll("\\s+", " ");
        if (out.isEmpty() || out.charAt(out.length() - 1) == '\n') text = text.stripLeading();
        out.append(decodeEntities(text).replace('§', '$')); // a raw § would be read as a formatting code
    }

    private static void newline(StringBuilder out, int max) {
        if (out.isEmpty()) return;
        int trailing = 0;
        for (int i = out.length() - 1; i >= 0 && out.charAt(i) == '\n'; i--) trailing++;
        for (int i = trailing; i < max; i++) out.append('\n');
    }

    static String decodeEntities(String s) {
        if (s.indexOf('&') < 0) return s;
        Matcher m = ENTITY.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String e = m.group(1);
            String replacement;
            if (e.startsWith("#")) {
                try {
                    int cp = e.length() > 1 && (e.charAt(1) == 'x' || e.charAt(1) == 'X')
                            ? Integer.parseInt(e.substring(2), 16) : Integer.parseInt(e.substring(1));
                    replacement = cp == 0xA0 ? " " : new String(Character.toChars(cp));
                }
                catch (IllegalArgumentException ex) {
                    replacement = m.group();
                }
            }
            else {
                replacement = NAMED_ENTITIES.getOrDefault(e.toLowerCase(Locale.ROOT), m.group());
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
