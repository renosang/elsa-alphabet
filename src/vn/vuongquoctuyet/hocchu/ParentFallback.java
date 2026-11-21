package vn.vuongquoctuyet.hocchu;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ParentFallback {
    private final ParentArea host;
    private final ParentArea.Bridge bridge;
    private final StringBuilder digits = new StringBuilder();
    private LinearLayout content;

    ParentFallback(ParentArea host, ParentArea.Bridge bridge) {
        this.host = host;
        this.bridge = bridge;
    }

    void open() {
        digits.setLength(0);
        page("Góc phụ huynh");
        label("Đọc ba số bằng chữ, rồi bấm các phím theo thứ tự.", 17);
        TextView code = label(bridge.prompt(), 23);
        TextView entered = label("–    –    –", 22);
        TextView status = label("", 15);
        for (int row = 0; row < 4; row++) {
            LinearLayout group = new LinearLayout(host);
            content.addView(group);
            for (int col = 0; col < (row == 3 ? 2 : 3); col++) {
                String value = row == 3 ? (col == 0 ? "0" : "⌫") : String.valueOf(1 + row * 3 + col);
                Button number = control(value, () -> {
                    if ("⌫".equals(value)) {
                        if (digits.length() > 0) digits.deleteCharAt(digits.length() - 1);
                    } else digits.append(value);
                    entered.setText(digits + "    " + (digits.length() < 2 ? "–" : ""));
                    if (digits.length() != 3) return;
                    if (bridge.verify(digits.toString())) dashboard("");
                    else {
                        digits.setLength(0);
                        code.setText(bridge.prompt());
                        entered.setText("–    –    –");
                        status.setText("Chưa đúng. Mời nhập dãy số mới.");
                    }
                });
                group.addView(number, new LinearLayout.LayoutParams(0, -2, "⌫".equals(value) ? 2 : 1));
            }
        }
    }

    private void dashboard(String id) {
        page("Hồ sơ và tiến độ");
        try {
            JSONObject state = new JSONObject(bridge.snapshot(id));
            JSONArray children = state.getJSONArray("profiles");
            String selected = state.getString("selected");
            JSONObject current = null;
            for (int i = 0; i < children.length(); i++) {
                JSONObject child = children.getJSONObject(i);
                String childId = child.getString("id");
                if (selected.equals(childId)) current = child;
                content.addView(control(child.getString("name") + (selected.equals(childId) ? " ✓" : ""),
                    () -> dashboard(childId)));
            }
            if (current == null) return;
            String name = current.getString("name");
            int avatar = current.getInt("avatar");
            if (children.length() < state.getInt("max")) content.addView(control("+ Thêm hồ sơ", () -> name("", "", 4)));
            content.addView(control("Sửa hồ sơ", () -> name(selected, name, avatar)));
            if (!selected.equals(state.getString("active")))
                content.addView(control("Cho " + name + " học", () -> bridge.activate(selected)));
            label(state.getInt("letterTotal") + "/29 chữ đã học · " + state.getInt("gameTotal") + " lượt chơi", 18);
            JSONArray letters = state.getJSONArray("letters");
            StringBuilder known = new StringBuilder();
            for (int i = 0; i < letters.length(); i++) {
                JSONObject letter = letters.getJSONObject(i);
                if (letter.getBoolean("done")) known.append(letter.getString("label")).append("  ");
            }
            if (known.length() > 0) label(known.toString().trim(), 16);
            JSONObject history = state.getJSONObject("days");
            label("7 ngày gần nhất", 19);
            for (int i = 0; i < 7; i++) {
                java.util.Calendar day = java.util.Calendar.getInstance();
                day.add(java.util.Calendar.DAY_OF_MONTH, -i);
                String stamp = FamilyProfiles.dayKey(day.getTime());
                String date = new java.text.SimpleDateFormat("dd/MM", Lesson.VI).format(day.getTime());
                label(date + " · " + history.optInt("day_letters_" + stamp) + " bài chữ · "
                    + history.optInt("day_games_" + stamp) + " lượt chơi", 15);
            }
            label("Mục tiêu · " + state.getInt("goal") + " hoạt động/ngày", 17);
            content.addView(control("Đổi mục tiêu", () -> {
                String[] choices = new String[12];
                for (int i = 0; i < 12; i++) choices[i] = (i + 1) + " hoạt động";
                new AlertDialog.Builder(host).setTitle("Mục tiêu hằng ngày").setItems(choices, (dialog, index) -> {
                    bridge.setGoal(selected, index + 1);
                    dashboard(selected);
                }).show();
            }));
            String[] modes = {"system", "sensor", "portrait"};
            String[] titles = {"Theo điện thoại", "Tự xoay ngang, dọc", "Giữ màn hình dọc"};
            int chosen = java.util.Arrays.asList(modes).indexOf(state.optString("orientation", "system"));
            content.addView(control("Hướng màn hình · " + titles[Math.max(0, chosen)], () ->
                new AlertDialog.Builder(host).setTitle("Hướng màn hình").setItems(titles, (dialog, index) -> {
                    bridge.setOrientation(modes[index]);
                    dashboard(selected);
                }).show()));
            int seconds = state.optInt("studyDelay", 0);
            label(seconds > 0 ? "Tự chuyển bài · " + seconds + " giây" : "Tự chuyển bài · Tắt", 17);
            content.addView(control("Đổi thời gian tự chuyển", () -> {
                int[] delays = FamilyProfiles.STUDY_DELAYS;
                String[] options = new String[delays.length];
                for (int i = 0; i < delays.length; i++)
                    options[i] = delays[i] == 0 ? "Tắt" : delays[i] + " giây";
                new AlertDialog.Builder(host).setTitle("Đợi sau khi bé hoàn thành bài").setItems(options, (dialog, index) -> {
                    bridge.setStudyDelay(selected, delays[index]);
                    dashboard(selected);
                }).show();
            }));
        } catch (JSONException error) {
            label("Chưa đọc được tiến độ. Mời mở lại Góc phụ huynh.", 16);
        }
    }

    private void name(String id, String initial, int avatar) {
        EditText text = new EditText(host);
        text.setSingleLine(true);
        text.setHint("Tên gọi của bé");
        text.setText(initial);
        text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        AlertDialog dialog = new AlertDialog.Builder(host).setTitle(id.isEmpty() ? "Thêm hồ sơ" : "Sửa hồ sơ")
            .setView(text).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String result = bridge.saveProfile(id, text.getText().toString(), avatar);
            if (result.isEmpty()) text.setError("Mời nhập tên của bé.");
            else {
                dialog.dismiss();
                dashboard(result);
            }
        });
    }

    private void page(String title) {
        ScrollView scroll = new ScrollView(host);
        scroll.setBackgroundColor(0xFFF8F4FF);
        scroll.setFillViewport(true);
        content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content);
        host.showSafe(scroll);
        content.addView(control("‹  " + bridge.returnLabel(), bridge::close));
        label(title, 26);
    }

    private TextView label(String text, int size) {
        TextView label = new TextView(host);
        label.setPadding(dp(6), dp(7), dp(6), dp(7));
        label.setTextColor(0xFF4B317D);
        label.setTextSize(size);
        label.setText(text);
        content.addView(label);
        return label;
    }

    private Button control(String text, Runnable action) {
        Button button = new Button(host);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setText(text);
        button.setTextColor(0xFF4B317D);
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.WHITE, 0xFFF1E9FF});
        shape.setCornerRadius(dp(13));
        shape.setStroke(dp(1), 0xFFE0D3F2);
        button.setBackground(shape);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private int dp(int value) { return Math.round(host.getResources().getDisplayMetrics().density * value); }
}
