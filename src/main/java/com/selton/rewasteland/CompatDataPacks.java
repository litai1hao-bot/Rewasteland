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
 * <p>The packs live inside this mod's jar under {@code compat_data/<mod>/} and are added as
 * always-active server data packs only when the source mod is present. That way a world gets the
 * extra structures automatically, and nobody sees missing template-pool errors when the other mod
 * is not installed.</p>
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
            add(event, "compat_data/tnas", "ReWasteland x TNAS structures");
        }
        if (ModList.get().isLoaded(LOST_CITY)) {
            add(event, "compat_data/lostcity", "ReWasteland x The Lost City structures");
        }
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
