package vn.vuongquoctuyet.hocchu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class WordPlay implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int QUESTIONS = 5;

    enum Mode {
        LETTERS("Nối chữ", "Ghép chữ hoa với chữ thường", "world_pairs", 0),
        FILL("Điền chữ", "Tìm chữ còn thiếu trong từ", "world_fill", 1),
        WORDS("Nối hình với từ", "Nghe tên hình và tìm thẻ từ", "world_words", 2);

        final String title, subtitle, audio;
        final int artwork;
        Mode(String title, String subtitle, String audio, int artwork) {
            this.title = title;
            this.subtitle = subtitle;
            this.audio = audio;
            this.artwork = artwork;
        }
    }

    static final class Card {
        final String key;
        final int example;
        Card(String key, int example) {
            this.key = key;
            this.example = example;
        }
        Lesson lesson() {
            for (Lesson entry : Lesson.ALL) if (entry.key.equals(key)) return entry;
            throw new IllegalArgumentException(key);
        }
        Lesson.Example picture() { return lesson().examples[example]; }
        String audio() { return key + "_word" + example; }
    }

    static final class Fill {
        final Card card;
        final String before, answer, after;
        final String[] options;

        Fill(String key, int example, String answer, String wrongOne, String wrongTwo) {
            card = new Card(key, example);
            String phrase = card.picture().phrase();
            int start = card.picture().before.isEmpty() ? 0 : card.picture().before.length() + 1;
            int index = phrase.indexOf(answer, start);
            if (index < start || index >= start + card.picture().word.length())
                throw new IllegalArgumentException(phrase + ": " + answer);
            before = phrase.substring(0, index);
            after = phrase.substring(index + answer.length());
            this.answer = answer;
            this.options = new String[]{answer, wrongOne, wrongTwo};
        }

        String prompt() { return before + "＿" + after; }
    }

    private static final Card[] PICTURES = {
        new Card("b", 0), new Card("c", 0), new Card("m", 0), new Card("g", 0),
        new Card("dd", 0), new Card("h", 0), new Card("v", 1), new Card("a", 0),
        new Card("o", 0), new Card("r", 0), new Card("oo", 0), new Card("x", 1),
        new Card("s", 0), new Card("d", 0), new Card("ee", 0), new Card("u", 0)
    };

    private static final Fill[] FIRST = {
        new Fill("b", 0, "b", "d", "m"), new Fill("c", 0, "c", "g", "s"),
        new Fill("m", 0, "m", "n", "b"), new Fill("g", 0, "g", "c", "h"),
        new Fill("dd", 0, "đ", "d", "b"), new Fill("h", 0, "h", "k", "n"),
        new Fill("v", 1, "v", "x", "t"), new Fill("r", 0, "r", "s", "t"),
        new Fill("s", 0, "s", "x", "r"), new Fill("d", 0, "d", "đ", "b")
    };
    private static final Fill[] INNER = {
        new Fill("aa", 0, "â", "a", "ă"), new Fill("aw", 0, "ă", "a", "â"),
        new Fill("a", 1, "a", "ă", "â"), new Fill("o", 0, "o", "ô", "ơ"),
        new Fill("oo", 0, "ô", "o", "ơ"), new Fill("ee", 0, "ê", "e", "ơ"),
        new Fill("b", 0, "á", "à", "ạ"), new Fill("x", 1, "í", "ì", "ỉ"),
        new Fill("dd", 0, "è", "é", "ẻ"), new Fill("v", 1, "ị", "ỉ", "í")
    };

    final Mode mode;
    final int level;
    private final long seed;
    int round = -1;
    int errors;
    int selected = -1;
    boolean selectedRight;
    int paired;
    int streak;
    int bestStreak;
    boolean solved;
    boolean rewardSaved;
    int reward = -1;
    boolean hint;

    WordPlay(Mode mode, int completed, long seed) {
        this.mode = mode;
        level = completed == 0 ? 1 : 2;
        this.seed = seed;
    }

    int pairCount() { return round <= 0 || level == 1 && round < 2 ? 2 : 3; }
    boolean tutorial() { return round < 0; }

    void advance() {
        if (!tutorial() && !solved) return;
        boolean leavingTutorial = tutorial();
        round++;
        if (leavingTutorial) streak = 0;
        selected = -1;
        paired = 0;
        solved = false;
        errors = 0;
        hint = false;
    }

    Fill fill() {
        if (tutorial()) return FIRST[0];
        if (level == 2) return INNER[shuffled(INNER.length, 17).get(round)];
        return round == 0 ? FIRST[0] : FIRST[1 + shuffled(FIRST.length - 1, 17).get(round - 1)];
    }

    List<String> fillChoices() {
        String[] options = fill().options;
        List<String> result = new ArrayList<>();
        for (int index : shuffled(options.length, 43 + round)) result.add(options[index]);
        return result;
    }

    List<Integer> letters() {
        if (tutorial()) {
            List<Integer> letters = new ArrayList<>();
            letters.add(0);
            letters.add(3);
            return letters;
        }
        List<Integer> order = shuffled(level == 1 ? 14 : Lesson.ALL.length, 29);
        int start = round * 2;
        return order.subList(start, start + pairCount());
    }

    List<Card> pictures() {
        List<Card> result = new ArrayList<>();
        if (tutorial()) {
            result.add(PICTURES[0]);
            result.add(PICTURES[1]);
        } else {
            List<Integer> order = shuffled(PICTURES.length, 81);
            for (int i = 0; i < pairCount(); i++) result.add(PICTURES[order.get(round * 2 + i)]);
        }
        return result;
    }

    List<Integer> rightOrder() {
        List<Integer> order = shuffled(pairCount(), 11 + round);
        boolean identical = true;
        for (int i = 0; i < order.size(); i++) identical &= order.get(i) == i;
        if (identical) Collections.rotate(order, 1);
        return order;
    }

    boolean isPaired(int id) { return (paired & (1 << id)) != 0; }

    boolean selectPair(boolean right, int id) {
        if (solved || isPaired(id)) return false;
        if (selected == -1 || selectedRight == right) {
            selected = id;
            selectedRight = right;
            return false;
        }
        return match(selected, id);
    }

    boolean match(int left, int right) {
        if (solved || isPaired(left) || isPaired(right)) return false;
        selected = -1;
        if (left != right) {
            miss();
            return false;
        }
        paired |= 1 << left;
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        errors = 0;
        hint = false;
        solved = Integer.bitCount(paired) == pairCount();
        return true;
    }

    boolean choose(String option) {
        if (solved) return false;
        if (!fill().answer.equals(option)) {
            miss();
            return false;
        }
        solved = true;
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        hint = false;
        return true;
    }

    int hintPair() {
        for (int i = 0; i < pairCount(); i++) if (!isPaired(i)) return i;
        return -1;
    }

    private void miss() {
        errors++;
        streak = 0;
        if (errors >= 2) hint = true;
    }

    private List<Integer> shuffled(int count, int salt) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < count; i++) indexes.add(i);
        Collections.shuffle(indexes, new Random(seed ^ (salt * 8191L)));
        return indexes;
    }
}
