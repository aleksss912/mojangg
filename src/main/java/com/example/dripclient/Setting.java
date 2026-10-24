package com.example.dripclient;

public abstract class Setting {
    public final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public abstract String save();

    public abstract void load(String s);

    public static class Bool extends Setting {
        public boolean value;

        public Bool(String name, boolean def) {
            super(name);
            this.value = def;
        }

        @Override
        public String save() {
            return String.valueOf(value);
        }

        @Override
        public void load(String s) {
            value = Boolean.parseBoolean(s);
        }
    }

    public static class Num extends Setting {
        public double value;
        public final double min, max, step;

        public Num(String name, double def, double min, double max, double step) {
            super(name);
            this.value = def;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public void set(double v) {
            v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }

        @Override
        public String save() {
            return String.valueOf(value);
        }

        @Override
        public void load(String s) {
            try {
                set(Double.parseDouble(s));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
