/**
 * Shared "YesMenn!" creative tab with sections (Lights, Technicals, Decor, Other), used by all YM mods.
 * <p>
 * YM Duct Tape registers the tab and draws the sections. A YM mod takes part like this:
 * <ol>
 *     <li>Depend on {@code ymducttape} (required) in {@code neoforge.mods.toml} / {@code fabric.mod.json}, and do not
 *     register an own creative tab.</li>
 *     <li>Put the mod's items into the section tags {@code data/yesmenn/tags/item/creative/<lights|technicals|decor|other>.json}
 *     ({@code "replace": false}). The order in the tag file is the order in the tab; mods load in ID order.</li>
 *     <li>Optional safety net: mark the mod in {@code neoforge.mods.toml}
 *     <pre>
 * [modproperties.${mod_id}]
 * yesmenn_tab = true</pre>
 *     or in {@code fabric.mod.json} ({@code "custom": { "yesmenn_tab": true }}); its items that are in no section
 *     tag then still show up under Other.</li>
 * </ol>
 * Banners are {@code assets/yesmenn/textures/gui/creative_sections/<section>.png} (ratio 9:1, e.g. 648x72) and the
 * texts {@code itemGroup.yesmenn} / {@code yesmenn.creative_section.<section>} in {@code assets/yesmenn/lang}; a missing
 * banner is drawn as a plain gradient. The tab icon is the first item of the first non-empty section.
 */
package com.yesmenn.ducttape.ymtab;
