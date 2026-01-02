package de.theidler.create_mobile_packages.blocks.bee_upgrade_station;

import de.theidler.create_mobile_packages.index.CMPDataComponents;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import de.theidler.create_mobile_packages.items.upgrades.EnderUpgradeItem;
import de.theidler.create_mobile_packages.items.upgrades.SpeedUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BeeUpgradeStationBlockEntity extends BlockEntity implements MenuProvider {

    // Inventory: slot 0 = bee, slot 1 = speed upgrades (stacks up to 8), slot 2 = ender upgrade (max 1)
    private final ItemStackHandler beeInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof RoboBeeItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler upgradeInventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() instanceof SpeedUpgradeItem;
            } else if (slot == 1) {
                return stack.getItem() instanceof EnderUpgradeItem;
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0) return 8; // Speed upgrades: max 8
            if (slot == 1) return 1; // Ender upgrade: max 1
            return 64;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == 1 && !getStackInSlot(slot).isEmpty()) {
                return stack; // Ender slot already has an item
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public BeeUpgradeStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public ItemStackHandler getBeeInventory() {
        return beeInventory;
    }

    public ItemStackHandler getUpgradeInventory() {
        return upgradeInventory;
    }

    public static final int MAX_SPEED_UPGRADES = 8;

    /**
     * Applies upgrades to the bee in the bee slot.
     * Adds upgrade data components directly to the bee item.
     * @return The upgraded bee, or empty if no bee in slot
     */
    public ItemStack applyUpgrades() {
        ItemStack beeStack = beeInventory.getStackInSlot(0);
        if (beeStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int speedToAdd = upgradeInventory.getStackInSlot(0).getCount();
        boolean enderToAdd = !upgradeInventory.getStackInSlot(1).isEmpty();

        if (speedToAdd == 0 && !enderToAdd) {
            return beeStack; // Nothing to apply
        }

        // Get current upgrade levels from the bee
        int currentSpeed = getSpeedUpgradeCount(beeStack);
        boolean currentEnder = hasEnderUpgrade(beeStack);

        // Apply speed upgrades (cap at 8)
        int newSpeed = Math.min(currentSpeed + speedToAdd, MAX_SPEED_UPGRADES);
        int speedConsumed = newSpeed - currentSpeed;
        setSpeedUpgradeCount(beeStack, newSpeed);

        // Apply ender upgrade
        boolean enderConsumed = false;
        if (enderToAdd && !currentEnder) {
            setEnderUpgrade(beeStack, true);
            enderConsumed = true;
        }

        // Consume upgrades
        if (speedConsumed > 0) {
            upgradeInventory.extractItem(0, speedConsumed, false);
        }
        if (enderConsumed) {
            upgradeInventory.extractItem(1, 1, false);
        }

        setChanged();
        return beeStack;
    }

    // ========== Static Upgrade Helper Methods ==========

    public static int getSpeedUpgradeCount(ItemStack stack) {
        Integer count = stack.get(CMPDataComponents.BEE_SPEED_UPGRADES);
        return count != null ? Math.min(count, MAX_SPEED_UPGRADES) : 0;
    }

    public static void setSpeedUpgradeCount(ItemStack stack, int count) {
        if (count <= 0) {
            stack.remove(CMPDataComponents.BEE_SPEED_UPGRADES);
        } else {
            stack.set(CMPDataComponents.BEE_SPEED_UPGRADES, Math.min(count, MAX_SPEED_UPGRADES));
        }
    }

    public static boolean hasEnderUpgrade(ItemStack stack) {
        Boolean has = stack.get(CMPDataComponents.BEE_ENDER_UPGRADE);
        return has != null && has;
    }

    public static void setEnderUpgrade(ItemStack stack, boolean has) {
        if (!has) {
            stack.remove(CMPDataComponents.BEE_ENDER_UPGRADE);
        } else {
            stack.set(CMPDataComponents.BEE_ENDER_UPGRADE, true);
        }
    }

    /**
     * Gets the current speed upgrade count on the bee.
     */
    public int getBeeSpeedUpgrades() {
        ItemStack beeStack = beeInventory.getStackInSlot(0);
        return getSpeedUpgradeCount(beeStack);
    }

    /**
     * Gets whether the bee has an ender upgrade.
     */
    public boolean getBeeHasEnderUpgrade() {
        ItemStack beeStack = beeInventory.getStackInSlot(0);
        return hasEnderUpgrade(beeStack);
    }

    public void dropContents() {
        if (level != null) {
            for (int i = 0; i < beeInventory.getSlots(); i++) {
                ItemStack stack = beeInventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
            for (int i = 0; i < upgradeInventory.getSlots(); i++) {
                ItemStack stack = upgradeInventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("BeeInventory", beeInventory.serializeNBT(registries));
        tag.put("UpgradeInventory", upgradeInventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BeeInventory")) {
            beeInventory.deserializeNBT(registries, tag.getCompound("BeeInventory"));
        }
        if (tag.contains("UpgradeInventory")) {
            upgradeInventory.deserializeNBT(registries, tag.getCompound("UpgradeInventory"));
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.create_mobile_packages.bee_upgrade_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return BeeUpgradeStationMenu.create(containerId, playerInventory, this);
    }
}
