package vn.vuongquoctuyet.hocchu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.*;
import org.json.JSONArray;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public final class PlayStudio extends Activity {
    private static final String ORIGIN = "https://play.tuyet.invalid/";
    private WebView web;
    private MediaPlayer voice;
    private ToneGenerator effects;
    private volatile boolean active;
    private File album;
    private SharedPreferences progress;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        FamilyProfiles profiles = new FamilyProfiles(this);
        profiles.applyOrientation(this);
        String child = profiles.active();
        album = new File(new File(getFilesDir(), "paintings"), child);
        progress = profiles.progress(child);
        openGames();
    }

    private void openGames() {
        web = new WebView(this);
        if (android.os.Build.FINGERPRINT.startsWith("generic") || android.os.Build.FINGERPRINT.contains("/sdk_phone"))
            web.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.addJavascriptInterface(new StudioBridge(), "Studio");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean onRenderProcessGone(WebView failed, RenderProcessGoneDetail reason) {
                stopVoice();
                if (failed.getParent() instanceof android.view.ViewGroup)
                    ((android.view.ViewGroup) failed.getParent()).removeView(failed);
                failed.destroy();
                web = null;
                android.widget.LinearLayout panel = new android.widget.LinearLayout(PlayStudio.this);
                panel.setGravity(android.view.Gravity.CENTER);
                panel.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.Button retry = new android.widget.Button(PlayStudio.this);
                retry.setText("Mở lại khu chơi");
                retry.setOnClickListener(v -> openGames());
                android.widget.Button back = new android.widget.Button(PlayStudio.this);
                back.setText("Về Học & Chơi");
                back.setOnClickListener(v -> finish());
                panel.addView(retry);
                panel.addView(back);
                SafeContent.attach(PlayStudio.this, panel, 0xFFEEE4FF, false);
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) { return true; }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith(ORIGIN)) {
                    String path = url.substring(ORIGIN.length());
                    try {
                        if (path.matches("album/[0-9]+-[0-9a-f-]+\\.png"))
                            return new WebResourceResponse("image/png", null, new FileInputStream(new File(album,path.substring(6))));
                        if (path.equals("nunito.ttf"))
                            return new WebResourceResponse("font/ttf",null,getAssets().open("parent/nunito.ttf"));
                        if (path.matches("[a-z]+\\.(html|css|js)")) {
                            String mime = path.endsWith(".html") ? "text/html" : path.endsWith(".css") ? "text/css" : "application/javascript";
                            return new WebResourceResponse(mime,"UTF-8",getAssets().open("play/"+path));
                        }
                    } catch (IOException ignored) { }
                }
                return new WebResourceResponse("text/plain","UTF-8",404,"Missing",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
            }
        });
        SafeContent.attach(this, web, 0xFFEEE4FF, false);
        web.loadUrl(ORIGIN+"index.html");
    }

    final class StudioBridge {
        @JavascriptInterface public String mode() { return "blocks".equals(getIntent().getStringExtra("mode")) ? "blocks" : "art"; }
        @JavascriptInterface public boolean reduced() { return new FamilyProfiles(PlayStudio.this).reduceMotion(); }
        @JavascriptInterface public void close() { runOnUiThread(() -> finish()); }
        @JavascriptInterface public String pictures() {
            String[] names = album.list((dir,name) -> name.matches("[0-9]+-[0-9a-f-]+\\.png"));
            JSONArray result = new JSONArray();
            if (names != null) {
                Arrays.sort(names, Collections.reverseOrder());
                for (String name : names) result.put(name);
            }
            return result.toString();
        }
        @JavascriptInterface public boolean save(String png) {
            if (!active || png.length() > 8000000) return false;
            File temporary = null;
            try {
                if (!album.isDirectory() && !album.mkdirs()) return false;
                byte[] bytes = Base64.decode(png,Base64.DEFAULT);
                temporary = File.createTempFile("painting", ".tmp", album);
                try (FileOutputStream out = new FileOutputStream(temporary)) { out.write(bytes); }
                return temporary.renameTo(new File(album,System.currentTimeMillis()+"-"+UUID.randomUUID()+".png"));
            } catch (IOException | IllegalArgumentException error) { return false; }
            finally { if (temporary != null && temporary.exists()) temporary.delete(); }
        }
        @JavascriptInterface public void effect(String key) {
            if (!key.matches("tap|bucket|stamp|shutter|reset")) return;
            runOnUiThread(() -> {
                if (!active) return;
                try {
                    if (effects == null) effects = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
                    int tone = ToneGenerator.TONE_PROP_BEEP;
                    if (key.equals("bucket")) tone = ToneGenerator.TONE_PROP_ACK;
                    else if (key.equals("stamp")) tone = ToneGenerator.TONE_PROP_BEEP2;
                    else if (key.equals("reset")) tone = ToneGenerator.TONE_PROP_NACK;
                    effects.startTone(tone, key.equals("shutter") ? 150 : 90);
                } catch (RuntimeException ignored) { }
            });
        }
        @JavascriptInterface public void completed() {
            if (active) FamilyProfiles.record(progress,progress.edit().putInt("blocks_complete", progress.getInt("blocks_complete", 0) + 1),false).apply();
        }
        @JavascriptInterface public void speak(String key) {
            if (!key.matches("[0-3]|hub|gallery|canvas|saved|album|reset")) return;
            runOnUiThread(() -> {
                stopVoice();
                if (!active) return;
                try (AssetFileDescriptor fd = getAssets().openFd("play/audio/"+key+".mp3")) {
                    voice = new MediaPlayer();
                    voice.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
                    voice.prepare();
                    voice.start();
                } catch (IOException | RuntimeException error) { stopVoice(); }
            });
        }
    }
    private void stopVoice() { if (voice != null) { voice.release(); voice=null; } }
    @Override public void onBackPressed() { if (web == null) { finish(); return; } web.evaluateJavascript("window.goBack ? goBack() : Studio.close()",null); }
    @Override protected void onResume() { super.onResume(); active=true; if(web!=null) web.onResume(); }
    @Override protected void onPause() { active=false; stopVoice(); if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("Studio"); web.destroy(); }
        if (effects != null) { effects.release(); effects=null; }
        super.onDestroy();
    }
}
