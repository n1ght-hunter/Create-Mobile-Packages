package de.theidler.create_mobile_packages.blocks.bee_upgrade_station;

import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.index.CMPMenuTypes;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import de.theidler.create_mobile_packages.items.upgrades.EnderUpgradeItem;
import de.theidler.create_mobile_packages.items.upgrades.SpeedUpgradeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class BeeUpgradeStationMenu extends AbstractContainerMenu {

    private static final int BEE_SLOT = 0;
    private static final int SPEED_UPGRADE_SLOT = 1;
    private static final int ENDER_UPGRADE_SLOT = 2;

    private static final int PLAYER_INV_START = 3;
    private static final int PLAYER_INV_END = 30;
    private static final int PLAYER_HOTBAR_START = 30;
    private static final int PLAYER_HOTBAR_END = 39;

    private final BeeUpgradeStationBlockEntity blockEntity;

    public BeeUpgradeStationMenu(MenuType<?> type, int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(type, id, playerInventory, createOnClient(extraData));
    }

    public BeeUpgradeStationMenu(MenuType<?> type, int id, Inventory playerInventory, BeeUpgradeStationBlockEntity blockEntity) {
        super(type, id);
        this.blockEntity = blockEntity;

        // Bee slot
        addSlot(new BeeSlotHandler(blockEntity.getBeeInventory(), 0, 44, 35));

        // Speed upgrade slot
        addSlot(new UpgradeSlotHandler(blockEntity.getUpgradeInventory(), 0, 98, 35, CMPItems.SPEED_UPGRADE.get(), 8));

        // Ender upgrade slot
        addSlot(new UpgradeSlotHandler(blockEntity.getUpgradeInventory(), 1, 134, 35, CMPItems.ENDER_UPGRADE.get(), 1));

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static BeeUpgradeStationBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BeeUpgradeStationBlockEntity station) {
                return station;
            }
        }
        return null;
    }

    public static BeeUpgradeStationMenu create(int id, Inventory playerInventory, BeeUpgradeStationBlockEntity blockEntity) {
        return new BeeUpgradeStationMenu(CMPMenuTypes.BEE_UPGRADE_STATION_MENU.get(), id, playerInventory, blockEntity);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();

        // From bee slot to player inventory
        if (index == BEE_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        }
        // From upgrade slots to player inventory
        else if (index == SPEED_UPGRADE_SLOT || index == ENDER_UPGRADE_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        }
        // From player inventory to block slots
        else if (index >= PLAYER_INV_START && index < PLAYER_HOTBAR_END) {
            if (stack.getItem() instanceof RoboBeeItem) {
                if (!moveItemStackTo(stack, BEE_SLOT, BEE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof SpeedUpgradeItem) {
                if (!moveToSpeedSlot(stack)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof EnderUpgradeItem) {
                if (!moveItemStackTo(stack, ENDER_UPGRADE_SLOT, ENDER_UPGRADE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move between inventory and hotbar
                if (!moveItemStackTo(stack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        // From hotbar to block slots or inventory
        else if (index >= PLAYER_HOTBAR_START && index < PLAYER_HOTBAR_END) {
            if (stack.getItem() instanceof RoboBeeItem) {
                if (!moveItemStackTo(stack, BEE_SLOT, BEE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof SpeedUpgradeItem) {
                if (!moveToSpeedSlot(stack)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof EnderUpgradeItem) {
                if (!moveItemStackTo(stack, ENDER_UPGRADE_SLOT, ENDER_UPGRADE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return originalStack;
    }

    private boolean moveToSpeedSlot(ItemStack stack) {
        Slot speedSlot = slots.get(SPEED_UPGRADE_SLOT);
        ItemStack targetStack = speedSlot.getItem();
        int maxStackSize = 8;
        int space = maxStackSize - (targetStack.isEmpty() ? 0 : targetStack.getCount());

        if (space > 0) {
            int toMove = Math.min(space, stack.getCount());
            if (targetStack.isEmpty()) {
                ItemStack moved = stack.copy();
                moved.setCount(toMove);
                speedSlot.set(moved);
            } else {
                targetStack.grow(toMove);
                speedSlot.setChanged();
            }
            stack.shrink(toMove);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return blockEntity != null && !blockEntity.isRemoved() &&
                player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    public BeeUpgradeStationBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * Gets the current speed upgrade count on the bee in the bee slot.
     */
    public int getBeeSpeedUpgrades() {
        if (blockEntity != null) {
            return blockEntity.getBeeSpeedUpgrades();
        }
        return 0;
    }

    /**
     * Gets whether the bee in the bee slot has an ender upgrade.
     */
    public boolean getBeeHasEnderUpgrade() {
        if (blockEntity != null) {
            return blockEntity.getBeeHasEnderUpgrade();
        }
        return false;
    }

    /**
     * Checks if there's a bee in the bee slot.
     */
    public boolean hasBee() {
        return !slots.get(BEE_SLOT).getItem().isEmpty();
    }

    /**
     * Checks if the bee in the slot has any upgrades applied.
     */
    public boolean isAdvancedBee() {
        ItemStack beeStack = slots.get(BEE_SLOT).getItem();
        if (beeStack.isEmpty()) return false;
        // A bee is "advanced" if it has any upgrades
        return BeeUpgradeStationBlockEntity.getSpeedUpgradeCount(beeStack) > 0
            || BeeUpgradeStationBlockEntity.hasEnderUpgrade(beeStack);
    }

    // Custom slot for bees
    private static class BeeSlotHandler extends SlotItemHandler {
        public BeeSlotHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof RoboBeeItem && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // Custom slot for upgrades
    private static class UpgradeSlotHandler extends SlotItemHandler {
        private final net.minecraft.world.item.Item validItem;
        private final int maxStackSize;

        public UpgradeSlotHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition, net.minecraft.world.item.Item validItem, int maxStackSize) {
            super(itemHandler, index, xPosition, yPosition);
            this.validItem = validItem;
            this.maxStackSize = maxStackSize;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() == validItem && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return maxStackSize;
        }
    }
}
