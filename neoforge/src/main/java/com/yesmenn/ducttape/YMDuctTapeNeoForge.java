package com.yesmenn.ducttape;

import com.yesmenn.ducttape.platform.NeoForgePlatform;
import com.yesmenn.ducttape.platform.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(YMDuctTape.MODID)
public final class YMDuctTapeNeoForge {

    public YMDuctTapeNeoForge(IEventBus modEventBus) {
        NeoForgePlatform platform = new NeoForgePlatform();
        Platform.init(platform);
        YMDuctTape.init();
        platform.registerAll(modEventBus);
    }
}
