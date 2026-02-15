package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    /** Allow duplicating flowers by bonemealing them. */
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    static private void inject$useOnFertilizable(ItemStack S, Level L, BlockPos Pos, CallbackInfoReturnable<Boolean> CIR) {
        var St = L.getBlockState(Pos);
        if (St.is(NguhBlocks.CAN_DUPLICATE_WITH_BONEMEAL)) {
            if (L instanceof ServerLevel SL) {
                Block.popResource(SL, Pos, new ItemStack(St.getBlock()));
                S.shrink(1);
            }

            CIR.setReturnValue(true);
        } else if (St.is(NguhBlocks.CAN_RANDOM_TICK_WITH_BONEMEAL)) {
            if (St.getBlock() instanceof BonemealableBlock)
                throw new IllegalStateException("Bonemealable blocks should not be in this tag!");

            if (L instanceof ServerLevel SL) {
                St.randomTick(SL, Pos, SL.getRandom());
                S.shrink(1);
            }

            CIR.setReturnValue(true);
        }
    }
}
