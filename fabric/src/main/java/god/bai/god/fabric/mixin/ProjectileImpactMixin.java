package god.bai.god.fabric.mixin;

import god.bai.god.shen.ShenEventDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 弹射物命中无敌目标时折返弹开（对应 Forge 的 ProjectileImpactEvent）。 */
@Mixin(Projectile.class)
public class ProjectileImpactMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void god$deflectFromInvincible(HitResult result, CallbackInfo ci) {
        if (!(result instanceof EntityHitResult entityHit)) return;
        Entity hit = entityHit.getEntity();
        if (!(hit instanceof LivingEntity living)) return;
        if (!ShenEventDispatcher.isInvincible(living)) return;

        Projectile self = (Projectile) (Object) this;
        self.setDeltaMovement(self.getDeltaMovement().scale(-1.1));
        ci.cancel();
    }
}
