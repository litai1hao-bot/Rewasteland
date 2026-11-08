package com.selton.rewasteland;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.joml.Vector3f;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ReWasteland.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ReWasteland.MODID, value = Dist.CLIENT)
public class ReWastelandClient {
    public ReWastelandClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Fluid chunk layers default to solid; without this the acid water would render opaque.
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ACID_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_ACID_WATER.get(), RenderType.translucent());
        });
    }

    /**
     * Visuals of acid water: custom acid-green water sprites (still/flow/overlay already baked
     * green), green fog and a green underwater overlay. The tint is white so the baked green is
     * not tinted twice.
     */
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL =
                    ResourceLocation.fromNamespaceAndPath(ReWasteland.MODID, "block/acid_water_still");
            private static final ResourceLocation FLOW =
                    ResourceLocation.fromNamespaceAndPath(ReWasteland.MODID, "block/acid_water_flow");
            private static final ResourceLocation OVERLAY =
                    ResourceLocation.fromNamespaceAndPath(ReWasteland.MODID, "block/acid_water_overlay");
            private static final ResourceLocation UNDERWATER =
                    ResourceLocation.fromNamespaceAndPath(ReWasteland.MODID, "textures/misc/acid_underwater.png");
            private static final int ACID_TINT = 0xFFFFFFFF;

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
                return UNDERWATER;
            }

            @Override
            public int getTintColor() {
                return ACID_TINT;
            }

            @Override
            public Vector3f modifyFogColor(
                    Camera camera,
                    float partialTick,
                    ClientLevel level,
                    int renderDistance,
                    float darkenWorldAmount,
                    Vector3f fluidFogColor) {
                return new Vector3f(0.16F, 0.30F, 0.05F);
            }

            @Override
            public void modifyFogRender(
                    Camera camera,
                    FogRenderer.FogMode mode,
                    float renderDistance,
                    float partialTick,
                    float nearDistance,
                    float farDistance,
                    FogShape shape) {
                // Same underwater fog distances as vanilla water.
                RenderSystem.setShaderFogStart(-8.0F);
                RenderSystem.setShaderFogEnd(96.0F);
                RenderSystem.setShaderFogShape(FogShape.CYLINDER);
            }
        }, ModFluids.ACID_WATER_TYPE.get());
    }
}
