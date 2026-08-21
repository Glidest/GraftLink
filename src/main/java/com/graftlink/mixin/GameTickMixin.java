package com.graftlink.mixin;

import com.graftlink.api.event.EventBus;
import com.graftlink.api.event.GameEvents;
import minicraft.core.Game;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Game.class, remap = false)
public class GameTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo ci) {
        EventBus.getInstance().post(new GameEvents.TickEvent.Pre());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostTick(CallbackInfo ci) {
        EventBus.getInstance().post(new GameEvents.TickEvent.Post());
    }
}