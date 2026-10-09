package krypton.zx.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;

/**
 * Auto Swap Lunge.
 *
 * Hold your normal item. Press the trigger key (default G) and the mod:
 *   1. switches to your spear slot,
 *   2. presses Attack for you in the same tick (swap + lunge),
 *   3. switches back to your normal item on the next tick.
 * COMBO CHAIN: keep HOLDING the trigger key and it repeats every chainIntervalTicks.
 * SCROLL: scrolling the mouse wheel up or down also fires one lunge (scrollTrigger setting).
 * Toggle key (default H) turns the mod on/off. Both keys are in Options > Controls.
 */
public class LungeClient implements ClientModInitializer {
	private static LungeSettings config;
	private static KeyMapping triggerKey;
	private static KeyMapping toggleKey;
	private boolean swapped = false;
	private int chainCooldown = 0;
	private static volatile boolean scrollPending = false;

	@Override
	public void onInitializeClient() {
		config = LungeSettings.load();
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kryptonx", "main"));

		triggerKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.kryptonx.trigger", InputConstants.Type.KEYSYM, 71, category)); // G
		toggleKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.kryptonx.toggle", InputConstants.Type.KEYSYM, 72, category)); // H

		// Start of the tick runs before the game handles input, so the attack we
		// trigger below is processed in this same tick.
		ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
	}

	/** Called by MouseHandlerMixin. Returns true when the scroll was used as a trigger. */
	public static boolean handleScroll(double yOffset) {
		Minecraft client = Minecraft.getInstance();
		if (config == null || !config.enabled || !config.scrollTrigger) return false;
		if (client.player == null || client.screen != null || yOffset == 0.0) return false;
		scrollPending = true;
		return true;
	}

	private void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;
		int primary = config.primarySlot - 1;
		int secondary = config.secondarySlot - 1;

		if (swapped) {
			switchTo(client, secondary);
			swapped = false;
		}
		if (chainCooldown > 0) chainCooldown--;

		while (toggleKey.consumeClick()) {
			config.enabled = !config.enabled;
			config.save();
			player.displayClientMessage(
					Component.literal("[Auto Swap Lunge] " + (config.enabled ? "ON" : "OFF")), true);
		}

		// A fresh key press always fires. Holding the key keeps firing on the timer.
		boolean fire = false;
		while (triggerKey.consumeClick()) fire = true;
		if (scrollPending) {
			scrollPending = false;
			fire = true;
		}
		if (!fire && config.chain && triggerKey.isDown() && chainCooldown == 0) fire = true;

		if (fire && config.enabled && client.screen == null && primary != secondary) {
			switchTo(client, primary);
			swapped = true;
			chainCooldown = config.chainIntervalTicks;
			// Same as the player pressing Attack this tick.
			KeyMapping.click(KeyBindingHelper.getBoundKeyOf(client.options.keyAttack));
		}
	}

	private void switchTo(Minecraft client, int slot) {
		LocalPlayer player = client.player;
		if (player == null || slot < 0 || slot > 8) return;
		player.getInventory().setSelectedSlot(slot);
		if (client.getConnection() != null) {
			client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
		}
	}
}
