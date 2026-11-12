# Flashcards: Anki + AI Edition
**Study your Anki deck in Minecraft, with answers graded by a local AI (Ollama).**

By [**IvanIvanochko**](https://github.com/IvanIvanochko). A fork of [Flashcards](https://modrinth.com/mod/flashcards) by **DennisOchulor** ([source](https://github.com/DennisOchulor/mc-flashcards)). All credit for the original mod goes to its author; this edition adds Anki sync, AI grading and more on top of it. It is **not** an official release of Flashcards, and it isn't affiliated with or endorsed by DennisOchulor, Anki/AnkiWeb, AnkiConnect, Ollama, Mojang or Microsoft.

## What this edition adds
- **Anki mode**: cards come from your real Anki deck and your answers are saved as real Anki reviews (Again / Hard / Good / Easy), so your spaced-repetition schedule keeps working while you play. Optional AnkiWeb sync on join/leave.
- **AI grading**: type your answer and a local AI grades it by meaning, with a key-point checklist and an explanation. The score unlocks Anki's buttons. One second chance for answers that are on the right track but too vague.
- **Play while the AI thinks**: leave a slow grading with Space and keep playing; the card comes back when it's graded.
- **Readable cards**: normal-size text in a scrollable panel, images, and LaTeX/MathJax formulas converted to Unicode.
- **Timer & stats HUD**: countdown to the next card, session stats, Anki's due counts.
- **Real time or game time** for the question timer, and intervals in minutes **and seconds**.
- Searchable deck and model lists.

Everything from the original mod still works too: your own question lists, automatic/manual answer checking, and reward/penalty commands.

## Requirements
- Minecraft **26.3** with [Fabric Loader](https://fabricmc.net/) 0.19.3+ and [Fabric API](https://modrinth.com/mod/fabric-api). [Mod Menu](https://modrinth.com/mod/modmenu) is optional.
- For Anki mode: [Anki](https://apps.ankiweb.net/) desktop with the [AnkiConnect](https://ankiweb.net/shared/info/2055492159) add-on, open while you play.
- For AI grading: [Ollama](https://ollama.com) with a model downloaded. About 5 GB of free RAM for a 7–8B model (e.g. `qwen2.5:7b`), less for a 3B one.

## Install
1. Build the jar (see [Building](#building)) or take it from a release, then put `flashcards-anki-ai-<version>.jar` into your `mods` folder.
2. **Remove the original Flashcards jar.** This edition uses the same mod id (`flashcards`) so your existing questions, stats and settings in `config/flashcards/` carry over, which also means the two can't be installed together.
3. Open the config in game with `H` (or through Mod Menu).

## Privacy
Everything runs on your computer. The mod only talks to Anki (AnkiConnect, `127.0.0.1:8765`) and Ollama (`127.0.0.1:11434`) on your own machine; your cards and answers are never sent anywhere by the mod. The only internet traffic is Anki's own AnkiWeb sync, and only if you turn **Sync with AnkiWeb** on. No telemetry.

## Anki mode
1. Install the [AnkiConnect](https://ankiweb.net/shared/info/2055492159) add-on in Anki desktop (Tools → Add-ons → Get Add-ons → code `2055492159`) and restart Anki.
2. Keep Anki open while you play.
3. In Minecraft, open the Flashcards config (default key `H`) → **Anki Sync...**, turn **Use Anki cards** ON and click your deck in the list (type in the search box to filter; subdecks are indented under their parent).

How it works:
- Cards come from Anki's own review queue for that deck, so daily limits, learning steps and new cards behave exactly as in Anki.
- The card is shown at normal text size in a panel that scrolls when it's long (mouse wheel, arrow keys, Page Up/Down, Home/End). When you reveal the answer of a long card, it jumps to the answer; scroll up to see the question again.
- After **Show Answer** you grade with **Again / Hard / Good / Easy**, each showing Anki's next interval. Keys: Space/Enter shows the answer, 1-4 grades, Space/Enter = Good.
- **Again** counts as a wrong answer for the mod's reward/penalty commands (Additional Config...), the other buttons count as correct.
- With **Sync with AnkiWeb on join/leave** ON, Anki syncs with AnkiWeb when you join and leave a world (you must be logged in to AnkiWeb inside Anki).
- Question screens pause the game in singleplayer.
- Images on the card are copied from Anki into `config/flashcards/media/anki/`. Audio isn't played.

### Math / LaTeX
Nothing to install. MathJax formulas (`\(...\)`, `\[...\]`, `$$...$$`) are converted to Unicode text that Minecraft's font can draw, for example:

| In Anki | In Minecraft |
|---|---|
| `\(\hat\beta_1\)` | βˆ₁ |
| `\(\sum_{i=1}^{n} x_i^2\)` | ∑ᵢ₌₁ⁿ xᵢ² |
| `\((X^\top X)^{-1}\)` | (Xᵀ X)⁻¹ |
| `\(\frac{RSS}{n-p-1}\)` | RSS/(n−p−1) |
| `\(\sqrt{n}\), \(\mathbb{R}^n\), \(\le\), \(\to\)` | √n, ℝⁿ, ≤, → |

Minecraft can't typeset real math, so this is an approximation: fractions are written `a/b`, letters with no Unicode superscript/subscript form fall back to `^(...)`/`_(...)`, accents that have no ready-made character are written after the letter (`βˆ` = β-hat, `x¯` = x-bar), and matrices are shown row by row. Anki's older `[latex]...[/latex]` tags are rendered by Anki itself as images, and those images are shown as normal card images.

## AI grading (type your answer)
Instead of only flipping the card, you can type your answer and a **local AI** grades it **by meaning** (0 to 1). Your wording doesn't have to match the card. Works with Anki mode.

Setup:
1. Install [Ollama](https://ollama.com) and keep it running (it serves at `http://127.0.0.1:11434`).
2. Download a model, e.g. `ollama pull qwen2.5:7b` (good grader, ~5 GB RAM). With 8 GB of RAM use a smaller one such as `qwen2.5:3b`.
3. Flashcards config → **AI Grading...**: turn **AI grading** ON, check that the status says *Connected to Ollama*, and click your model in the list.

On a card:
- Type your answer in the box. **Enter** checks it (Shift+Enter starts a new line). While the AI thinks you see what it's doing and a progress bar.
- **Slow AI?** If it thinks longer than your limit (default 15 s), a **Keep playing** button appears in the bottom-right corner (or press **Space** / Esc). It closes the card and you play while the AI finishes, with a small "thinking" indicator on screen (center by default). When the grade is ready, the card comes back and the game pauses as usual, but never sooner than your minimum play time (default 5 s), and only once you've closed other menus and haven't just been hurt.
- With **Play while thinking** ON (default OFF) you're sent back to the game as soon as you check an answer, without waiting on the card.
- **Show Answer** means "I don't know": you see the answer and only **Again** is available.
- **Second chance:** if your answer is *on the right track but too vague* (something true is in it, it just isn't pinned down) and it would fail (Again), the AI tells you what to be more specific about (without giving the answer away) and you can try once more; your first answer stays in the box to extend. There's no third try: after the second answer you get the grade, whatever it is. "I don't know", excuses, off-topic and wrong answers don't get a second chance, they're simply Again.
- After grading you see the question, your answer, the AI's grade with a **key-point checklist** (✔ correct, ~ partly, ✘ missing/wrong) and an explanation, then the card's answer.
- All four buttons are shown, but the score unlocks them: **Again** is always clickable, **Hard** from 0.50, **Good** from 0.70, **Easy** from 0.90. For example, 0.73 allows Again, Hard or Good. **Enter** picks the best unlocked button; 1-4 also work.
- If Ollama isn't reachable or the reply can't be read, you can **Retry** or **Grade myself** (plain Anki buttons).

What the grades mean (the AI is given exactly these, and they're in the button tooltips). Only what the question asks for counts; extra material on the card is a bonus. Time taken doesn't matter.

| Grade | Meaning |
|---|---|
| **Again** | You didn't remember it: the core idea is missing or wrong, a key point is factually wrong, the answer is too vague to show understanding, or you didn't know. |
| **Hard** | You remembered only part of it: the core idea is right, but a key point is missing, or something the question explicitly asks for (an example, an analogy, a formula...) is missing. |
| **Good** | You remembered it properly: the core idea and most key points are right; only minor details or exact terms are missing, and nothing is wrong. |
| **Easy** | You know it completely: every key point the question asks for, stated precisely, no gaps. |

If the question explicitly asks for something (e.g. "with an analogy") and it's missing, the grade is at most Hard. The AI's score always lands inside its grade's band, using your cut-offs.

Settings in **AI Grading...** (saved in `config/flashcards/config.json`, so they survive restarts):
- **Score needed for each button**: the Hard / Good / Easy cut-offs (↺ resets to 0.50 / 0.70 / 0.90). They always stay in order Hard ≤ Good ≤ Easy.
- **Keep playing after N s** (default 15, 0 = at once): when the Keep playing button appears.
- **Play at least N s** (default 5): minimum time in the game before a graded card comes back.
- **Play while thinking** (default OFF): go back to the game right away every time.
- **Indicator** position (Center, Top center, Top left, Top right) for the thinking animation while you play.
- **Adjusting score** (default OFF): when ON, a slider under the result lets you change the score if you disagree with the AI, which unlocks buttons accordingly.

The model is loaded while you type and kept in memory for 30 minutes, so grading usually takes a few seconds. Small models sometimes misjudge, which is why Again is always available and score adjusting exists.

## Timer and study HUD
A small panel (top left by default) shows:
- the countdown to the next question (yellow in the last 10 seconds),
- how many cards you answered this session (correct ✔ / wrong ✘),
- in Anki mode: cards reviewed today in Anki (all devices) and what's still due in your deck (new / learning / review, after Anki's daily limits).

Settings are in Flashcards config → **General Config...**:
- **Question timer: Real time / Game time**
  - *Real time* (default) counts real-world minutes, even while the game is paused or a menu is open.
  - *Game time* counts only while the world is running, so it stops while the game is paused and follows `/tick rate`. 1 minute = 1200 game ticks (a full Minecraft day is 20 minutes). In multiplayer the game never pauses, so this behaves almost like real time.
- **Timer & stats HUD: ON/OFF** and **HUD position** (any corner). The HUD is hidden with F1 and while the F3 debug screen is open.

The interval itself is set on the main config screen: "Prompt a question every [N] min [N] sec" (at least 1 second in total).

## Building
Needs JDK 25 and [Gradle](https://gradle.org/install/) 9.5.1 (the repo has no `gradlew` script).
```
gradle build
```
The mod jar ends up in `build/libs/` (the `-sources` jar is not the one to install).

## Credits and license
- [IvanIvanochko](https://github.com/IvanIvanochko): author of the Anki + AI Edition.
- [Flashcards](https://github.com/DennisOchulor/mc-flashcards) by DennisOchulor: the original mod this edition is built on.
- [AnkiConnect](https://ankiweb.net/shared/info/2055492159) for talking to Anki, and [Ollama](https://ollama.com) for running the AI locally.

Released under the MIT License, like the original. The original copyright and license notice are kept in [LICENSE](LICENSE), as the MIT License requires.
