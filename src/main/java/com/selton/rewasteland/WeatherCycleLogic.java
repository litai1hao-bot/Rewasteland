package com.selton.rewasteland;

import java.util.function.IntBinaryOperator;

/**
 * Pure state machine for the wasteland weather cycle.
 *
 * <p>Deliberately free of Minecraft imports so the transitions and duration rolls can be tested
 * on their own. Mirrors the Acid Wasteland datapack: vanilla's weather cycle is disabled and this
 * machine drives clear / rain / thunder transitions with random durations.</p>
 *
 * <p>Cycle: clear --(10% prepare)--> thunder -> rain -> clear, or clear --(90%)--> rain -> clear.</p>
 */
public final class WeatherCycleLogic {
    public enum State {
        CLEAR(false, false),
        RAIN(true, false),
        THUNDER_PREPARE(true, false),
        THUNDER(true, true);

        private final boolean raining;
        private final boolean thundering;

        State(boolean raining, boolean thundering) {
            this.raining = raining;
            this.thundering = thundering;
        }

        public boolean raining() {
            return raining;
        }

        public boolean thundering() {
            return thundering;
        }

        public static State byName(String name) {
            for (State state : values()) {
                if (state.name().equals(name)) {
                    return state;
                }
            }
            return CLEAR;
        }
    }

    /** Tunables loaded from {@code config/rewasteland/weather_cycle.json}. */
    public record Settings(
            int clearMin, int clearMax,
            int rainMin, int rainMax,
            int thunderChancePercent,
            int prepareMin, int prepareMax,
            int thunderMin, int thunderMax,
            int thunderEndMin, int thunderEndMax) {

        public static Settings defaults() {
            return new Settings(6000, 9600, 6000, 24000, 10, 3600, 6000, 6000, 12000, 3600, 6000);
        }
    }

    /** One transition: the new state and how long it lasts. */
    public record Step(State state, int ticks) {
    }

    private WeatherCycleLogic() {
    }

    public static Step start(Settings settings, IntBinaryOperator roll) {
        return new Step(State.CLEAR, roll(settings.clearMin(), settings.clearMax(), roll));
    }

    public static Step advance(State current, Settings settings, IntBinaryOperator roll) {
        return switch (current) {
            case CLEAR -> {
                int chance = roll.applyAsInt(1, 100);
                if (chance <= settings.thunderChancePercent()) {
                    yield new Step(State.THUNDER_PREPARE, roll(settings.prepareMin(), settings.prepareMax(), roll));
                }
                yield new Step(State.RAIN, roll(settings.rainMin(), settings.rainMax(), roll));
            }
            case RAIN -> new Step(State.CLEAR, roll(settings.clearMin(), settings.clearMax(), roll));
            case THUNDER_PREPARE -> new Step(State.THUNDER, roll(settings.thunderMin(), settings.thunderMax(), roll));
            case THUNDER -> new Step(State.RAIN, roll(settings.thunderEndMin(), settings.thunderEndMax(), roll));
        };
    }

    /** Duration used by the debug command when a state is forced. */
    public static int durationFor(Settings settings, State state, IntBinaryOperator roll) {
        return switch (state) {
            case CLEAR -> roll(settings.clearMin(), settings.clearMax(), roll);
            case RAIN -> roll(settings.rainMin(), settings.rainMax(), roll);
            case THUNDER_PREPARE -> roll(settings.prepareMin(), settings.prepareMax(), roll);
            case THUNDER -> roll(settings.thunderMin(), settings.thunderMax(), roll);
        };
    }

    private static int roll(int min, int max, IntBinaryOperator roll) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return roll.applyAsInt(low, high);
    }
}
