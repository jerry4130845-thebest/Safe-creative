package com.example.safecreative.mixin;

import com.example.safecreative.SafePick;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "pickBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;setPickedItem(Lnet/minecraft/world/item/ItemStack;)V"),
            cancellable = true)
    private void safecreative$pickBlock(CallbackInfo ci, @Local(ordinal = 0) ItemStack candidate) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player != null && minecraft.player.isCreative()
                && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            // Vanilla/NeoForge has resolved the clone stack, including any Ctrl-copy data.
            SafePick.pick(minecraft, hit, candidate);
            ci.cancel();
        } else {
            // Entity picks proceed normally, but may change selection or overwrite the waiting slot.
            SafePick.cancel();
        }
    }

    @Inject(method = "handleKeybinds", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/world/entity/player/Inventory;selected:I", shift = At.Shift.AFTER))
    private void safecreative$hotbarSelected(CallbackInfo ci) {
        SafePick.discardIfInvalid();
    }
}
