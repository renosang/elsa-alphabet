package vn.vuongquoctuyet.hocchu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

final class DaylightArt {
    static final int[] SCENES = {R.drawable.daylight_village, R.drawable.daylight_forest, R.drawable.daylight_park};
    static final int[] SCENE_COLORS = {0xFFD5EEFE, 0xFFDAF0BA, 0xFFFFE7AB};
    static final String[] SCENE_NAMES = {"Làng Tuyết", "Rừng Cầu Vồng", "Công viên"};
    static final String[] PROPS = {"Cây thông", "Người tuyết", "Bông hoa", "Ghế dài", "Thỏ con", "Đèn lồng",
        "Ngôi sao", "Hộp quà", "Cầu vồng", "Vương miện", "Chong chóng", "Lâu đài nhỏ"};
    static final int BASIC_PROPS = 6;

    interface Motion { boolean enabled(); }

    private static Bitmap princessAtlas, propAtlas;

    private static Bitmap readAtlas(Context context, int id) {
        Bitmap image = BitmapFactory.decodeResource(context.getResources(), id);
        image.setHasMipMap(true);
        return image;
    }

    static final class Princess extends View {
        static final int IDLE = 0, TALK = 1, POINT = 2, CHEER = 4, ENCOURAGE = 5;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Rect source = new Rect();
        private final RectF target = new RectF();
        private final Motion motion;
        private int pose;
        private long started;
        private long expires;

        Princess(Context context, Motion motion) {
            super(context);
            this.motion = motion;
            if (princessAtlas == null)
                princessAtlas = readAtlas(context, R.drawable.princess_frames);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void pose(int next, long duration) {
            if (next == pose && duration == 0) return;
            pose = next;
            started = SystemClock.uptimeMillis();
            expires = duration == 0 ? 0 : started + duration;
            invalidate();
        }

        void speaking() {
            if (pose == IDLE || pose == TALK) pose(TALK, 0);
        }

        @Override protected void onDraw(Canvas canvas) {
            long now = SystemClock.uptimeMillis();
            if (expires != 0 && now >= expires) {
                pose = IDLE;
                expires = 0;
            }
            boolean animate = motion.enabled() && isShown() && hasWindowFocus();
            long time = now - started;
            int frame = 0;
            if (pose == TALK) frame = !animate || time % 560 < 280 ? 2 : 3;
            else if (pose == POINT) frame = 4;
            else if (pose == CHEER) frame = !animate || time % 480 < 240 ? 5 : 6;
            else if (pose == ENCOURAGE) frame = 3;
            else if (animate && now % 4720 > 4550) frame = 1;
            float shift = animate ? (float) Math.sin(time / (pose == CHEER ? 125.0 : 440.0)) : 0;
            int frameWidth = princessAtlas.getWidth() / 4;
            int frameHeight = princessAtlas.getHeight() / 2;
            float scale = Math.min(getWidth() / (float) frameWidth, getHeight() / (float) frameHeight);
            float width = scale * frameWidth, height = scale * frameHeight;
            float left = (getWidth() - width) / 2, top = (getHeight() - height) / 2;
            paint.setColor(0x3843639C);
            canvas.drawOval(left + width * .28f, top + height * .875f,
                left + width * .73f, top + height * .94f, paint);
            float bob = height * .011f * shift;
            target.set(left, top + bob, left + width, top + height + bob);
            int x = frame % 4 * frameWidth, y = frame / 4 * frameHeight;
            source.set(x, y, x + frameWidth, y + frameHeight);
            paint.setColor(android.graphics.Color.WHITE);
            canvas.drawBitmap(princessAtlas, source, target, paint);
            if (animate) postInvalidateDelayed(70);
            else if (expires != 0) postInvalidateDelayed(Math.max(1, expires - now));
        }

        @Override public void onWindowFocusChanged(boolean focused) {
            super.onWindowFocusChanged(focused);
            if (focused) invalidate();
        }
    }

    static final class PropIcon extends View {
        private final Rect source;
        private final RectF target = new RectF();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        PropIcon(Context context, int prop) {
            super(context);
            if (propAtlas == null)
                propAtlas = readAtlas(context, R.drawable.prop_frames);
            int tileSize = propAtlas.getWidth() / 4;
            int x = prop % 4 * tileSize, y = prop / 4 * tileSize;
            source = new Rect(x, y, x + tileSize, y + tileSize);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(Canvas canvas) {
            float size = Math.min(getWidth(), getHeight());
            float x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            paint.setColor(0x3348676B);
            canvas.drawOval(x + size * .20f, y + size * .76f, x + size * .80f, y + size * .88f, paint);
            target.set(x, y, x + size, y + size);
            paint.setColor(android.graphics.Color.WHITE);
            canvas.drawBitmap(propAtlas, source, target, paint);
        }
    }
}
