package com.yesmenn.ducttape;

import com.yesmenn.ducttape.platform.FabricPlatform;
import com.yesmenn.ducttape.platform.Platform;
import net.fabricmc.api.ModInitializer;

public final class YMDuctTapeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Platform.init(new FabricPlatform());
        YMDuctTape.init();
    }
}
