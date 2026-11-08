package com.selton.rewasteland;

/**
 * Pure state machine for the acid rain exposure meter.
 *
 * <p>Deliberately free of Minecraft imports so the ramp/tier rules can be tested on their own.
 * The rule mirrors the Acid Wasteland datapack: every tick the meter either gains while the
 * player is exposed (acid water / open-sky rain) or loses while dry, clamped to {@code 0..max}.
 */
public final class AcidRainLogic {
    /** Tunables loaded from {@code config/rewasteland/acid_rain.json}. */
    public record Settings(
            boolean enabled,
            boolean acidWaterToxic,
            boolean plainWaterToxic,
            boolean rainAcidic,
            boolean affectCreative,
            int gainPerTick,
            int lossPerTick,
            int maxExposure,
            int tier1,
            int tier2,
            int tier3) {
    }

    /** Persistent per-player state. {@code level} is 0..3. */
    public record State(int exposure, int level) {
    }

    private AcidRainLogic() {
    }

    public static Settings defaults() {
        return new Settings(true, true, false, true, false, 1, 1, 1200, 200, 600, 1000);
    }

    /** Advances one game tick; {@code exposed} means acid water or acid rain is hitting the player. */
    public static State advance(State state, boolean exposed, Settings settings) {
        int time = state.exposure();
        if (exposed) {
            time = Math.min(settings.maxExposure(), time + Math.max(0, settings.gainPerTick()));
        } else {
            time = Math.max(0, time - Math.max(0, settings.lossPerTick()));
        }
        return new State(time, tier(time, settings));
    }

    public static int tier(int exposure, Settings settings) {
        if (exposure >= settings.tier3()) {
            return 3;
        }
        if (exposure >= settings.tier2()) {
            return 2;
        }
        if (exposure >= settings.tier1()) {
            return 1;
        }
        return 0;
    }
}
