package com.example.safecreative;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public final class OriginalBlockDrops {
    // Written on the client thread, read on the integrated server thread.
    private static volatile boolean enabled;

    private OriginalBlockDrops() {}

    static boolean toggle() {
        return enabled = !enabled;
    }

    public static ItemStack prepare(ServerPlayer player, ServerLevel level, BlockPos pos, Block block) {
        if (!isEnabledFor(player)) {
            return ItemStack.EMPTY;
        }
        // Vanilla's playerWillDestroy drops nonempty shulker boxes with their components.
        if (block instanceof ShulkerBoxBlock
                && level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity box && !box.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // Use the block's item, not a loot-table result or a copy of its block entity.
        return new ItemStack(block.asItem());
    }

    public static void drop(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack item) {
        if (item != null && !item.isEmpty() && isEnabledFor(player)
                && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            Block.popResource(level, pos, item);
        }
    }

    private static boolean isEnabledFor(ServerPlayer player) {
        return enabled && player.getServer() instanceof IntegratedServer server
                && server.isSingleplayerOwner(player.getGameProfile()) && player.isCreative();
    }
}
