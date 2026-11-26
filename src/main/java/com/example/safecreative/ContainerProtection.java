package com.example.safecreative;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.InputEvent;

final class ContainerProtection {
    private static boolean enabled;

    private ContainerProtection() {}

    static boolean toggle() {
        return enabled = !enabled;
    }

    static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!enabled || !event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isCreative() || minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (isContainer(minecraft.level, hit.getBlockPos())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static boolean isContainer(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof Container || state.getBlock() instanceof Container
                || state.getBlock() instanceof EnderChestBlock || state.getBlock() instanceof LecternBlock) {
            return true;
        }
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, entity, null) != null) {
            return true;
        }
        for (Direction side : Direction.values()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, entity, side) != null) {
                return true;
            }
        }
        return false;
    }
}
