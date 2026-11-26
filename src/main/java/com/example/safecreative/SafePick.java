package com.example.safecreative;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class SafePick {
    private static final int HOTBAR_MENU_START = 36;
    private static final int STATE_ID_MASK = 0x7fff;
    private static final int NO_SLOT = -1;
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(3);

    // Accessed only on the client thread. Packet hooks run after vanilla has applied the update.
    private static Pending pending;

    private SafePick() {}

    public static void pick(Minecraft minecraft, BlockHitResult hit, ItemStack candidate) {
        if (pending != null || candidate.isEmpty()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        ClientPacketListener connection = minecraft.getConnection();
        if (!canUseInventory(minecraft, player) || level == null || gameMode == null || connection == null
                || level.getBlockState(hit.getBlockPos()).isAir()) {
            return;
        }
        double reach = player.blockInteractionRange();
        if (!(player.getEyePosition().distanceToSqr(hit.getLocation()) <= reach * reach)) {
            return;
        }

        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        if (!Inventory.isHotbarSlot(selected)) {
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        ItemStack held = inventory.getItem(selected);
        if (ItemStack.isSameItemSameComponents(held, candidate)) {
            return;
        }

        int matching = NO_SLOT;
        int empty = NO_SLOT;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (i == selected) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, candidate)) {
                matching = i;
                break;
            }
            if (stack.isEmpty() && empty == NO_SLOT) {
                empty = i;
            }
        }

        Slot heldSlot = menu.getSlot(menuSlot(selected));
        if (matching != NO_SLOT) {
            Slot matchingSlot = menu.getSlot(menuSlot(matching));
            ItemStack match = inventory.getItem(matching);
            if (matchingSlot.mayPickup(player) && fits(heldSlot, match) && fits(matchingSlot, held)) {
                sendClick(minecraft, connection, selected, matching, held, match, ItemStack.EMPTY);
            }
            return;
        }
        if (!fits(heldSlot, candidate) || !candidate.isItemEnabled(level.enabledFeatures())) {
            return;
        }
        if (held.isEmpty()) {
            gameMode.handleCreativeModeItemAdd(candidate.copy(), menuSlot(selected));
            return;
        }
        if (!heldSlot.mayPickup(player) || (empty != NO_SLOT && !fits(menu.getSlot(menuSlot(empty)), held))) {
            return;
        }
        sendClick(minecraft, connection, selected, empty, held, ItemStack.EMPTY, candidate);
    }

    private static boolean fits(Slot slot, ItemStack stack) {
        return stack.isEmpty() || (slot.mayPlace(stack) && stack.getCount() <= slot.getMaxStackSize(stack));
    }

    private static boolean canUseInventory(Minecraft minecraft, LocalPlayer player) {
        return player != null && player.isAlive() && player.isCreative() && minecraft.screen == null
                && minecraft.getOverlay() == null && player.containerMenu == player.inventoryMenu
                && player.inventoryMenu.getCarried().isEmpty();
    }

    private static int menuSlot(int inventorySlot) {
        // InventoryMenu maps the hotbar to 36-44 and main inventory to 9-35.
        return Inventory.isHotbarSlot(inventorySlot) ? HOTBAR_MENU_START + inventorySlot : inventorySlot;
    }

    private static void sendClick(Minecraft minecraft, ClientPacketListener connection, int selected,
            int other, ItemStack held, ItemStack otherStack, ItemStack candidate) {
        InventoryMenu menu = minecraft.player.inventoryMenu;
        pending = new Pending(minecraft, connection, selected, other, held.copy(), otherStack.copy(), candidate.copy());
        boolean throwStack = other == NO_SLOT;
        // No local click/prediction and no claimed slot changes: let the server execute and sync the click.
        connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(),
                menuSlot(throwStack ? selected : other), throwStack ? 1 : selected,
                throwStack ? ClickType.THROW : ClickType.SWAP, ItemStack.EMPTY, Int2ObjectMaps.emptyMap()));
    }

    public static void cancel() {
        pending = null;
    }

    public static void discardIfInvalid() {
        if (pending != null && !pending.isValid(Minecraft.getInstance())) {
            cancel();
        }
    }

    public static void onSlot(ClientPacketListener listener, int container, int state, int slot, ItemStack stack) {
        Pending transaction = observedTransaction(listener, container);
        if (transaction != null && transaction.isNewer(state) && !transaction.confirmSlot(slot, stack)) {
            cancel();
        }
    }

    public static void onContent(ClientPacketListener listener, int container, int state, List<ItemStack> items) {
        Pending transaction = observedTransaction(listener, container);
        if (transaction == null || !transaction.isNewer(state)) {
            return;
        }
        if (!transaction.confirmFromContent(items, transaction.selected)
                || (transaction.other != NO_SLOT && !transaction.confirmFromContent(items, transaction.other))) {
            cancel();
        }
    }

    private static Pending observedTransaction(ClientPacketListener listener, int container) {
        discardIfInvalid();
        return pending != null && pending.connection == listener && pending.menu.containerId == container
                ? pending : null;
    }

    static void afterTick() {
        discardIfInvalid();
        Pending transaction = pending;
        if (transaction == null) {
            return;
        }
        Inventory inventory = transaction.player.getInventory();
        // Unconfirmed slots must still be as they were before the click. Confirmed slots must still
        // match the response, including components and count. A local prediction is never a receipt.
        if (!ItemStack.matches(inventory.getItem(transaction.selected),
                transaction.selectedConfirmed ? transaction.otherStack : transaction.held)
                || (transaction.other != NO_SLOT && !ItemStack.matches(inventory.getItem(transaction.other),
                        transaction.otherConfirmed ? transaction.held : transaction.otherStack))) {
            cancel();
            return;
        }
        if (transaction.selectedConfirmed && (transaction.other == NO_SLOT || transaction.otherConfirmed)) {
            cancel();
            if (!transaction.candidate.isEmpty() && inventory.getItem(transaction.selected).isEmpty()) {
                // The move/drop is confirmed. Let vanilla sync the new item too; don't predict it.
                transaction.gameMode.handleCreativeModeItemAdd(transaction.candidate, menuSlot(transaction.selected));
            }
        }
    }

    private static final class Pending {
        private final LocalPlayer player;
        private final InventoryMenu menu;
        private final ClientLevel level;
        private final ClientPacketListener connection;
        private final MultiPlayerGameMode gameMode;
        private final int startState;
        private final int selected;
        private final int other;
        private final ItemStack held;
        private final ItemStack otherStack;
        private final ItemStack candidate;
        private final long started = System.nanoTime();
        private boolean selectedConfirmed;
        private boolean otherConfirmed;

        private Pending(Minecraft minecraft, ClientPacketListener connection, int selected, int other,
                ItemStack held, ItemStack otherStack, ItemStack candidate) {
            this.player = minecraft.player;
            this.menu = player.inventoryMenu;
            this.level = minecraft.level;
            this.connection = connection;
            this.gameMode = minecraft.gameMode;
            this.startState = menu.getStateId();
            this.selected = selected;
            this.other = other;
            this.held = held;
            this.otherStack = otherStack;
            this.candidate = candidate;
        }

        private boolean isValid(Minecraft minecraft) {
            return System.nanoTime() - started < TIMEOUT_NANOS && minecraft.player == player
                    && minecraft.level == level && minecraft.gameMode == gameMode
                    && minecraft.getConnection() == connection && connection.getConnection().isConnected()
                    && canUseInventory(minecraft, player) && player.getInventory().selected == selected;
        }

        private boolean isNewer(int state) {
            int delta = (state - startState) & STATE_ID_MASK;
            return delta > 0 && delta <= STATE_ID_MASK / 2;
        }

        private boolean confirmFromContent(List<ItemStack> items, int index) {
            int slot = menuSlot(index);
            return slot < items.size() && confirmSlot(slot, items.get(slot));
        }

        private boolean confirmSlot(int slot, ItemStack stack) {
            if (slot == menuSlot(selected)) {
                selectedConfirmed = ItemStack.matches(stack, otherStack);
                return selectedConfirmed;
            }
            if (other != NO_SLOT && slot == menuSlot(other)) {
                otherConfirmed = ItemStack.matches(stack, held);
                return otherConfirmed;
            }
            return true;
        }
    }
}
