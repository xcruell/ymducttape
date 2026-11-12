package com.yesmenn.ducttape.platform;

import com.yesmenn.ducttape.YMDuctTape;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class NeoForgePlatform implements Platform.Impl {
    private final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> registers = new LinkedHashMap<>();

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Set<String> modsWithProperty(String key) {
        Set<String> mods = new HashSet<>();
        for (IModInfo mod : ModList.get().getMods()) {
            Object value = mod.getModProperties().get(key);
            if (value != null && !"false".equals(String.valueOf(value))) {
                mods.add(mod.getModId());
            }
        }
        return mods;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) registers.computeIfAbsent(registry.key(),
                key -> DeferredRegister.create((ResourceKey<? extends Registry<R>>) key, YMDuctTape.MODID));
        return register.register(name, factory);
    }

    /** Hooks every register filled through {@link #register} to the mod bus; call after the common init. */
    public void registerAll(IEventBus modBus) {
        registers.values().forEach(register -> register.register(modBus));
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }
}
