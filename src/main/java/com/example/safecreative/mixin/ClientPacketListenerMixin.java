package com.example.safecreative.mixin;

import com.example.safecreative.SafePick;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleContainerSetSlot", at = @At("RETURN"))
    private void safecreative$slotApplied(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        SafePick.onSlot((ClientPacketListener) (Object) this, packet.getContainerId(),
                packet.getStateId(), packet.getSlot(), packet.getItem());
    }

    @Inject(method = "handleContainerContent", at = @At("RETURN"))
    private void safecreative$contentApplied(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        SafePick.onContent((ClientPacketListener) (Object) this, packet.getContainerId(),
                packet.getStateId(), packet.getItems());
    }

    @Inject(method = "handleSetCarriedItem", at = @At("RETURN"))
    private void safecreative$selectionApplied(ClientboundSetCarriedItemPacket packet, CallbackInfo ci) {
        SafePick.discardIfInvalid();
    }

    @Inject(method = "handleGameEvent", at = @At("RETURN"))
    private void safecreative$gameEventApplied(ClientboundGameEventPacket packet, CallbackInfo ci) {
        SafePick.discardIfInvalid();
    }
}
