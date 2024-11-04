package dev.kleinbox.roehrchen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.common.core.tracker.ChunkTransactionsAttachment;
import dev.kleinbox.roehrchen.common.feature.transaction.ItemTransaction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import static dev.kleinbox.roehrchen.Roehrchen.REGISTERED;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class TransactionsRenderer {

    @SubscribeEvent
    public static void onRenderLevelStageEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
            return;

        Minecraft instance = Minecraft.getInstance();
        ClientLevel level = instance.level;
        if (level == null)
            return;

        float partialTicks = event.getPartialTick().getRealtimeDeltaTicks();

        int simulationDistance = instance.options.simulationDistance().get();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = instance.renderBuffers().bufferSource();

        Camera info = instance.gameRenderer.getMainCamera();
        Vec3 view = info.getPosition();

        if (instance.player == null)
            return;

        // Render

        ChunkPos centerChunkPos = instance.player.chunkPosition();

        for (int x = -simulationDistance; x <= simulationDistance; x++) {
            for (int z = -simulationDistance; z <= simulationDistance; z++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkPos.x + x, centerChunkPos.z + z);
                LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);

                ChunkTransactionsAttachment transactions = chunk.getData(REGISTERED.CHUNK_TRANSACTIONS);

                for (Transaction<?, ?> transaction : transactions) {
                    if (!(transaction instanceof ItemTransaction itemTransaction))
                        continue;

                    BlockPos blockPos = transaction.blockPos;

                    poseStack.pushPose();
                    poseStack.translate(blockPos.getX()+0.5- view.x(), blockPos.getY()+0.4- view.y(), blockPos.getZ()+0.5- view.z());
                    // Rotate here

                    BakedModel model = instance.getItemRenderer().getModel(itemTransaction.product, level, null, 0);
                    instance.getItemRenderer().render(
                            itemTransaction.product,
                            ItemDisplayContext.GROUND,
                            false,
                            poseStack,
                            bufferSource,
                            level.getLightEmission(itemTransaction.blockPos),
                            OverlayTexture.NO_OVERLAY,
                            model);

                    poseStack.popPose();
                }
            }
        }
    }
}
