package com.yesmenn.ducttape.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu's config button opens the YM kit demo. */
public final class YMDuctTapeModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KitDemoScreen::new;
    }
}
