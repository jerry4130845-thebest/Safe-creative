package com.example.safecreative.mixin;

import com.example.safecreative.OriginalBlockDrops;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState safecreative$prepareDrop(Block block, Level world, BlockPos pos,
            BlockState state, Player breaker, Operation<BlockState> original,
            @Share("originalDrop") LocalRef<ItemStack> drop) {
        drop.set(OriginalBlockDrops.prepare(player, level, pos, block));
        return original.call(block, world, pos, state, breaker);
    }

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z"))
    private boolean safecreative$dropAfterRemoval(ServerPlayerGameMode gameMode, BlockPos pos,
            BlockState state, boolean canHarvest, Operation<Boolean> original,
            @Share("originalDrop") LocalRef<ItemStack> drop) {
        boolean removed = original.call(gameMode, pos, state, canHarvest);
        if (removed) {
            OriginalBlockDrops.drop(player, level, pos, drop.get());
        }
        return removed;
    }
}
