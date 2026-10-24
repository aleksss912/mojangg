package com.example.dripclient;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public void toggle() {
        enabled = !enabled;
    }

    /** Wywolywane co klatke (HUD), gdy modul jest wlaczony. */
    public void onHud(DrawContext ctx, float delta) {
    }
}
