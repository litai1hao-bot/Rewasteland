package com.selton.rewasteland;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Acid rain / toxic water.
 *
 * <p>Every tick a server player in the overworld is checked for exposure:
 * <ul>
 *   <li>standing in water (feet or head block), unless riding a vehicle;</li>
 *   <li>standing under the open sky while it rains or thunders.</li>
 * </ul>
 * Exposure rises by {@code exposure_gain_per_tick} while exposed and falls by
 * {@code exposure_loss_per_tick} while dry. Tier thresholds then apply an infinite poison effect:
 * tier 1 = poison, tier 2 = poison I, tier 3 = poison II + green particles.
 *
 * <p>All tunables live in {@code config/rewasteland/acid_rain.json}; the file is re-read on every
 * server start and defaults are written on first launch.
 */
public final class AcidRain {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String TAG_EXPOSURE = "rw_acid_exposure";
    private static final String TAG_LEVEL = "rw_acid_level";
    private static final String TAG_POISON = "rw_acid_poison";

    private static final String CONFIG_NAME = "acid_rain.json";

    private static final String DEFAULT_CONFIG = """
            {
              "enabled": true,
              "water_is_acidic": true,
              "rain_is_acidic": true,
              "affect_creative_players": false,
              "exposure_gain_per_tick": 1,
              "exposure_loss_per_tick": 1,
              "max_exposure": 1200,
              "tier1_threshold": 200,
              "tier2_threshold": 600,
              "tier3_threshold": 1000
            }
            """;

    private static final String README = """
            ReWasteland - acid rain / toxic water
            =====================================

            File: config/rewasteland/acid_rain.json
            Re-read every time the world starts.

            Every tick, for each non-creative player in the overworld:
              - in water (feet or head), or
              - under open sky while raining / thundering
              -> exposure += exposure_gain_per_tick
              otherwise
              -> exposure -= exposure_loss_per_tick
            Exposure is clamped to 0..max_exposure.

            Tiers (infinite poison while exposed):
              tier1_threshold .. tier2-1   poison 0      (default 200..599)
              tier2_threshold .. tier3-1   poison I      (default 600..999)
              tier3_threshold and above    poison II + green particles (default 1000+)

            With the defaults (gain 1 / loss 1 per tick) that is 10 s / 30 s / 50 s of
            continuous exposure. Riding a vehicle protects from water, not from rain.
            Set "affect_creative_players" to true if you want to test in creative.
            """;

    private static AcidRainLogic.Settings settings = AcidRainLogic.defaults();

    private AcidRain() {
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
            Path readme = dir.resolve("README-acid-rain.txt");
            if (Files.notExists(readme)) {
                Files.writeString(readme, README);
            }
            Path json = configPath();
            if (Files.notExists(json)) {
                Files.writeString(json, DEFAULT_CONFIG);
            }
        } catch (IOException e) {
            LOGGER.error("Could not create acid rain config in {}", dir, e);
        }
        load();
    }

    /** Re-reads {@code acid_rain.json}; keeps the current settings if the file is broken. */
    public static void load() {
        Path json = configPath();
        if (Files.notExists(json)) {
            settings = AcidRainLogic.defaults();
            return;
        }
        try {
            JsonConfig cfg = GSON.fromJson(Files.readString(json), JsonConfig.class);
            if (cfg == null) {
                throw new JsonParseException("empty acid rain config");
            }
            int t1 = Math.max(1, cfg.tier1_threshold);
            int t2 = Math.max(t1 + 1, cfg.tier2_threshold);
            int t3 = Math.max(t2 + 1, cfg.tier3_threshold);
            settings = new AcidRainLogic.Settings(
                    cfg.enabled,
                    cfg.water_is_acidic,
                    cfg.rain_is_acidic,
                    cfg.affect_creative_players,
                    Math.max(0, cfg.exposure_gain_per_tick),
                    Math.max(0, cfg.exposure_loss_per_tick),
                    Math.max(1, cfg.max_exposure),
                    t1, t2, t3);
            LOGGER.info("Acid rain config loaded: enabled={}, thresholds={}/{}/{}",
                    settings.enabled(), settings.tier1(), settings.tier2(), settings.tier3());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read {} - keeping previous acid rain settings", json, e);
        }
    }

    /* ---------------------------------------------------------------- events */

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        tick(level, player, null);
    }

    /** Keeps the exposure meter across death, so dying does not wipe the acid build-up. */
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (!(event.getOriginal() instanceof ServerPlayer old) || !(event.getEntity() instanceof ServerPlayer fresh)) {
            return;
        }
        CompoundTag from = old.getPersistentData();
        CompoundTag to = fresh.getPersistentData();
        to.putInt(TAG_EXPOSURE, from.getInt(TAG_EXPOSURE));
        to.putInt(TAG_LEVEL, from.getInt(TAG_LEVEL));
        to.putBoolean(TAG_POISON, false);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        load();
    }

    /* ---------------------------------------------------------------- logic */

    /**
     * Advances the meter for one player tick.
     *
     * @param forcedExposed when non-null, overrides the environment check (used by the debug command)
     */
    static void tick(ServerLevel level, ServerPlayer player, Boolean forcedExposed) {
        if (!settings.enabled() || player.isSpectator()) {
            return;
        }
        if (!settings.affectCreative() && player.isCreative()) {
            return;
        }
        boolean exposed = forcedExposed != null ? forcedExposed : isExposed(level, player);
        CompoundTag data = player.getPersistentData();
        AcidRainLogic.State next = AcidRainLogic.advance(
                new AcidRainLogic.State(data.getInt(TAG_EXPOSURE), data.getInt(TAG_LEVEL)),
                exposed, settings);
        int oldLevel = data.getInt(TAG_LEVEL);
        data.putInt(TAG_EXPOSURE, next.exposure());
        data.putInt(TAG_LEVEL, next.level());
        apply(level, player, data, oldLevel, next.level());
    }

    private static boolean isExposed(ServerLevel level, ServerPlayer player) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        BlockPos pos = player.blockPosition();
        if (settings.waterAcidic() && player.getVehicle() == null) {
            if (level.getFluidState(pos).is(FluidTags.WATER)
                    || level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                return true;
            }
        }
        if (settings.rainAcidic() && (level.isRaining() || level.isThundering())) {
            return level.canSeeSky(pos.above());
        }
        return false;
    }

    private static void apply(ServerLevel level, ServerPlayer player, CompoundTag data, int oldLevel, int newLevel) {
        if (newLevel <= 0) {
            if (data.getBoolean(TAG_POISON)) {
                player.removeEffect(MobEffects.POISON);
                data.putBoolean(TAG_POISON, false);
                if (oldLevel > 0) {
                    play(level, player, false);
                }
            }
            return;
        }

        int amplifier = newLevel - 1;
        if (newLevel != oldLevel) {
            // addEffect() never downgrades an existing stronger effect, so clear before re-applying.
            player.removeEffect(MobEffects.POISON);
        }
        MobEffectInstance current = player.getEffect(MobEffects.POISON);
        boolean needsEffect = current == null
                || current.getDuration() != MobEffectInstance.INFINITE_DURATION
                || current.getAmplifier() != amplifier;
        if (needsEffect) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.POISON, MobEffectInstance.INFINITE_DURATION, amplifier, false, true, true));
        }
        data.putBoolean(TAG_POISON, true);

        if (newLevel > oldLevel) {
            play(level, player, true);
        } else if (newLevel < oldLevel) {
            play(level, player, false);
        }
        if (newLevel >= 3) {
            level.sendParticles(
                    new DustColorTransitionOptions(
                            new Vector3f(0.133F, 1.0F, 0.0F),
                            new Vector3f(0.831F, 1.0F, 0.0F),
                            1.0F),
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    5, 0.2, 0.2, 0.2, 3.0);
        }
    }

    private static void play(ServerLevel level, ServerPlayer player, boolean worsening) {
        if (worsening) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0F, 0.5F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
    }

    /* ---------------------------------------------------------------- debug command */

    /** Op-only harness: /rwacid &lt;ticks&gt; &lt;exposed&gt;  |  /rwacid reset */
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rwacid")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    ServerPlayer fake = FakePlayerFactory.getMinecraft(level);
                    fake.getPersistentData().remove(TAG_EXPOSURE);
                    fake.getPersistentData().remove(TAG_LEVEL);
                    fake.getPersistentData().remove(TAG_POISON);
                    fake.removeEffect(MobEffects.POISON);
                    ctx.getSource().sendSuccess(() -> Component.literal("rwacid: state reset"), false);
                    return 1;
                }))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 5000))
                        .then(Commands.argument("exposed", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    boolean exposed = BoolArgumentType.getBool(ctx, "exposed");
                                    return runHarness(ctx.getSource(), ticks, exposed);
                                })))
                .then(Commands.literal("auto")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 5000))
                                .executes(ctx -> runHarness(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "ticks"), null)))));
    }

    private static int runHarness(net.minecraft.commands.CommandSourceStack source, int ticks, Boolean exposed) {
        ServerLevel level = source.getLevel();
        ServerPlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
        boolean wasExposed = exposed != null ? exposed : isExposed(level, fake);
        for (int i = 0; i < ticks; i++) {
            tick(level, fake, exposed);
        }
        CompoundTag data = fake.getPersistentData();
        MobEffectInstance effect = fake.getEffect(MobEffects.POISON);
        String effectText = effect == null
                ? "none"
                : "poison amp=" + effect.getAmplifier() + " dur=" + effect.getDuration();
        String msg = "rwacid: ticks=" + ticks
                + " exposed=" + wasExposed
                + " exposure=" + data.getInt(TAG_EXPOSURE)
                + " level=" + data.getInt(TAG_LEVEL)
                + " effect=" + effectText;
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Shape of acid_rain.json (Gson field names must match the file). */
    private static final class JsonConfig {
        boolean enabled = true;
        boolean water_is_acidic = true;
        boolean rain_is_acidic = true;
        boolean affect_creative_players = false;
        int exposure_gain_per_tick = 1;
        int exposure_loss_per_tick = 1;
        int max_exposure = 1200;
        int tier1_threshold = 200;
        int tier2_threshold = 600;
        int tier3_threshold = 1000;
    }
}
