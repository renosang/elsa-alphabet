package vn.vuongquoctuyet.hocchu;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

final class SafeContent {
    private SafeContent() {}

    static void attach(Activity activity, View content, int barColor, boolean includeKeyboard) {
        if (Build.VERSION.SDK_INT < 30) {
            activity.setContentView(content);
            return;
        }

        activity.getWindow().setDecorFitsSystemWindows(false);
        FrameLayout safeArea = new FrameLayout(activity);
        safeArea.setBackgroundColor(barColor);
        FrameLayout.LayoutParams frame = new FrameLayout.LayoutParams(-1, -1);
        safeArea.addView(content, frame);
        safeArea.setOnApplyWindowInsetsListener((view, insets) -> {
            int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
            android.graphics.Insets bars = insets.getInsetsIgnoringVisibility(types);
            int bottom = includeKeyboard ? Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom)
                : bars.bottom;
            if (frame.leftMargin != bars.left || frame.topMargin != bars.top
                    || frame.rightMargin != bars.right || frame.bottomMargin != bottom) {
                frame.setMargins(bars.left, bars.top, bars.right, bottom);
                content.setLayoutParams(frame);
            }
            return WindowInsets.CONSUMED;
        });
        activity.setContentView(safeArea);
        safeArea.requestApplyInsets();
    }
}
