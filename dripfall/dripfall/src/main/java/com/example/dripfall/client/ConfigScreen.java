package com.example.dripfall.client;

import com.example.dripfall.SettingsPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/** Menu moda z dwiema zakladkami: Dripstone (serwer/solo) i Klapy (dziala wszedzie). */
public class ConfigScreen extends Screen {

    // --- kolory (ARGB) ---
    private static final int ACCENT = 0xFFD9923F;
    private static final int ACCENT_DARK = 0xFF8A5A22;
    private static final int PANEL = 0xF0181A20;
    private static final int TRACK = 0xFF2A2D36;
    private static final int BTN = 0xFF2F333D;
    private static final int BTN_HOVER = 0xFF42475A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A4B0;
    private static final int ON = 0xFF4CAF50;
    private static final int OFF = 0xFF5A5E6B;

    private static final float SPEED_MIN = 0.5f, SPEED_MAX = 10.0f;
    private static final float RANGE_MIN = 1.0f, RANGE_MAX = 4.5f;

    private static final String[] PRESET_NAMES = {"Wolno", "Normalnie", "Szybko", "MEGA"};
    private static final float[] SPEED_PRESETS = {1.0f, 3.0f, 6.0f, 10.0f};
    private static final int[] FLICK_PRESETS = {10, 5, 2, 1};

    private static final int PW = 300;
    private static final int PH = 302;

    private record Rect(int x, int y, int w, int h) {
        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;

    // wartosci - dripstone
    private boolean enabled;
    private float speed;
    private int radius;
    private int interval;
    private boolean onlyOpen;

    // wartosci - klapy
    private boolean flickEnabled;
    private int flickInterval;
    private float flickRange;

    private int tab = 0; // 0 = dripstone, 1 = klapy

    // uklad
    private int px, py;
    private Rect tabDrip, tabFlick;
    private Rect modToggle, openToggle, speedSlider, radiusSlider, intervalSlider;
    private Rect flickToggle, flickIntSlider, flickRangeSlider;
    private Rect btnReset, btnDone;
    private final Rect[] presets = new Rect[4];

    // 0 predkosc, 1 zasieg, 2 odstep, 3 klapy: odstep, 4 klapy: zasieg
    private int dragging = -1;

    public ConfigScreen(Screen parent) {
        super(Text.literal("DripFall"));
        this.parent = parent;
        SettingsPayload c = DripFallClient.current;
        enabled = c.enabled();
        speed = c.speed();
        radius = c.radius();
        interval = c.interval();
        onlyOpen = c.onlyOpen();
        flickEnabled = DripFallClient.flickEnabled;
        flickInterval = DripFallClient.flickInterval;
        flickRange = DripFallClient.flickRange;
    }

    @Override
    protected void init() {
        px = width / 2 - PW / 2;
        py = Math.max(4, (height - PH) / 2);
        int cx = px + 16;
        int cw = PW - 32;
        int gap = 6;

        int halfTab = (cw - gap) / 2;
        tabDrip = new Rect(cx, py + 40, halfTab, 18);
        tabFlick = new Rect(cx + halfTab + gap, py + 40, halfTab, 18);

        modToggle = new Rect(cx, py + 70, cw, 18);
        speedSlider = new Rect(cx, py + 98, cw, 28);
        radiusSlider = new Rect(cx, py + 130, cw, 28);
        intervalSlider = new Rect(cx, py + 162, cw, 28);
        openToggle = new Rect(cx, py + 198, cw, 18);

        flickToggle = new Rect(cx, py + 70, cw, 18);
        flickIntSlider = new Rect(cx, py + 98, cw, 28);
        flickRangeSlider = new Rect(cx, py + 130, cw, 28);

        int bw = (cw - gap * 3) / 4;
        for (int i = 0; i < 4; i++) {
            presets[i] = new Rect(cx + i * (bw + gap), py + 238, bw, 18);
        }

        int half = (cw - gap) / 2;
        btnReset = new Rect(cx, py + 268, half, 20);
        btnDone = new Rect(cx + half + gap, py + 268, half, 20);
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.fill(px - 3, py - 3, px + PW + 3, py + PH + 3, 0x66000000);
        ctx.fill(px, py, px + PW, py + PH, PANEL);
        ctx.drawBorder(px, py, PW, PH, ACCENT_DARK);

        ctx.fillGradient(px + 1, py + 1, px + PW - 1, py + 34, ACCENT, ACCENT_DARK);
        ctx.fill(px + 1, py + 34, px + PW - 1, py + 35, 0xFF000000);
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float) (px + 14), (float) (py + 8), 0f);
        ctx.getMatrices().scale(1.6f, 1.6f, 1.0f);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Mojang"), 0, 0, TEXT);
        ctx.getMatrices().pop();
        String sub = "dripstone i szybkie klapy";
        ctx.drawTextWithShadow(textRenderer, Text.literal(sub),
                px + PW - 14 - textRenderer.getWidth(sub), py + 14, 0xFFFFE9C8);

        drawButton(ctx, tabDrip, "Dripstone", mouseX, mouseY, tab == 0);
        drawButton(ctx, tabFlick, "Klapy", mouseX, mouseY, tab == 1);

        if (tab == 0) {
            drawToggle(ctx, modToggle, "Mod (solo / serwer z modem)", enabled, mouseX, mouseY);
            drawSlider(ctx, speedSlider, "Prędkość spadania",
                    String.format(Locale.ROOT, "%.1f bl/tick", speed),
                    (speed - SPEED_MIN) / (SPEED_MAX - SPEED_MIN), mouseX, mouseY, dragging == 0);
            drawSlider(ctx, radiusSlider, "Zasięg wokół Ciebie",
                    radius + " bloków",
                    (radius - 2) / 14.0f, mouseX, mouseY, dragging == 1);
            drawSlider(ctx, intervalSlider, "Częstotliwość",
                    String.format(Locale.ROOT, "co %d t (%.1f/s)", interval, 20.0f / interval),
                    (40 - interval) / 39.0f, mouseX, mouseY, dragging == 2);
            drawToggle(ctx, openToggle, "Tylko otwarte klapy", onlyOpen, mouseX, mouseY);

            ctx.drawTextWithShadow(textRenderer, Text.literal("Szybkie ustawienia prędkości"),
                    px + 16, py + 226, TEXT_DIM);
            for (int i = 0; i < 4; i++) {
                boolean active = Math.abs(speed - SPEED_PRESETS[i]) < 0.05f;
                drawButton(ctx, presets[i], PRESET_NAMES[i], mouseX, mouseY, active);
            }
        } else {
            drawToggle(ctx, flickToggle, "Szybkie klapy (klawisz J)", flickEnabled, mouseX, mouseY);
            drawSlider(ctx, flickIntSlider, "Szybkość klapy",
                    String.format(Locale.ROOT, "co %d t (%.1f/s)", flickInterval, 20.0f / flickInterval),
                    (20 - flickInterval) / 19.0f, mouseX, mouseY, dragging == 3);
            drawSlider(ctx, flickRangeSlider, "Zasięg klap",
                    String.format(Locale.ROOT, "%.1f bloku", flickRange),
                    (flickRange - RANGE_MIN) / (RANGE_MAX - RANGE_MIN), mouseX, mouseY, dragging == 4);

            String[] note = {
                    "Klapy w zasięgu otwierają się i zamykają bez przerwy.",
                    "Działa na każdym serwerze, bo to tylko klient.",
                    "Zasięg gry to ok. 4,5 bloku, dalej serwer odrzuci.",
                    "Antycheat może to wykryć. Używaj na własne ryzyko."
            };
            for (int i = 0; i < note.length; i++) {
                ctx.drawTextWithShadow(textRenderer, Text.literal(note[i]), px + 16, py + 170 + i * 12, TEXT_DIM);
            }

            ctx.drawTextWithShadow(textRenderer, Text.literal("Szybkie ustawienia szybkości klap"),
                    px + 16, py + 226, TEXT_DIM);
            for (int i = 0; i < 4; i++) {
                drawButton(ctx, presets[i], PRESET_NAMES[i], mouseX, mouseY, flickInterval == FLICK_PRESETS[i]);
            }
        }

        drawButton(ctx, btnReset, "Reset", mouseX, mouseY, false);
        drawButton(ctx, btnDone, "Gotowe", mouseX, mouseY, true);
    }

    private void drawToggle(DrawContext ctx, Rect r, String label, boolean on, int mx, int my) {
        boolean hover = r.hit(mx, my);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), r.x(), r.y() + 5, hover ? TEXT : 0xFFE0E2EA);

        int tw = 34, th = 14;
        int tx = r.x() + r.w() - tw;
        int ty = r.y() + 2;
        ctx.fill(tx, ty, tx + tw, ty + th, on ? ON : OFF);
        ctx.drawBorder(tx, ty, tw, th, 0xFF000000);
        int kx = on ? tx + tw - th + 2 : tx + 2;
        ctx.fill(kx, ty + 2, kx + th - 4, ty + th - 2, TEXT);

        String state = on ? "WŁ." : "WYŁ.";
        ctx.drawTextWithShadow(textRenderer, Text.literal(state),
                tx - 8 - textRenderer.getWidth(state), r.y() + 5, on ? ON : TEXT_DIM);
    }

    private void drawSlider(DrawContext ctx, Rect r, String label, String value, float t,
                            int mx, int my, boolean active) {
        t = MathHelper.clamp(t, 0f, 1f);
        boolean hover = r.hit(mx, my) || active;
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), r.x(), r.y(), 0xFFE0E2EA);
        ctx.drawTextWithShadow(textRenderer, Text.literal(value),
                r.x() + r.w() - textRenderer.getWidth(value), r.y(), ACCENT);

        int ty = r.y() + 16;
        ctx.fill(r.x(), ty, r.x() + r.w(), ty + 6, TRACK);
        int fw = (int) (r.w() * t);
        ctx.fill(r.x(), ty, r.x() + fw, ty + 6, ACCENT);
        ctx.drawBorder(r.x(), ty, r.w(), 6, 0xFF000000);

        int kx = r.x() + fw;
        ctx.fill(kx - 3, ty - 3, kx + 3, ty + 9, hover ? 0xFFFFE9C8 : TEXT);
        ctx.drawBorder(kx - 3, ty - 3, 6, 12, 0xFF000000);
    }

    private void drawButton(DrawContext ctx, Rect r, String label, int mx, int my, boolean highlighted) {
        boolean hover = r.hit(mx, my);
        int bg = highlighted ? ACCENT_DARK : (hover ? BTN_HOVER : BTN);
        if (highlighted && hover) bg = ACCENT;
        ctx.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), bg);
        ctx.drawBorder(r.x(), r.y(), r.w(), r.h(), highlighted ? ACCENT : 0xFF000000);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label),
                r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2, TEXT);
    }

    // ------------------------------------------------------------------ myszka

    private void click() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (tabDrip.hit(mx, my)) { tab = 0; click(); return true; }
            if (tabFlick.hit(mx, my)) { tab = 1; click(); return true; }

            if (tab == 0) {
                if (modToggle.hit(mx, my)) { enabled = !enabled; click(); return true; }
                if (openToggle.hit(mx, my)) { onlyOpen = !onlyOpen; click(); return true; }
                if (speedSlider.hit(mx, my)) { dragging = 0; setSlider(0, mx); return true; }
                if (radiusSlider.hit(mx, my)) { dragging = 1; setSlider(1, mx); return true; }
                if (intervalSlider.hit(mx, my)) { dragging = 2; setSlider(2, mx); return true; }
                for (int i = 0; i < 4; i++) {
                    if (presets[i].hit(mx, my)) { speed = SPEED_PRESETS[i]; click(); return true; }
                }
            } else {
                if (flickToggle.hit(mx, my)) { flickEnabled = !flickEnabled; click(); return true; }
                if (flickIntSlider.hit(mx, my)) { dragging = 3; setSlider(3, mx); return true; }
                if (flickRangeSlider.hit(mx, my)) { dragging = 4; setSlider(4, mx); return true; }
                for (int i = 0; i < 4; i++) {
                    if (presets[i].hit(mx, my)) { flickInterval = FLICK_PRESETS[i]; click(); return true; }
                }
            }

            if (btnReset.hit(mx, my)) {
                if (tab == 0) {
                    SettingsPayload d = SettingsPayload.defaults();
                    speed = d.speed();
                    radius = d.radius();
                    interval = d.interval();
                    onlyOpen = d.onlyOpen();
                } else {
                    flickInterval = 4;
                    flickRange = 4.0f;
                }
                click();
                return true;
            }
            if (btnDone.hit(mx, my)) { click(); close(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) {
            setSlider(dragging, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = -1;
        return super.mouseReleased(mx, my, button);
    }

    private void setSlider(int which, double mx) {
        Rect r = switch (which) {
            case 0 -> speedSlider;
            case 1 -> radiusSlider;
            case 2 -> intervalSlider;
            case 3 -> flickIntSlider;
            default -> flickRangeSlider;
        };
        float t = MathHelper.clamp((float) ((mx - r.x()) / r.w()), 0f, 1f);
        switch (which) {
            case 0 -> speed = Math.round((SPEED_MIN + t * (SPEED_MAX - SPEED_MIN)) * 10f) / 10f;
            case 1 -> radius = 2 + Math.round(t * 14f);
            case 2 -> interval = 40 - Math.round(t * 39f);
            case 3 -> flickInterval = 20 - Math.round(t * 19f);
            case 4 -> flickRange = Math.round((RANGE_MIN + t * (RANGE_MAX - RANGE_MIN)) * 10f) / 10f;
        }
    }

    // ------------------------------------------------------------------ zamykanie

    @Override
    public void removed() {
        DripFallClient.current = new SettingsPayload(enabled, speed, radius, interval, onlyOpen).clamped();
        DripFallClient.flickEnabled = flickEnabled;
        DripFallClient.flickInterval = MathHelper.clamp(flickInterval, 1, 20);
        DripFallClient.flickRange = MathHelper.clamp(flickRange, RANGE_MIN, RANGE_MAX);
        DripFallClient.send();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
