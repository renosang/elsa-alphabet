package vn.vuongquoctuyet.hocchu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

final class LearningGame implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int QUESTIONS = 5;
    private static final int[] PICTURES = {0, 1, 2, 4, 5, 6, 9, 12, 13, 19, 21, 22};
    private static final String[] PICTURE_NAMES = {
        "quả cam", "con ong", "cái ô", "con dê", "con mèo", "mặt trăng",
        "con cá", "con gà", "bông hoa", "cái quạt", "ngôi sao", "con tôm"
    };

    enum Mode {
        COUNT("Đếm hình", "Chạm từng hình để đếm rồi chọn số.", "game_count"),
        COMPARE("Nhóm nào nhiều hơn?", "Chọn nhóm có nhiều hình hơn.", "game_compare"),
        PATTERN("Xếp tiếp dãy hình", "Chọn hình sẽ đứng sau dấu hỏi.", "game_pattern"),
        ODD("Tìm hình khác", "Chạm vào hình khác với những hình còn lại.", "game_odd"),
        MEMORY("Lật cặp giống nhau", "Lật hai thẻ có hình giống nhau.", "game_memory");

        final String title, instruction, audio;

        Mode(String title, String instruction, String audio) {
            this.title = title;
            this.instruction = instruction;
            this.audio = audio;
        }
    }

    enum Move { IGNORED, WRONG, RIGHT, FIRST, PAIR }

    final Mode mode;
    final int level;
    private final Random random;
    int round;
    int rightWithoutHelp;
    int mistakes;
    int counted;
    int firstCard = -1, secondCard = -1, pairs;
    int leftCount, rightCount, picture;
    int answer, selected = -1;
    int[] prompt, options = new int[0];
    boolean[] paired;
    boolean solved, hint;

    LearningGame(Mode mode, int level, Random random) {
        this.mode = mode;
        this.level = Math.max(1, Math.min(3, level));
        this.random = random;
        nextQuestion();
    }

    void nextQuestion() {
        round++;
        mistakes = 0;
        selected = -1;
        counted = 0;
        solved = false;
        hint = false;
        picture = random.nextInt(PICTURES.length);
        switch (mode) {
            case COUNT: {
                int count = 1 + random.nextInt(level == 1 ? 4 : level == 2 ? 6 : 9);
                prompt = new int[count];
                int minimum = Math.max(1, count - 1);
                options = shuffled(minimum, minimum + 1, minimum + 2);
                answer = positionOf(options, count);
                break;
            }
            case COMPARE:
                leftCount = 1 + random.nextInt(level + 2);
                rightCount = leftCount + (level == 1 ? 2 : 1);
                if (random.nextBoolean()) {
                    int smaller = leftCount;
                    leftCount = rightCount;
                    rightCount = smaller;
                }
                answer = leftCount > rightCount ? 0 : 1;
                break;
            case PATTERN: {
                int[] available = images(3);
                int[] cycle = level == 1
                    ? new int[]{available[0], available[1]}
                    : new int[]{available[0], available[level == 2 ? 0 : 1], available[level == 2 ? 1 : 2]};
                prompt = new int[level == 1 ? 3 + random.nextInt(2) : 4 + random.nextInt(2)];
                for (int i = 0; i < prompt.length; i++) prompt[i] = cycle[i % cycle.length];
                options = shuffled(available);
                answer = positionOf(options, cycle[prompt.length % cycle.length]);
                break;
            }
            case ODD: {
                int[] available = images(2);
                prompt = new int[level == 1 ? 3 : level == 2 ? 4 : 5];
                java.util.Arrays.fill(prompt, available[0]);
                answer = random.nextInt(prompt.length);
                prompt[answer] = available[1];
                break;
            }
            case MEMORY:
                int[] available = images(level + 1);
                int[] duplicate = new int[available.length * 2];
                for (int i = 0; i < available.length; i++) {
                    duplicate[i * 2] = available[i];
                    duplicate[i * 2 + 1] = available[i];
                }
                prompt = shuffled(duplicate);
                paired = new boolean[prompt.length];
                break;
        }
    }

    Move answer(int position) {
        if (solved || mode == Mode.MEMORY) return Move.IGNORED;
        selected = position;
        if (position != answer) {
            if (++mistakes >= 2) hint = true;
            return Move.WRONG;
        }
        solved = true;
        if (mistakes == 0 && !hint) rightWithoutHelp++;
        return Move.RIGHT;
    }

    Move flip(int index) {
        if (mode != Mode.MEMORY || solved || secondCard >= 0 || firstCard == index || paired[index])
            return Move.IGNORED;
        if (firstCard < 0) {
            firstCard = index;
            return Move.FIRST;
        }
        if (prompt[firstCard] != prompt[index]) {
            mistakes++;
            secondCard = index;
            return Move.WRONG;
        }
        paired[firstCard] = true;
        paired[index] = true;
        firstCard = -1;
        pairs++;
        solved = pairs == prompt.length / 2;
        return Move.PAIR;
    }

    void hideMismatch() {
        firstCard = -1;
        secondCard = -1;
    }

    boolean isFinished() {
        return solved && (mode == Mode.MEMORY || round == QUESTIONS);
    }

    boolean visible(int index) {
        return paired[index] || index == firstCard || index == secondCard;
    }

    int countPicture(int index) {
        counted |= 1 << index;
        return Integer.bitCount(counted);
    }

    boolean wasCounted(int index) {
        return (counted & (1 << index)) != 0;
    }

    static int art(int item) { return PICTURES[item]; }

    static String name(int item) { return PICTURE_NAMES[item]; }

    private int[] images(int count) {
        int[] pool = new int[PICTURES.length];
        for (int i = 0; i < pool.length; i++) pool[i] = i;
        return java.util.Arrays.copyOf(shuffled(pool), count);
    }

    private int[] shuffled(int... values) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int value : values) list.add(value);
        Collections.shuffle(list, random);
        int[] output = new int[values.length];
        for (int i = 0; i < output.length; i++) output[i] = list.get(i);
        return output;
    }

    private int positionOf(int[] items, int value) {
        for (int i = 0; i < items.length; i++) if (items[i] == value) return i;
        throw new IllegalStateException("Missing answer " + value);
    }
}
