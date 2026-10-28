
package com.luigi.enderinfestation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = EnderInfestation.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME
)
public final class EnderfestedEmissiveRenderer {

    private static final ResourceLocation OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    EnderInfestation.MOD_ID,
                    "textures/misc/enderfested_overlay.png"
            );

    private static final int HORIZONTAL_RANGE = 12;
    private static final int VERTICAL_RANGE = 8;
    private static final float FACE_EPSILON = 0.001F;

    // Purple tint. Set to 255, 255, 255 for the PNG's original colors.
    private static final int TINT_RED = 210;
    private static final int TINT_GREEN = 135;
    private static final int TINT_BLUE = 255;
    private static final int TINT_ALPHA = 255;

    private EnderfestedEmissiveRenderer() {
    }

    @SubscribeEvent
    public static void renderEmissiveOverlays(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null || minecraft.player == null) {
            return;
        }

        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;

        BlockPos cameraBlock = BlockPos.containing(
                cameraX, cameraY, cameraZ
        );

        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();

        RenderType renderType = RenderType.entityTranslucent(OVERLAY_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -HORIZONTAL_RANGE; x <= HORIZONTAL_RANGE; x++) {
            for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
                for (int z = -HORIZONTAL_RANGE; z <= HORIZONTAL_RANGE; z++) {

                    mutable.set(
                            cameraBlock.getX() + x,
                            cameraBlock.getY() + y,
                            cameraBlock.getZ() + z
                    );

                    BlockState state = level.getBlockState(mutable);

                    if (!isEnderfested(state)) {
                        continue;
                    }

                    if (!event.getFrustum().isVisible(new AABB(mutable))) {
                        continue;
                    }

                    poseStack.pushPose();

                    try {
                        poseStack.translate(
                                mutable.getX(),
                                mutable.getY(),
                                mutable.getZ()
                        );

                        drawCubeOverlay(consumer, poseStack);
                    } finally {
                        poseStack.popPose();
                    }
                }
            }
        }

        buffers.endBatch(renderType);
    }

    private static boolean isEnderfested(BlockState state) {
        return state.is(ModBlocks.ENDERFESTED_STONE.get())
                || state.is(ModBlocks.ENDERFESTED_COBBLESTONE.get())
                || state.is(ModBlocks.ENDERFESTED_DEEPSLATE.get())
                || state.is(ModBlocks.ENDERFESTED_COBBLED_DEEPSLATE.get())
                || state.is(ModBlocks.ENDERFESTED_DIRT.get())
                || state.is(ModBlocks.ENDERFESTED_COARSE_DIRT.get())
                || state.is(ModBlocks.ENDERFESTED_GRAVEL.get())
                || state.is(ModBlocks.ENDERFESTED_SAND.get())
                || state.is(ModBlocks.ENDERFESTED_SANDSTONE.get())
                || state.is(ModBlocks.ENDERFESTED_RED_SANDSTONE.get())
                || state.is(ModBlocks.ENDERFESTED_OAK_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_OAK_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_SPRUCE_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_SPRUCE_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_BIRCH_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_BIRCH_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_JUNGLE_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_JUNGLE_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_ACACIA_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_ACACIA_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_DARK_OAK_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_DARK_OAK_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_MANGROVE_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_MANGROVE_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_CHERRY_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_CHERRY_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_AZALEA_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_FLOWERING_AZALEA_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_CRIMSON_STEM.get())
                || state.is(ModBlocks.ENDERFESTED_WARPED_STEM.get());
    }

    private static void drawCubeOverlay(
            VertexConsumer consumer,
            PoseStack poseStack
    ) {
        PoseStack.Pose pose = poseStack.last();

        float lo = -FACE_EPSILON;
        float hi = 1.0F + FACE_EPSILON;

        // Front (+Z)
        vertex(consumer, pose, lo, lo, hi, 0, 1, 0, 0, 1);
        vertex(consumer, pose, hi, lo, hi, 1, 1, 0, 0, 1);
        vertex(consumer, pose, hi, hi, hi, 1, 0, 0, 0, 1);
        vertex(consumer, pose, lo, hi, hi, 0, 0, 0, 0, 1);

        // Back (-Z)
        vertex(consumer, pose, hi, lo, lo, 0, 1, 0, 0, -1);
        vertex(consumer, pose, lo, lo, lo, 1, 1, 0, 0, -1);
        vertex(consumer, pose, lo, hi, lo, 1, 0, 0, 0, -1);
        vertex(consumer, pose, hi, hi, lo, 0, 0, 0, 0, -1);

        // Right (+X)
        vertex(consumer, pose, hi, lo, hi, 0, 1, 1, 0, 0);
        vertex(consumer, pose, hi, lo, lo, 1, 1, 1, 0, 0);
        vertex(consumer, pose, hi, hi, lo, 1, 0, 1, 0, 0);
        vertex(consumer, pose, hi, hi, hi, 0, 0, 1, 0, 0);

        // Left (-X)
        vertex(consumer, pose, lo, lo, lo, 0, 1, -1, 0, 0);
        vertex(consumer, pose, lo, lo, hi, 1, 1, -1, 0, 0);
        vertex(consumer, pose, lo, hi, hi, 1, 0, -1, 0, 0);
        vertex(consumer, pose, lo, hi, lo, 0, 0, -1, 0, 0);

        // Top (+Y)
        vertex(consumer, pose, lo, hi, hi, 0, 1, 0, 1, 0);
        vertex(consumer, pose, hi, hi, hi, 1, 1, 0, 1, 0);
        vertex(consumer, pose, hi, hi, lo, 1, 0, 0, 1, 0);
        vertex(consumer, pose, lo, hi, lo, 0, 0, 0, 1, 0);

        // Bottom (-Y)
        vertex(consumer, pose, lo, lo, lo, 0, 1, 0, -1, 0);
        vertex(consumer, pose, hi, lo, lo, 1, 1, 0, -1, 0);
        vertex(consumer, pose, hi, lo, hi, 1, 0, 0, -1, 0);
        vertex(consumer, pose, lo, lo, hi, 0, 0, 0, -1, 0);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(
                        TINT_RED,
                        TINT_GREEN,
                        TINT_BLUE,
                        TINT_ALPHA
                )
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
