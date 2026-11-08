package com.selton.rewasteland;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Acid water: the fluid type, the still/flowing fluids, the fluid block and the bucket.
 *
 * <p>Fluids are created lazily during the fluid registry event. NeoForge fires the built-in
 * registries in vanilla order (fluid before block), so the block constructor can safely resolve
 * {@link #ACID_WATER} while the block registry event runs.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ReWasteland.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, ReWasteland.MODID);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ReWasteland.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ReWasteland.MODID);

    /** The custom fluid type. Must be registered: {@code Fluid#getFluidType} throws for mod fluids otherwise. */
    public static final DeferredHolder<FluidType, AcidWaterType> ACID_WATER_TYPE =
            FLUID_TYPES.register("acid_water", AcidWaterType::new);

    public static final DeferredHolder<Fluid, AcidWaterFluid.Source> ACID_WATER =
            FLUIDS.register("acid_water", AcidWaterFluid.Source::new);
    public static final DeferredHolder<Fluid, AcidWaterFluid.Flowing> FLOWING_ACID_WATER =
            FLUIDS.register("flowing_acid_water", AcidWaterFluid.Flowing::new);

    public static final DeferredBlock<LiquidBlock> ACID_WATER_BLOCK =
            BLOCKS.register("acid_water", () -> new LiquidBlock(ACID_WATER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    public static final DeferredItem<BucketItem> ACID_WATER_BUCKET =
            ITEMS.register("acid_water_bucket", () -> new BucketItem(ACID_WATER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private ModFluids() {
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
