package dev.nivek.utility.gui;

/** Tempo entre quadros, usado nas animações suaves. */
public final class Ui {
    private static long last = System.nanoTime();
    public static float dt = 0.016f;

    private Ui() {
    }

    public static void tick() {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
    }

    public static float approach(float cur, float target) {
        return cur + (target - cur) * Math.min(1f, dt * 16f);
    }
}
