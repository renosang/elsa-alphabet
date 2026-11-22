package vn.vuongquoctuyet.hocchu;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public final class MainActivity extends Activity {
    static final int INK = 0xFF213453;
    static final int MUTED = 0xFF465C70;
    static final int STROKE = 0xFFB7CDDE;
    static final int PLUM = 0xFF64468B;
    private static final int SKY = 0xFFEDF8FE;
    private static final int SURFACE = 0xFFFFFFFF;
    private static final int TEAL = 0xFF206A60;
    private static final int MAIN_ACTION = 0xFFBFEFE2;
    private static final int AMBER = 0xFF79501A;

    private static final int LISTEN = 1, LEGACY_SPEAK = 2, LEGACY_PICTURE = 3, FIND = 4, DONE = 5, MATCH = 6;
    private static final int HUB = 7, GAME_MENU = 8, GAME = 9, GAME_DONE = 10;
    private static final String[] GAME_GROUPS = {"Toán vui", "Tư duy", "Ghi nhớ"};
    private static final LearningGame.Mode[][] GAME_MODES = {
        {LearningGame.Mode.COUNT, LearningGame.Mode.COMPARE},
        {LearningGame.Mode.PATTERN, LearningGame.Mode.ODD}, {LearningGame.Mode.MEMORY}
    };
    private static final int[] GAME_ACCENTS = {0xFF78501B, PLUM, TEAL};
    private static final String[] CHAPTERS = {"Hồ Băng", "Rừng Tuyết", "Đỉnh Sao", "Cung Điện"};
    private static final String[] CHAPTER_ICONS = {"❄", "🌲", "★", "♜"};
    private static final int[] CHAPTER_START = {0, 6, 14, 22, 29};
    private static final int[] CHAPTER_ACCENT = {0xFF176881, TEAL, PLUM, AMBER};
    private static final int WORD_MENU = 11, WORD_GAME = 12, SCENE_MENU = 13, STUDIO = 14, THINK_MENU = 15;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences progress;
    private FamilyProfiles family;
    private static final int FAMILY_REQUEST = 71;
    private LinearLayout page;
    private LinearLayout landscapeTools;
    private int alphabetSheet;
    private boolean chapterMapOpen;

    boolean landscapeMode() {
        android.content.res.Configuration c = getResources().getConfiguration();
        return c.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && c.screenWidthDp >= 480;
    }

    int landscapeHeight(int minimum, float fraction, int maximum) {
        return Math.max(minimum, Math.min(maximum, Math.round(getResources().getConfiguration().screenHeightDp * fraction)));
    }

    LinearLayout toolPane(LinearLayout usual) {
        return landscapeTools == null ? usual : landscapeTools;
    }

    private void landscapePanes(float mainWeight) {
        LinearLayout columns = row();
        columns.setGravity(Gravity.TOP);
        page.addView(columns, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout activity = column();
        activity.setGravity(Gravity.CENTER_VERTICAL);
        landscapeTools = column();
        landscapeTools.setBackground(background(0xFFF9F8FF, 20, STROKE));
        landscapeTools.setGravity(Gravity.CENTER_VERTICAL);
        landscapeTools.setPadding(dp(10), dp(8), dp(10), dp(8));
        columns.addView(scrollPane(activity), new LinearLayout.LayoutParams(0, -1, mainWeight));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -1, 10 - mainWeight);
        right.leftMargin = dp(10);
        columns.addView(scrollPane(landscapeTools), right);
        page = activity;
    }

    private boolean splitActivity() {
        return wideLayout() && (scene == GAME || scene == WORD_GAME || scene == STUDIO
            || scene == LISTEN || scene == FIND || scene == MATCH);
    }

    private ScrollView scrollPane(View content) {
        ScrollView viewport = new ScrollView(this);
        viewport.setFillViewport(true);
        viewport.addView(content, new ScrollView.LayoutParams(-1, -2));
        return viewport;
    }
    private MediaPlayer player;
    private final android.util.LruCache<Integer, Bitmap> cardImages =
        new android.util.LruCache<Integer, Bitmap>(18 * 1024 * 1024) {
            @Override protected int sizeOf(Integer key, Bitmap image) { return image.getAllocationByteCount(); }
        };
    private Runnable hintTask;
    private Runnable listenAdvance;
    private Runnable studyAdvance;
    private TextView nextCountdown;
    private TextView studyPlay;
    private TextView nextPauseButton;
    private boolean nextPaused;
    private boolean reviewSequence;
    private boolean listenComplete;
    private int promptVariant;
    private int introState;
    private int announcedNextIndex = -1;
    private boolean skipIntroOnce;
    private ToneGenerator effects;
    private Typeface mediumText, boldText;
    private int listenVersion;
    private TextView sampleStatus;
    private View sampleGlyph, samplePicture;
    private TextView message;
    private int chapterIndex;
    private boolean sceneMenuFromWordGame;
    private int gameGroup;
    private int studioIndex;
    private String studioUndo;
    private SceneStudio studio;
    private WordPlay wordGame;
    private WordPlayScreen wordScreen;
    private DaylightArt.Princess princess;
    private LearningGame game;
    private int scene, lessonIndex, exampleIndex, errors;
    private boolean reveal, challengeFound;
    private int pageRevision;
    private ArrayList<Integer> choices;
    private boolean active;
    private Lesson lesson() { return Lesson.ALL[lessonIndex]; }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(SKY);
        getWindow().setNavigationBarColor(SKY);
        int systemUi = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) systemUi |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(systemUi);
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controls = getWindow().getInsetsController();
            if (controls != null) {
                int style = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controls.setSystemBarsAppearance(style, style);
            }
        }
        mediumText = Typeface.createFromAsset(getAssets(), "fonts/nunito-medium.ttf");
        boldText = Typeface.createFromAsset(getAssets(), "fonts/nunito-bold.ttf");
        family = new FamilyProfiles(this);
        progress = family.progress(family.active());
        if (saved != null) {
            alphabetSheet = saved.getInt("alphabet_sheet");
            chapterMapOpen = saved.getBoolean("chapter_map");
            lessonIndex = saved.getInt("lesson");
            scene = studyStage(saved.getInt("scene"));
            gameGroup = saved.getInt("game_group");
            game = (LearningGame) saved.getSerializable("game");
            if (scene == GAME_MENU || ((scene == GAME || scene == GAME_DONE) && game == null)) scene = THINK_MENU;
            wordGame = (WordPlay) saved.getSerializable("word_game");
            if (scene == WORD_GAME && wordGame == null) scene = WORD_MENU;
            sceneMenuFromWordGame = saved.getBoolean("scene_from_word") && wordGame != null;
            studioIndex = Math.max(0, Math.min(2, saved.getInt("studio_index")));
            studioUndo = saved.getString("studio_undo");
            errors = saved.getInt("errors");
            choices = saved.getIntegerArrayList("choices");
            reveal = saved.getBoolean("reveal");
            challengeFound = saved.getBoolean("challenge_found");
            nextPaused = saved.getBoolean("next_paused");
            reviewSequence = saved.getBoolean("review_sequence");
            listenComplete = saved.getBoolean("listen_complete");
            promptVariant = Math.floorMod(saved.getInt("prompt_variant"), 5);
            introState = Math.max(0, Math.min(2, saved.getInt("intro_state")));
            announcedNextIndex = saved.getInt("announced_next", -1);
            skipIntroOnce = saved.getBoolean("skip_intro_once");
            exampleIndex = saved.getInt("example");
            chapterIndex = scene > 0 && scene <= MATCH ? chapterOf(lessonIndex) : saved.getInt("chapter");
        } else {
            scene = HUB;
            int resume = progress.getInt("resume_lesson", -1);
            int next = resume >= 0 && resume < Lesson.ALL.length ? resume : nextLesson(-1);
            chapterIndex = chapterOf(next < 0 ? Lesson.atAlphabetPosition(0) : next);
        }
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        family.applyOrientation(this);
        active = true;
        if (scene == LISTEN && player == null && !listenComplete) playListenIntro();
        if (scene == GAME) scheduleMemoryReset();
        if (scene == DONE && !nextPaused) announceNext(false);
        if (scene == FIND || scene == MATCH) playChallenge();
        if (scene == HUB && !progress.getBoolean("daylight_welcome_played", false)) {
            int revision = pageRevision;
            handler.postDelayed(() -> {
                if (!active || revision != pageRevision || player != null) return;
                progress.edit().putBoolean("daylight_welcome_played", true).apply();
                voice("world_welcome");
            }, 400);
        }
        armHint();
    }

    @Override protected void onPause() {
        active = false;
        stopVoice();
        cancelHint();
        cancelListenAdvance();
        cancelStudyAdvance();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("alphabet_sheet", alphabetSheet);
        state.putBoolean("chapter_map", chapterMapOpen);
        state.putInt("lesson", lessonIndex);
        state.putInt("scene", scene);
        state.putInt("example", exampleIndex);
        state.putInt("chapter", chapterIndex);
        state.putInt("game_group", gameGroup);
        state.putSerializable("game", game);
        state.putSerializable("word_game", wordGame);
        state.putBoolean("scene_from_word", sceneMenuFromWordGame);
        state.putInt("studio_index", studioIndex);
        state.putString("studio_undo", studio == null ? studioUndo : studio.undoState());
        state.putInt("errors", errors);
        state.putIntegerArrayList("choices", choices);
        state.putBoolean("reveal", reveal);
        state.putBoolean("challenge_found", challengeFound);
        state.putBoolean("next_paused", nextPaused);
        state.putBoolean("review_sequence", reviewSequence);
        state.putBoolean("listen_complete", listenComplete);
        state.putInt("prompt_variant", promptVariant);
        state.putInt("intro_state", introState);
        state.putInt("announced_next", announcedNextIndex);
        state.putBoolean("skip_intro_once", skipIntroOnce);
    }

    @Override public void onBackPressed() {
        if (scene == HUB) super.onBackPressed();
        else if (scene == WORD_GAME) openWordMenu();
        else if (scene == STUDIO) openSceneMenu(false);
        else if (scene == SCENE_MENU) leaveSceneMenu();
        else if (scene == GAME || scene == GAME_DONE) openThinkMenu();
        else if (scene == 0 || scene >= WORD_MENU) openHub();
        else home(false);
    }

    private void openHub() {
        scene = HUB;
        render();
    }

    private void home(boolean talk) {
        scene = 0;
        render();
        if (talk) voice("home");
    }

    private void startLesson(int index, int stage, int example) {
        lessonIndex = index;
        chapterIndex = chapterOf(index);
        reviewSequence = progress.getBoolean(lesson().key + "_done", false);
        if (studyStage(stage) == LISTEN) {
            int visits = progress.getInt(lesson().key + "_visits", 0) + 1;
            String today = FamilyProfiles.dayKey(new Date());
            boolean firstToday = !today.equals(progress.getString("study_session_day", ""));
            introState = reviewSequence ? 2 : firstToday ? 0 : 1;
            progress.edit().putInt(lesson().key + "_visits", visits)
                .putString("study_session_day", today).apply();
            promptVariant = new java.util.Random(System.nanoTime() ^ ((long)index << 20) ^ visits).nextInt(5);
            listenComplete = false;
            skipIntroOnce = announcedNextIndex == index;
            announcedNextIndex = -1;
        }
        exampleIndex = example < 0 ? (progress.getBoolean(lesson().key + "_done", false) ? 1 : 0) : example;
        enter(stage);
    }

    private int studyStage(int stage) {
        return stage == LEGACY_SPEAK || stage == LEGACY_PICTURE ? FIND : stage;
    }

    private void enter(int next) {
        scene = studyStage(next);
        if (scene == LISTEN) listenComplete = false;
        errors = 0;
        reveal = false;
        nextPaused = false;
        challengeFound = false;
        choices = null;
        if (scene != DONE) {
            progress.edit().putInt("resume_lesson", lessonIndex).putInt("resume_scene", scene)
                .putInt("resume_example", exampleIndex).apply();
        }
        render();
        if (scene == LISTEN) {
            playListenIntro();
        }
        if (scene == FIND || scene == MATCH) playChallenge();
        if (scene == DONE) announceNext(true);
        armHint();
    }

    private void render() {
        pageRevision++;
        landscapeTools = null;
        princess = null;
        studio = null;
        wordScreen = null;
        stopVoice();
        cancelHint();
        message = null;
        cancelStudyAdvance();
        nextCountdown = null;
        nextPauseButton = null;
        sampleStatus = null;
        studyPlay = null;
        sampleGlyph = null;
        samplePicture = null;
        cancelListenAdvance();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SKY);
        root.addView(new SkyView(), new FrameLayout.LayoutParams(-1, -1));
        page = column();
        int inset = dp(wideLayout() ? 14 : 20);
        page.setPadding(inset, dp(8), inset, dp(wideLayout() ? 8 : 24));
        root.addView(landscapeMode() || splitActivity() ? page : scrollPane(page), new FrameLayout.LayoutParams(-1, -1));
        SafeContent.attach(this, root, SKY, false);
        if (landscapeMode() && (scene == THINK_MENU || scene == WORD_MENU || scene == SCENE_MENU)) showLandscapeMenu();
        else if (scene == HUB) showHub();
        else if (scene == THINK_MENU) showThinkMenu();
        else if (scene == GAME_DONE) {
            showGameDone();
            if (!landscapeMode()) showMentorDock(page, false);
        }
        else if (scene == WORD_MENU) showWordMenu();
        else if (scene == SCENE_MENU) showSceneMenu();
        else if (scene == 0) showHome();
        else if (scene == DONE) {
            showDone();
            if (!landscapeMode()) showMentorDock(page, false);
        }
        else showWithMentor();
    }

    private void showHub() {
        LinearLayout bar = row();
        LinearLayout learner = column();
        learner.addView(text("VƯƠNG QUỐC TUYẾT", 15, PLUM, true));
        learner.addView(text(family.name(family.active()), 14, MUTED, false));
        bar.addView(learner, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(button("Phụ huynh", false, this::showParents), new LinearLayout.LayoutParams(-2, -2));
        page.addView(bar);
        gap(10);

        if (landscapeMode()) {
            LinearLayout bounded = column();
            page.addView(scrollPane(bounded), new LinearLayout.LayoutParams(-1, 0, 1));
            page = bounded;
        }
        LinearLayout sections = column();
        if (wideLayout()) sections.setOrientation(LinearLayout.HORIZONTAL);
        sections.setGravity(Gravity.TOP);
        page.addView(sections);
        LinearLayout intro = column();
        if (wideLayout()) {
            sections.addView(intro, new LinearLayout.LayoutParams(0, -2, .91f));
        } else {
            sections.addView(intro, new LinearLayout.LayoutParams(-1, -2));
        }

        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(background(0xFFD9F2FC, 24, STROKE));
        hero.setClipToOutline(true);
        hero.addView(landscape(0), new FrameLayout.LayoutParams(-1, -1));
        View fade = new View(this);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFFF0FAFF, 0xEEEDF9FE, 0x00C8ECFA}));
        hero.addView(fade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout greeting = row();
        greeting.setPadding(dp(15), dp(9), 0, dp(8));
        LinearLayout copy = column();
        copy.addView(text("Học & Chơi", landscapeMode() ? 24 : 31, INK, true));
        TextView prompt = text("Con muốn chơi gì?", 16, MUTED, true);
        prompt.setPadding(0, dp(5), 0, dp(10));
        copy.addView(prompt);
        copy.addView(button("▶  Trò chuyện", false, this::answerMentor));
        greeting.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        greeting.addView(mentorPortrait(), new LinearLayout.LayoutParams(dp(landscapeMode() ? landscapeHeight(58,.18f,100) : 130), dp(landscapeMode() ? landscapeHeight(88,.36f,150) : 151)));
        hero.addView(greeting, new FrameLayout.LayoutParams(-1, -1));
        int heroHeight = Math.max(174, Math.round(getResources().getConfiguration().fontScale * 140));
        intro.addView(hero, new LinearLayout.LayoutParams(-1, dp(landscapeMode() ? Math.max(landscapeHeight(126,.42f,210), Math.round(130 * getResources().getConfiguration().fontScale)) : heroHeight)));

        int resume = progress.getInt("resume_lesson", -1);
        if (resume >= 0 && resume < Lesson.ALL.length) {
            TextView continuation = button("Học tiếp chữ " + Lesson.ALL[resume].letter.toUpperCase(Lesson.VI) + "  ›",
                true, () -> startLesson(resume, progress.getInt("resume_scene", LISTEN), progress.getInt("resume_example", 0)));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
            size.topMargin = dp(10);
            intro.addView(continuation, size);
        }
        LinearLayout destinations = column();
        LinearLayout.LayoutParams mapSize = wideLayout()
            ? new LinearLayout.LayoutParams(0, -2, 1.24f) : new LinearLayout.LayoutParams(-1, -2);
        if (wideLayout()) mapSize.leftMargin = dp(14);
        else mapSize.topMargin = dp(13);
        sections.addView(destinations, mapSize);
        LinearLayout top = row(), bottom = row();
        top.setGravity(Gravity.TOP);
        bottom.setGravity(Gravity.TOP);
        destinations.addView(top);
        destinations.addView(bottom);
        mapTile(top, "Học chữ", "29 chữ cái", 0, 11, () -> home(false));
        mapTile(top, "Chơi cùng chữ", "Nối chữ, điền chữ", 1, 6, this::openWordMenu);
        mapTile(bottom, "Cảnh của bé", "Chọn cảnh và trang trí", 2, 2, () -> openSceneMenu(true));
        mapTile(bottom, "Toán & tư duy", "Đếm, xếp dãy, ghi nhớ", 1, 7, this::openThinkMenu);
        LinearLayout newGames = row();
        destinations.addView(newGames);
        mapTile(newGames, "Đếm khối màu", "Chạm khối để đếm", 0, 7, () -> openStudio("blocks"));
        mapTile(newGames, "Sáng tạo & tô màu", "Vẽ và lưu tranh", 2, 6, () -> openStudio("art"));
    }

    private void openStudio(String mode) {
        stopVoice();
        startActivity(new Intent(this, PlayStudio.class).putExtra("mode", mode));
    }

    private ImageView landscape(int index) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageResource(DaylightArt.SCENES[index]);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return image;
    }

    private void mapTile(LinearLayout row, String title, String subtitle, int sceneArt, int item, Runnable action) {
        LinearLayout tile = column();
        int tint = DaylightArt.SCENE_COLORS[sceneArt];
        tile.setBackground(background(tint, 21, STROKE));
        tile.setClipToOutline(true);
        FrameLayout illustration = new FrameLayout(this);
        illustration.addView(landscape(sceneArt), new FrameLayout.LayoutParams(-1, -1));
        if (item >= 0) {
            DaylightArt.PropIcon icon = new DaylightArt.PropIcon(this, item);
            illustration.addView(icon, new FrameLayout.LayoutParams(dp(62), dp(64), Gravity.CENTER));
        }
        tile.addView(illustration, new LinearLayout.LayoutParams(-1, dp(landscapeMode() ? landscapeHeight(40,.14f,68) : 75)));
        LinearLayout words = column();
        words.setPadding(dp(9), dp(7), dp(8), dp(9));
        words.addView(text(title, 18, INK, true));
        TextView copy = text(subtitle, 13, INK, false);
        copy.setPadding(0, dp(4), 0, 0);
        words.addView(copy);
        tile.addView(words);
        tile.setContentDescription(title + ". " + subtitle);
        makePressable(tile, action);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
        size.bottomMargin = dp(9);
        if (row.getChildCount() > 0) size.leftMargin = dp(9);
        row.addView(tile, size);
    }

    private void openThinkMenu() { scene = THINK_MENU; render(); }

    private void showLandscapeMenu() {
        boolean scenes = scene == SCENE_MENU;
        boolean math = scene == THINK_MENU;
        String name = scenes ? "Cảnh của bé" : math ? "Toán & tư duy" : "Chơi cùng chữ";
        LinearLayout header = row();
        header.addView(backControl(scenes && sceneMenuFromWordGame ? wordGame.mode.title : "Học & Chơi",
            scenes ? this::leaveSceneMenu : this::openHub));
        TextView heading = text(name, 23, INK, true);
        heading.setGravity(Gravity.END);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(header);
        LinearLayout catalog = column();
        page.addView(scrollPane(catalog), new LinearLayout.LayoutParams(-1, 0, 1));
        page = catalog;
        LinearLayout cards = column();
        page = cards;
        if (math) {
            for (int group = 0; group < GAME_GROUPS.length; group++) showGameChoices(group);
        } else if (scenes) {
            sceneChoices();
        } else {
            for (WordPlay.Mode mode : WordPlay.Mode.values())
                worldTile(mode.title, mode.subtitle, mode.artwork, () -> openWordGame(mode));
        }
        page = catalog;
        int across = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        LinearLayout line = null;
        int n = 0;
        while (cards.getChildCount() > 0) {
            View card = cards.getChildAt(0);
            cards.removeViewAt(0);
            if (!card.isClickable()) continue;
            if (n % across == 0) {
                line = row();
                LinearLayout.LayoutParams band = new LinearLayout.LayoutParams(-1, -2);
                band.topMargin = dp(8);
                catalog.addView(line, band);
            }
            card.setMinimumHeight(dp(landscapeHeight(78, .27f, 130)));
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, -2, 1);
            if (n % across != 0) slot.leftMargin = dp(7);
            line.addView(card, slot);
            n++;
        }
        while (n % across != 0) {
            line.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            n++;
        }
    }

    private void showThinkMenu() {
        worldHeader("Học & Chơi", this::openHub);
        title("Toán & tư duy", "Chọn trò con muốn chơi.");
        gap(14);
        for (int group = 0; group < GAME_GROUPS.length; group++) {
            page.addView(text(GAME_GROUPS[group], 18, GAME_ACCENTS[group], true));
            gap(6);
            showGameChoices(group);
        }
    }

    private void openWordMenu() { scene = WORD_MENU; render(); }

    private void showWordMenu() {
        worldHeader("Học & Chơi", this::openHub);
        title("Chơi cùng chữ", "Con muốn nối chữ hay tìm chữ còn thiếu?");
        gap(14);
        for (WordPlay.Mode mode : WordPlay.Mode.values()) {
            worldTile(mode.title, mode.subtitle, mode.artwork, () -> openWordGame(mode));
        }
    }

    void openWordGame(WordPlay.Mode mode) {
        wordGame = new WordPlay(mode, progress.getInt("word_rounds_" + mode.name(), 0), System.nanoTime());
        scene = WORD_GAME;
        render();
        wordScreen.speak(false);
        mentorPose(DaylightArt.Princess.POINT, 1800);
    }

    void refreshWordGame() {
        render();
        if (wordGame.round >= WordPlay.QUESTIONS) {
            voice("world_reward");
            mentorPose(DaylightArt.Princess.CHEER, 1500);
        }
    }

    private void saveWordReward() {
        if (wordGame.rewardSaved) return;
        wordGame.rewardSaved = true;
        String key = "word_rounds_" + wordGame.mode.name();
        int count = progress.getInt("studio_rewards", 0);
        int possible = DaylightArt.PROPS.length - DaylightArt.BASIC_PROPS;
        wordGame.reward = DaylightArt.BASIC_PROPS + (count % possible);
        FamilyProfiles.record(progress, progress.edit().putInt(key, progress.getInt(key, 0) + 1)
            .putInt("studio_rewards", count + 1), false).apply();
    }

    private void openSceneMenu(boolean speak) {
        if (scene != STUDIO) sceneMenuFromWordGame = scene == WORD_GAME;
        scene = SCENE_MENU;
        studioUndo = null;
        render();
        if (speak) voice("world_scene");
    }

    private void leaveSceneMenu() {
        if (sceneMenuFromWordGame && wordGame != null) {
            scene = WORD_GAME;
            render();
        } else openHub();
    }

    private void showSceneMenu() {
        worldHeader(sceneMenuFromWordGame ? wordGame.mode.title : "Học & Chơi", this::leaveSceneMenu);
        title("Cảnh của bé", "Con chọn nơi muốn trang trí nhé.");
        gap(14);
        sceneChoices();
    }

    private void sceneChoices() {
        for (int i = 0; i < DaylightArt.SCENES.length; i++) {
            int index = i;
            int pieces = SceneStudio.piecesCount(progress, index);
            worldTile(DaylightArt.SCENE_NAMES[i], pieces == 0 ? "Bắt đầu trang trí"
                : "Mở lại cảnh đã lưu · " + pieces + " món", i, () -> {
                    studioIndex = index;
                    studioUndo = null;
                    scene = STUDIO;
                    render();
                    studio.speak();
                });
        }
    }

    private void worldTile(String name, String subtitle, int art, Runnable action) {
        LinearLayout tile = landscapeMode() ? column() : row();
        tile.setBackground(background(DaylightArt.SCENE_COLORS[art], 18, STROKE));
        tile.setClipToOutline(true);
        tile.addView(landscape(art), new LinearLayout.LayoutParams(landscapeMode() ? -1 : dp(101), dp(landscapeMode() ? landscapeHeight(58, .26f, 150) : 86)));
        LinearLayout words = column();
        words.setPadding(dp(12), dp(7), dp(5), dp(7));
        words.addView(text(name, 20, INK, true));
        TextView detail = text(subtitle, 14, MUTED, false);
        detail.setPadding(0, dp(4), 0, 0);
        words.addView(detail);
        tile.addView(words, landscapeMode() ? new LinearLayout.LayoutParams(-1,-2) : new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text("›", 25, PLUM, true);
        arrow.setPadding(0, 0, dp(10), 0);
        if (!landscapeMode()) tile.addView(arrow);
        tile.setContentDescription(name + ". " + subtitle);
        makePressable(tile, action);
        page.addView(tile);
        gap(9);
    }

    private void worldHeader(String title, Runnable back) {
        LinearLayout bar = row();
        bar.addView(backControl(title, back));
        if (landscapeMode()) {
            bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            addLandscapeGuide(bar);
        }
        page.addView(bar);
        gap(9);
    }

    private void addLandscapeGuide(LinearLayout bar) {
        LinearLayout help = row();
        help.setPadding(dp(6), 0, dp(8), 0);
        help.setBackground(background(0xFFF1EBFF, 18, 0));
        help.addView(character(), new LinearLayout.LayoutParams(dp(36), dp(44)));
        help.addView(text("Nghe cô", 15, PLUM, true));
        help.setMinimumHeight(dp(48));
        help.setContentDescription("Nghe cô hướng dẫn");
        makePressable(help, this::answerMentor);
        bar.addView(help);
    }

    boolean wideLayout() {
        android.content.res.Configuration configuration = getResources().getConfiguration();
        return landscapeMode() || configuration.screenWidthDp >= 650
            || (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                && configuration.screenWidthDp >= 560);
    }

    private void showWithMentor() {
        if (scene == WORD_GAME) worldHeader("Chơi cùng chữ", this::openWordMenu);
        if (scene == STUDIO) worldHeader("Cảnh của bé", () -> openSceneMenu(false));
        LinearLayout sidebar = null;
        if (scene == GAME) worldHeader("Toán & tư duy", this::openThinkMenu);
        if (scene == LISTEN || scene == FIND || scene == MATCH) lessonNavigation();
        if (landscapeMode()) {
            landscapePanes(scene == STUDIO ? 6.2f : 5.8f);
        } else if (splitActivity()) {
            LinearLayout columns = row();
            columns.setGravity(Gravity.TOP);
            LinearLayout activities = column();
            sidebar = column();
            columns.addView(scrollPane(activities), new LinearLayout.LayoutParams(0, -1, 7));
            LinearLayout.LayoutParams pane = new LinearLayout.LayoutParams(0, -1, 3);
            pane.leftMargin = dp(16);
            columns.addView(scrollPane(sidebar), pane);
            page.addView(columns, new LinearLayout.LayoutParams(-1, 0, 1));
            page = activities;
        }
        if (scene == WORD_GAME) {
            wordScreen = new WordPlayScreen(this, wordGame, page, this::saveWordReward, () -> openSceneMenu(true));
            wordScreen.show();
        } else if (scene == STUDIO) {
            studio = new SceneStudio(this, progress, studioIndex, page, studioUndo);
            studioUndo = null;
            studio.show();
        } else if (scene == GAME) {
            showGame();
        } else {
            showLesson();
        }
        if (!landscapeMode()) showMentorDock(sidebar == null ? page : sidebar, sidebar != null);

    }

    private void academyTile(String name, String detail, int artwork, int accent, Runnable action) {
        LinearLayout tile = row();
        tile.setPadding(dp(10), dp(8), dp(11), dp(8));
        tile.setBackground(background(SURFACE, 19, STROKE));
        FlashcardView image = new FlashcardView(artwork);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(image, new LinearLayout.LayoutParams(dp(landscapeMode() ? 45 : 65), dp(landscapeMode() ? 52 : 62)));
        LinearLayout words = column();
        words.setPadding(dp(10), 0, dp(5), 0);
        words.addView(text(name, landscapeMode() ? 17 : 21, accent, true));
        TextView sub = text(detail, 13, MUTED, false);
        sub.setPadding(0, dp(5), 0, 0);
        words.addView(sub);
        tile.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        if (!landscapeMode()) tile.addView(text("›", 23, accent, true));
        tile.setContentDescription(name + ". " + detail);
        makePressable(tile, action);
        page.addView(tile, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showGameChoices(int group) {
        for (LearningGame.Mode mode : GAME_MODES[group]) {
            int art;
            switch (mode) {
                case COUNT: art = 0; break;
                case COMPARE: art = 13; break;
                case PATTERN: art = 21; break;
                case ODD: art = 9; break;
                default: art = 5; break;
            }
            int level = gameLevel(mode);
            academyTile(mode.title, "Mức " + level + (landscapeMode() ? "" : " · " + mode.instruction), art, GAME_ACCENTS[group],
                () -> { gameGroup = group; startGame(mode); });
            gap(9);
        }
    }

    private String gameKey(LearningGame.Mode mode) {
        return "academy_" + mode.name().toLowerCase(java.util.Locale.ROOT);
    }

    private int gameLevel(LearningGame.Mode mode) {
        return Math.min(3, 1 + progress.getInt(gameKey(mode) + "_complete", 0));
    }

    private void startGame(LearningGame.Mode mode) {
        game = new LearningGame(mode, gameLevel(mode), new java.util.Random());
        scene = GAME;
        render();
        voice(mode.audio);
    }

    private void showGame() {
        LinearLayout board = page;
        page = toolPane(page);
        guide(game.mode.title, game.mode.instruction);
        String tally = game.mode == LearningGame.Mode.MEMORY
            ? "Tìm được " + game.pairs + "/" + game.prompt.length / 2 + " cặp"
            : "Lượt " + game.round + " / " + LearningGame.QUESTIONS;
        TextView status = text(tally + "  ·  Mức " + game.level, 14, GAME_ACCENTS[gameGroup], true);
        page.addView(status);
        gap(11);
        page = board;
        switch (game.mode) {
            case COUNT: showCountGame(); break;
            case COMPARE: showCompareGame(); break;
            case PATTERN: showPatternGame(); break;
            case ODD: showOddGame(); break;
            case MEMORY: showMemoryGame(); break;
        }
        page = toolPane(page);
        gap(10);
        if (game.solved) {
            addButton(game.isFinished() ? "Nhận thưởng  ✦" : "Lượt tiếp theo  ›", true, () -> {
                if (game.isFinished()) finishGame();
                else {
                    game.nextQuestion();
                    render();
                    voice(game.mode.audio);
                }
            });
        } else {
            if (!landscapeMode()) addButton("▶  Nghe hướng dẫn", false, () -> voice(game.mode.audio));
            if (game.mode != LearningGame.Mode.MEMORY && !game.hint) {
                gap(7);
                addButton("Gợi ý", false, () -> {
                    game.hint = true;
                    render();
                    voice("game_hint");
                });
            }
        }
        feedback();
        if (game.solved) setFeedback(game.isFinished() ? "Con hoàn thành rồi!" : "Đúng rồi, con tìm được rồi!");
        else if (game.mode == LearningGame.Mode.MEMORY && game.secondCard >= 0) {
            setFeedback("Hai hình này khác nhau. Mình thử cặp khác nhé.");
        } else if (game.hint) setFeedback("Con thử chọn ô đang sáng nhé.");
        else if (game.selected >= 0) setFeedback("Mình nhìn lại một chút nhé.");
    }

    private void showCountGame() {
        TextView counted = text("Con đã đếm " + Integer.bitCount(game.counted) + " hình", 14, MUTED, true);
        counted.setGravity(Gravity.CENTER);
        for (int first = 0; first < game.prompt.length; first += 3) {
            LinearLayout pictures = row();
            for (int offset = 0; offset < 3; offset++) {
                int index = first + offset;
                if (index >= game.prompt.length) {
                    pictures.addView(new View(this), gridCell(offset, 60));
                    continue;
                }
                FrameLayout tile = gamePicture(game.picture, game.wasCounted(index));
                tile.setContentDescription(LearningGame.name(game.picture) + " " + (index + 1));
                makePressable(tile, () -> {
                    int total = game.countPicture(index);
                    tile.setBackground(background(0xFFD3EFDF, 15, TEAL));
                    tile.setSelected(true);
                    counted.setText("Con đã đếm " + total + " hình");
                    pulse(tile);
                });
                tile.setSelected(game.wasCounted(index));
                pictures.addView(tile, gridCell(offset, 60));
            }
            page.addView(pictures);
            gap(6);
        }
        page.addView(counted);
        gap(12);
        LinearLayout answers = row();
        for (int i = 0; i < game.options.length; i++) {
            int choice = i;
            TextView option = button(String.valueOf(game.options[i]), highlighted(i), () -> answerGame(choice));
            option.setTextSize(29);
            answers.addView(option, gridCell(i, 64));
        }
        page.addView(answers);
    }

    private void showCompareGame() {
        LinearLayout sides = row();
        int rows = (Math.max(game.leftCount, game.rightCount) + 2) / 3;
        sides.addView(compareGroup(game.leftCount, rows, 0), gridCell(0, -2));
        sides.addView(compareGroup(game.rightCount, rows, 1), gridCell(1, -2));
        page.addView(sides);
    }

    private View compareGroup(int count, int rows, int option) {
        LinearLayout group = column();
        group.setGravity(Gravity.CENTER_HORIZONTAL);
        group.setPadding(dp(5), dp(9), dp(5), dp(9));
        group.setBackground(background(highlighted(option) ? 0xFFD3EFDF : SURFACE, 16, highlighted(option) ? TEAL : STROKE));
        for (int y = 0; y < rows; y++) {
            LinearLayout row = row();
            for (int x = 0; x < 3; x++) {
                View picture = y * 3 + x < count ? new FlashcardView(LearningGame.art(game.picture)) : new View(this);
                picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                row.addView(picture, new LinearLayout.LayoutParams(0, dp(40), 1));
            }
            group.addView(row);
        }
        String name = option == 0 ? "Nhóm trái" : "Nhóm phải";
        TextView caption = text(game.solved || game.hint ? count + " hình" : name, 15, INK, true);
        caption.setGravity(Gravity.CENTER);
        group.addView(caption, new LinearLayout.LayoutParams(-1, -2));
        group.setContentDescription(name + ", " + count + " hình");
        makePressable(group, () -> answerGame(option));
        return group;
    }

    private void showPatternGame() {
        LinearLayout chain = row();
        for (int i = 0; i < game.prompt.length; i++) {
            chain.addView(gamePicture(game.prompt[i], false), gridCell(i, 63));
        }
        TextView question = text("?", 27, AMBER, true);
        question.setGravity(Gravity.CENTER);
        question.setBackground(background(0xFFFFF0CE, 16, AMBER));
        chain.addView(question, gridCell(game.prompt.length, 63));
        page.addView(chain);
        gap(12);
        LinearLayout answers = row();
        for (int i = 0; i < game.options.length; i++) {
            int choice = i;
            FrameLayout tile = gamePicture(game.options[i], highlighted(i));
            makePressable(tile, () -> answerGame(choice));
            answers.addView(tile, gridCell(i, 87));
        }
        page.addView(answers);
    }

    private void showOddGame() {
        LinearLayout options = row();
        for (int i = 0; i < game.prompt.length; i++) {
            int choice = i;
            FrameLayout tile = gamePicture(game.prompt[i], highlighted(i));
            makePressable(tile, () -> answerGame(choice));
            options.addView(tile, gridCell(i, 104));
        }
        page.addView(options);
    }

    private void showMemoryGame() {
        int columns = game.prompt.length == 6 ? 3 : game.prompt.length == 4 ? 2 : 4;
        for (int row = 0; row < 2; row++) {
            LinearLayout cards = row();
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                Runnable flip = () -> {
                    LearningGame.Move move = game.flip(index);
                    if (move == LearningGame.Move.IGNORED) return;
                    render();
                    if (move == LearningGame.Move.PAIR) voice("game_pair");
                    if (move == LearningGame.Move.WRONG) voice("game_more_pair");
                };
                View tile;
                if (game.visible(index)) {
                    tile = gamePicture(game.prompt[index], game.paired[index]);
                    makePressable(tile, flip);
                    tile.setSelected(game.paired[index]);
                } else {
                    tile = button("✧", false, flip);
                    ((TextView) tile).setTextSize(32);
                    tile.setContentDescription("Lật thẻ " + (index + 1));
                }
                cards.addView(tile, gridCell(column, 90));
            }
            page.addView(cards);
            gap(8);
        }
        scheduleMemoryReset();
    }

    private void scheduleMemoryReset() {
        if (game == null || game.mode != LearningGame.Mode.MEMORY || game.secondCard < 0) return;
        int revision = pageRevision;
        handler.postDelayed(() -> {
            if (!active || scene != GAME || pageRevision != revision || game.secondCard < 0) return;
            if (player != null) {
                scheduleMemoryReset();
                return;
            }
            game.hideMismatch();
            render();
        }, 1550);
    }

    private LinearLayout.LayoutParams gridCell(int column, int height) {
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, height < 0 ? height : dp(height), 1);
        if (column > 0) size.leftMargin = dp(6);
        return size;
    }

    private FrameLayout gamePicture(int item, boolean highlight) {
        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(background(highlight ? 0xFFD3EFDF : SURFACE, 15, highlight ? TEAL : STROKE));
        FlashcardView image = new FlashcardView(LearningGame.art(item));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
        tile.setContentDescription(LearningGame.name(item));
        return tile;
    }

    private boolean highlighted(int option) {
        return (game.solved || game.hint) && option == game.answer;
    }

    private void answerGame(int option) {
        LearningGame.Move move = game.answer(option);
        if (move == LearningGame.Move.IGNORED) return;
        render();
        if (move == LearningGame.Move.RIGHT) voice("game_correct");
        else voice(game.hint ? "game_hint" : "game_try");
    }

    private void finishGame() {
        String key = gameKey(game.mode);
        int completed = progress.getInt(key + "_complete", 0);
        FamilyProfiles.record(progress, progress.edit().putInt(key + "_complete", completed + 1), false).apply();
        scene = GAME_DONE;
        render();
        voice("game_complete");
    }

    private void showGameDone() {
        worldHeader("Toán & tư duy", this::openThinkMenu);
        if (landscapeMode()) landscapePanes(5.6f);
        FrameLayout image = sceneCard(landscapeMode() ? landscapeHeight(110,.54f,250) : 165);
        addCharacter(image, dp(140), dp(153), Gravity.END | Gravity.BOTTOM, 6);
        TextView stars = text("✦", 76, AMBER, true);
        stars.setGravity(Gravity.CENTER);
        image.addView(stars, new FrameLayout.LayoutParams(dp(113), -1, Gravity.START));
        pulse(stars);
        gap(12);
        page = toolPane(page);
        title("Con hoàn thành rồi!", game.mode == LearningGame.Mode.MEMORY
            ? "Con đã tìm đủ " + game.pairs + " cặp hình."
            : "Con tự tìm được " + game.rightWithoutHelp + "/" + LearningGame.QUESTIONS + " đáp án.");
        addButton("▶  Chơi thêm", true, () -> startGame(game.mode));

    }

    private void showHome() {
        LinearLayout top = row();
        top.addView(backControl("Học & Chơi", this::openHub), new LinearLayout.LayoutParams(-2, -2));
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        top.addView(button("Phụ huynh", false, this::showParents), new LinearLayout.LayoutParams(-2, -2));
        page.addView(top);
        gap(10);

        if (landscapeMode()) {
            landscapePanes(6.1f);
            showAlphabet();
            page = landscapeTools;
        }
        if (landscapeMode() && chapterMapOpen) {
            addButton("‹  " + CHAPTERS[chapterIndex], false, () -> { chapterMapOpen = false; render(); });
            chapterMap(chapterIndex);
            return;
        }
        int currentChapter = chapterIndex;
        if (landscapeMode()) {
            LinearLayout chapterBar = row();
            TextView chapter = text(CHAPTERS[currentChapter], 21, INK, true);
            chapterBar.addView(chapter, new LinearLayout.LayoutParams(0, -2, 1));
            TextView map = button("Bản đồ ›", false, () -> { chapterMapOpen = true; render(); });
            chapterBar.addView(map);
            page.addView(chapterBar);
            gap(5);
            addButton("▶  Nghe chuyện", false, () -> voice("chapter_story" + currentChapter));
        } else {
            int bannerHeight = Math.max(174, Math.round(150 * getResources().getConfiguration().fontScale));
            FrameLayout hero = sceneCard(bannerHeight);
            LinearLayout copy = column();
            copy.setGravity(Gravity.CENTER_VERTICAL);
            copy.setPadding(dp(16), 0, dp(4), 0);
            copy.addView(text("Công chúa Tuyết", 12, AMBER, true));
            TextView chapterTitle = text(CHAPTERS[currentChapter], 33, INK, true);
            chapterTitle.setPadding(0, dp(8), 0, dp(11));
            copy.addView(chapterTitle);
            TextView story = button("▶  Nghe chuyện", false, () -> voice("chapter_story" + currentChapter));
            story.setTextSize(14);
            copy.addView(story, new LinearLayout.LayoutParams(-2, -2));
            int width = getResources().getConfiguration().screenWidthDp - 40;
            int portraitWidth = Math.min(161, Math.round(width * (getResources().getConfiguration().fontScale > 1.3f ? .31f : .41f)));
            addCharacter(hero, dp(portraitWidth), dp(159), Gravity.END | Gravity.BOTTOM, 5);
            hero.addView(copy, new FrameLayout.LayoutParams(dp(Math.max(120, width - portraitWidth - 7)), -1, Gravity.START));
        }
        gap(10);

        int resume = progress.getInt("resume_lesson", -1);
        if (resume >= 0 && resume < Lesson.ALL.length) {
            addButton("▶  Chơi tiếp chữ " + Lesson.ALL[resume].letter, true, () -> startLesson(resume,
                progress.getInt("resume_scene", LISTEN), progress.getInt("resume_example", 0)));
        } else {
            int next = nextLesson(-1);
            if (next >= 0) addButton("▶  Học chữ " + Lesson.ALL[next].letter.toUpperCase(Lesson.VI), true,
                () -> startLesson(next, LISTEN, -1));
            else addButton("↻  Ôn lại từ chữ A", true, this::startReview);
        }

        gap(17);
        if (!landscapeMode()) showAlphabet();
        gap(12);
        if (completedCount() > 0 && completedCount() < Lesson.ALL.length) {
            addButton("↻  Ôn cùng Công chúa", false, this::startReview);
            gap(12);
        }
        if (!landscapeMode()) chapterMap(currentChapter);
    }

    private void chapterMap(int currentChapter) {
        LinearLayout chapterHeading = row();
        chapterHeading.addView(text("Bản đồ", 22, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        chapterHeading.addView(text(completedCount() + " / " + Lesson.ALL.length + " đèn", 13, AMBER, true));
        page.addView(chapterHeading);
        gap(8);
        for (int first = 0; first < CHAPTERS.length; first += 2) {
            LinearLayout pair = row();
            for (int index = first; index < first + 2; index++) {
                int selected = index;
                TextView destination = button(CHAPTER_ICONS[index] + "  " + CHAPTERS[index],
                    index == currentChapter, () -> {
                        chapterIndex = selected;
                        chapterMapOpen = false;
                        render();
                        voice("chapter_story" + selected);
                    });
                destination.setTextSize(14);
                destination.setSelected(index == currentChapter);
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
                if (index > first) size.leftMargin = dp(8);
                pair.addView(destination, size);
            }
            page.addView(pair);
            gap(6);
        }

        gap(4);
        showTrail(currentChapter, false);
    }

    private void showAlphabet() {
        LinearLayout heading = row();
        heading.addView(text("Bảng chữ cái", 23, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text(completedCount() + "/" + Lesson.ALL.length + " chữ", 13, TEAL, true));
        page.addView(heading);
        gap(9);
        android.content.res.Configuration config = getResources().getConfiguration();
        int columns = Math.max(2, Math.min(6,
            (config.screenWidthDp - 40) / Math.round(77 * Math.max(1, config.fontScale))));
        int from = 0, end = Lesson.ALL.length;
        if (landscapeMode()) {
            columns = Math.max(2, Math.min(7, (int)((config.screenWidthDp - 44) * .60f / (68 * Math.max(1, config.fontScale)))));
            int rows = Math.max(1, Math.min(4, (config.screenHeightDp - 166) / 82));
            int perSheet = rows * columns;
            int sheets = (end + perSheet - 1) / perSheet;
            alphabetSheet = Math.max(0, Math.min(sheets - 1, alphabetSheet));
            from = alphabetSheet * perSheet;
            end = Math.min(end, from + perSheet);
            LinearLayout pager = row();
            TextView prev = button("‹", false, () -> { alphabetSheet--; render(); });
            TextView next = button("›", false, () -> { alphabetSheet++; render(); });
            prev.setContentDescription("Trang chữ trước");
            next.setContentDescription("Trang chữ tiếp theo");
            prev.setEnabled(alphabetSheet > 0);
            next.setEnabled(alphabetSheet + 1 < sheets);
            pager.addView(prev, new LinearLayout.LayoutParams(dp(48),-2));
            TextView range = text(Lesson.ALL[Lesson.atAlphabetPosition(from)].letter.toUpperCase(Lesson.VI)
                + "  –  " + Lesson.ALL[Lesson.atAlphabetPosition(end-1)].letter.toUpperCase(Lesson.VI), 18, TEAL, true);
            range.setGravity(Gravity.CENTER);
            pager.addView(range, new LinearLayout.LayoutParams(0,-2,1));
            pager.addView(next, new LinearLayout.LayoutParams(dp(48),-2));
            page.addView(pager);
            gap(5);
        }
        for (int first = from; first < end; first += columns) {
            LinearLayout group = row();
            for (int column = 0; column < columns; column++) {
                int position = first + column;
                LinearLayout.LayoutParams space = new LinearLayout.LayoutParams(0, -2, 1);
                if (column > 0) space.leftMargin = dp(7);
                if (position >= end) {
                    group.addView(new View(this), space);
                    continue;
                }
                int index = Lesson.atAlphabetPosition(position);
                Lesson entry = Lesson.ALL[index];
                Lesson.Example example = entry.examples[0];
                boolean done = progress.getBoolean(entry.key + "_done", false);
                LinearLayout tile = column();
                tile.setGravity(Gravity.CENTER_HORIZONTAL);
                tile.setPadding(0, dp(5), 0, dp(3));
                tile.setBackground(background(done ? 0xFFE7F5E1 : SURFACE, 16, done ? 0xFF98BE91 : STROKE));
                TextView glyph = text(entry.letter.toUpperCase(Lesson.VI) + " " + entry.letter, 24,
                    done ? AMBER : TEAL, true);
                glyph.setGravity(Gravity.CENTER);
                glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                tile.addView(glyph, new LinearLayout.LayoutParams(-1, -2));
                View picture = new FlashcardView(example.art);
                picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                tile.addView(picture, new LinearLayout.LayoutParams(-1, dp(landscapeMode() ? 37 : 53)));
                makePressable(tile, () -> startLesson(index, LISTEN, -1));
                tile.setContentDescription("Học chữ " + entry.letter.toUpperCase(Lesson.VI) + ", " + example.phrase()
                    + (done ? ", đã hoàn thành" : ""));
                group.addView(tile, space);
            }
            page.addView(group);
            gap(7);
        }
    }

    private FrameLayout sceneCard(int height) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(background(SURFACE, 24, STROKE));
        frame.setClipToOutline(true);
        frame.addView(landscape(chapterIndex % DaylightArt.SCENES.length), new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFFF3FAFF, 0xDAE8F7FC, 0x03E7F7FF}));
        frame.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        page.addView(frame, new LinearLayout.LayoutParams(-1, dp(height)));
        return frame;
    }

    private void addCharacter(FrameLayout frame, int width, int height, int gravity, int margin) {
        FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(width, height, gravity);
        size.setMargins(dp(margin), dp(margin), dp(margin), dp(margin));
        frame.addView(mentorPortrait(), size);
    }

    private DaylightArt.Princess character() {
        return new DaylightArt.Princess(this, this::motionEnabled);
    }

    private FrameLayout mentorPortrait() {
        FrameLayout frame = new FrameLayout(this);
        DaylightArt.Princess portrait = character();
        princess = portrait;
        frame.addView(portrait, new FrameLayout.LayoutParams(-1, -1));
        frame.setContentDescription("Công chúa Tuyết. Chạm để nghe hướng dẫn.");
        makePressable(frame, () -> {
            answerMentor();
            if (!motionEnabled()) return;
            pulse(portrait);
            SparkleBurst burst = new SparkleBurst();
            frame.addView(burst, new FrameLayout.LayoutParams(-1, -1));
            handler.postDelayed(() -> frame.removeView(burst), 760);
        });
        return frame;
    }

    private void answerMentor() {
        if (scene == WORD_GAME && wordScreen != null) {
            wordScreen.speak(false);
        } else if (scene == STUDIO && studio != null) {
            studio.speak();
        } else if (scene == LISTEN) {
            playListenIntro();
        } else if (scene == FIND || scene == MATCH) {
            playChallenge();
        } else if (scene == DONE) {
            cancelStudyAdvance();
            if (nextPaused) voice("study_stay");
            else announceNext(false);
        } else if (scene == GAME && game != null) {
            voice(game.mode.audio);
        } else {
            voice(scene == GAME_DONE || scene == DONE ? "game_complete" : scene == HUB ? "world_welcome" : "mentor_home");
        }
    }

    private void showMentorDock(LinearLayout container, boolean side) {
        if (!side) container.addView(new View(this), new LinearLayout.LayoutParams(1, dp(13), 1));
        LinearLayout panel = side ? column() : row();
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(8), dp(7), dp(10), dp(9));
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFFE4F6FB, 0xFFF2E8FD});
        gradient.setCornerRadius(dp(22));
        gradient.setStroke(dp(1), STROKE);
        panel.setBackground(gradient);
        FrameLayout character = mentorPortrait();
        panel.addView(character, new LinearLayout.LayoutParams(dp(side ? Math.min(151, getResources().getConfiguration().screenWidthDp / 5) : 111), dp(side ? Math.min(168, getResources().getConfiguration().screenHeightDp / 3) : 116)));
        if (scene == DONE || scene == GAME_DONE) mentorPose(DaylightArt.Princess.CHEER, 1500);
        if (scene == WORD_GAME && wordGame != null && wordGame.tutorial())
            mentorPose(DaylightArt.Princess.POINT, 1500);
        if (scene == WORD_GAME && wordGame != null && wordGame.round >= WordPlay.QUESTIONS)
            mentorPose(DaylightArt.Princess.CHEER, 1500);
        LinearLayout words = column();
        words.setPadding(dp(side ? 0 : 7), 0, 0, dp(side ? 5 : 0));
        TextView name = text("Công chúa Tuyết", 19, PLUM, true);
        if (side) name.setGravity(Gravity.CENTER);
        words.addView(name);
        TextView action = button("▶  Nghe hướng dẫn", false, this::answerMentor);
        action.setTextSize(14);
        LinearLayout.LayoutParams control = new LinearLayout.LayoutParams(-1, -2);
        control.topMargin = dp(6);
        words.addView(action, control);
        panel.addView(words, side ? new LinearLayout.LayoutParams(-1, -2) : new LinearLayout.LayoutParams(0, -2, 1));
        container.addView(panel);
    }

    void mentorPose(int pose, long duration) {
        if (princess != null) princess.pose(pose, duration);
    }

    private void showTrail(int chapter, boolean animated) {
        LinearLayout panel = column();
        panel.setPadding(dp(12), dp(9), dp(12), dp(8));
        panel.setBackground(background(0xFFFFF4DD, 18, STROKE));
        int lit = chapterCompleted(chapter);
        TextView tally = text(CHAPTERS[chapter] + "   ·   " + lit + " / " + chapterLength(chapter) + " đèn", 13,
            CHAPTER_ACCENT[chapter], true);
        panel.addView(tally);
        panel.addView(new LampTrail(lit, chapterLength(chapter), CHAPTER_ACCENT[chapter], animated),
            new LinearLayout.LayoutParams(-1, dp(animated ? 71 : 44)));
        page.addView(panel);
    }

    private void startReview() {
        int choice = -1;
        for (int position = 0; position < Lesson.ALL.length; position++) {
            int index = Lesson.atAlphabetPosition(position);
            Lesson item = Lesson.ALL[index];
            if (!progress.getBoolean(item.key + "_done", false)) continue;
            choice = index;
            break;
        }
        if (choice >= 0) startLesson(choice, LISTEN, -1);
    }

    private int chapterOf(int index) {
        int position = Lesson.alphabetPosition(index);
        for (int chapter = 0; chapter < CHAPTERS.length; chapter++) {
            if (position < CHAPTER_START[chapter + 1]) return chapter;
        }
        return CHAPTERS.length - 1;
    }

    private int chapterLength(int chapter) {
        return CHAPTER_START[chapter + 1] - CHAPTER_START[chapter];
    }

    private int chapterCompleted(int chapter) {
        int count = 0;
        for (int position = CHAPTER_START[chapter]; position < CHAPTER_START[chapter + 1]; position++) {
            Lesson item = Lesson.ALL[Lesson.atAlphabetPosition(position)];
            if (progress.getBoolean(item.key + "_done", false)) count++;
        }
        return count;
    }

    private void lessonNavigation() {
        LinearLayout top = row();
        top.addView(backControl("Bảng chữ", () -> home(false)), new LinearLayout.LayoutParams(-2, -2));
        TextView chapter = text(CHAPTERS[chapterIndex], 16, CHAPTER_ACCENT[chapterIndex], true);
        chapter.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(chapter, new LinearLayout.LayoutParams(0, -2, 1));
        if (landscapeMode()) {
            chapter.setPadding(0, 0, dp(12), 0);
            addLandscapeGuide(top);
        }
        page.addView(top);
        gap(10);

    }

    private void showLesson() {
        LinearLayout board = page;
        page = toolPane(page);
        showLessonSteps();
        gap(splitActivity() ? 8 : 18);

        switch (scene) {
            case LISTEN:
                String letter = lesson().letter.toUpperCase(Lesson.VI);
                String[] transitionVisit = {
                    "Bây giờ mình làm quen với chữ " + letter + " nhé!",
                    "Tiếp theo, cùng khám phá chữ " + letter + " nào con!",
                    "Giờ thì chúng mình chuyển sang chữ " + letter + " nha!",
                    "Chữ mới tiếp theo của con là chữ " + letter + " đấy!",
                    "Sẵn sàng chưa nào? Mình cùng học chữ " + letter + " nhé!"
                };
                String[] directions = {
                    "Con nhìn chữ, lắng nghe rồi đọc theo cô nhé.",
                    "Tai lắng nghe, mắt nhìn chữ và mình đọc thật chậm nào.",
                    "Cô đọc ba lần, sau đó con cùng gọi tên hình nhé.",
                    "Con sẵn sàng chưa? Mình phát âm rõ từng tiếng nào.",
                    "Nghe cô trước, rồi con sẽ mở khóa trò chơi nhé."
                };
                String heading = introState == 2 ? "Mình cùng ôn lại chữ " + letter + " cho thật nhớ nhé!"
                    : introState == 0 ? "Chào con! Hôm nay mình bắt đầu với chữ " + letter + " nhé!"
                    : transitionVisit[promptVariant];
                lessonHeading(heading, directions[promptVariant]);
                page = board;
                letterCard();
                page = toolPane(page);
                studyPlay = addButton(listenComplete ? "▶  Bắt đầu chơi" : "🔒  Nghe cô đọc trước", true, () -> {
                    if (listenComplete) enter(FIND);
                    else {
                        if (sampleStatus != null) sampleStatus.setText("Con nghe đủ ba lượt trước nhé");
                        pulse(sampleGlyph);
                    }
                });
                studyPlay.setTextSize(21);
                studyPlay.setEnabled(listenComplete);
                studyPlay.setAlpha(listenComplete ? 1f : .55f);
                studyPlay.setContentDescription(listenComplete
                    ? "Bắt đầu hai trò chơi với chữ " + lesson().letter
                    : "Trò chơi sẽ mở sau khi con nghe xong bài mẫu");
                gap(12);
                TextView lockedNext = text("🔒  Bài tiếp theo mở sau khi con hoàn thành", 14, MUTED, true);
                lockedNext.setGravity(Gravity.CENTER);
                page.addView(lockedNext);
                break;
            case FIND:
            case MATCH:
                page = board;
                showChallenge();
                break;
            default:
                throw new IllegalStateException("Unknown lesson step " + scene);
        }
    }

    private void lessonHeading(String heading, String direction) {
        TextView name = text(heading, landscapeMode() ? 21 : 27, INK, true);
        page.addView(name);
        TextView caption = text(direction, 16, MUTED, false);
        caption.setPadding(0, dp(4), 0, 0);
        page.addView(caption);
        gap(13);
    }

    private void showLessonSteps() {
        String[] names = {"Nghe mẫu", "Tìm chữ", "Nghe và ghép"};
        int[] order = {LISTEN, FIND, MATCH};
        int selected = 0;
        for (int i = 0; i < order.length; i++) if (order[i] == scene) selected = i;
        LinearLayout label = row();
        label.addView(text(names[selected], 15, TEAL, true), new LinearLayout.LayoutParams(0, -2, 1));
        label.addView(text("Bước " + (selected + 1) + " / " + order.length, 13, MUTED, true));
        page.addView(label);
        gap(6);
        LinearLayout steps = row();
        steps.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        for (int i = 0; i < order.length; i++) {
            View step = new View(this);
            step.setBackground(background(i <= selected ? TEAL : STROKE, 4, 0));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, dp(5), 1);
            if (i > 0) size.leftMargin = dp(5);
            steps.addView(step, size);
        }
        page.addView(steps);
    }

    private void guide(String heading, String instruction) {
        LinearLayout bubble = row();
        bubble.setPadding(dp(8), dp(7), dp(11), dp(7));
        bubble.setBackground(background(0xFFE6F5ED, 19, STROKE));
        View portrait = character();
        bubble.addView(portrait, new LinearLayout.LayoutParams(dp(58), dp(60)));
        LinearLayout words = column();
        words.setPadding(dp(11), 0, 0, 0);
        words.addView(text(heading, 22, INK, true));
        TextView detail = text(instruction, 14, MUTED, false);
        detail.setPadding(0, dp(4), 0, 0);
        words.addView(detail);
        bubble.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(bubble);
        gap(11);
    }

    private void playListenIntro() {
        cancelListenAdvance();
        cancelHint();
        listenComplete = false;
        if (studyPlay != null) {
            studyPlay.setEnabled(false);
            studyPlay.setAlpha(.55f);
            studyPlay.setText("🔒  Nghe cô đọc trước");
        }
        int version = listenVersion;
        if (skipIntroOnce) {
            skipIntroOnce = false;
            if (sampleStatus != null) sampleStatus.setText("Cùng nghe âm và xem hình nhé");
            scheduleSample(version, 320, () -> playSample(version));
            return;
        }
        if (sampleStatus != null) sampleStatus.setText(introState == 2
            ? "Cùng ôn lại với cô" : introState == 0 ? "Bài đầu tiên hôm nay" : "Khám phá chữ tiếp theo");
        String intro = introState == 2 ? lesson().key + "_review"
            : introState == 1 ? lesson().key + "_next" + promptVariant : lesson().key + "_intro";
        voice(intro, .94f, () -> scheduleSample(version, 420, () -> playSample(version)));
    }

    private void playSample() {
        cancelListenAdvance();
        cancelHint();
        playSample(listenVersion);
    }

    private void playSample(int version) {
        if (!active || scene != LISTEN || version != listenVersion) return;
        if (sampleStatus != null) sampleStatus.setText("Nghe âm 3 lần rồi xem hình");
        if (sampleGlyph != null) pulse(sampleGlyph);
        int revision = pageRevision;
        handler.postDelayed(() -> {
            if (active && scene == LISTEN && pageRevision == revision && sampleGlyph != null) pulse(sampleGlyph);
        }, 1050);
        handler.postDelayed(() -> {
            if (active && scene == LISTEN && pageRevision == revision && sampleGlyph != null) pulse(sampleGlyph);
        }, 2100);
        handler.postDelayed(() -> {
            if (active && scene == LISTEN && pageRevision == revision && samplePicture != null) pulse(samplePicture);
        }, 3150);
        voice(lesson().key + "_sample" + exampleIndex, .90f, () -> {
            if (version != listenVersion || scene != LISTEN) return;
            listenComplete = true;
            if (sampleStatus != null) sampleStatus.setText("Đã mở khóa trò chơi!");
            if (studyPlay != null) {
                studyPlay.setEnabled(true);
                studyPlay.setAlpha(1f);
                studyPlay.setText("▶  Bắt đầu chơi");
                studyPlay.setContentDescription("Bắt đầu hai trò chơi với chữ " + lesson().letter);
            }
            voice("study_play");
            if (studyPlay != null) pulse(studyPlay);
            mentorPose(DaylightArt.Princess.POINT, 2200);
        });
    }

    private void scheduleSample(int version, long delay, Runnable callback) {
        if (version != listenVersion || scene != LISTEN || !active) return;
        listenAdvance = () -> {
            listenAdvance = null;
            if (version == listenVersion && scene == LISTEN && active) callback.run();
        };
        handler.postDelayed(listenAdvance, delay);
    }

    private void cancelListenAdvance() {
        listenVersion++;
        if (listenAdvance != null) handler.removeCallbacks(listenAdvance);
        listenAdvance = null;
    }

    private void letterCard() {
        Lesson.Example example = lesson().examples[exampleIndex];
        LinearLayout card = column();
        card.setPadding(dp(13), dp(12), dp(13), dp(13));
        GradientDrawable frost = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFFF0FAFF, 0xFFEDE6FA});
        frost.setCornerRadius(dp(25));
        frost.setStroke(dp(1), STROKE);
        card.setBackground(frost);
        LinearLayout visuals = row();
        TextView glyph = text(lesson().letter.toUpperCase(Lesson.VI) + " " + lesson().letter,
            55, CHAPTER_ACCENT[chapterIndex], true);
        glyph.setGravity(Gravity.CENTER);
        visuals.addView(glyph, new LinearLayout.LayoutParams(0, -1, 1));
        View picture = examplePicture(example, 78);
        visuals.addView(picture, new LinearLayout.LayoutParams(0, -1, 1.12f));
        int screen = getResources().getConfiguration().screenHeightDp;
        int visualHeight = landscapeMode() ? landscapeHeight(65,.25f,170) : Math.max(147, Math.min(222, (int) (screen * .285)));
        card.addView(visuals, new LinearLayout.LayoutParams(-1, dp(visualHeight)));

        TextView phrase = text("", 22, INK, true);
        phrase.setText(highlightedExample(example));
        phrase.setGravity(Gravity.CENTER);
        phrase.setPadding(dp(6), dp(5), dp(6), dp(5));
        phrase.setMinHeight(dp(41));
        card.addView(phrase, new LinearLayout.LayoutParams(-1, -2));
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
        gap(10);
        sampleGlyph = glyph;
        samplePicture = picture;
        sampleStatus = text("Ba lượt đọc chữ, rồi đến tên hình", 14, MUTED, true);
        sampleStatus.setGravity(Gravity.CENTER);
        sampleStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        page.addView(sampleStatus, new LinearLayout.LayoutParams(-1, -2));
        Runnable readLetter = () -> playSample();
        Runnable readPicture = () -> replayStudyAudio(wordAudio(), picture);
        glyph.setContentDescription("Nghe chữ " + lesson().letter.toUpperCase(Lesson.VI));
        picture.setContentDescription("Nghe " + example.phrase());
        makePressable(glyph, readLetter);
        makePressable(picture, readPicture);
        makePressable(phrase, readPicture);
        LinearLayout playback = row();
        TextView letterButton = button("▶  Nghe lại mẫu", false, readLetter);
        TextView exampleButton = button("▶  Nghe tên hình", false, readPicture);
        playback.addView(letterButton, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, -2, 1);
        second.leftMargin = dp(8);
        playback.addView(exampleButton, second);
        page.addView(playback);
        gap(7);
    }

    private CharSequence highlightedExample(Lesson.Example example) {
        android.text.SpannableString phrase = new android.text.SpannableString(example.phrase());
        int offset = example.before.isEmpty() ? 0 : example.before.length() + 1;
        for (int start = 0; start < example.word.length();) {
            int end = example.word.offsetByCodePoints(start, 1);
            if (lesson().isTarget(example.word.substring(start, end))) {
                phrase.setSpan(new android.text.style.ForegroundColorSpan(AMBER), offset + start, offset + end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                phrase.setSpan(new android.text.style.UnderlineSpan(), offset + start, offset + end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            start = end;
        }
        return phrase;
    }

    private void replayStudyAudio(String clip, View target) {
        cancelListenAdvance();
        pulse(target);
        cancelHint();
        if (sampleStatus != null) sampleStatus.setText("Nghe tên hình");
        voice(clip, .86f, null);
    }

    View examplePicture(Lesson.Example example, int emojiSize) {
        if (example.art >= 0) return new FlashcardView(example.art);
        TextView symbol = text(example.picture, emojiSize, INK, false);
        symbol.setGravity(Gravity.CENTER);
        return symbol;
    }

    private LinearLayout challengeRow;
    private TextView challengeNext;
    private void showChallenge() {
        LinearLayout board = page;
        page = toolPane(page);
        if (scene == FIND) guide("Tìm chữ cái", "Con chọn chữ " + lesson().letter + " nhé.");
        else guide("Nghe rồi chọn", "Chạm vào chữ con vừa nghe.");
        page = board;
        LinearLayout answers = null;
        if (splitActivity() && scene == FIND) {
            answers = row();
            page.addView(answers);
        }
        if (scene == FIND) {
            Lesson.Example sample = lesson().examples[exampleIndex];
            View picture = examplePicture(sample, 55);
            picture.setContentDescription("Nghe " + sample.phrase());
            makePressable(picture, () -> {
                if (challengeFound) continueAnswer();
                else voiceSequence(wordAudio(), lesson().key + "_pick");
            });
            if (answers == null) page.addView(picture, new LinearLayout.LayoutParams(-1, dp(90)));
            else answers.addView(picture, new LinearLayout.LayoutParams(0, dp(96), 1));

        }
        if (choices == null) {
            int neighbor = lesson().neighborIndex();
            choices = new ArrayList<>();
            choices.add(lessonIndex);
            choices.add(neighbor);
            if (exampleIndex == 1) {
                int extra = (lessonIndex + 2) % Lesson.ALL.length;
                if (extra == neighbor) extra = (extra + 1) % Lesson.ALL.length;
                choices.add(extra);
            }
            Collections.shuffle(choices);
        }
        challengeRow = row();
        if (answers == null) page.addView(challengeRow);
        else answers.addView(challengeRow, new LinearLayout.LayoutParams(0, -2, 3));
        paintChoices();
        page = toolPane(page);
        gap(12);
        addButton("▶  Nghe lại", false, () -> { playChallenge(); armHint(); });
        gap(8);
        challengeNext = scene == FIND
            ? addButton("Ghép âm  ›", true, () -> enter(MATCH))
            : addButton("Thắp đèn  ✦", true, this::finishLesson);
        challengeNext.setVisibility(challengeFound ? View.VISIBLE : View.GONE);
        feedback();
    }

    private void playChallenge() {
        if (challengeFound) {
            continueAnswer();
            return;
        }
        if (scene == FIND) voice(lesson().key + "_pick");
        else voiceSequence("study_listen_choose", lesson().key + "_letter");
    }

    private void continueAnswer() {
        int stage = scene;
        scheduleStudy(950, () -> {
            if (!challengeFound || scene != stage) return;
            if (stage == FIND) enter(MATCH);
            else if (stage == MATCH) finishLesson();
        });
    }

    private void paintChoices() {
        challengeRow.removeAllViews();
        for (int candidate : choices) {
            boolean target = candidate == lessonIndex;
            String glyph = Lesson.ALL[candidate].letter;
            if (exampleIndex == 1) glyph = glyph.toUpperCase(Lesson.VI);
            TextView tile = button(glyph, target && (reveal || challengeFound), () -> {
                if (challengeFound) return;
                if (target) {
                    challengeFound = true;
                    SharedPreferences.Editor result = progress.edit();
                    if (scene == FIND) result.putBoolean(lesson().key + (reveal ? "_assisted" : "_found"), true);
                    else result.putBoolean(lesson().key + (reveal ? "_match_assisted" : "_matched"), true);
                    result.apply();
                    challengeNext.setVisibility(View.VISIBLE);
                    voice("found", 1f, this::continueAnswer);
                    setFeedback("Con tìm thấy chữ " + lesson().letter + " rồi!");
                    cancelHint();
                } else {
                    errors++;
                    reveal = true;
                    voice("again");
                    setFeedback("Con thử chữ đang sáng nhé.");
                    if (errors >= 2 && choices.size() > 2) choices.remove(Integer.valueOf(candidate));
                    armHint();
                }
                paintChoices();
            });
            tile.setTextSize(38);
            tile.setContentDescription("Chữ " + glyph);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, dp(landscapeMode() ? landscapeHeight(74,.34f,155) : 137), 1);
            if (challengeRow.getChildCount() > 0) size.leftMargin = dp(8);
            challengeRow.addView(tile, size);
            if (target && challengeFound) pulse(tile);
        }
    }

    private void finishLesson() {
        FamilyProfiles.record(progress, progress.edit().putBoolean(lesson().key + "_done", true)
            .putLong(lesson().key + "_practiced", System.currentTimeMillis())
            .putInt("last_lesson", lessonIndex).remove("resume_lesson"), true).apply();
        enter(DONE);
    }

    private void showDone() {
        page.addView(backControl("Bảng chữ", () -> home(false)),
            new LinearLayout.LayoutParams(-2, -2));
        gap(10);
        int count = completedCount();
        if (landscapeMode()) landscapePanes(5.7f);
        FrameLayout scene = sceneCard(landscapeMode() ? landscapeHeight(105,.45f,205) : 205);
        addCharacter(scene, dp(144), dp(186), Gravity.END | Gravity.BOTTOM, 5);
        boolean umbrella = lesson().key.equals("oo");
        TextView star = text(umbrella ? "☂" : "✦", 92, umbrella ? TEAL : AMBER, true);
        star.setGravity(Gravity.CENTER);
        scene.addView(star, new FrameLayout.LayoutParams(dp(125), -1, Gravity.START));
        star.setContentDescription(umbrella ? "Chiếc ô che ngọn đèn" : "Đèn sao phát sáng");
        if (umbrella) {
            TextView lantern = text("✦", 33, AMBER, true);
            lantern.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(dp(125), dp(66),
                Gravity.START | Gravity.BOTTOM);
            scene.addView(lantern, position);
            pulse(lantern);
        }
        if (motionEnabled()) {
            AnimatorSet dance = new AnimatorSet();
            dance.playTogether(ObjectAnimator.ofFloat(star, View.ROTATION, -35, 12, 0),
                ObjectAnimator.ofFloat(star, View.SCALE_X, .5f, 1.2f, 1f),
                ObjectAnimator.ofFloat(star, View.SCALE_Y, .5f, 1.2f, 1f),
                ObjectAnimator.ofFloat(star, View.ALPHA, .25f, 1f));
            dance.setDuration(900);
            dance.setInterpolator(new DecelerateInterpolator());
            dance.start();
        }
        gap(12);
        title(count == Lesson.ALL.length ? "Vương quốc đã sáng!" : "Đèn đã sáng!",
            "Con đã thắp sáng " + count + " / " + Lesson.ALL.length + " ngọn đèn.");
        showTrail(chapterIndex, true);
        gap(11);
        page = toolPane(page);
        showNextSuggestion(true);
        gap(8);
        addButton("↻  Ôn chữ " + lesson().letter, false, () -> startLesson(lessonIndex, LISTEN, 1 - exampleIndex));
    }

    private void showNextSuggestion(boolean completed) {
        int next = nextLesson(lessonIndex);
        if (next < 0) return;
        Lesson item = Lesson.ALL[next];
        Lesson.Example example = item.examples[0];
        LinearLayout suggestion = row();
        suggestion.setPadding(dp(9), dp(5), dp(12), dp(5));
        suggestion.setBackground(background(0xFFFFF1DC, 20, 0xFFE3C996));
        View image = examplePicture(example, 43);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        suggestion.addView(image, new LinearLayout.LayoutParams(dp(66), dp(62)));
        LinearLayout label = column();
        label.setPadding(dp(7), 0, dp(6), 0);
        label.addView(text("Bài tiếp theo · " + item.letter.toUpperCase(Lesson.VI), 16, PLUM, true));
        label.addView(text(example.phrase(), 15, MUTED, false));
        suggestion.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        suggestion.addView(text("▶", 25, PLUM, true));
        suggestion.setContentDescription("Học tiếp chữ " + item.letter.toUpperCase(Lesson.VI) + ", " + example.phrase());
        makePressable(suggestion, () -> startLesson(next, LISTEN, 0));
        page.addView(suggestion);

        if (completed && family.studyDelay(family.active()) > 0) {
            nextCountdown = text(nextPaused ? "Đã dừng tự chuyển" : "Cô giới thiệu bài tiếp theo…", 16, PLUM, true);
            nextCountdown.setGravity(Gravity.CENTER);
            nextCountdown.setPadding(0, dp(6), 0, 0);
            page.addView(nextCountdown);
            nextPauseButton = addButton(nextPaused ? "▶  Bật lại tự chuyển" : "Ⅱ  Dừng tự chuyển", false, () -> {
                nextPaused = !nextPaused;
                cancelStudyAdvance();
                if (nextPaused) {
                    nextCountdown.setText("Đã dừng tự chuyển");
                    nextPauseButton.setText("▶  Bật lại tự chuyển");
                    voice("study_stay");
                } else {
                    nextPauseButton.setText("Ⅱ  Dừng tự chuyển");
                    nextCountdown.setText("Cô giới thiệu bài tiếp theo…");
                    announceNext(false);
                }
            });
        }
    }

    private void announceNext(boolean reward) {
        int next = nextLesson(lessonIndex);
        int revision = pageRevision;
        Runnable introduce = () -> {
            if (!active || revision != pageRevision || nextPaused || next < 0) return;
            if (announcedNextIndex == next) {
                tickNext(next, family.studyDelay(family.active()));
                return;
            }
            voice(Lesson.ALL[next].key + "_bridge", .94f, () -> {
                if (!active || revision != pageRevision || nextPaused) return;
                announcedNextIndex = next;
                tickNext(next, family.studyDelay(family.active()));
            });
        };
        if (reward) {
            String clip = next < 0 ? "finished" : chapterCompleted(chapterIndex) == chapterLength(chapterIndex)
                ? "chapter_done" : "celebrate";
            voice(clip, 1f, introduce);
        } else introduce.run();
    }

    private void tickNext(int next, int seconds) {
        if (nextCountdown == null || nextPaused) return;
        if (seconds <= 0) {
            startLesson(next, LISTEN, 0);
            return;
        }
        nextCountdown.setText("▶  Học tiếp sau " + seconds + " giây");
        scheduleStudy(1000, () -> tickNext(next, seconds - 1));
    }

    private void scheduleStudy(long delay, Runnable action) {
        cancelStudyAdvance();
        int revision = pageRevision;
        studyAdvance = () -> {
            studyAdvance = null;
            if (active && pageRevision == revision) action.run();
        };
        handler.postDelayed(studyAdvance, delay);
    }

    private void cancelStudyAdvance() {
        if (studyAdvance != null) handler.removeCallbacks(studyAdvance);
        studyAdvance = null;
    }

    private int nextLesson(int after) {
        int previous = after < 0 ? -1 : Lesson.alphabetPosition(after);
        int total = Lesson.ALL.length;
        if (after >= 0 && reviewSequence)
            return Lesson.atAlphabetPosition((previous + 1) % total);
        int choices = after < 0 ? total : total - 1;
        for (int offset = 1; offset <= choices; offset++) {
            int index = Lesson.atAlphabetPosition((previous + offset) % total);
            if (!progress.getBoolean(Lesson.ALL[index].key + "_done", false)) return index;
        }
        return -1;
    }

    private int completedCount() {
        int count = 0;
        for (Lesson item : Lesson.ALL) if (progress.getBoolean(item.key + "_done", false)) count++;
        return count;
    }

    private void showParents() {
        stopVoice();
        startActivityForResult(new Intent(this, ParentArea.class).putExtra("fromAlphabet", scene == 0), FAMILY_REQUEST);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != FAMILY_REQUEST) return;
        String child = result == RESULT_OK && data != null ? data.getStringExtra(ParentArea.PROFILE) : null;
        if (child != null && family.profiles().contains(child)) useProfile(child);
        else render();
    }

    void useProfile(String id) {
        family.activate(id);
        chapterMapOpen = false;
        alphabetSheet = 0;
        progress = family.progress(id);
        wordGame = null;
        game = null;
        studioUndo = null;
        int resume = progress.getInt("resume_lesson", -1);
        int next = resume >= 0 && resume < Lesson.ALL.length ? resume : nextLesson(-1);
        chapterIndex = chapterOf(next < 0 ? Lesson.atAlphabetPosition(0) : next);
        openHub();
    }

    private boolean motionEnabled() {
        return !family.reduceMotion()
            && (Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled());
    }

    private String wordAudio() {
        return lesson().key + "_word" + exampleIndex;
    }

    private void armHint() {
        cancelHint();
        if (scene != FIND && scene != MATCH) return;
        hintTask = () -> {
            if (!active) return;
            if (player != null) {
                armHint();
                return;
            }
            if ((scene == FIND || scene == MATCH) && !challengeFound) {
                reveal = true;
                paintChoices();
                playChallenge();
            }
        };
        handler.postDelayed(hintTask, 8500);
    }

    private void cancelHint() {
        if (hintTask != null) handler.removeCallbacks(hintTask);
        hintTask = null;
    }

    void voice(String name) { voice(name, 1f, null); }

    void voiceSequence(String first, String second) {
        int revision = pageRevision;
        voice(first, 1f, () -> {
            if (active && pageRevision == revision) voice(second);
        });
    }

    void voiceChain(String... clips) {
        playVoiceChain(pageRevision, clips, 0);
    }

    private void playVoiceChain(int revision, String[] clips, int index) {
        if (!active || revision != pageRevision || index >= clips.length) return;
        voice(clips[index], 1f, () -> playVoiceChain(revision, clips, index + 1));
    }

    private void voice(String name, float speed, Runnable complete) {
        stopVoice();
        MediaPlayer clip = new MediaPlayer();
        player = clip;
        try (AssetFileDescriptor file = getAssets().openFd("voice/" + name + ".mp3")) {
            clip.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            clip.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            clip.setOnPreparedListener(ready -> {
                if (player != ready) return;
                // Some Android vendors throw from PlaybackParams for perfectly valid MP3
                // streams (notably after an app update changes encoder/sample rate). Audio
                // speed is an enhancement, so fall back to the original speed instead of
                // allowing the UI thread to crash when a child opens a lesson.
                if (speed != 1f) {
                    try {
                        ready.setPlaybackParams(ready.getPlaybackParams().setSpeed(speed));
                    } catch (RuntimeException unsupportedSpeed) {
                        // Keep playing at the file's native speed.
                    }
                }
                try {
                    ready.start();
                    if (princess != null) princess.speaking();
                } catch (RuntimeException playbackFailure) {
                    voiceEnded(ready, complete);
                }
            });
            clip.setOnCompletionListener(ended -> voiceEnded(ended, complete));
            clip.setOnErrorListener((failed, what, extra) -> { voiceEnded(failed, complete); return true; });
            clip.prepareAsync();
        } catch (IOException | RuntimeException failure) {
            voiceEnded(clip, complete);
        }
    }

    private void voiceEnded(MediaPlayer clip, Runnable complete) {
        if (player != clip) return;
        stopVoice();
        if (complete != null) complete.run();
    }

    private void stopVoice() {
        if (player != null) {
            MediaPlayer ending = player;
            player = null;
            try {
                ending.reset();
            } catch (RuntimeException ignored) { }
            try {
                ending.release();
            } catch (RuntimeException ignored) { }
            mentorPose(DaylightArt.Princess.IDLE, 0);
        }
    }

    private void title(String headline, String instruction) {
        TextView heading = text(headline, 32, INK, true);
        heading.setGravity(Gravity.CENTER);
        page.addView(heading);
        TextView instructions = text(instruction, 16, MUTED, false);
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(0, dp(5), 0, 0);
        page.addView(instructions);
        gap(14);
    }

    private void feedback() {
        message = text("", 15, TEAL, true);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(9), 0, 0);
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        message.setVisibility(View.GONE);
        page.addView(message);
    }

    private void setFeedback(String copy) {
        if (message == null) return;
        message.setText(copy);
        message.setVisibility(View.VISIBLE);
    }

    private void pulse(View target) {
        if (!motionEnabled()) return;
        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.035f, 1f),
            ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.035f, 1f));
        animator.setDuration(570);
        animator.start();
    }

    void gentleShake(View target) {
        if (!motionEnabled() || target == null) return;
        ObjectAnimator shake = ObjectAnimator.ofFloat(target, View.TRANSLATION_X,
            0, -dp(7), dp(7), -dp(5), dp(5), 0);
        shake.setDuration(430);
        shake.start();
    }

    void magicBurst(View target, int streak) {
        if (!motionEnabled() || target == null) return;
        AnimatorSet pop = new AnimatorSet();
        pop.playTogether(ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.1f, .98f, 1f),
            ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.1f, .98f, 1f));
        pop.setDuration(520);
        pop.start();
        FrameLayout root = findViewById(android.R.id.content);
        if (root == null) return;
        int[] point = new int[2], origin = new int[2];
        target.getLocationOnScreen(point);
        root.getLocationOnScreen(origin);
        int count = streak >= 3 ? 15 : 8;
        for (int i = 0; i < count; i++) {
            TextView star = text(i % 3 == 0 ? "❄" : "✦", streak >= 3 ? 25 : 19,
                i % 2 == 0 ? 0xFFFFC83D : 0xFF57CFBD, true);
            FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(dp(32), dp(32));
            size.leftMargin = point[0] - origin[0] + target.getWidth() / 2 - dp(16);
            size.topMargin = point[1] - origin[1] + target.getHeight() / 2 - dp(16);
            root.addView(star, size);
            double angle = Math.PI * 2 * i / count;
            float distance = dp(streak >= 3 ? 78 : 52);
            AnimatorSet fly = new AnimatorSet();
            fly.playTogether(ObjectAnimator.ofFloat(star, View.TRANSLATION_X, 0, (float)Math.cos(angle) * distance),
                ObjectAnimator.ofFloat(star, View.TRANSLATION_Y, 0, (float)Math.sin(angle) * distance - dp(18)),
                ObjectAnimator.ofFloat(star, View.SCALE_X, .45f, 1.2f, .2f),
                ObjectAnimator.ofFloat(star, View.SCALE_Y, .45f, 1.2f, .2f),
                ObjectAnimator.ofFloat(star, View.ALPHA, 1f, 1f, 0f),
                ObjectAnimator.ofFloat(star, View.ROTATION, 0, 140 + i * 17));
            long delay = i * 24L;
            long duration = streak >= 3 ? 950 : 690;
            fly.setStartDelay(delay);
            fly.setDuration(duration);
            fly.start();
            handler.postDelayed(() -> root.removeView(star), delay + duration + 60);
        }
    }

    void playTapSfx() { tone(ToneGenerator.TONE_PROP_BEEP, 35); }

    void playFeedbackSfx(boolean correct, int streak) {
        tone(correct ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, correct ? 105 : 85);
        if (correct && streak >= 2)
            handler.postDelayed(() -> tone(ToneGenerator.TONE_PROP_BEEP, 90), 105);
    }

    private void tone(int type, int duration) {
        try {
            if (effects == null) effects = new ToneGenerator(AudioManager.STREAM_MUSIC, 48);
            effects.startTone(type, duration);
        } catch (RuntimeException ignored) { }
    }

    @Override protected void onDestroy() {
        stopVoice();
        if (effects != null) { effects.release(); effects = null; }
        super.onDestroy();
    }

    private TextView addButton(String label, boolean solid, Runnable action) {
        TextView view = button(label, solid, action);
        view.setMinHeight(dp(53));
        page.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private View backControl(String label, Runnable action) {
        LinearLayout group = row();
        View chevron = new View(this) {
            private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path arrow = new Path();

            @Override protected void onDraw(Canvas canvas) {
                float x = getWidth() / 2f, y = getHeight() / 2f;
                ink.setColor(0xFFE1E9FF);
                ink.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, y, dp(16), ink);
                ink.setColor(0xFF465CAD);
                ink.setStyle(Paint.Style.STROKE);
                ink.setStrokeWidth(dp(3));
                ink.setStrokeCap(Paint.Cap.ROUND);
                ink.setStrokeJoin(Paint.Join.ROUND);
                arrow.reset();
                arrow.moveTo(x + dp(2), y - dp(5));
                arrow.lineTo(x - dp(3), y);
                arrow.lineTo(x + dp(2), y + dp(5));
                canvas.drawPath(arrow, ink);
            }
        };
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        group.addView(chevron, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView destination = text(label, 16, INK, true);
        destination.setPadding(0, 0, dp(6), 0);
        group.addView(destination, new LinearLayout.LayoutParams(-2, -2));
        group.setContentDescription("Quay lại " + label.toLowerCase(Lesson.VI));
        makePressable(group, action);
        return group;
    }

    TextView button(String label, boolean solid, Runnable action) {
        TextView view = text(label, 16, solid ? INK : INK, true);
        view.setGravity(Gravity.CENTER);
        view.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);
        view.setMinHeight(dp(48));
        view.setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable face = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            solid ? new int[]{0xFFE1FBF1, MAIN_ACTION} : new int[]{SURFACE, 0xFFF2F7FF});
        face.setCornerRadius(dp(17));
        face.setStroke(dp(1), solid ? 0xFF8CCEBB : STROKE);
        view.setBackground(face);
        makePressable(view, action);
        return view;
    }

    boolean dragLocally(View view, Object token) {
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
        if (Build.VERSION.SDK_INT >= 24) return view.startDragAndDrop(null, shadow, token, 0);
        return view.startDrag(null, shadow, token, 0);
    }

    void makePressable(View view, Runnable action) {
        view.setFocusable(true);
        android.util.TypedValue selectable = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true);
        if (selectable.resourceId != 0) view.setForeground(getDrawable(selectable.resourceId));
        view.setOnClickListener(tapped -> action.run());
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host,
                android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
    }

    TextView text(String value, float size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(bold ? boldText : mediumText);
        text.setIncludeFontPadding(true);
        return text;
    }

    LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setBaselineAligned(false);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private void gap(int height) {
        page.addView(new View(this), new LinearLayout.LayoutParams(1, dp(landscapeMode() ? Math.min(6, height) : height)));
    }

    GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class SkyView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stars = new Paint(Paint.ANTI_ALIAS_FLAG);
        SkyView() {
            super(MainActivity.this);
            stars.setColor(0x66FFFFFF);
        }
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            paint.setShader(new android.graphics.LinearGradient(0, 0, w, h,
                new int[]{0xFFE2F5FE, 0xFFF0FAFC, 0xFFFFF1DC}, null, android.graphics.Shader.TileMode.CLAMP));
        }
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            for (int i = 0; i < 37; i++) {
                float x = ((i * 83 + 24) % 379) / 379f * getWidth();
                float y = ((i * 219 + 80) % 731) / 731f * getHeight();
                canvas.drawCircle(x, y, dp(i % 6 == 0 ? 2 : 1), stars);
            }
        }
    }

    private final class SparkleBurst extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long started = SystemClock.uptimeMillis();

        SparkleBurst() {
            super(MainActivity.this);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(Canvas canvas) {
            float elapsed = Math.min(1f, (SystemClock.uptimeMillis() - started) / 680f);
            float spread = (float) Math.sin(elapsed * Math.PI / 2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int point = 0; point < 9; point++) {
                double angle = (point / 9.0) * 2.0 * Math.PI;
                float x = getWidth() * .52f + (float) Math.cos(angle) * getWidth() * .4f * spread;
                float y = getHeight() * .46f + (float) Math.sin(angle) * getHeight() * .38f * spread;
                float arm = dp(2 + point % 3);
                paint.setColor(point % 3 == 0 ? AMBER : TEAL);
                paint.setAlpha((int) (255 * (1f - elapsed)));
                paint.setStrokeWidth(dp(1));
                canvas.drawLine(x - arm, y, x + arm, y, paint);
                canvas.drawLine(x, y - arm, x, y + arm, paint);
            }
            if (elapsed < 1f && isShown()) postInvalidateOnAnimation();
        }
    }

    private final class LampTrail extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path bridge = new Path();
        private final int lit, total, accent;
        private final boolean animated;
        private final long started = SystemClock.uptimeMillis();

        LampTrail(int lit, int total, int accent, boolean animated) {
            super(MainActivity.this);
            this.lit = lit;
            this.total = total;
            this.accent = accent;
            this.animated = animated;
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(Canvas canvas) {
            float startX = dp(15), endX = getWidth() - dp(15);
            float middle = getHeight() * .54f, height = getHeight() * .26f;
            float elapsed = Math.min(1, (SystemClock.uptimeMillis() - started) / 950f);
            float progress = animated && motionEnabled() ? elapsed : 1;
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(6));
            paint.setColor(0xFFB2CBDD);
            bridge.reset();
            for (int i = 0; i < total; i++) {
                float phase = i / (float) (total - 1);
                float x = startX + (endX - startX) * phase;
                float y = middle - (float) Math.sin(Math.PI * phase) * height;
                if (i == 0) bridge.moveTo(x, y); else bridge.lineTo(x, y);
            }
            canvas.drawPath(bridge, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < total; i++) {
                float phase = i / (float) (total - 1);
                float x = startX + (endX - startX) * phase;
                float y = middle - (float) Math.sin(Math.PI * phase) * height;
                boolean glow = i < lit;
                if (glow) {
                    paint.setColor(accent);
                    paint.setAlpha(i + 1 == lit ? (int) (65 + 85 * Math.sin(Math.PI * progress)) : 65);
                    canvas.drawCircle(x, y, dp(11), paint);
                }
                paint.setColor(glow ? AMBER : 0xFFCEE3EF);
                canvas.drawCircle(x, y, dp(7), paint);
                paint.setColor(glow ? Color.WHITE : 0xFF7994AF);
                canvas.drawCircle(x, y, dp(2), paint);
            }
            if (progress < 1 && isShown()) postInvalidateOnAnimation();
        }
    }



    private Bitmap illustration(int index) {
        Bitmap existing = cardImages.get(index);
        if (existing != null) return existing;
        try (java.io.InputStream input = getAssets().open("illustrations/card" + index + ".webp")) {
            Bitmap image = BitmapFactory.decodeStream(input);
            if (image == null) throw new IOException("Invalid illustration");
            image.setHasMipMap(true);
            cardImages.put(index, image);
            return image;
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to open illustration " + index, failure);
        }
    }

    private final class FlashcardView extends View {
        private final Paint picturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Rect destination = new Rect();
        private final Bitmap image;

        FlashcardView(int index) {
            super(MainActivity.this);
            image = illustration(index);
        }

        @Override protected void onDraw(Canvas canvas) {
            int size = Math.max(0, Math.min(getHeight(), getWidth()) - dp(5));
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            destination.set(x, y, x + size, y + size);
            canvas.drawBitmap(image, null, destination, picturePaint);
        }
    }
}
