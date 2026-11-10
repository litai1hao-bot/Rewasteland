package com.selton.rewasteland;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists the weather state machine (current state + deadline) in the overworld level data.
 *
 * <p>The deadline is an absolute {@link ServerLevel#getGameTime()} value, so it survives restarts
 * and the per-tick check is just a {@code long} comparison.</p>
 */
public final class WeatherCycleData extends SavedData {
    private static final String NAME = "rewasteland_weather";

    private WeatherCycleLogic.State state = WeatherCycleLogic.State.CLEAR;
    private long nextTransitionGameTime;

    public static WeatherCycleData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WeatherCycleData::new, WeatherCycleData::load), NAME);
    }

    private static WeatherCycleData load(CompoundTag tag, HolderLookup.Provider registries) {
        WeatherCycleData data = new WeatherCycleData();
        data.state = WeatherCycleLogic.State.byName(tag.getString("state"));
        data.nextTransitionGameTime = tag.getLong("next_transition");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("state", state.name());
        tag.putLong("next_transition", nextTransitionGameTime);
        return tag;
    }

    public WeatherCycleLogic.State state() {
        return state;
    }

    public long nextTransitionGameTime() {
        return nextTransitionGameTime;
    }

    public void set(WeatherCycleLogic.State state, long nextTransitionGameTime) {
        this.state = state;
        this.nextTransitionGameTime = nextTransitionGameTime;
        setDirty();
    }
}
