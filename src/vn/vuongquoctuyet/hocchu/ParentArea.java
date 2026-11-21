package vn.vuongquoctuyet.hocchu;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public final class ParentArea extends Activity {
    static final String PROFILE = "profile";
    private static final String ORIGIN = "https://family.vuongquoctuyet.invalid/";
    private FamilyProfiles family;
    private WebView web;
    private volatile boolean closed;
    private boolean reopen;
    private volatile boolean webReady;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable webTimeout = () -> {
        if (!closed && !webReady) showFallback();
    };
    private Bridge bridge;
    private ParentFallback fallback;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        family = new FamilyProfiles(this);
        family.applyOrientation(this);
        getWindow().setStatusBarColor(0xFFF8F4FF);
        getWindow().setNavigationBarColor(0xFFF8F4FF);
        int flags = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        bridge = new Bridge();
        android.widget.TextView loading = new android.widget.TextView(this);
        loading.setText("Đang mở Góc phụ huynh…");
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setTextColor(0xFF4B317D);
        loading.setBackgroundColor(0xFFF8F4FF);
        showSafe(loading);
        loading.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            try { createWeb(); }
            catch (RuntimeException unavailable) { showFallback(); }
        });
    }

    private void createWeb() {
        web = new WebView(this);
        if (Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.contains("/sdk_phone"))
            web.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
        web.setBackgroundColor(0xFFF8F4FF);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setUseWideViewPort(true);
        web.addJavascriptInterface(bridge, "Family");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (!"https".equals(url.getScheme()) || !"family.vuongquoctuyet.invalid".equals(url.getHost())
                        || url.getPort() != -1 || !"GET".equals(request.getMethod()))
                    return rejected();
                String path = url.getPath();
                if (path == null || !path.matches("/[a-zA-Z0-9_-]+\\.(html|css|js|webp|ttf)")) return rejected();
                String mime = path.endsWith(".html") ? "text/html" : path.endsWith(".css") ? "text/css"
                    : path.endsWith(".js") ? "application/javascript" : path.endsWith(".ttf") ? "font/ttf" : "image/webp";
                try {
                    return new WebResourceResponse(mime, (path.endsWith(".webp") || path.endsWith(".ttf")) ? null : "UTF-8",
                        getAssets().open("parent" + path));
                } catch (IOException missing) {
                    return rejected();
                }
            }

            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                showFallback();
                return true;
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showFallback();
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (!closed && (ORIGIN + "index.html").equals(url))
                    view.setVisibility(android.view.View.VISIBLE);
            }

            private WebResourceResponse rejected() {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Unavailable",
                    java.util.Collections.emptyMap(), new java.io.ByteArrayInputStream(new byte[0]));
            }
        });
        showSafe(web);
        loadParentPage();
    }

    void showSafe(android.view.View content) {
        SafeContent.attach(this, content, 0xFFF8F4FF, true);
    }

    private void loadParentPage() {
        webReady = false;
        handler.removeCallbacks(webTimeout);
        handler.postDelayed(webTimeout, 12_000);
        web.loadUrl(ORIGIN + "index.html");
    }

    private void showFallback() {
        if (fallback != null || isFinishing() || isDestroyed()) return;
        handler.removeCallbacks(webTimeout);
        disposeWeb();
        bridge.gate = new ParentGate();
        fallback = new ParentFallback(this, bridge);
        fallback.open();
    }

    private void disposeWeb() {
        if (web == null) return;
        WebView current = web;
        web = null;
        current.removeJavascriptInterface("Family");
        if (current.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) current.getParent()).removeView(current);
        current.destroy();
    }

    @Override protected void onStop() {
        super.onStop();
        closed = true;
        handler.removeCallbacks(webTimeout);
        reopen = true;
        if (web != null) web.setVisibility(android.view.View.INVISIBLE);
    }

    @Override protected void onStart() {
        super.onStart();
        if (reopen) {
            reopen = false;
            bridge.gate = new ParentGate();
            closed = false;
            if (web != null) loadParentPage();
            else if (fallback != null) fallback.open();
        }
    }

    @Override protected void onDestroy() {
        closed = true;
        handler.removeCallbacks(webTimeout);
        disposeWeb();
        super.onDestroy();
    }

    final class Bridge {
        private volatile ParentGate gate = new ParentGate();
        @JavascriptInterface public void ready() {
            if (!closed) webReady = true;
        }
        @JavascriptInterface public String prompt() { return closed || gate.unlocked() ? "" : gate.prompt(); }
        @JavascriptInterface public boolean verify(String digits) { return !closed && gate.submit(digits); }
        private boolean allowed() { return !closed && gate.unlocked(); }

        @JavascriptInterface public String snapshot(String selected) {
            if (!allowed()) return "{}";
            if (!family.profiles().contains(selected)) selected = family.active();
            try {
                JSONObject result = new JSONObject();
                JSONArray profiles = new JSONArray();
                for (String id : family.profiles())
                    profiles.put(new JSONObject().put("id", id).put("name", family.name(id)).put("avatar", family.avatar(id)));
                SharedPreferences prefs = family.progress(selected);
                JSONArray letters = new JSONArray(), games = new JSONArray(), rewards = new JSONArray();
                for (int i = 0; i < Lesson.ALL.length; i++) {
                    Lesson item = Lesson.ALL[Lesson.atAlphabetPosition(i)];
                    letters.put(new JSONObject().put("label", item.letter.toUpperCase(Lesson.VI))
                        .put("done", prefs.getBoolean(item.key + "_done", false)));
                }
                for (WordPlay.Mode mode : WordPlay.Mode.values())
                    games.put(new JSONObject().put("label", mode.title)
                        .put("count", prefs.getInt("word_rounds_" + mode.name(), 0)).put("group", "Chơi cùng chữ"));
                for (LearningGame.Mode mode : LearningGame.Mode.values())
                    games.put(new JSONObject().put("label", mode.title).put("group", "Toán & tư duy")
                        .put("count", prefs.getInt("academy_" + mode.name().toLowerCase(Locale.ROOT) + "_complete", 0)));
                games.put(new JSONObject().put("label","Đếm khối màu").put("group","Toán & tư duy")
                    .put("count",prefs.getInt("blocks_complete",0)));
                int unlocks = Math.min(DaylightArt.PROPS.length - DaylightArt.BASIC_PROPS,
                    prefs.getInt("studio_rewards", 0));
                for (int i = DaylightArt.BASIC_PROPS; i < DaylightArt.PROPS.length; i++)
                    rewards.put(new JSONObject().put("label", DaylightArt.PROPS[i]).put("index", i)
                        .put("done", i < unlocks + DaylightArt.BASIC_PROPS));
                JSONObject dates = new JSONObject();
                for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                    if (!(entry.getValue() instanceof Integer)) continue;
                    String key = entry.getKey();
                    if (key.matches("day_(letters|games)_[0-9]{8}"))
                        dates.put(key, (Integer) entry.getValue());
                }
                return result.put("active", family.active()).put("selected", selected)
                    .put("profiles", profiles).put("max", FamilyProfiles.MAX_PROFILES)
                    .put("days", dates).put("letters", letters).put("games", games).put("rewards", rewards)
                    .put("orientation", family.orientation()).put("studyDelay", family.studyDelay(selected)).put("goal", family.goal(selected)).put("reduceMotion", family.reduceMotion())
                    .put("letterTotal", FamilyProfiles.completedLetters(prefs))
                    .put("gameTotal", FamilyProfiles.completedGames(prefs)).toString();
            } catch (JSONException error) {
                return "{}";
            }
        }

        @JavascriptInterface public String saveProfile(String id, String name, int avatar) {
            if (!allowed() || name == null || id == null) return "";
            name = name.trim();
            if (name.isEmpty() || name.length() > 24 || avatar < 0 || avatar >= DaylightArt.BASIC_PROPS) return "";
            if (id.isEmpty()) id = family.add(name);
            if (id == null || !family.profiles().contains(id)) return "";
            family.rename(id, name);
            family.avatar(id, avatar);
            return id;
        }

        @JavascriptInterface public void setStudyDelay(String id, int seconds) {
            if (allowed() && family.profiles().contains(id)) family.studyDelay(id, seconds);
        }

        @JavascriptInterface public void setGoal(String id, int goal) {
            if (allowed() && family.profiles().contains(id) && goal >= 1 && goal <= 12) family.goal(id, goal);
        }

        @JavascriptInterface public void setOrientation(String mode) {
            if (!allowed()) return;
            family.setOrientation(mode);
            runOnUiThread(() -> family.applyOrientation(ParentArea.this));
        }

        @JavascriptInterface public void reduceMotion(boolean reduced) {
            if (allowed()) family.reduceMotion(reduced);
        }

        @JavascriptInterface public String returnLabel() {
            return getIntent().getBooleanExtra("fromAlphabet", false) ? "Bảng chữ" : "Học & Chơi";
        }
        @JavascriptInterface public void close() { runOnUiThread(() -> finish()); }

        @JavascriptInterface public void activate(String id) {
            if (!allowed() || !family.profiles().contains(id)) return;
            runOnUiThread(() -> {
                if (closed) return;
                setResult(RESULT_OK, new Intent().putExtra(PROFILE, id));
                finish();
            });
        }
    }
}
