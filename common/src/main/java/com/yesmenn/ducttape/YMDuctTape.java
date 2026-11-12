package com.yesmenn.ducttape;

import com.mojang.logging.LogUtils;
import com.yesmenn.ducttape.ymtab.YmCreativeTab;
import org.slf4j.Logger;

/**
 * YM Duct Tape: shared core of the YesMenn! mods (the "YesMenn!" creative tab and code used by several YM mods).
 * Loader independent; the NeoForge and Fabric entrypoints set the {@link com.yesmenn.ducttape.platform.Platform}
 * first and then call {@link #init()}.
 */
public final class YMDuctTape {
    public static final String MODID = "ymducttape";
    public static final Logger LOGGER = LogUtils.getLogger();

    private YMDuctTape() {
    }

    public static void init() {
        YmCreativeTab.register();
    }
}
