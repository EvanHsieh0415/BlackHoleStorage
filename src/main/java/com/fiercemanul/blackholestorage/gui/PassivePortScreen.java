package com.fiercemanul.blackholestorage.gui;

import com.fiercemanul.blackholestorage.BlackHoleStorage;
import com.fiercemanul.blackholestorage.channel.ClientChannelManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PassivePortScreen extends AbstractContainerScreen<PassivePortMenu> {

    private static final ResourceLocation GUI_IMG = new ResourceLocation(BlackHoleStorage.MODID, "textures/gui/passive_port.png");
    public final int imageWidth = 202;
    public final int imageHeight = 249;
    private BlockState passivePortBlockState = menu.player.level.getBlockState(menu.blockPos);
    private final ItemStack passivePortItem = new ItemStack(BlackHoleStorage.PASSIVE_PORT_ITEM.get());
    private final String ownerName = ClientChannelManager.getInstance().getUserName(menu.owner);


    public PassivePortScreen(PassivePortMenu menu, Inventory pPlayerInventory, Component pTitle) {
        super(menu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - imageWidth + 4) / 2;
        this.topPos = (this.height - imageHeight) / 2;
        this.addRenderableWidget(new FaceButton(leftPos + 45, topPos + 164, 0, BlockStateProperties.NORTH));
        this.addRenderableWidget(new FaceButton(leftPos + 45, topPos + 202, 1, BlockStateProperties.SOUTH));
        this.addRenderableWidget(new FaceButton(leftPos + 26, topPos + 183, 2, BlockStateProperties.WEST));
        this.addRenderableWidget(new FaceButton(leftPos + 64, topPos + 183, 3, BlockStateProperties.EAST));
        this.addRenderableWidget(new FaceButton(leftPos + 140, topPos + 164, 4, BlockStateProperties.UP));
        this.addRenderableWidget(new FaceButton(leftPos + 140, topPos + 202, 5, BlockStateProperties.DOWN));
        this.addRenderableWidget(new ToggleLockButton(leftPos + 121, topPos + 183));
        this.addRenderableWidget(new ChannelButton(leftPos + 159, topPos + 183));
        setPassivePortItemNbt();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderBigBlock();
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI_IMG);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);
    }

    private void renderBigBlock() {
        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.translate(leftPos + 101.0D, topPos + 84.0D, 200.0F);
        poseStack.scale(128.0F, -128.0F, 128.0F);
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        this.itemRenderer.renderStatic(
                passivePortItem,
                ItemTransforms.TransformType.GUI,
                15728880,
                OverlayTexture.NO_OVERLAY,
                new PoseStack(),
                multibuffersource$buffersource,
                0);
        poseStack.popPose();
        multibuffersource$buffersource.endBatch();
    }

    @Override
    protected void containerTick() {
        if (menu.player.level.getBlockState(menu.blockPos) != passivePortBlockState) {
            passivePortBlockState = menu.player.level.getBlockState(menu.blockPos);
            setPassivePortItemNbt();
        }
    }

    private void setPassivePortItemNbt() {
        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.putString("north", passivePortBlockState.getValue(BlockStateProperties.NORTH).toString());
        blockEntityTag.putString("south", passivePortBlockState.getValue(BlockStateProperties.SOUTH).toString());
        blockEntityTag.putString("west", passivePortBlockState.getValue(BlockStateProperties.WEST).toString());
        blockEntityTag.putString("east", passivePortBlockState.getValue(BlockStateProperties.EAST).toString());
        blockEntityTag.putString("up", passivePortBlockState.getValue(BlockStateProperties.UP).toString());
        blockEntityTag.putString("down", passivePortBlockState.getValue(BlockStateProperties.DOWN).toString());
        CompoundTag nbt = new CompoundTag();
        nbt.put("BlockStateTag", blockEntityTag);
        passivePortItem.setTag(nbt);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
    }

    private void renderToolTip(PoseStack pPoseStack, List<? extends FormattedCharSequence> pTooltips, int pMouseX, int pMouseY) {
        super.renderTooltip(pPoseStack, pTooltips, pMouseX, pMouseY);
    }

    private class FaceButton extends ImageButton {

        private final int id;
        private final BooleanProperty property;
        public FaceButton(int pX, int pY, int id, BooleanProperty property) {
            super(pX, pY, 17, 17, 202, 0, GUI_IMG, pButton ->
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id));
            this.id = id;
            this.property = property;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, GUI_IMG);
            RenderSystem.enableDepthTest();
            float uOffset = this.isHoveredOrFocused() ? 219.0F : 202.0F;
            blit(pPoseStack, this.x, this.y, uOffset, id * 17, this.width, this.height, 256, 256);
            if (passivePortBlockState.getValue(property))
                blit(pPoseStack, this.x, this.y, 236, 0, this.width, this.height, 256, 256);
        }
    }

    private class ToggleLockButton extends ImageButton {

        public ToggleLockButton(int pX, int pY) {
            super(pX, pY, 17, 17, 202, 102, GUI_IMG, pButton -> {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 6);
                menu.locked = !menu.locked;
            });
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, GUI_IMG);
            RenderSystem.enableDepthTest();
            float uOffset = this.isHoveredOrFocused() ? 219.0F : 202.0F;
            float vOffset = menu.locked ? 119.0F : 102.0F;
            blit(pPoseStack, this.x, this.y, uOffset, vOffset, this.width, this.height, 256, 256);
            if (this.isHovered) this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }

        @Override
        public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
            List<FormattedCharSequence> list = new ArrayList<>();
            UUID owner = menu.owner;
            UUID user = menu.player.getUUID();
            if (owner.equals(user)) {
                list.add(Component.translatable("bhs.GUI.owner", "§a" + menu.player.getGameProfile().getName()).getVisualOrderText());
            } else if (menu.locked) {
                list.add(Component.translatable("bhs.GUI.owner", "§c" + ownerName).getVisualOrderText());
            } else {
                list.add(Component.translatable("bhs.GUI.owner", ownerName).getVisualOrderText());
            }
            PassivePortScreen.this.renderToolTip(pPoseStack, list, pMouseX, pMouseY);
        }
    }

    private class ChannelButton extends ImageButton {

        public ChannelButton(int pX, int pY) {
            super(pX, pY, 17, 17, 202, 136, GUI_IMG, pButton ->
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 7));
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, GUI_IMG);
            RenderSystem.enableDepthTest();
            float uOffset = this.isHoveredOrFocused() ? 219.0F : 202.0F;
            blit(pPoseStack, this.x, this.y, uOffset, 136.0F, this.width, this.height, 256, 256);
            if (this.isHovered) this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }

        @Override
        public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
            List<FormattedCharSequence> list = new ArrayList<>();
            String flag = "";
            if (menu.channelOwner.equals(menu.player.getUUID())) flag = "§a";
            else if (!menu.channelOwner.equals(BlackHoleStorage.FAKE_PLAYER_UUID)) flag = "§c";
            list.add(Component.translatable("bhs.GUI.channel.tip1", flag + menu.channelName).getVisualOrderText());
            list.add(Component.translatable("bhs.GUI.channel.tip2",
                    flag + ClientChannelManager.getInstance().getUserName(menu.channelOwner)).getVisualOrderText());
            PassivePortScreen.this.renderToolTip(pPoseStack, list, pMouseX, pMouseY);
        }
    }


}
