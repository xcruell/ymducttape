package com.yesmenn.ducttape.platform;

import com.yesmenn.ducttape.YMDuctTape;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class FabricPlatform implements Platform.Impl {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Set<String> modsWithProperty(String key) {
        Set<String> mods = new HashSet<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (!mod.getMetadata().containsCustomValue(key)) {
                continue;
            }
            CustomValue value = mod.getMetadata().getCustomValue(key);
            boolean marked = value.getType() == CustomValue.CvType.BOOLEAN
                    ? value.getAsBoolean()
                    : value.getType() != CustomValue.CvType.STRING || !"false".equals(value.getAsString());
            if (marked) {
                mods.add(mod.getMetadata().getId());
            }
        }
        return mods;
    }

    @Override
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        T value = Registry.register(registry, ResourceLocation.fromNamespaceAndPath(YMDuctTape.MODID, name),
                factory.get());
        return () -> value;
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }
}
