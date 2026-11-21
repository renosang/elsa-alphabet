package vn.vuongquoctuyet.hocchu;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import java.util.ArrayList;

final class SceneStudio {
    private static final int CAPACITY = 24;
    private static final int TOKEN_DP = 64;
    private final MainActivity host;
    private final SharedPreferences store;
    private final int scene;
    private final LinearLayout outer;
    private final ArrayList<Piece> pieces;
    private final ArrayList<View> paletteTiles = new ArrayList<>();
    private String previous;
    private int selected = -1;
    private int palette = -1;
    private boolean moving;
    private Stage stage;
    private TextView status, undo, delete, place, move;
    private LinearLayout arrows;

    SceneStudio(MainActivity host, SharedPreferences store, int scene, LinearLayout outer, String previous) {
        this.host = host;
        this.store = store;
        this.scene = scene;
        this.outer = outer;
        this.previous = previous;
        pieces = decode(store.getString(key(), "[]"));
    }

    String undoState() { return previous; }
    private String key() { return "studio_scene_" + scene; }

    static int piecesCount(SharedPreferences store, int scene) {
        return decode(store.getString("studio_scene_" + scene, "[]")).size();
    }

    private static ArrayList<Piece> decode(String raw) {
        ArrayList<Piece> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < Math.min(array.length(), CAPACITY); i++) {
                JSONArray entry = array.optJSONArray(i);
                if (entry == null) continue;
                int prop = entry.optInt(0, -1);
                double x = entry.optDouble(1, Double.NaN), y = entry.optDouble(2, Double.NaN);
                if (prop >= 0 && prop < DaylightArt.PROPS.length && x >= 0 && x <= 1 && y >= 0 && y <= 1)
                    result.add(new Piece(prop, (float) x, (float) y));
            }
        } catch (org.json.JSONException ignored) {
            // A damaged local draft opens as an empty scene.
        }
        return result;
    }

    private String encode() {
        JSONArray output = new JSONArray();
        for (Piece piece : pieces) {
            JSONArray entry = new JSONArray();
            entry.put(piece.prop);
            try {
                entry.put((double) piece.x);
                entry.put((double) piece.y);
            } catch (org.json.JSONException invalidPosition) {
                throw new IllegalStateException(invalidPosition);
            }
            output.put(entry);
        }
        return output.toString();
    }

    void show() {
        TextView heading = host.text(DaylightArt.SCENE_NAMES[scene], 29, MainActivity.INK, true);
        host.toolPane(outer).addView(heading);
        status = host.text("Chọn một món đồ rồi đặt vào cảnh.", 15, MainActivity.MUTED, true);
        status.setMinHeight(host.dp(40));
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        host.toolPane(outer).addView(status);
        stage = new Stage();
        int height = host.landscapeMode() ? host.landscapeHeight(100,.42f,420) : host.wideLayout() ? 245 :
            Math.max(205, Math.min(340, Math.round(host.getResources().getConfiguration().screenWidthDp * .61f)));
        outer.addView(stage, new LinearLayout.LayoutParams(-1, host.dp(height)));
        if (host.landscapeMode()) {
            addPalette();
            addToolbar();
        } else {
            addToolbar();
            addPalette();
        }
        refresh("Chọn món đồ hoặc chạm vào một món trong cảnh.");
    }

    void speak() {
        host.voice(palette >= 0 || moving ? "world_pick_place" : "world_scene_pick");
    }

    private void addToolbar() {
        LinearLayout row = host.row();
        row.setPadding(0, host.dp(7), 0, host.dp(7));
        undo = tool(row, "↶  Hoàn tác", this::undo);
        move = tool(row, "Di chuyển", () -> {
            if (selected < 0) return;
            moving = !moving;
            refresh(moving ? "Chạm nơi con muốn chuyển món đồ đến." : "Đã chọn " + pieceName(selected) + ".");
            if (moving) host.voice("world_pick_place");
        });
        delete = tool(row, "Xóa món", () -> {
            if (selected < 0) return;
            previous = encode();
            pieces.remove(selected);
            selected = -1;
            moving = false;
            save("Đã xóa món đồ.");
        });
        outer.addView(row);
        LinearLayout movement = host.row();
        place = tool(movement, "Đặt ở giữa", () -> applyAt(.5f, .72f));
        outer.addView(movement);
        arrows = host.row();
        TextView left = tool(arrows, "←", () -> nudge(-.1f, 0));
        TextView up = tool(arrows, "↑", () -> nudge(0, -.1f));
        TextView down = tool(arrows, "↓", () -> nudge(0, .1f));
        TextView right = tool(arrows, "→", () -> nudge(.1f, 0));
        left.setContentDescription("Sang trái");
        up.setContentDescription("Lên trên");
        down.setContentDescription("Xuống dưới");
        right.setContentDescription("Sang phải");
        outer.addView(arrows);
    }

    private void nudge(float x, float y) {
        if (selected < 0) return;
        Piece current = pieces.get(selected);
        moveProp(selected, Math.max(0, Math.min(1, current.x + x)), Math.max(0, Math.min(1, current.y + y)));
        moving = true;
        refresh("Đã di chuyển " + pieceName(selected) + ". Đã lưu.");
    }

    private TextView tool(LinearLayout bar, String label, Runnable action) {
        TextView button = host.button(label, false, action);
        button.setTextSize(14);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
        if (bar.getChildCount() > 0) size.leftMargin = host.dp(6);
        bar.addView(button, size);
        return button;
    }

    private void addPalette() {
        host.toolPane(outer).addView(host.text("Đồ trang trí", 19, MainActivity.INK, true));
        HorizontalScrollView scroll = new HorizontalScrollView(host) {
            @Override public boolean onInterceptTouchEvent(MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
                    getParent().requestDisallowInterceptTouchEvent(true);
                return super.onInterceptTouchEvent(event);
            }
        };
        scroll.setFillViewport(false);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout props = host.row();
        props.setPadding(0, host.dp(6), host.dp(4), host.dp(4));
        int rewards = Math.min(store.getInt("studio_rewards", 0),
            DaylightArt.PROPS.length - DaylightArt.BASIC_PROPS);
        for (int prop = 0; prop < DaylightArt.BASIC_PROPS + rewards; prop++) {
            int id = prop;
            LinearLayout tile = host.column();
            tile.setGravity(Gravity.CENTER);
            tile.setContentDescription("Chọn " + DaylightArt.PROPS[prop]);
            tile.addView(new DaylightArt.PropIcon(host, prop), new LinearLayout.LayoutParams(host.dp(53), host.dp(48)));
            TextView caption = host.text(DaylightArt.PROPS[prop], 12, MainActivity.INK, true);
            caption.setGravity(Gravity.CENTER);
            caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.addView(caption);
            host.makePressable(tile, () -> {
                palette = palette == id ? -1 : id;
                selected = -1;
                moving = false;
                refresh(palette < 0 ? "Chọn một món đồ." : "Chạm vào cảnh để đặt " + DaylightArt.PROPS[id] + ".");
                if (palette >= 0) host.voice("world_pick_place");
            });
            enableDrag(tile, new DragPiece(id, false));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(host.dp(85), host.dp(86));
            if (prop > 0) size.leftMargin = host.dp(7);
            props.addView(tile, size);
            paletteTiles.add(tile);
        }
        scroll.addView(props);
        host.toolPane(outer).addView(scroll);
    }

    private void refresh(String message) {
        status.setText(message);
        undo.setEnabled(previous != null);
        undo.setAlpha(previous == null ? .55f : 1f);
        move.setVisibility(selected < 0 ? View.GONE : View.VISIBLE);
        delete.setVisibility(selected < 0 ? View.GONE : View.VISIBLE);
        move.setText(moving ? "Xong" : "Di chuyển");
        arrows.setVisibility(moving ? View.VISIBLE : View.GONE);
        place.setVisibility(palette >= 0 || moving ? View.VISIBLE : View.GONE);
        place.setText(moving ? "Chuyển đến giữa" : "Đặt ở giữa");
        for (int i = 0; i < paletteTiles.size(); i++) {
            View tile = paletteTiles.get(i);
            tile.setSelected(i == palette);
            tile.setBackground(host.background(i == palette ? 0xFFFFE5A5 : Color.WHITE, 16,
                i == palette ? MainActivity.PLUM : MainActivity.STROKE));
        }
        stage.drawPieces();
    }

    private void undo() {
        if (previous == null) return;
        pieces.clear();
        pieces.addAll(decode(previous));
        previous = null;
        selected = -1;
        palette = -1;
        moving = false;
        save("Đã hoàn tác.");
    }

    private void applyAt(float x, float y) {
        if (palette >= 0) placeProp(palette, x, y);
        else if (moving && selected >= 0) moveProp(selected, x, y);
    }

    private void placeProp(int prop, float x, float y) {
        if (pieces.size() >= CAPACITY) {
            palette = -1;
            refresh("Cảnh đã đầy. Con có thể xóa một món rồi thêm món khác.");
            return;
        }
        previous = encode();
        pieces.add(new Piece(prop, x, y));
        selected = pieces.size() - 1;
        palette = -1;
        moving = false;
        save("Đã đặt " + DaylightArt.PROPS[prop] + ".");
        if (!store.getBoolean("studio_placement_help", false)) {
            store.edit().putBoolean("studio_placement_help", true).apply();
            host.voice("world_scene_placed");
        }
    }

    private void moveProp(int index, float x, float y) {
        if (index < 0 || index >= pieces.size()) return;
        previous = encode();
        Piece current = pieces.get(index);
        pieces.set(index, new Piece(current.prop, x, y));
        selected = index;
        palette = -1;
        moving = false;
        save("Đã di chuyển " + DaylightArt.PROPS[current.prop] + ".");
    }

    private String pieceName(int index) { return DaylightArt.PROPS[pieces.get(index).prop]; }

    private void save(String message) {
        store.edit().putString(key(), encode()).apply();
        refresh(message + " Đã lưu.");
    }

    private void enableDrag(View tile, DragPiece entry) {
        tile.setOnLongClickListener(v -> host.dragLocally(v, entry));
        int slop = ViewConfiguration.get(host).getScaledTouchSlop();
        tile.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            @Override public boolean onTouch(View view, MotionEvent touch) {
                switch (touch.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = touch.getX();
                        downY = touch.getY();
                        if (entry.placed) view.getParent().requestDisallowInterceptTouchEvent(true);
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float vertical = Math.abs(touch.getY() - downY);
                        float horizontal = Math.abs(touch.getX() - downX);
                        if (entry.placed ? Math.max(vertical, horizontal) > slop : vertical > slop && vertical > horizontal) {
                            view.setPressed(false);
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                            return host.dragLocally(view, entry);
                        }
                        return false;
                    default: return false;
                }
            }
        });
    }

    private final class Stage extends FrameLayout {
        Stage() {
            super(host);
            setBackground(host.background(DaylightArt.SCENE_COLORS[scene], 22, MainActivity.STROKE));
            setClipToOutline(true);
            ImageView art = new ImageView(host);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setImageResource(DaylightArt.SCENES[scene]);
            art.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(art, new LayoutParams(-1, -1));
            setOnDragListener((view, event) -> {
                Object state = event.getLocalState();
                if (!(state instanceof DragPiece)) return false;
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    DragPiece entry = (DragPiece) state;
                    if (entry.placed) moveProp(entry.id, normalizedX(event.getX()), normalizedY(event.getY()));
                    else placeProp(entry.id, normalizedX(event.getX()), normalizedY(event.getY()));
                }
                return true;
            });
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w != oldw || h != oldh) post(this::drawPieces);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    return palette >= 0 || moving;
                case MotionEvent.ACTION_UP:
                    performClick();
                    applyAt(normalizedX(event.getX()), normalizedY(event.getY()));
                    return true;
                default: return true;
            }
        }

        @Override public boolean performClick() { return super.performClick(); }

        void drawPieces() {
            if (getWidth() == 0 || getHeight() == 0) return;
            while (getChildCount() > 1) removeViewAt(getChildCount() - 1);
            int size = host.dp(TOKEN_DP);
            for (int i = 0; i < pieces.size(); i++) {
                int index = i;
                Piece piece = pieces.get(i);
                FrameLayout item = new FrameLayout(host);
                item.setContentDescription(pieceName(i) + (selected == i ? ", đang chọn" : ""));
                if (selected == i) item.setBackground(host.background(0xAAFDF8DD, 16, MainActivity.PLUM));
                item.addView(new DaylightArt.PropIcon(host, piece.prop), new LayoutParams(-1, -1));
                host.makePressable(item, () -> {
                    if (palette >= 0 || moving) {
                        applyAt(piece.x, piece.y);
                        return;
                    }
                    selected = selected == index ? -1 : index;
                    refresh(selected < 0 ? "Chọn món đồ hoặc đặt thêm đồ." : "Đã chọn " + pieceName(index) + ".");
                });
                enableDrag(item, new DragPiece(i, true));
                LayoutParams bounds = new LayoutParams(size, size);
                bounds.leftMargin = Math.round(piece.x * Math.max(0, getWidth() - size));
                bounds.topMargin = Math.round(piece.y * Math.max(0, getHeight() - size));
                addView(item, bounds);
            }
        }

        private float normalizedX(float tap) { return normalized(tap, getWidth()); }
        private float normalizedY(float tap) { return normalized(tap, getHeight()); }
        private float normalized(float tap, int dimension) {
            int icon = host.dp(TOKEN_DP);
            return Math.max(0f, Math.min(1f, (tap - icon / 2f) / Math.max(1, dimension - icon)));
        }
    }

    private static final class Piece {
        final int prop;
        final float x, y;
        Piece(int prop, float x, float y) { this.prop = prop; this.x = x; this.y = y; }
    }

    private static final class DragPiece {
        final int id;
        final boolean placed;
        DragPiece(int id, boolean placed) { this.id = id; this.placed = placed; }
    }
}
