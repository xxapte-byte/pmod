package pl.pmod;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;

public class Renderer {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config c = Config.get();
        if (mc.world == null || mc.player == null || ctx.matrixStack() == null) return;
        if (!c.tracers && !c.nametags) return;

        float td = ctx.tickCounter().getTickDelta(false);
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        MatrixStack ms = ctx.matrixStack();

        List<AbstractClientPlayerEntity> players = new ArrayList<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !p.isSpectator()) players.add(p);
        }

        if (c.tracers) drawTracers(mc, c, players, cam, cp, ms, td);
        if (c.nametags) drawTags(mc, c, players, cam, cp, ms, td);
    }

    // ---------------- TRACERS ----------------
    private static void drawTracers(MinecraftClient mc, Config c, List<AbstractClientPlayerEntity> players,
                                    Camera cam, Vec3d cp, MatrixStack ms, float td) {
        Matrix4f m = ms.peek().getPositionMatrix();
        Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw()).multiply(0.3);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0f);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int count = 0;
        for (AbstractClientPlayerEntity p : players) {
            float dist = mc.player.distanceTo(p);
            if (dist > c.range) continue;
            Vec3d pos = p.getLerpedPos(td);
            float ex = (float) (pos.x - cp.x), ey = (float) (pos.y + p.getHeight() / 2 - cp.y), ez = (float) (pos.z - cp.z);
            float t = Math.min(1f, dist / c.range);
            int r = (int) (255 * (1 - t)), g = (int) (255 * t);
            b.vertex(m, (float) look.x, (float) look.y, (float) look.z).color(r, g, 60, 255);
            b.vertex(m, ex, ey, ez).color(r, g, 60, 255);
            count++;
        }
        if (count > 0) BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.enableDepthTest();
    }

    // ---------------- NAMETAGS ----------------
    private static void drawTags(MinecraftClient mc, Config c, List<AbstractClientPlayerEntity> players,
                                 Camera cam, Vec3d cp, MatrixStack ms, float td) {
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate vc = mc.getBufferBuilders().getEntityVertexConsumers();

        for (AbstractClientPlayerEntity p : players) {
            List<Text> lines = buildLines(p, c);
            if (lines.isEmpty()) continue;

            Vec3d pos = p.getLerpedPos(td);
            double dx = pos.x - cp.x, dy = pos.y + p.getHeight() + 0.75 - cp.y, dz = pos.z - cp.z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float s = 0.025f * c.tagScale * Math.max(1f, dist / 10f);

            ms.push();
            ms.translate(dx, dy, dz);
            ms.multiply(cam.getRotation());
            ms.scale(-s, -s, s);
            Matrix4f m = ms.peek().getPositionMatrix();
            int n = lines.size();
            for (int i = 0; i < n; i++) {
                Text t = lines.get(i);
                float x = -tr.getWidth(t) / 2f;
                float y = (i - n + 1) * 10f;
                tr.draw(t, x, y, 0xFFFFFFFF, false, m, vc,
                        TextRenderer.TextLayerType.SEE_THROUGH, 0x55000000, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            }
            ms.pop();
        }
        vc.draw();
    }

    private static List<Text> buildLines(AbstractClientPlayerEntity p, Config c) {
        List<Text> out = new ArrayList<>();

        // 1) nick + serca
        Text name = Text.literal(p.getName().getString()).formatted(Formatting.WHITE);
        if (c.showHearts) {
            float hearts = (p.getHealth() + p.getAbsorptionAmount()) / 2f;
            name = Text.empty().append(name)
                    .append(Text.literal(String.format(" %.1f\u2665", hearts)).formatted(Formatting.RED));
        }
        out.add(name);

        // 2) set
        if (c.showSet) out.add(Text.literal(setName(p)).formatted(Formatting.AQUA));

        // 3) enchanty armora
        if (c.showEnchants) {
            Map<String, Integer> ench = new LinkedHashMap<>();
            for (EquipmentSlot slot : ARMOR) collect(p.getEquippedStack(slot), ench);
            if (!ench.isEmpty()) out.add(Text.literal(format(ench)).formatted(Formatting.LIGHT_PURPLE));
        }

        // 4) bron w rece
        if (c.showWeapon) {
            ItemStack w = p.getMainHandStack();
            if (!w.isEmpty()) {
                Map<String, Integer> ench = new LinkedHashMap<>();
                collect(w, ench);
                String txt = w.getItem().getName().getString();
                if (!ench.isEmpty()) txt += " [" + format(ench) + "]";
                out.add(Text.literal(txt).formatted(Formatting.YELLOW));
            }
        }
        return out;
    }

    private static String setName(AbstractClientPlayerEntity p) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = p.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            String path = Registries.ITEM.getId(s.getItem()).getPath();
            String mat = path.contains("_") ? path.substring(0, path.indexOf('_')) : path;
            counts.merge(mat, 1, Integer::sum);
        }
        if (counts.isEmpty()) return "Brak armora";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(cap(e.getKey())).append(' ').append(e.getValue()).append("/4");
        }
        return sb.toString();
    }

    private static void collect(ItemStack s, Map<String, Integer> out) {
        if (s.isEmpty()) return;
        ItemEnchantmentsComponent e = EnchantmentHelper.getEnchantments(s);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> en : e.getEnchantmentEntries()) {
            String key = en.getKey().getKey().map(k -> k.getValue().getPath()).orElse("?");
            out.merge(key, en.getIntValue(), Math::max);
        }
    }

    private static String format(Map<String, Integer> ench) {
        StringJoiner j = new StringJoiner(", ");
        for (Map.Entry<String, Integer> e : ench.entrySet()) j.add(shortName(e.getKey()) + " " + roman(e.getValue()));
        return j.toString();
    }

    private static String shortName(String k) {
        return switch (k) {
            case "protection" -> "Prot";
            case "fire_protection" -> "FireProt";
            case "blast_protection" -> "BlastProt";
            case "projectile_protection" -> "ProjProt";
            case "unbreaking" -> "Unb";
            case "mending" -> "Mend";
            case "thorns" -> "Thorns";
            case "feather_falling" -> "FF";
            case "depth_strider" -> "DS";
            case "respiration" -> "Resp";
            case "aqua_affinity" -> "Aqua";
            case "soul_speed" -> "SoulSpd";
            case "swift_sneak" -> "Swift";
            case "sharpness" -> "Sharp";
            case "fire_aspect" -> "FireAsp";
            case "knockback" -> "KB";
            case "looting" -> "Loot";
            case "sweeping_edge" -> "Sweep";
            case "bane_of_arthropods" -> "Bane";
            case "binding_curse" -> "Bind";
            case "vanishing_curse" -> "Van";
            default -> cap(k.replace('_', ' '));
        };
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
