package vn.vuongquoctuyet.hocchu;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class FamilyProfiles {
    static final String FIRST = "first";
    static final int MAX_PROFILES = 8;
    private final Context context;
    private final SharedPreferences store;

    FamilyProfiles(Context context) {
        this.context = context.getApplicationContext();
        store = context.getSharedPreferences("family", Context.MODE_PRIVATE);
        if (!store.contains("profiles")) store.edit().putString("profiles", FIRST)
            .putBoolean("reduce_motion", progress(FIRST).getBoolean("reduce_motion", false)).apply();
        if (store.contains("pin_hash") || store.contains("pin_until")) store.edit().remove("pin_hash")
            .remove("pin_salt").remove("pin_failures").remove("pin_until").apply();
        importLessonDates(progress(FIRST));
    }

    private static void importLessonDates(SharedPreferences prefs) {
        if (prefs.getBoolean("daily_history_imported", false)) return;
        java.util.Map<String, Integer> history = new java.util.HashMap<>();
        for (Lesson lesson : Lesson.ALL) {
            long last = prefs.getLong(lesson.key + "_practiced", 0);
            if (last <= 0) continue;
            String key = "day_letters_" + dayKey(new Date(last));
            Integer count = history.get(key);
            history.put(key, count == null ? 1 : count + 1);
        }
        SharedPreferences.Editor update = prefs.edit().putBoolean("daily_history_imported", true);
        for (java.util.Map.Entry<String, Integer> entry : history.entrySet()) {
            update.putInt(entry.getKey(), entry.getValue() + prefs.getInt(entry.getKey(), 0));
        }
        update.apply();
    }

    List<String> profiles() {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, store.getString("profiles", FIRST).split(","));
        return result;
    }

    String active() {
        String id = store.getString("active", FIRST);
        return profiles().contains(id) ? id : FIRST;
    }

    SharedPreferences progress(String id) {
        return context.getSharedPreferences(FIRST.equals(id) ? "lesson_progress" : "lesson_progress_" + id,
            Context.MODE_PRIVATE);
    }

    String name(String id) { return store.getString("name_" + id, "Bé 1"); }

    String add(String name) {
        List<String> ids = profiles();
        if (ids.size() >= MAX_PROFILES) return null;
        String id = UUID.randomUUID().toString();
        store.edit().putString("profiles", store.getString("profiles", FIRST) + "," + id)
            .putString("name_" + id, name).apply();
        return id;
    }

    void rename(String id, String name) { store.edit().putString("name_" + id, name).apply(); }
    void activate(String id) { if (profiles().contains(id)) store.edit().putString("active", id).apply(); }
    String orientation() { return store.getString("orientation", "system"); }

    void setOrientation(String mode) {
        if ("system".equals(mode) || "sensor".equals(mode) || "portrait".equals(mode))
            store.edit().putString("orientation", mode).apply();
    }

    void applyOrientation(android.app.Activity activity) {
        int requested = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        String mode = orientation();
        if ("sensor".equals(mode)) requested = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR;
        if ("portrait".equals(mode)) requested = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        if (activity.getRequestedOrientation() != requested) activity.setRequestedOrientation(requested);
    }

    boolean reduceMotion() { return store.getBoolean("reduce_motion", false); }
    void reduceMotion(boolean enabled) { store.edit().putBoolean("reduce_motion", enabled).apply(); }
    int avatar(String id) { return store.getInt("avatar_" + id, FIRST.equals(id) ? 1 : 4); }
    void avatar(String id, int value) { store.edit().putInt("avatar_" + id, value).apply(); }
    static final int[] STUDY_DELAYS = {0, 5, 10, 15, 20, 30};

    int studyDelay(String id) { return store.getInt("study_delay_" + id, 0); }

    void studyDelay(String id, int seconds) {
        for (int option : STUDY_DELAYS) {
            if (seconds != option) continue;
            store.edit().putInt("study_delay_" + id, seconds).apply();
            return;
        }
    }

    int goal(String id) { return store.getInt("goal_" + id, 3); }
    void goal(String id, int value) { store.edit().putInt("goal_" + id, value).apply(); }

    static String dayKey(Date date) { return new SimpleDateFormat("yyyyMMdd", Locale.US).format(date); }

    static SharedPreferences.Editor record(SharedPreferences progress, SharedPreferences.Editor update, boolean lesson) {
        String key = (lesson ? "day_letters_" : "day_games_") + dayKey(new Date());
        return update.putInt(key, progress.getInt(key, 0) + 1);
    }

    static int completedLetters(SharedPreferences prefs) {
        int total = 0;
        for (Lesson lesson : Lesson.ALL) if (prefs.getBoolean(lesson.key + "_done", false)) total++;
        return total;
    }

    static int completedGames(SharedPreferences prefs) {
        int total = prefs.getInt("blocks_complete", 0);
        for (LearningGame.Mode mode : LearningGame.Mode.values())
            total += prefs.getInt("academy_" + mode.name().toLowerCase(Locale.ROOT) + "_complete", 0);
        for (WordPlay.Mode mode : WordPlay.Mode.values()) total += prefs.getInt("word_rounds_" + mode.name(), 0);
        return total;
    }

    static int activity(SharedPreferences prefs, Calendar day, boolean letter) {
        return prefs.getInt((letter ? "day_letters_" : "day_games_") + dayKey(day.getTime()), 0);
    }
}
