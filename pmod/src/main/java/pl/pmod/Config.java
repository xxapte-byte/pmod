package pl.pmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public boolean nametags = true;
    public boolean showHearts = true;
    public boolean showSet = true;
    public boolean showEnchants = true;
    public boolean showWeapon = true;
    public boolean tracers = true;
    public int range = 50;          // zasieg tracerow (kratki)
    public float tagScale = 1.0f;   // rozmiar nametagow
    public boolean noPush = true;
    public boolean fastPlace = true;

    private static Config INSTANCE = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pmod.json");

    public static Config get() { return INSTANCE; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) { e.printStackTrace(); }
        save();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(INSTANCE)); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
