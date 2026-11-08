package dev.nivek.utility.util;

public final class Colors {
    public static final int PANEL = 0xFF171717;
    public static final int SIDEBAR = 0xFF0F0F0F;
    public static final int ROW = 0xFF222222;
    public static final int ROW_HOVER = 0xFF2E2E2E;
    public static final int ACCENT = 0xFFD61F2C;
    public static final int ACCENT_DARK = 0xFF7A1018;
    public static final int TEXT = 0xFFEDEDED;
    public static final int MUTED = 0xFF8A8A8A;
    public static final int GOOD = 0xFF55FF55;
    public static final int WARN = 0xFFFFAA00;
    public static final int BAD = 0xFFFF5555;

    private Colors() {
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** 1.0 = verde, 0.0 = vermelho. */
    public static int health(float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return fraction > 0.5f ? lerp(WARN, GOOD, (fraction - 0.5f) * 2f) | 0xFF000000
                : lerp(BAD, WARN, fraction * 2f) | 0xFF000000;
    }
}
