package com.fiercemanul.blackholestorage.block;

import com.fiercemanul.blackholestorage.BlackHoleStorage;
import com.fiercemanul.blackholestorage.channel.*;
import com.fiercemanul.blackholestorage.network.ChannelSetPack;
import com.fiercemanul.blackholestorage.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.UUID;

import static com.fiercemanul.blackholestorage.BlackHoleStorage.PASSIVE_PORT_BLOCK_ENTITY;

public class PassivePortBlockEntity extends BlockEntity implements IChannelTerminal {

    private UUID owner;
    private boolean locked = false;
    private UUID channelOwner;
    private int channelID = -1;
    private ServerChannel channel = NullChannel.INSTANCE;
    private boolean north = true;
    private boolean south = true;
    private boolean east = true;
    private boolean west = true;
    private boolean up = true;
    private boolean down = true;
    private boolean waterlogged = false;
    @Nullable
    private ServerPlayer user;

    private LazyOptional<IItemHandler> capability = LazyOptional.of(() -> channel);


    public PassivePortBlockEntity(BlockPos pos, BlockState state) {
        super(PASSIVE_PORT_BLOCK_ENTITY.get(), pos, state);
        onBlockStateChange();
    }


    @Override
    public void load(CompoundTag pTag) {
        if (pTag.contains("owner")) {
            owner = pTag.getUUID("owner");
            locked = pTag.getBoolean("locked");
        }
        if (pTag.contains("channel")) {
            CompoundTag channel = pTag.getCompound("channel");
            channelOwner = channel.getUUID("channelOwner");
            channelID = channel.getInt("channelID");
        }
        channel = ServerChannelManager.getInstance().getChannel(channelOwner, channelID);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        if (owner != null) {
            pTag.putUUID("owner", owner);
            pTag.putBoolean("locked", locked);
        }
        if (channelID >= 0) {
            CompoundTag channel = new CompoundTag();
            channel.putUUID("channelOwner", channelOwner);
            channel.putInt("channelID", channelID);
            pTag.put("channel", channel);
        }
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        this.setChanged();
    }

    public void setUser(ServerPlayer user) {
        this.user = user;
    }

    public boolean hasUser() {
        return user != null;
    }

    public String getChannelName() {
        return channel.getName();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PassivePortBlockEntity blockEntity) {
        if (level.isClientSide) return;
        if (blockEntity.waterlogged && !blockEntity.channel.isRemoved()) blockEntity.channel.addFluid(new FluidStack(Fluids.WATER, 1000));
    }

    public void onBlockStateChange() {
        BlockState state = getBlockState();
        north = state.getValue(BlockStateProperties.NORTH);
        south = state.getValue(BlockStateProperties.SOUTH);
        west = state.getValue(BlockStateProperties.WEST);
        east = state.getValue(BlockStateProperties.EAST);
        up = state.getValue(BlockStateProperties.UP);
        down = state.getValue(BlockStateProperties.DOWN);
        waterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
    }


    @Override
    public UUID getTerminalOwner() {
        return owner;
    }

    @Override
    public @Nullable ChannelInfo getChannelInfo() {
        if (channelID >= 0) return new ChannelInfo(channelOwner, channelID);
        return null;
    }

    @Override
    public void setChannel(UUID channelOwner, int channelID) {
        this.channelOwner = channelOwner;
        this.channelID = channelID;
        this.setChanged();
        this.channel = ServerChannelManager.getInstance().getChannel(channelOwner, channelID);
        this.capability = LazyOptional.of(() -> channel);
        if (user != null) ServerChannelManager.sendChannelSet(user, owner, channelOwner, channelID);
    }

    @Override
    public void removeChannel(ServerPlayer actor) {
        this.channelID = -1;
        this.channelOwner = null;
        this.setChanged();
        this.channel = NullChannel.INSTANCE;
        if (user != null) NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> user), new ChannelSetPack((byte) -1, -1, ""));
        if (!actor.addItem(new ItemStack(BlackHoleStorage.STORAGE_CORE.get())))
            actor.drop(new ItemStack(BlackHoleStorage.STORAGE_CORE.get()), false);
    }

    @Override
    public void renameChannel(ServerPlayer actor, String name) {
        if (channelID < 0) return;
        if (actor.getUUID().equals(channelOwner) || channelOwner.equals(BlackHoleStorage.FAKE_PLAYER_UUID))
            ServerChannelManager.getInstance().renameChannel(new ChannelInfo(channelOwner, channelID), name);
    }

    @Override
    public void addChannelSelector(ServerPlayer player) {
        this.user = player;
        if (channelID < 0) return;
        ServerChannelManager.sendChannelSet(player, owner, channelOwner, channelID);
    }

    @Override
    public void removeChannelSelector(ServerPlayer player) {
        this.user = null;
    }

    @Override
    public boolean stillValid() {
        return !isRemoved();
    }

    @Override
    public void tryReOpenMenu(ServerPlayer player) {
        if (channelID >= 0) this.getBlockState().use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(
                new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5), Direction.UP, worldPosition, false));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (channel.isRemoved()) return super.getCapability(cap, side);
        if (side == Direction.NORTH && !north) return super.getCapability(cap, side);
        else if (side == Direction.SOUTH && !south) return super.getCapability(cap, side);
        else if (side == Direction.WEST && !west) return super.getCapability(cap, side);
        else if (side == Direction.EAST && !east) return super.getCapability(cap, side);
        else if (side == Direction.UP && !up) return super.getCapability(cap, side);
        else if (side == Direction.DOWN && !down) return super.getCapability(cap, side);
        else if (cap == ForgeCapabilities.ITEM_HANDLER
                || cap == ForgeCapabilities.FLUID_HANDLER
                || cap == ForgeCapabilities.ENERGY) {
            return capability.cast();
        }
        return super.getCapability(cap, side);
    }
}