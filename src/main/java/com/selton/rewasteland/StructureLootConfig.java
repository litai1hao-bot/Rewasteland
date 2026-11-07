package com.selton.rewasteland;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Structure loot tables are driven by plain JSON files in
 * {@code config/rewasteland/structure_looting/}.
 *
 * <p>Every file declares the table it overrides with a {@code "loot_table"} field, followed by a
 * friendly {@code "pools"} list. Defaults are copied out of the jar (see
 * {@code /structure_looting/defaults/}) the first time the game starts; afterwards the config file
 * is the source of truth and the in-jar table is only a fallback if the file is broken.
 *
 * <p>Edits take effect on the next loot table reload (world start or {@code /reload}).
 */
public final class StructureLootConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] DEFAULT_FILES = {
            "survivor_camp_scraps.json",
            "survivor_camp_tools.json",
    };

    private static final String README = """
            ReWasteland - structure loot tables
            ===================================

            Location: config/rewasteland/structure_looting/  (one JSON file per loot table)

            One JSON file per loot table. The file name is free, but "loot_table" must match the
            table the structures use. Files are read on every loot table reload: restart the world
            or run /reload after editing.

            Structure tables:
              rewasteland:chests/survivor_camp_scraps   (barrels in ruins / camps, junk)
              rewasteland:chests/survivor_camp_tools    (barrels in ruins / camps, tools)

            Any other table id can be listed too (e.g. minecraft:chests/village/village_armorer);
            it is then replaced by your file.

            Format
            ------
            {
              "loot_table": "rewasteland:chests/survivor_camp_scraps",
              "pools": [
                {
                  "rolls": 3,                                  // or {"min": 1, "max": 4}
                  "entries": [
                    {
                      "item": "minecraft:bone",
                      "weight": 10,                            // optional, default 1
                      "count": {"min": 1, "max": 8}            // optional, default 1
                    }
                  ]
                }
              ]
            }

            Optional entry fields:
              "name":      text component, e.g. {"translate": "rewasteland.crowbar"}
              "lore":      array of text components
              "lore_mode": "append" (default) | "replace_all"
              "custom_model_data": integer
              "components": { ... }             raw item components (1.21.1 syntax)
              "custom_data": { ... }            item custom_data component
              "enchant_randomly": "#minecraft:on_random_loot"  (tag or single enchantment)

            Invalid JSON or an unknown item id only logs a warning: the built-in table is kept.
            """;

    private static Map<ResourceLocation, Path> cache = Map.of();
    private static long cacheStamp = Long.MIN_VALUE;

    private StructureLootConfig() {
    }

    private static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve("rewasteland").resolve("structure_looting");
    }

    /** Creates the folder, the bundled defaults and a short README on first run. */
    public static void ensureDefaults() {
        Path dir = configDir();
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.txt");
            if (Files.notExists(readme)) {
                Files.writeString(readme, README);
            }
            for (String name : DEFAULT_FILES) {
                Path target = dir.resolve(name);
                if (Files.exists(target)) {
                    continue;
                }
                try (InputStream in = StructureLootConfig.class.getResourceAsStream("/structure_looting/defaults/" + name)) {
                    if (in == null) {
                        ReWasteland.LOGGER.warn("Missing bundled structure loot default '{}'", name);
                        continue;
                    }
                    Files.copy(in, target);
                    ReWasteland.LOGGER.info("Created structure loot config {}", target);
                }
            }
        } catch (IOException e) {
            ReWasteland.LOGGER.error("Could not prepare structure loot config folder {}", dir, e);
        }
    }

    /** Replaces a loaded loot table with the version described by the matching config file. */
    public static void onLootTableLoad(LootTableLoadEvent event) {
        Path file = index().get(event.getName());
        if (file == null) {
            return;
        }
        try {
            JsonObject cfg = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonObject vanilla = toVanillaLootTable(cfg);
            DataResult<LootTable> parsed = LootTable.DIRECT_CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, event.getRegistries()), vanilla);
            Optional<LootTable> table = parsed.result();
            if (table.isEmpty()) {
                String message = parsed.error().map(DataResult.Error::message).orElse("unknown error");
                throw new JsonParseException(message);
            }
            event.setTable(table.get());
            ReWasteland.LOGGER.info("Structure loot config applied: {} <- {}", event.getName(), file.getFileName());
        } catch (Exception e) {
            ReWasteland.LOGGER.error("Invalid structure loot config {} ({}); keeping the built-in table for {}",
                    file.getFileName(), e.getMessage(), event.getName());
        }
    }

    // ---------------------------------------------------------------- config index

    private static Map<ResourceLocation, Path> index() {
        Path dir = configDir();
        if (!Files.isDirectory(dir)) {
            return Map.of();
        }
        try {
            long stamp = 0L;
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(StructureLootConfig::isJson).toList()) {
                    stamp = stamp * 31L + (Files.getLastModifiedTime(p).toMillis() ^ Files.size(p));
                }
            }
            if (stamp == cacheStamp && !cache.isEmpty()) {
                return cache;
            }
            Map<ResourceLocation, Path> map = new HashMap<>();
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(StructureLootConfig::isJson).toList()) {
                    try {
                        JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                        if (!o.has("loot_table")) {
                            continue;
                        }
                        map.put(ResourceLocation.parse(o.get("loot_table").getAsString()), p);
                    } catch (Exception e) {
                        ReWasteland.LOGGER.warn("Skipping unreadable structure loot config {}: {}", p.getFileName(), e.getMessage());
                    }
                }
            }
            cache = Map.copyOf(map);
            cacheStamp = stamp;
            return cache;
        } catch (IOException e) {
            ReWasteland.LOGGER.error("Could not read structure loot config folder {}", dir, e);
            return Map.of();
        }
    }

    private static boolean isJson(Path p) {
        return p.getFileName().toString().endsWith(".json");
    }

    // ---------------------------------------------------------------- config -> vanilla loot table json

    private static JsonObject toVanillaLootTable(JsonObject cfg) {
        JsonObject vanilla = new JsonObject();
        vanilla.addProperty("type", cfg.has("type") ? cfg.get("type").getAsString() : "minecraft:chest");
        JsonArray pools = new JsonArray();
        for (JsonElement poolElement : cfg.getAsJsonArray("pools")) {
            JsonObject poolCfg = poolElement.getAsJsonObject();
            JsonObject pool = new JsonObject();
            pool.add("rolls", numberProvider(poolCfg.get("rolls"), 1));
            pool.addProperty("bonus_rolls", 0);
            JsonArray entries = new JsonArray();
            for (JsonElement entryElement : poolCfg.getAsJsonArray("entries")) {
                entries.add(toVanillaEntry(entryElement.getAsJsonObject()));
            }
            pool.add("entries", entries);
            pools.add(pool);
        }
        vanilla.add("pools", pools);
        return vanilla;
    }

    private static JsonObject toVanillaEntry(JsonObject entry) {
        JsonObject out = new JsonObject();
        out.addProperty("type", "minecraft:item");
        out.addProperty("name", entry.get("item").getAsString());
        if (entry.has("weight")) {
            out.addProperty("weight", entry.get("weight").getAsInt());
        }

        JsonArray functions = new JsonArray();

        JsonObject components = new JsonObject();
        boolean hasComponents = false;
        if (entry.has("custom_model_data")) {
            components.addProperty("minecraft:custom_model_data", entry.get("custom_model_data").getAsInt());
            hasComponents = true;
        }
        if (entry.has("components")) {
            for (Map.Entry<String, JsonElement> e : entry.getAsJsonObject("components").entrySet()) {
                components.add(e.getKey(), e.getValue());
            }
            hasComponents = true;
        }
        if (entry.has("custom_data")) {
            components.add("minecraft:custom_data", entry.get("custom_data"));
            hasComponents = true;
        }
        if (hasComponents) {
            JsonObject fn = new JsonObject();
            fn.addProperty("function", "minecraft:set_components");
            fn.add("components", components);
            functions.add(fn);
        }
        if (entry.has("count")) {
            JsonObject fn = new JsonObject();
            fn.addProperty("function", "minecraft:set_count");
            fn.add("count", numberProvider(entry.get("count"), 1));
            functions.add(fn);
        }
        if (entry.has("name")) {
            JsonObject fn = new JsonObject();
            fn.addProperty("function", "minecraft:set_name");
            fn.add("name", entry.get("name"));
            functions.add(fn);
        }
        if (entry.has("lore")) {
            JsonObject fn = new JsonObject();
            fn.addProperty("function", "minecraft:set_lore");
            fn.add("lore", entry.get("lore"));
            fn.addProperty("mode", entry.has("lore_mode") ? entry.get("lore_mode").getAsString() : "append");
            functions.add(fn);
        }
        if (entry.has("enchant_randomly")) {
            JsonObject fn = new JsonObject();
            fn.addProperty("function", "minecraft:enchant_randomly");
            fn.add("options", entry.get("enchant_randomly"));
            functions.add(fn);
        }
        if (!functions.isEmpty()) {
            out.add("functions", functions);
        }
        return out;
    }

    private static JsonElement numberProvider(JsonElement value, int fallback) {
        if (value == null || value.isJsonNull()) {
            return new com.google.gson.JsonPrimitive(fallback);
        }
        if (value.isJsonPrimitive()) {
            return value;
        }
        JsonObject uniform = new JsonObject();
        uniform.addProperty("type", "minecraft:uniform");
        uniform.add("min", value.getAsJsonObject().get("min"));
        uniform.add("max", value.getAsJsonObject().get("max"));
        return uniform;
    }
}


