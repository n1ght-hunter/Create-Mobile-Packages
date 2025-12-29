package de.theidler.create_mobile_packages.blocks.advanced_bee_port;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import de.theidler.create_mobile_packages.CMPHelper;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.RoboRequest;
import de.theidler.create_mobile_packages.index.CMPBlockEntities;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.index.config.CMPConfigs;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import de.theidler.create_mobile_packages.items.upgrades.EnderUpgradeItem;
import de.theidler.create_mobile_packages.items.upgrades.SpeedUpgradeItem;
import de.theidler.create_mobile_packages.robo.RoboManager;
import de.theidler.create_mobile_packages.robo.VirtualRobo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlock.IS_OPEN_TEXTURE;

/**
 * Advanced Bee Port with upgrade slots for Speed and Ender upgrades.
 */
public class AdvancedBeePortBlockEntity extends PackagePortBlockEntity {

    private static final int ROBOBEE_INVENTORY_STACK_SIZE = 64;
    private final ContainerData data = new SimpleContainerData(2);
    private final ItemStackHandler roboBeeInventory = new ItemStackHandler(1);
    private final ItemStackHandler upgradeInventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof SpeedUpgradeItem;
            if (slot == 1) return stack.getItem() instanceof EnderUpgradeItem;
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // Only one upgrade per slot
        }
    };

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots() + roboBeeInventory.getSlots() + upgradeInventory.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < inventory.getSlots()) {
                return inventory.getStackInSlot(slot);
            } else if (slot < inventory.getSlots() + roboBeeInventory.getSlots()) {
                return roboBeeInventory.getStackInSlot(slot - inventory.getSlots());
            } else {
                return upgradeInventory.getStackInSlot(slot - inventory.getSlots() - roboBeeInventory.getSlots());
            }
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.getItem() instanceof RoboBeeItem) {
                if (slot >= inventory.getSlots() && slot < inventory.getSlots() + roboBeeInventory.getSlots()) {
                    return roboBeeInventory.insertItem(slot - inventory.getSlots(), stack, simulate);
                } else {
                    return stack;
                }
            } else if (stack.getItem() instanceof SpeedUpgradeItem || stack.getItem() instanceof EnderUpgradeItem) {
                if (slot >= inventory.getSlots() + roboBeeInventory.getSlots()) {
                    return upgradeInventory.insertItem(slot - inventory.getSlots() - roboBeeInventory.getSlots(), stack, simulate);
                } else {
                    return stack;
                }
            } else {
                if (slot < inventory.getSlots()) {
                    return inventory.insertItem(slot, stack, simulate);
                } else {
                    return stack;
                }
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < inventory.getSlots()) {
                return inventory.extractItem(slot, amount, simulate);
            } else if (slot < inventory.getSlots() + roboBeeInventory.getSlots()) {
                return roboBeeInventory.extractItem(slot - inventory.getSlots(), amount, simulate);
            } else {
                return upgradeInventory.extractItem(slot - inventory.getSlots() - roboBeeInventory.getSlots(), amount, simulate);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < inventory.getSlots()) {
                return inventory.getSlotLimit(slot);
            } else if (slot < inventory.getSlots() + roboBeeInventory.getSlots()) {
                return roboBeeInventory.getSlotLimit(slot - inventory.getSlots());
            } else {
                return upgradeInventory.getSlotLimit(slot - inventory.getSlots() - roboBeeInventory.getSlots());
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.getItem() instanceof RoboBeeItem) {
                return slot >= inventory.getSlots() && slot < inventory.getSlots() + roboBeeInventory.getSlots();
            } else if (stack.getItem() instanceof SpeedUpgradeItem) {
                return slot == inventory.getSlots() + roboBeeInventory.getSlots();
            } else if (stack.getItem() instanceof EnderUpgradeItem) {
                return slot == inventory.getSlots() + roboBeeInventory.getSlots() + 1;
            } else {
                return slot < inventory.getSlots() && inventory.isItemValid(slot, stack);
            }
        }
    };

    public LogisticallyLinkedBehaviour behaviour;
    private int tickCounter = 0;
    private int roboSendCooldown = 0;

    public AdvancedBeePortBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CMPBlockEntities.ADVANCED_BEE_PORT.get(),
                (be, direction) -> be.handler
        );
    }

    // Upgrade helper methods
    public boolean hasSpeedUpgrade() {
        return !upgradeInventory.getStackInSlot(0).isEmpty();
    }

    public boolean hasEnderUpgrade() {
        return !upgradeInventory.getStackInSlot(1).isEmpty();
    }

    public int getEffectiveSpeed() {
        int baseSpeed = CMPConfigs.server().beeSpeed.get();
        if (hasSpeedUpgrade()) {
            return baseSpeed * CMPConfigs.server().speedUpgradeMultiplier.get();
        }
        return baseSpeed;
    }

    public boolean isCrossDimensionalEnabled() {
        return hasEnderUpgrade() && CMPConfigs.server().enderUpgradeEnabled.get();
    }

    public ItemStackHandler getUpgradeInventory() {
        return upgradeInventory;
    }

    // Static methods
    public static void setOpen(AdvancedBeePortBlockEntity entity, boolean open) {
        if (entity == null || entity.level == null) return;
        if (entity.isRemoved()) return;
        entity.level.setBlockAndUpdate(entity.getBlockPos(), entity.getBlockState().setValue(IS_OPEN_TEXTURE, open));
        entity.level.playSound(null, entity.getBlockPos(), open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS);
    }

    private synchronized void requestRoboEntity() {
        if (level instanceof ServerLevel serverLevel) {
            RoboManager.get(serverLevel).requestRobo(this.getBlockPos(), this.getLogisticsNetworkId());
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("RoboBeeInventory", roboBeeInventory.serializeNBT(registries));
        tag.put("UpgradeInventory", upgradeInventory.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("RoboBeeInventory")) {
            roboBeeInventory.deserializeNBT(registries, tag.getCompound("RoboBeeInventory"));
        }
        if (tag.contains("UpgradeInventory")) {
            upgradeInventory.deserializeNBT(registries, tag.getCompound("UpgradeInventory"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCounter % 20 == 0) {
            processItems();
        }
        if (level instanceof ServerLevel serverLevel) {
            List<Integer> eta = RoboManager.get(serverLevel).getETAs(this.getBlockPos());
            int minEta = eta.stream().min(Comparator.naturalOrder()).orElse(-1);
            this.data.set(0, minEta);
            this.data.set(1, eta.isEmpty() ? 0 : 1);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(behaviour = new LogisticallyLinkedBehaviour(this, true));
        super.addBehaviours(behaviours);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide()) return;
        if (level.hasNeighborSignal(worldPosition)) {
            tryPushingToAdjacentInventories();
        } else {
            tryPullingFromAdjacentInventories();
        }
    }

    private void tryPushingToAdjacentInventories() {
        boolean stackToPush = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                stackToPush = true;
            }
        }
        if (!stackToPush) return;

        for (IItemHandler adjacentInventory : getAdjacentInventories()) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stackInSlot = inventory.extractItem(i, 1, true);
                if (stackInSlot.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(adjacentInventory, stackInSlot, false);
                if (remainder.isEmpty() && level != null) {
                    inventory.extractItem(i, 1, false);
                    level.blockEntityChanged(worldPosition);
                }
            }
        }
    }

    private void tryPullingFromAdjacentInventories() {
        getAdjacentInventories().forEach((inv) -> {
            if (inv == null) return;
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack itemStack = inv.getStackInSlot(i);
                if (!itemStack.isEmpty() && PackageItem.isPackage(itemStack)) {
                    addItemStack(inv.extractItem(i, 1, false));
                }
            }
        });
    }

    private List<IItemHandler> getAdjacentInventories() {
        List<IItemHandler> inventories = new java.util.ArrayList<>();
        for (Direction side : Direction.values()) {
            IItemHandler inv = getAdjacentInventory(side);
            if (inv != null) {
                inventories.add(inv);
            }
        }
        return inventories;
    }

    private IItemHandler getAdjacentInventory(Direction side) {
        if (level == null) return null;
        BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(side));
        if (blockEntity == null || blockEntity instanceof FrogportBlockEntity)
            return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), side.getOpposite());
    }

    private void processItems() {
        if (level == null || level.isClientSide) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            if (roboSendCooldown-- > 0) {
                return;
            }
            ItemStack itemStack = inventory.getStackInSlot(i);
            if (!itemStack.isEmpty()) {
                sendItem(itemStack, i);
            }
        }
    }

    private void sendItem(ItemStack itemStack, int slot) {
        if (level == null || !PackageItem.isPackage(itemStack)) return;
        String address = PackageItem.getAddress(itemStack);
        if (address.isBlank()) return;

        for (Player player : level.players()) {
            if (CMPHelper.doesAddressMatchPlayer(player, address) && CMPHelper.isWithinRange(player.blockPosition(), this.getBlockPos())) {
                sendToPlayer(player, itemStack, slot);
                return;
            }
        }

        if (CMPConfigs.server().portToPort.get() && !PackageItem.matchAddress(address, addressFilter)) {
            BeePortBlockEntity beePortBlockEntity = CMPHelper.getClosestBeePort(level, address, this.getBlockPos(), null, getLogisticsNetworkId());
            if (beePortBlockEntity != null && !beePortBlockEntity.isFull()) {
                sendAdvancedDrone(itemStack, slot);
            }
        }
    }

    private void sendToPlayer(Player player, ItemStack itemStack, int slot) {
        if (roboBeeInventory.getStackInSlot(0).getCount() <= 0) {
            if (!hasRoboRequest() && level != null) {
                requestRoboEntity();
                return;
            }
            return;
        }
        roboSendCooldown = 2;
        CreateMobilePackages.LOGGER.info("Advanced Bee Port: Sending package to player: {}", player.getName().getString());
        sendAdvancedDrone(itemStack, slot);
    }

    private boolean hasRoboRequest() {
        if (level instanceof ServerLevel serverLevel) {
            return RoboManager.get(serverLevel).getRoboRequests(this.getBlockPos()).stream()
                    .anyMatch(roboRequest -> roboRequest.getStatus() == RoboRequest.Status.PENDING || roboRequest.getStatus() == RoboRequest.Status.IN_PROGRESS);
        }
        return false;
    }

    /**
     * Sends an advanced drone with upgrade capabilities.
     */
    private void sendAdvancedDrone(ItemStack itemStack, int slot) {
        if (!tryConsumeDrone()) {
            if (!hasRoboRequest() && level != null) {
                requestRoboEntity();
                return;
            }
            return;
        }
        roboSendCooldown = 2;
        if (level instanceof ServerLevel serverLevel) {
            // Use the advanced robo creation with speed and cross-dimensional support
            RoboManager.get(serverLevel).newAdvancedRobo(
                    serverLevel,
                    itemStack,
                    this.getBlockPos(),
                    this.getLogisticsNetworkId(),
                    0,
                    getEffectiveSpeed(),
                    isCrossDimensionalEnabled()
            );
        }
        inventory.setStackInSlot(slot, ItemStack.EMPTY);
    }

    private boolean tryConsumeDrone() {
        ItemStack usedBee = roboBeeInventory.extractItem(0, 1, false);
        return !usedBee.isEmpty();
    }

    public boolean addItemStack(ItemStack itemStack) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.insertItem(i, itemStack, false);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onOpenChange(boolean open) {
        if (level == null) return;
        level.playSound(null, worldPosition, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS);
        setOpen(this, open);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    private void invalidateTarget() {
        if (level instanceof ServerLevel serverLevel) {
            RoboManager.get(serverLevel).getRoboRequests(this.getBlockPos()).forEach(roboRequest -> roboRequest.setStatus(RoboRequest.Status.CANCELLED));
        }
    }

    private void dropItems() {
        ItemStack bees = roboBeeInventory.getStackInSlot(0);
        if (bees.getCount() > 0 && level != null) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), bees));
        }
        // Drop upgrades
        for (int i = 0; i < upgradeInventory.getSlots(); i++) {
            ItemStack upgrade = upgradeInventory.getStackInSlot(i);
            if (!upgrade.isEmpty() && level != null) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), upgrade));
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) {
            this.invalidateTarget();
        }
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide) {
            this.invalidateTarget();
        }
        super.remove();
    }

    @Override
    public void destroy() {
        this.dropItems();
        super.destroy();
    }

    public boolean hasFullInventory(int slotsToLeaveEmpty) {
        int emptySlots = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots <= slotsToLeaveEmpty;
    }

    public synchronized boolean hasFullRoboSlot(int leaveEmpty) {
        return roboBeeInventory.getStackInSlot(0).getCount() >= ROBOBEE_INVENTORY_STACK_SIZE - leaveEmpty;
    }

    public boolean isFull() {
        return isFull(0);
    }

    public boolean isFull(int slotsToLeaveEmpty) {
        return hasFullInventory(slotsToLeaveEmpty) || hasFullRoboSlot(0);
    }

    public synchronized boolean canAcceptEntity(VirtualRobo entity, Boolean hasPackage) {
        if (this.isRemoved()) return false;
        if (entity == null) return hasPackage ? !isFull() : !hasFullRoboSlot(0);
        if (hasRoboRequest()) return false;
        return hasPackage ? !isFull() : !hasFullRoboSlot(0);
    }

    public ItemStackHandler getRoboBeeInventory() {
        return roboBeeInventory;
    }

    public void addBeeToRoboBeeInventory(int amount) {
        roboBeeInventory.insertItem(0, new ItemStack(CMPItems.ROBO_BEE.get(), amount), false);
    }

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return AdvancedBeePortMenu.create(pContainerId, pPlayerInventory, this);
    }

    public ContainerData getData() {
        return data;
    }

    public UUID getLogisticsNetworkId() {
        return behaviour.freqId;
    }

    @Override
    public ItemInteractionResult use(Player player) {
        if (!behaviour.mayInteractMessage(player)) {
            return ItemInteractionResult.SUCCESS;
        }
        return super.use(player);
    }

    public void handleRequest(RoboRequest request) {
        if (!tryConsumeDrone()) return;
        request.setStatus(RoboRequest.Status.IN_PROGRESS);
        roboSendCooldown = 2;
        if (level instanceof ServerLevel serverLevel) {
            // Use advanced robo for requests too
            RoboManager.get(serverLevel).newAdvancedRobo(
                    serverLevel,
                    ItemStack.EMPTY,
                    this.getBlockPos(),
                    request.getLogisticsNetworkId(),
                    0,
                    getEffectiveSpeed(),
                    isCrossDimensionalEnabled()
            );
        }
    }
}
