package com.yesmenn.ducttape.mixin.neoforge;

import com.yesmenn.ducttape.ymtab.client.YmCreativeSections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No hover highlight on section banners in the shared YesMenn! creative tab. NeoForge only: the slot-aware
 * {@code renderSlotHighlight} overload is a NeoForge patch (Fabric: {@code MixinYmAbstractContainerScreenFabric}).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinYmAbstractContainerScreen {
    @Inject(method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ym$noBannerHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick,
                                      CallbackInfo ci) {
        if ((Object) this instanceof CreativeModeInventoryScreen && YmCreativeSections.isRenderedBannerSlot(slot)) {
            ci.cancel();
        }
    }
}
