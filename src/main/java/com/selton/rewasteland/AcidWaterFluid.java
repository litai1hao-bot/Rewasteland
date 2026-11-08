package com.selton.rewasteland;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Acid water flows like vanilla water: same slope-find distance, drop-off, tick delay, infinite
 * source rule and bucket sound, all inherited from {@link WaterFluid}. Only the identity (fluid
 * type, source/flowing fluids, legacy block, bucket, {@link #isSame}) is replaced.
 */
public abstract class AcidWaterFluid extends WaterFluid {
    @Override
    public FluidType getFluidType() {
        return ModFluids.ACID_WATER_TYPE.get();
    }

    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_ACID_WATER.get();
    }

    @Override
    public Fluid getSource() {
        return ModFluids.ACID_WATER.get();
    }

    @Override
    public Item getBucket() {
        return ModFluids.ACID_WATER_BUCKET.get();
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return ModFluids.ACID_WATER_BLOCK.get()
                .defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.ACID_WATER.get() || fluid == ModFluids.FLOWING_ACID_WATER.get();
    }

    public static class Source extends AcidWaterFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends AcidWaterFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
