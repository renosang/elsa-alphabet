package vn.vuongquoctuyet.hocchu;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

final class WordPlayScreen {
    private static final int PICKED = 0xFFFFE5AA, CONNECTED = 0xFFD8F7DF, HINT = 0xFFFFEDAF,
        WRONG = 0xFFFFE1BF, WRONG_EDGE = 0xFFE39A42;
    private final MainActivity host;
    private final WordPlay play;
    private final LinearLayout content;
    private final Runnable award;
    private final Runnable scenePicker;
    private TextView status;
    private TextView next;
    private TextView[] choices;
    private List<String> options;
    private final View[][] pairCards;
    private LinkBoard board;

    WordPlayScreen(MainActivity host, WordPlay play, LinearLayout content, Runnable award, Runnable scenePicker) {
        this.host = host;
        this.play = play;
        this.content = content;
        this.award = award;
        this.scenePicker = scenePicker;
        pairCards = new View[2][play.pairCount()];
    }

    void show() {
        LinearLayout heading = host.row();
        heading.addView(host.text(play.mode.title, 27, MainActivity.INK, true),
            new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(host.text(play.round >= WordPlay.QUESTIONS ? "★" : play.tutorial()
            ? "Làm mẫu" : (play.round + 1) + " / " + WordPlay.QUESTIONS,
            14, MainActivity.PLUM, true));
        host.toolPane(content).addView(heading);
        space(8);
        if (play.round >= WordPlay.QUESTIONS) {
            showReward();
            return;
        }
        status = host.text(instruction(), 16, MainActivity.MUTED, true);
        status.setMinHeight(host.dp(38));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        host.toolPane(content).addView(status);
        if (play.mode == WordPlay.Mode.FILL) showFill();
        else showPairs();
        space(10);
        next = host.button(play.tutorial() ? "Bắt đầu  ›" :
            play.round + 1 == WordPlay.QUESTIONS ? "Nhận quà  ›" : "Câu tiếp theo  ›", true, () -> {
                play.advance();
                if (play.round == WordPlay.QUESTIONS) award.run();
                host.refreshWordGame();
                if (play.round < WordPlay.QUESTIONS && play.mode == WordPlay.Mode.FILL) speak(true);
            });
        host.toolPane(content).addView(next);
        update();
    }

    void speak(boolean justPicture) {
        if (play.round >= WordPlay.QUESTIONS) host.voice("world_reward");
        else if (play.mode == WordPlay.Mode.FILL) {
            if (justPicture) host.voice(play.fill().card.audio());
            else host.voiceSequence(play.mode.audio, play.fill().card.audio());
        } else {
            host.voice(play.mode.audio);
        }
    }

    private String instruction() {
        if (play.solved) return play.streak >= 3 ? "Tuyệt quá! Con đang có chuỗi " + play.streak + " tia sáng!"
            : "Đúng rồi! Mình chơi tiếp nhé.";
        if (play.hint) return "Cô đã làm sáng ô gợi ý.";
        if (play.errors > 0) {
            String[] retry = {"Suýt đúng rồi, mình thử lại nhé.", "Nhìn kỹ thêm một chút nào.",
                "Nhầm chút xíu thôi, con chọn lại nhé."};
            return retry[Math.floorMod(play.round + play.errors, retry.length)];
        }
        if (play.tutorial()) return play.mode == WordPlay.Mode.FILL
            ? "Ô sáng là chữ còn thiếu. Con thử chọn nhé." : "Hai ô sáng là một cặp. Con thử nối nhé.";
        return play.mode.subtitle + ".";
    }

    private void showReward() {
        TextView title = host.text("Món quà của con", 24, MainActivity.INK, true);
        title.setGravity(Gravity.CENTER);
        content.addView(title);
        if (play.reward >= 0) {
            DaylightArt.PropIcon icon = new DaylightArt.PropIcon(host, play.reward);
            content.addView(icon, new LinearLayout.LayoutParams(-1, host.dp(135)));
            TextView label = host.text(DaylightArt.PROPS[play.reward], 20, MainActivity.PLUM, true);
            label.setGravity(Gravity.CENTER);
            content.addView(label);
        }
        space(14);
        host.toolPane(content).addView(host.button("Đến Cảnh của bé  ›", true, scenePicker));
        space(7);
        host.toolPane(content).addView(host.button("Chơi lại", false, () -> host.openWordGame(play.mode)));
    }

    private void showFill() {
        WordPlay.Fill question = play.fill();
        LinearLayout picturePanel = host.column();
        picturePanel.setGravity(Gravity.CENTER);
        picturePanel.setPadding(host.dp(12), host.dp(8), host.dp(12), host.dp(12));
        picturePanel.setBackground(host.background(0xFFFFF5DA, 24, 0xFFF2D694));
        View image = host.examplePicture(question.card.picture(), 70);
        image.setContentDescription("Nghe " + question.card.picture().phrase());
        host.makePressable(image, () -> speak(true));
        picturePanel.addView(image, new LinearLayout.LayoutParams(-1, host.dp(host.landscapeMode() ? host.landscapeHeight(58,.25f,115) : host.wideLayout() ? 116 : 145)));
        picturePanel.addView(host.text(question.prompt(), 30, MainActivity.INK, true));
        TextView listen = host.button("▶  Nghe tên hình", false, () -> speak(true));
        LinearLayout.LayoutParams listenSize = new LinearLayout.LayoutParams(-2, -2);
        listenSize.topMargin = host.dp(6);
        picturePanel.addView(listen, listenSize);
        content.addView(picturePanel);
        space(10);
        LinearLayout keys = host.row();
        options = play.fillChoices();
        choices = new TextView[options.size()];
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            TextView key = host.button(option, false, () -> fillAnswer(option));
            key.setTextSize(30);
            key.setMinHeight(host.dp(64));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) size.leftMargin = host.dp(8);
            keys.addView(key, size);
            choices[i] = key;
        }
        host.toolPane(content).addView(keys);
    }

    private void fillAnswer(String answer) {
        if (play.solved) return;
        boolean correct = play.choose(answer);
        update();
        View target = choices[options.indexOf(answer)];
        react(correct, target, -1);
    }

    private void showPairs() {
        board = new LinkBoard();
        content.addView(board, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout columns = host.row();
        columns.setGravity(Gravity.TOP);
        columns.setPadding(host.dp(5), host.dp(8), host.dp(5), host.dp(8));
        LinearLayout left = host.column(), right = host.column();
        columns.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rightSize = new LinearLayout.LayoutParams(0, -2, 1);
        rightSize.leftMargin = host.dp(34);
        columns.addView(right, rightSize);
        board.addView(columns, new FrameLayout.LayoutParams(-1, -2));
        for (int i = 0; i < play.pairCount(); i++) appendPair(left, false, i);
        for (int i : play.rightOrder()) appendPair(right, true, i);
    }

    private void appendPair(LinearLayout column, boolean right, int id) {
        LinearLayout cell = host.column();
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(host.dp(5), host.dp(5), host.dp(5), host.dp(5));
        String label;
        WordPlay.Card card = null;
        String audio;
        if (play.mode == WordPlay.Mode.LETTERS) {
            Lesson lesson = Lesson.ALL[Lesson.atAlphabetPosition(play.letters().get(id))];
            String letter = lesson.letter;
            label = right ? letter : letter.toUpperCase(Lesson.VI);
            audio = lesson.key + "_letter";
            cell.addView(host.text(label, 33, MainActivity.INK, true));
        } else {
            card = play.pictures().get(id);
            label = card.picture().phrase();
            audio = card.audio();
            if (right) {
                TextView word = host.text(label, 16, MainActivity.INK, true);
                word.setGravity(Gravity.CENTER);
                cell.addView(word);
            } else {
                View art = host.examplePicture(card.picture(), 41);
                art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                cell.addView(art, new LinearLayout.LayoutParams(-1, host.dp(46)));
            }
        }
        WordPlay.Card speakCard = card;
        String side = play.mode == WordPlay.Mode.LETTERS ? (right ? "Chữ thường " : "Chữ hoa ")
            : (right ? "Thẻ từ " : "Hình ");
        cell.setContentDescription(side + label);
        host.makePressable(cell, () -> {
            if (play.solved || play.isPaired(id)) {
                host.voice(audio);
                return;
            }
            int previousErrors = play.errors;
            boolean matched = play.selectPair(right, id);
            update();
            if (matched || play.errors > previousErrors) react(matched, cell, id);
            else {
                host.playTapSfx();
                host.voice(audio);
            }
        });
        if (speakCard != null && right) {
            if (host.landscapeMode()) cell.setOrientation(LinearLayout.HORIZONTAL);
            TextView preview = host.button("▶", false, () -> host.voice(speakCard.audio()));
            preview.setContentDescription("Nghe " + label);
            cell.addView(preview, new LinearLayout.LayoutParams(host.dp(48), -2));
            cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
        enableDrag(cell, right, id);
        cell.setMinimumHeight(host.dp(host.landscapeMode() ? 54 : play.mode == WordPlay.Mode.WORDS ? 80 : 62));
        LinearLayout.LayoutParams cellSize = new LinearLayout.LayoutParams(-1, -2);
        cellSize.bottomMargin = host.dp(7);
        column.addView(cell, cellSize);
        pairCards[right ? 1 : 0][id] = cell;
    }

    private void enableDrag(View cell, boolean right, int id) {
        int slop = ViewConfiguration.get(host).getScaledTouchSlop();
        cell.setOnTouchListener(new View.OnTouchListener() {
            float x, y;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        x = event.getX();
                        y = event.getY();
                        host.playTapSfx();
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        if (!play.solved && !play.isPaired(id)
                            && (Math.abs(event.getX() - x) > slop || Math.abs(event.getY() - y) > slop)) {
                            view.setPressed(false);
                            return host.dragLocally(view, new DragCard(right, id));
                        }
                        return false;
                    default: return false;
                }
            }
        });
        cell.setOnDragListener((view, event) -> {
            Object local = event.getLocalState();
            if (!(local instanceof DragCard)) return false;
            DragCard card = (DragCard) local;
            boolean canDrop = !play.solved && card.right != right && !play.isPaired(id);
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: return canDrop;
                case DragEvent.ACTION_DRAG_ENTERED: view.setAlpha(.74f); return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED: view.setAlpha(1f); return true;
                case DragEvent.ACTION_DROP:
                    if (!canDrop) return false;
                    view.setAlpha(1f);
                    boolean matched = play.match(card.id, id);
                    update();
                    react(matched, view, matched ? id : card.id);
                    return true;
                default: return true;
            }
        });
    }

    private void react(boolean correct, View target, int id) {
        if (correct) {
            host.playFeedbackSfx(true, play.streak);
            host.magicBurst(target, play.streak);
            host.mentorPose(DaylightArt.Princess.CHEER, play.streak >= 3 ? 1500 : 850);
            String keyword = keywordAudio(id);
            if (play.streak >= 3 && play.streak % 3 == 0)
                host.voiceChain("praise_streak3", keyword);
            else if (((play.round + play.streak) & 1) == 0)
                host.voiceChain("praise_correct" + Math.floorMod(play.round + play.streak, 3), keyword);
            else host.voice(keyword);
        } else if (play.hint) {
            wrongFlash(target);
            host.playFeedbackSfx(false, 0);
            String encouragement = "praise_wrong" + Math.floorMod(play.round + play.errors, 3);
            if (play.mode == WordPlay.Mode.FILL)
                host.voiceChain(encouragement, "world_hint_fill", play.fill().card.audio());
            else host.voiceChain(encouragement, "world_hint");
            host.mentorPose(DaylightArt.Princess.POINT, 1400);
        } else {
            wrongFlash(target);
            host.playFeedbackSfx(false, 0);
            host.voice("praise_wrong" + Math.floorMod(play.round + play.errors, 3));
            host.mentorPose(DaylightArt.Princess.ENCOURAGE, 850);
        }
    }

    private String keywordAudio(int id) {
        if (play.mode == WordPlay.Mode.FILL) return play.fill().card.audio();
        int safe = Math.max(0, id);
        if (play.mode == WordPlay.Mode.LETTERS) {
            Lesson lesson = Lesson.ALL[Lesson.atAlphabetPosition(play.letters().get(safe))];
            return lesson.key + "_letter";
        }
        return play.pictures().get(safe).audio();
    }

    private void wrongFlash(View target) {
        if (target == null) return;
        target.setBackground(host.background(WRONG, 16, WRONG_EDGE));
        host.gentleShake(target);
        if (board != null) board.elasticSnap();
        target.postDelayed(() -> {
            if (target.isAttachedToWindow())
                target.setBackground(host.background(0xFFFFFFFF, 16, MainActivity.STROKE));
        }, 820);
    }

    private void update() {
        status.setText(instruction());
        if (play.streak >= 2 && !play.solved)
            status.setText("✦ Chuỗi " + play.streak + " tia sáng! Tiếp tục nhé!");
        next.setVisibility(play.solved || play.tutorial() ? View.VISIBLE : View.GONE);
        if (play.mode == WordPlay.Mode.FILL) {
            for (int i = 0; i < choices.length; i++) {
                boolean correct = options.get(i).equals(play.fill().answer);
                int color = correct && play.solved ? CONNECTED : correct && (play.tutorial() || play.hint) ? HINT : 0xFFFFFFFF;
                choices[i].setBackground(host.background(color, 16, MainActivity.STROKE));
            }
            if (play.hint) {
                int answer = options.indexOf(play.fill().answer);
                choices[answer].post(() -> host.magicBurst(choices[answer], 1));
            }
            return;
        }
        for (int side = 0; side < 2; side++) {
            for (int id = 0; id < play.pairCount(); id++) {
                View card = pairCards[side][id];
                boolean selected = play.selected == id && play.selectedRight == (side == 1);
                card.setSelected(selected);
                int color = play.isPaired(id) ? CONNECTED : selected ? PICKED
                    : (play.tutorial() || play.hint) && play.hintPair() == id ? HINT : 0xFFFFFFFF;
                card.setBackground(host.background(color, 16, selected ? MainActivity.PLUM : MainActivity.STROKE));
            }
        }
        board.invalidate();
        if (play.hint) {
            int hint = play.hintPair();
            if (hint >= 0) pairCards[0][hint].post(() -> host.magicBurst(pairCards[0][hint], 1));
        }
    }

    private void space(int pixels) {
        content.addView(new View(host), new LinearLayout.LayoutParams(1, host.dp(pixels)));
    }

    private static final class DragCard {
        final boolean right;
        final int id;
        DragCard(boolean right, int id) {
            this.right = right;
            this.id = id;
        }
    }

    private final class LinkBoard extends FrameLayout {
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);

        LinkBoard() {
            super(host);
            setWillNotDraw(false);
            setBackground(host.background(0xFFE5F5ED, 22, 0xFFB8D9C7));
            line.setColor(0xFF2C7B63);
            line.setStrokeWidth(host.dp(4));
            line.setStrokeCap(Paint.Cap.ROUND);
            glow.setColor(0x55FFD45C);
            glow.setStrokeWidth(host.dp(11));
            glow.setStrokeCap(Paint.Cap.ROUND);
        }

        void elasticSnap() { host.gentleShake(this); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int[] boardLocation = new int[2], leftLocation = new int[2], rightLocation = new int[2];
            getLocationInWindow(boardLocation);
            for (int i = 0; i < play.pairCount(); i++) {
                if (!play.isPaired(i)) continue;
                View left = pairCards[0][i], right = pairCards[1][i];
                left.getLocationInWindow(leftLocation);
                right.getLocationInWindow(rightLocation);
                float x1 = leftLocation[0] + left.getWidth() - boardLocation[0];
                float y1 = leftLocation[1] + left.getHeight() / 2f - boardLocation[1];
                float x2 = rightLocation[0] - boardLocation[0];
                float y2 = rightLocation[1] + right.getHeight() / 2f - boardLocation[1];
                line.setShader(new LinearGradient(x1, y1, x2, y2,
                    new int[]{0xFF4DD6BF, 0xFFFFD45C, 0xFF9C79EB}, null, Shader.TileMode.CLAMP));
                canvas.drawLine(x1, y1, x2, y2, glow);
                canvas.drawLine(x1, y1, x2, y2, line);
                line.setShader(null);
            }
        }
    }
}
