package pl.pmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.pmod.Config;

@Mixin(Entity.class)
public class EntityMixin {
    // brak popychania przez innych graczy / moby
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void pmod$noPush(Entity other, CallbackInfo ci) {
        if (Config.get().noPush && (Object) this == MinecraftClient.getInstance().player) ci.cancel();
    }

    // brak znoszenia przez wode/lawe
    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void pmod$noFluidPush(CallbackInfoReturnable<Boolean> cir) {
        if (Config.get().noPush && (Object) this == MinecraftClient.getInstance().player) cir.setReturnValue(false);
    }
}
