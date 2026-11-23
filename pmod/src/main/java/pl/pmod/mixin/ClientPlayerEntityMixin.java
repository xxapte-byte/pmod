package pl.pmod.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.pmod.Config;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    // brak wypychania z blokow
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void pmod$noBlockPush(double x, double z, CallbackInfo ci) {
        if (Config.get().noPush) ci.cancel();
    }
}
