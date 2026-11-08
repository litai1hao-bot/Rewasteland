package com.selton.rewasteland;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type of acid water.
 *
 * <p>NeoForge 1.21.1 drives entity water behaviour from the {@link FluidType}, so these properties
 * make acid water drown, push, extinguish, float boats and break falls exactly like vanilla water.
 * {@code canHydrate} is deliberately false so farmland, sponges and sugar cane do not treat acid
 * water as clean water.
 */
public class AcidWaterType extends FluidType {
    public AcidWaterType() {
        super(FluidType.Properties.create()
                .descriptionId("block.rewasteland.acid_water")
                .density(1000)
                .viscosity(1000)
                .temperature(300)
                .motionScale(0.014)
                .canPushEntity(true)
                .canSwim(true)
                .canDrown(true)
                .fallDistanceModifier(0.0F)
                .canExtinguish(true)
                .canConvertToSource(true)
                .supportsBoating(true)
                .canHydrate(false)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH));
    }

    /** Mirrors vanilla water: infinite sources only while the water-source game rule is on. */
    @Override
    public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
        if (reader instanceof Level level) {
            return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
        }
        return super.canConvertToSource(state, reader, pos);
    }
}
