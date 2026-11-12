package com.yesmenn.ducttape.mixin.fabric;

import com.yesmenn.ducttape.ymtab.client.YmCreativeSections;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No hover highlight on section banners in the shared YesMenn! creative tab. Fabric counterpart of the NeoForge
 * mixin: vanilla has no slot-aware {@code renderSlotHighlight}, but {@code render} stores the slot in
 * {@code hoveredSlot} right before drawing the highlight.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinYmAbstractContainerScreenFabric {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
    private boolean ym$noBannerHighlight(GuiGraphics graphics, int x, int y, int blitOffset) {
        return !((Object) this instanceof CreativeModeInventoryScreen
                && hoveredSlot != null
                && YmCreativeSections.isRenderedBannerSlot(hoveredSlot));
    }
}
