package com.yesmenn.ducttape.client.kit.element;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 3D live preview of a block, an item or anything a mod draws itself (e.g. particles of a particle editor). Drag with
 * the left mouse button to turn it; right click resets the view; after a click the scroll wheel zooms. Optional slow
 * auto-rotation, paused while dragging.
 * <p>
 * Content is drawn in block space: the unit cube from (0,0,0) to (1,1,1), centered in the preview.
 */
public class YmPreview extends YmElement {
    /** Draws the previewed thing into the unit cube. Called every frame, so it can animate. */
    @FunctionalInterface
    public interface Content {
        void render(PoseStack pose, MultiBufferSource buffers, float partialTick);
    }

    private static final float DEFAULT_YAW = -35.0F;
    private static final float DEFAULT_PITCH = 25.0F;
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Z of the preview's center; content stays well below the popups (300+). */
    private static final float DEPTH = 150.0F;

    private final Content content;
    private float homeYaw = DEFAULT_YAW;
    private float homePitch = DEFAULT_PITCH;
    private float homeZoom = 1.0F;
    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float zoom = 1.0F;
    private boolean autoRotate;
    private float rotateSpeed = 30.0F;
    private boolean dragging;
    private long lastFrame;

    public YmPreview(Content content) {
        super(0, 96);
        this.content = content;
    }

    /** Preview of a block state (follows the supplier, e.g. a block the player picks). */
    public static YmPreview block(Supplier<BlockState> state) {
        return new YmPreview((pose, buffers, partialTick) -> Minecraft.getInstance().getBlockRenderer()
                .renderSingleBlock(state.get(), pose, buffers, FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
    }

    /**
     * Preview of the block at a position of the client world as it looks there: its model tinted with the world's
     * block colors, plus what its block entity renderer draws (moving parts, models drawn in code). Follows changes
     * live, e.g. a lamp whose color or direction is being edited.
     */
    public static YmPreview blockAt(Supplier<BlockPos> pos) {
        return blockAt(pos, state -> null);
    }

    /**
     * Like {@link #blockAt(Supplier)}, but turned so that the side {@code up} names points up (null: as placed). For
     * blocks placed on walls or ceilings that look best lying on the floor (panels, lamps).
     */
    public static YmPreview blockAt(Supplier<BlockPos> pos, Function<BlockState, Direction> up) {
        return new YmPreview((pose, buffers, partialTick) -> {
            Minecraft minecraft = Minecraft.getInstance();
            BlockPos blockPos = pos.get();
            if (minecraft.level == null || blockPos == null) {
                return;
            }
            BlockState state = minecraft.level.getBlockState(blockPos);
            Direction side = up.apply(state);
            if (side != null && side != Direction.UP) {
                pose.translate(0.5F, 0.5F, 0.5F);
                pose.mulPose(switch (side) {
                    case DOWN -> Axis.XP.rotationDegrees(180.0F);
                    case NORTH -> Axis.XP.rotationDegrees(90.0F);
                    case SOUTH -> Axis.XP.rotationDegrees(-90.0F);
                    case EAST -> Axis.ZP.rotationDegrees(90.0F);
                    default -> Axis.ZP.rotationDegrees(-90.0F);
                });
                pose.translate(-0.5F, -0.5F, -0.5F);
            }
            if (state.getRenderShape() == RenderShape.MODEL) {
                BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
                int tint = minecraft.getBlockColors().getColor(state, minecraft.level, blockPos, 0);
                float red = tint == -1 ? 1.0F : (tint >> 16 & 0xFF) / 255.0F;
                float green = tint == -1 ? 1.0F : (tint >> 8 & 0xFF) / 255.0F;
                float blue = tint == -1 ? 1.0F : (tint & 0xFF) / 255.0F;
                // One translucent sheet for all models: also right for solid and cutout textures.
                minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                        buffers.getBuffer(Sheets.translucentCullBlockSheet()), state, model, red, green, blue,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                minecraft.getBlockEntityRenderDispatcher().renderItem(blockEntity, pose, buffers, FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            }
        });
    }

    /** Preview of an item (follows the supplier). */
    public static YmPreview item(Supplier<ItemStack> stack) {
        return new YmPreview((pose, buffers, partialTick) -> {
            pose.translate(0.5F, 0.5F, 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack.get(), ItemDisplayContext.GUI, FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
        });
    }

    /** Turns slowly on its own ({@code degreesPerSecond}), paused while the player drags. */
    public YmPreview setAutoRotate(boolean autoRotate, float degreesPerSecond) {
        this.autoRotate = autoRotate;
        this.rotateSpeed = degreesPerSecond;
        return this;
    }

    public YmPreview setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
        return this;
    }

    /** Sets the view; it is also the one a right click returns to. */
    public YmPreview setView(float yaw, float pitch, float zoom) {
        this.homeYaw = yaw;
        this.homePitch = pitch;
        this.homeZoom = zoom;
        resetView();
        return this;
    }

    public void resetView() {
        yaw = homeYaw;
        pitch = homePitch;
        zoom = homeZoom;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        if (autoRotate && !dragging && lastFrame != 0) {
            yaw += rotateSpeed * Math.min(200, now - lastFrame) / 1000.0F;
        }
        lastFrame = now;

        YmStyle style = style();
        int x = getX();
        int y = getY();
        style.drawInset(graphics, x, y, width, height);
        style.horizontalGradient(graphics, x + 1, y + height / 2, x + width - 1, y + height - 1, 0x18FFFFFF,
                0x06FFFFFF);
        if (isFocused() || dragging) {
            style.outline(graphics, x, y, width, height, style.accentDim);
        }

        graphics.flush();
        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // Scale so the cube's diagonal fits; capped so the content stays in front of the GUI and below popups.
        float size = Math.min(Math.min(width, height) * 0.5F * zoom, 100.0F);
        pose.translate(x + width / 2.0F, y + height / 2.0F, DEPTH);
        pose.scale(size, -size, size);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5F, -0.5F, -0.5F);
        Lighting.setupFor3DItems();
        content.render(pose, graphics.bufferSource(), partialTick);
        graphics.flush();
        pose.popPose();
        graphics.disableScissor();
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            resetView();
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        requestFocus();
        dragging = true;
        captureMouse();
        return true;
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            yaw += (float) dragX * 1.5F;
            pitch = Math.max(-89.0F, Math.min(89.0F, pitch + (float) dragY * 1.5F));
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isFocused()) {
            return false;
        }
        zoom = Math.max(0.4F, Math.min(3.0F, zoom * (amount > 0 ? 1.12F : 1.0F / 1.12F)));
        return true;
    }
}
