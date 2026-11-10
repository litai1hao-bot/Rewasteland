package com.selton.rewasteland;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Wasteland weather cycle.
 *
 * <p>Vanilla's random weather is disabled ({@code doWeatherCycle=false}) in worlds created with
 * the ReWasteland world type, and this machine drives a fixed cycle with random durations:
 * clear -> (90% rain | 10% thunder preparation -> thunder) -> rain -> clear. It mirrors the Acid
 * Wasteland datapack's weather machine.</p>
 *
 * <p>The state machine and deadline live in {@link WeatherCycleData} (SavedData), so the cycle
 * survives restarts. Scheduling is a single {@code long} deadline comparison per server tick,
 * which is far cheaper than per-tick decrements and cannot drift.</p>
 *
 * <p>All tunables live in {@code config/rewasteland/weather_cycle.json}.</p>
 */
public final class WastelandWeather {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_NAME = "weather_cycle.json";

    private static final String DEFAULT_CONFIG = """
            {
              "enabled": true,
              "clear_min_ticks": 6000,
              "clear_max_ticks": 9600,
              "rain_min_ticks": 6000,
              "rain_max_ticks": 24000,
              "thunder_chance_percent": 10,
              "thunder_prepare_min_ticks": 3600,
              "thunder_prepare_max_ticks": 6000,
              "thunder_min_ticks": 6000,
              "thunder_max_ticks": 12000,
              "thunder_end_min_ticks": 3600,
              "thunder_end_max_ticks": 6000
            }
            """;

    private static final String README = """
            ReWasteland - weather cycle
            ===========================

            File: config/rewasteland/weather_cycle.json
            Re-read every time the world starts.

            Only applies to worlds created with the ReWasteland world type
            (world preset "rewasteland:wasteland"). Vanilla worlds keep their normal weather.

            The machine disables vanilla's doWeatherCycle and drives:
              clear (6000..9600 ticks, 5..8 min)
                -> 90% rain (6000..24000 ticks, 5..20 min) -> clear
                -> 10% thunder preparation (3600..6000 ticks) -> thunder (6000..12000) -> rain (3600..6000) -> clear

            All durations are in ticks (20 ticks = 1 second).
            "thunder_chance_percent" is the chance to enter the thunder path each time the cycle
            leaves "clear".

            The state survives world restarts. Only thunder changes the acid rain intensity
            indirectly through vanilla weather checks; the acid thresholds stay in acid_rain.json.
            """;

    private static final class JsonConfig {
        Boolean enabled;
        int clear_min_ticks = 6000;
        int clear_max_ticks = 9600;
        int rain_min_ticks = 6000;
        int rain_max_ticks = 24000;
        int thunder_chance_percent = 10;
        int thunder_prepare_min_ticks = 3600;
        int thunder_prepare_max_ticks = 6000;
        int thunder_min_ticks = 6000;
        int thunder_max_ticks = 12000;
        int thunder_end_min_ticks = 3600;
        int thunder_end_max_ticks = 6000;
    }

    private static WeatherCycleLogic.Settings settings = WeatherCycleLogic.Settings.defaults();
    private static boolean enabled = true;
    private static boolean active;
    private static MinecraftServer activeServer;

    private WastelandWeather() {
    }

    private static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve("rewasteland");
    }

    private static Path configPath() {
        return configDir().resolve(CONFIG_NAME);
    }

    /** Creates the config folder, README and default config on first launch, then loads it. */
    public static void ensureDefaults() {
        Path dir = configDir();
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve("README-weather-cycle.txt");
            if (Files.notExists(readme)) {
                Files.writeString(readme, README);
            }
            Path json = configPath();
            if (Files.notExists(json)) {
                Files.writeString(json, DEFAULT_CONFIG);
            }
        } catch (IOException e) {
            LOGGER.error("Could not create weather cycle config in {}", dir, e);
        }
        load();
    }

    /** Re-reads {@code weather_cycle.json}; keeps the current settings if the file is broken. */
    public static void load() {
        Path json = configPath();
        if (Files.notExists(json)) {
            settings = WeatherCycleLogic.Settings.defaults();
            enabled = true;
            return;
        }
        try {
            JsonConfig cfg = GSON.fromJson(Files.readString(json), JsonConfig.class);
            if (cfg == null) {
                throw new JsonParseException("empty weather cycle config");
            }
            enabled = cfg.enabled == null || cfg.enabled;
            settings = new WeatherCycleLogic.Settings(
                    clamp(cfg.clear_min_ticks, 1, 240000), clamp(cfg.clear_max_ticks, 1, 240000),
                    clamp(cfg.rain_min_ticks, 1, 240000), clamp(cfg.rain_max_ticks, 1, 240000),
                    clamp(cfg.thunder_chance_percent, 0, 100),
                    clamp(cfg.thunder_prepare_min_ticks, 1, 240000), clamp(cfg.thunder_prepare_max_ticks, 1, 240000),
                    clamp(cfg.thunder_min_ticks, 1, 240000), clamp(cfg.thunder_max_ticks, 1, 240000),
                    clamp(cfg.thunder_end_min_ticks, 1, 240000), clamp(cfg.thunder_end_max_ticks, 1, 240000));
            LOGGER.info("Weather cycle config loaded: enabled={}, thunderChance={}%", enabled, settings.thunderChancePercent());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read {} - keeping previous weather cycle settings", json, e);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /* ---------------------------------------------------------------- events */

    public static void onServerStarted(ServerStartedEvent event) {
        load();
        activeServer = event.getServer();
        ServerLevel overworld = activeServer.overworld();
        boolean wasteland = WastelandWorld.isWasteland(overworld);
        if (!wasteland) {
            active = false;
            return;
        }
        // Vanilla weather must not fight the state machine. Restore the gamerule when disabled.
        overworld.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(!enabled, activeServer);
        active = enabled;
        if (!active) {
            return;
        }
        WeatherCycleData data = WeatherCycleData.get(overworld);
        if (data.nextTransitionGameTime() <= 0) {
            WeatherCycleLogic.Step step = WeatherCycleLogic.start(settings, roller(overworld));
            apply(overworld, data, step, false);
        } else {
            WeatherCycleLogic.State state = data.state();
            overworld.setWeatherParameters(0, 0, state.raining(), state.thundering());
            LOGGER.info("Weather cycle resumed: state={}, next transition in {} ticks",
                    state, Math.max(0, data.nextTransitionGameTime() - overworld.getGameTime()));
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!active || event.getServer() != activeServer) {
            return;
        }
        ServerLevel overworld = activeServer.overworld();
        WeatherCycleData data = WeatherCycleData.get(overworld);
        long now = overworld.getGameTime();
        if (data.nextTransitionGameTime() > now) {
            return;
        }
        WeatherCycleLogic.Step step = WeatherCycleLogic.advance(data.state(), settings, roller(overworld));
        apply(overworld, data, step, true);
    }

    private static void apply(ServerLevel level, WeatherCycleData data, WeatherCycleLogic.Step step, boolean log) {
        data.set(step.state(), level.getGameTime() + step.ticks());
        level.setWeatherParameters(0, 0, step.state().raining(), step.state().thundering());
        if (log) {
            LOGGER.info("Weather -> {} for {} ticks ({} min)",
                    step.state(), step.ticks(), String.format(Locale.ROOT, "%.1f", step.ticks() / 1200.0));
        }
    }

    private static java.util.function.IntBinaryOperator roller(ServerLevel level) {
        return (min, max) -> min + level.getRandom().nextInt(max - min + 1);
    }

    /* -------------------------------------------------------------- commands */

    public static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rwweather")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> status(ctx.getSource()));
        root.then(Commands.literal("clear").executes(ctx -> force(ctx.getSource(), WeatherCycleLogic.State.CLEAR, -1)));
        root.then(Commands.literal("rain").executes(ctx -> force(ctx.getSource(), WeatherCycleLogic.State.RAIN, -1)));
        root.then(Commands.literal("thunder").executes(ctx -> force(ctx.getSource(), WeatherCycleLogic.State.THUNDER, -1)));
        root.then(Commands.literal("set")
                .then(Commands.argument("state", StringArgumentType.word())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(ctx -> {
                                    String stateName = ctx.getArgument("state", String.class);
                                    int seconds = ctx.getArgument("seconds", Integer.class);
                                    return force(ctx.getSource(),
                                            WeatherCycleLogic.State.byName(stateName.toUpperCase(Locale.ROOT)),
                                            seconds * 20);
                                }))));
        event.getDispatcher().register(root);
    }

    private static int status(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        if (!WastelandWorld.isWasteland(overworld)) {
            source.sendFailure(Component.literal("This world is not a ReWasteland world."));
            return 0;
        }
        WeatherCycleData data = WeatherCycleData.get(overworld);
        long remaining = Math.max(0, data.nextTransitionGameTime() - overworld.getGameTime());
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Weather: %s, next transition in %d ticks (%s min), raining=%s thundering=%s",
                data.state(), remaining, String.format(Locale.ROOT, "%.1f", remaining / 1200.0),
                overworld.isRaining(), overworld.isThundering())), false);
        return 1;
    }

    private static int force(CommandSourceStack source, WeatherCycleLogic.State state, int ticks) {
        ServerLevel overworld = source.getServer().overworld();
        if (!WastelandWorld.isWasteland(overworld)) {
            source.sendFailure(Component.literal("This world is not a ReWasteland world."));
            return 0;
        }
        int duration = ticks > 0 ? ticks : WeatherCycleLogic.durationFor(settings, state, roller(overworld));
        WeatherCycleData data = WeatherCycleData.get(overworld);
        data.set(state, overworld.getGameTime() + duration);
        overworld.setWeatherParameters(0, 0, state.raining(), state.thundering());
        source.sendSuccess(() -> Component.literal("Weather set to " + state + " for " + duration + " ticks"), true);
        return 1;
    }
}
