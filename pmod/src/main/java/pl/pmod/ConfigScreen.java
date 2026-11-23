package pl.pmod;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.literal("PMod"));
        this.parent = parent;
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private void toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set, int x, int y) {
        addDrawableChild(ButtonWidget.builder(label(name, get.get()), btn -> {
            set.accept(!get.get());
            btn.setMessage(label(name, get.get()));
        }).dimensions(x, y, 150, 20).build());
    }

    @Override
    protected void init() {
        Config c = Config.get();
        int lx = width / 2 - 155, rx = width / 2 + 5, y0 = height / 4 - 10, dy = 24;

        // lewa kolumna - nametagi
        toggle("Nametagi", () -> c.nametags, v -> c.nametags = v, lx, y0);
        toggle("Serca", () -> c.showHearts, v -> c.showHearts = v, lx, y0 + dy);
        toggle("Set armora", () -> c.showSet, v -> c.showSet = v, lx, y0 + dy * 2);
        toggle("Enchanty armora", () -> c.showEnchants, v -> c.showEnchants = v, lx, y0 + dy * 3);
        toggle("Mieczyk/bron", () -> c.showWeapon, v -> c.showWeapon = v, lx, y0 + dy * 4);
        addDrawableChild(new SliderWidget(lx, y0 + dy * 5, 150, 20,
                Text.literal("Rozmiar tagow: " + String.format("%.1f", c.tagScale)),
                (c.tagScale - 0.5) / 2.5) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Rozmiar tagow: " + String.format("%.1f", 0.5 + value * 2.5)));
            }
            @Override protected void applyValue() { c.tagScale = (float) (0.5 + value * 2.5); }
        });

        // prawa kolumna - reszta
        toggle("Tracery", () -> c.tracers, v -> c.tracers = v, rx, y0);
        addDrawableChild(new SliderWidget(rx, y0 + dy, 150, 20,
                Text.literal("Zasieg tracerow: " + c.range), (c.range - 10) / 190.0) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Zasieg tracerow: " + (10 + (int) Math.round(value * 190))));
            }
            @Override protected void applyValue() { c.range = 10 + (int) Math.round(MathHelper.clamp(value, 0, 1) * 190); }
        });
        toggle("NoPush", () -> c.noPush, v -> c.noPush = v, rx, y0 + dy * 2);
        toggle("FastPlace", () -> c.fastPlace, v -> c.fastPlace = v, rx, y0 + dy * 3);

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(width / 2 - 75, y0 + dy * 7, 150, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, "PMod - ustawienia (P)", width / 2, height / 4 - 30, 0xFFFFFF);
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
