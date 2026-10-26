package god.bai.god.fabric.mixin;

import god.bai.god.shen.ShenEventDispatcher;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 无敌目标不被击退（对应 Forge 的 LivingKnockBackEvent 取消）。 */
@Mixin(LivingEntity.class)
public class LivingEntityKnockbackMixin {

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void god$cancelKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (ShenEventDispatcher.isInvincible((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
