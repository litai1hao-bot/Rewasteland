package com.selton.rewasteland;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Registers the optional third-party structure bridges as built-in data packs.
 *
 * <p>The packs live inside this mod's jar under {@code compat_data/<pack>/} and are added as
 * always-active server data packs only when the source mod is present <b>and</b> its version has a
 * matching bridge. The version gate matters: a bridge references the source mod's template-pool
 * ids, and those get renamed between mod versions. Registering a mismatched bridge makes the
 * worldgen registry fail with "Unbound values in ... template_pool" and breaks world creation.
 * Unknown versions are skipped with a warning instead of crashing.</p>
 */
public final class CompatDataPacks {
    public static final String TNAS = "tnew_apocalypse_structure_tnas";
    public static final String LOST_CITY = "the_lost_city";

    private CompatDataPacks() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        if (ModList.get().isLoaded(TNAS)) {
            String version = version(TNAS);
            if (version.startsWith("1.0.1")) {
                add(event, "compat_data/tnas", "ReWasteland x TNAS structures");
            } else {
                ReWasteland.LOGGER.warn("ReWasteland: no built-in structure bridge for TNAS {}; skipping compatibility pack", version);
            }
        }
        if (ModList.get().isLoaded(LOST_CITY)) {
            String version = version(LOST_CITY);
            if (version.startsWith("1.5")) {
                add(event, "compat_data/lostcity_1_5", "ReWasteland x The Lost City 1.5 structures");
            } else if (version.startsWith("1.2")) {
                add(event, "compat_data/lostcity_1_2", "ReWasteland x The Lost City 1.2 structures");
            } else {
                ReWasteland.LOGGER.warn("ReWasteland: no built-in structure bridge for The Lost City {}; skipping compatibility pack", version);
            }
        }
    }

    private static String version(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
    }

    private static void add(AddPackFindersEvent event, String path, String name) {
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(ReWasteland.MODID, path),
                PackType.SERVER_DATA,
                Component.literal(name),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP);
        ReWasteland.LOGGER.info("Registered built-in structure pack: {} ({})", name, path);
    }
}
