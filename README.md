# YM Duct Tape

Core mod of the YesMenn! mods for Minecraft 1.21.1 (NeoForge and Fabric).

It holds what several YM mods share, so it exists only once:

- the "YesMenn!" creative tab with its sections (Lights, Technicals, Decor, Other)

YM Duct Tape adds no content of its own. It is required by the YesMenn! mods that use it.

## For YM mods

Add `ymducttape` as a required dependency and put your items into the item tags
`yesmenn:creative/<lights|technicals|decor|other>` (`data/yesmenn/tags/item/creative/<section>.json`,
`"replace": false`). See `common/src/main/java/com/yesmenn/ducttape/ymtab/package-info.java`.
