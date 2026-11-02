package pl.alek.alekclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/** Menu ustawien moda (klawisz: Prawy Ctrl). Zmiany zapisuja sie przy zamknieciu. */
public class AlekSettingsScreen extends Screen {
    private final Screen parent;
    private final List<Label> labels = new ArrayList<>();

    private record Label(String text, int x, int y) {
    }

    public AlekSettingsScreen(Screen parent) {
        super(Text.literal("Alek Client - ustawienia"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        labels.clear();
        AlekConfig c = AlekClient.config;

        int colW = 150;
        int left = this.width / 2 - colW - 5;
        int right = this.width / 2 + 5;
        int step = 24;
        int y = 36;

        // ---- lewa kolumna: funkcje ----
        addDrawableChild(new ConfigSlider(left, y, colW, 20, 2, 100, c.zoomFactor,
                v -> Text.literal("Zoom domyślny: x" + Math.round(v)),
                v -> c.zoomFactor = Math.round(v)));
        y += step;
        addDrawableChild(toggle(left, y, colW, "Własny FOV", () -> c.fovEnabled, v -> c.fovEnabled = v));
        y += step;
        addDrawableChild(new ConfigSlider(left, y, colW, 20, 30, 179, c.fov,
                v -> Text.literal("FOV: " + Math.round(v)),
                v -> c.fov = (int) Math.round(v)));
        y += step;
        addDrawableChild(toggle(left, y, colW, "Kolorowe hitboxy", () -> c.hitboxesEnabled, v -> c.hitboxesEnabled = v));
        y += step;
        addDrawableChild(toggle(left, y, colW, "Własny celownik", () -> c.customCrosshair, v -> c.customCrosshair = v));
        y += step;
        addDrawableChild(toggle(left, y, colW, "Tabelka z klawiszami", () -> c.keyOverlay, v -> c.keyOverlay = v));
        y += step;
        addDrawableChild(new ConfigSlider(left, y, colW, 20, 0.5, 2.0, c.keyOverlayScale,
                v -> Text.literal("Rozmiar tabelki: " + Math.round(v * 100) + "%"),
                v -> c.keyOverlayScale = Math.round(v * 20) / 20.0));

        // ---- prawa kolumna: kolory ----
        labels.add(new Label("Kolory (HEX, np. #FF69B4)", right, 26));
        int ry = 36;
        colorRow(right, ry, "Gracze", () -> c.playerHitboxColor, v -> c.playerHitboxColor = v);
        ry += step;
        colorRow(right, ry, "Zwierzęta", () -> c.animalHitboxColor, v -> c.animalHitboxColor = v);
        ry += step;
        colorRow(right, ry, "Moby", () -> c.mobHitboxColor, v -> c.mobHitboxColor = v);
        ry += step;
        colorRow(right, ry, "Przedmioty", () -> c.itemHitboxColor, v -> c.itemHitboxColor = v);
        ry += step;
        colorRow(right, ry, "Reszta", () -> c.otherHitboxColor, v -> c.otherHitboxColor = v);
        ry += step;
        colorRow(right, ry, "Celownik", () -> c.crosshairColor, v -> c.crosshairColor = v);
        ry += step;
        colorRow(right, ry, "Cel. na gracza", () -> c.crosshairTargetColor, v -> c.crosshairTargetColor = v);

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    private ButtonWidget toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return ButtonWidget.builder(toggleText(label, get.getAsBoolean()), b -> {
            boolean now = !get.getAsBoolean();
            set.accept(now);
            b.setMessage(toggleText(label, now));
        }).dimensions(x, y, w, 20).build();
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "WŁ." : "WYŁ."));
    }

    private void colorRow(int x, int y, String label, Supplier<String> get, Consumer<String> set) {
        labels.add(new Label(label, x, y + 6));
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x + 80, y, 70, 20, Text.literal(label));
        field.setMaxLength(7);
        field.setText(get.get());
        field.setChangedListener(s -> {
            if (s.matches("#[0-9a-fA-F]{6}")) {
                set.accept(s);
                AlekClient.config.refresh();
            }
        });
        addDrawableChild(field);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFF69B4);
        for (Label l : labels) {
            ctx.drawTextWithShadow(this.textRenderer, l.text(), l.x(), l.y(), 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        AlekClient.config.refresh();
        AlekClient.config.save();
        this.client.setScreen(this.parent);
    }

    /** Suwak z wlasnym zakresem i podpisem. */
    private static class ConfigSlider extends SliderWidget {
        private final double min;
        private final double max;
        private final DoubleFunction<Text> label;
        private final DoubleConsumer onChange;

        ConfigSlider(int x, int y, int width, int height, double min, double max, double current,
                     DoubleFunction<Text> label, DoubleConsumer onChange) {
            super(x, y, width, height, label.apply(current), (current - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.onChange = onChange;
        }

        private double actual() {
            return min + (max - min) * this.value;
        }

        @Override
        protected void updateMessage() {
            if (label != null) {
                setMessage(label.apply(actual()));
            }
        }

        @Override
        protected void applyValue() {
            onChange.accept(actual());
        }
    }
}
