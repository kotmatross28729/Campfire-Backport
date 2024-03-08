package connor135246.campfirebackport.mixin.witchery.symbols;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.entity.EntitySpellEffect;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * This mixin allows campfires to be ignited by the Incendio symbol. It must be level 1.
 */
@Mixin(targets = "com.emoniph.witchery.infusion.infusions.symbols.EffectRegistry$22")
public abstract class MixinIncendio
{

    @Inject(method = "Lcom/emoniph/witchery/infusion/infusions/symbols/EffectRegistry$22;onCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/MovingObjectPosition;Lcom/emoniph/witchery/entity/EntitySpellEffect;)V",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lcom/emoniph/witchery/util/BlockUtil;getBlock(Lnet/minecraft/world/World;Lnet/minecraft/util/MovingObjectPosition;)Lnet/minecraft/block/Block;",
                    ordinal = 0),
            cancellable = true, remap = false)
    public void onOnCollision(World world, EntityLivingBase caster, MovingObjectPosition mop, EntitySpellEffect spell, CallbackInfo ci)
    {
        if (BlockCampfire.igniteOrReigniteCampfire(null, world, mop.blockX, mop.blockY, mop.blockZ) != 0)
            ci.cancel();
    }

}
