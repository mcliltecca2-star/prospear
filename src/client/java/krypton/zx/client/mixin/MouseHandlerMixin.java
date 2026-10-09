package krypton.zx.client.mixin;

import krypton.zx.client.LungeClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Catches the mouse wheel. When the scroll trigger is on, scrolling up OR down
 * fires the lunge and the hotbar does not change slot.
 * require = 0 means: if this method can't be found, only scrolling stops working;
 * the game still starts.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
	private void kryptonx$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (LungeClient.handleScroll(yOffset)) {
			ci.cancel();
		}
	}
}
