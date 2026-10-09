package krypton.zx.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings file: .minecraft/config/kryptonx.json (created on first launch)
 *   enabled             - true/false (also toggled in game with the toggle key)
 *   primarySlot         - hotbar slot of your spear, 1-9
 *   secondarySlot       - hotbar slot of the item you normally hold, 1-9
 *   chain               - true: HOLD the trigger key to repeat the lunge on a timer
 *   chainIntervalTicks  - ticks between lunges while holding (20 ticks = 1 second)
 *   scrollTrigger       - true: scrolling the mouse wheel up OR down fires a lunge
 *                         (and the hotbar no longer scrolls while the mod is ON)
 */
public class LungeSettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("kryptonx.json");

	public boolean enabled = true;
	public int primarySlot = 4;
	public int secondarySlot = 1;
	public boolean chain = true;
	public int chainIntervalTicks = 12;
	public boolean scrollTrigger = true;

	private static int clampInt(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	public void clamp() {
		primarySlot = clampInt(primarySlot, 1, 9);
		secondarySlot = clampInt(secondarySlot, 1, 9);
		chainIntervalTicks = clampInt(chainIntervalTicks, 2, 200);
	}

	public static LungeSettings load() {
		LungeSettings settings;
		try {
			if (Files.exists(PATH)) {
				settings = GSON.fromJson(Files.readString(PATH), LungeSettings.class);
				if (settings == null) settings = new LungeSettings();
			} else {
				settings = new LungeSettings();
			}
		} catch (Exception e) {
			settings = new LungeSettings();
		}
		settings.save();
		return settings;
	}

	public void save() {
		clamp();
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (Exception ignored) {
		}
	}
}
