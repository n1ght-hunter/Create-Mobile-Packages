package de.theidler.create_mobile_packages.blocks.advanced_bee_port;

import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBeeStackHandler;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.index.CMPMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class AdvancedBeePortMenu extends PackagePortMenu {

    private ContainerData data;

    // Slot indices (after the parent class slots)
    // Parent class has: 18 package slots (0-17) + 36 player inventory slots (18-53)
    // Total parent slots: 54
    // Our additions: RoboBee slot (54), Speed upgrade slot (55), Ender upgrade slot (56)
    private static final int ROBO_BEE_SLOT = 54;
    private static final int SPEED_UPGRADE_SLOT = 55;
    private static final int ENDER_UPGRADE_SLOT = 56;

    public AdvancedBeePortMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
        if (contentHolder instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
            this.data = advancedBeePortBlockEntity.getData();
            this.addDataSlots(this.data);
        }
    }

    public AdvancedBeePortMenu(MenuType<?> type, int id, Inventory inv, AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
        super(type, id, inv, advancedBeePortBlockEntity);
        this.data = advancedBeePortBlockEntity.getData();
        this.addDataSlots(this.data);
    }

    @Override
    protected AdvancedBeePortBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        BlockPos readBlockPos = extraData.readBlockPos();
        ClientLevel world = Minecraft.getInstance().level;
        BlockEntity blockEntity = world != null ? world.getBlockEntity(readBlockPos) : null;
        if (blockEntity instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity)
            return advancedBeePortBlockEntity;
        return null;
    }

    public static AdvancedBeePortMenu create(int id, Inventory inv, AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
        return new AdvancedBeePortMenu(CMPMenuTypes.ADVANCED_BEE_PORT_MENU.get(), id, inv, advancedBeePortBlockEntity);
    }

    @Override
    protected void addSlots() {
        super.addSlots();
        if (contentHolder instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
            // RoboBee slot - same position as regular BeePort
            addSlot(new BeePortBeeStackHandler(advancedBeePortBlockEntity.getRoboBeeInventory(), 0, 12, 60));

            // Upgrade slots - positioned in the lower section, next to the bee slot
            addSlot(new UpgradeSlotHandler(advancedBeePortBlockEntity.getUpgradeInventory(), 0, 140, 58, CMPItems.SPEED_UPGRADE.get()));
            addSlot(new UpgradeSlotHandler(advancedBeePortBlockEntity.getUpgradeInventory(), 1, 158, 58, CMPItems.ENDER_UPGRADE.get()));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return super.quickMoveStack(player, index);
        }

        ItemStack stack = slot.getItem();

        // Move from RoboBee-Slot to Player Inventory
        if (index == ROBO_BEE_SLOT) {
            int originalCount = stack.getCount();
            if (moveItemStackTo(stack, 18, 54, false)) {
                int moved = originalCount - stack.getCount();
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                ItemStack result = stack.copy();
                result.setCount(moved);
                return result;
            }
        }
        // Move from upgrade slots to Player Inventory
        else if (index == SPEED_UPGRADE_SLOT || index == ENDER_UPGRADE_SLOT) {
            if (moveItemStackTo(stack, 18, 54, false)) {
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                return stack.copy();
            }
        }
        // Move from Player Inventory to RoboBee-Slot
        else if (stack.getItem() == CMPItems.ROBO_BEE.get()) {
            Slot roboBeeSlot = slots.get(ROBO_BEE_SLOT);
            ItemStack targetStack = roboBeeSlot.getItem();

            int maxStackSize = stack.getMaxStackSize();
            int space = maxStackSize - (targetStack.isEmpty() ? 0 : targetStack.getCount());

            if (space > 0) {
                int toMove = Math.min(space, stack.getCount());
                if (targetStack.isEmpty()) {
                    ItemStack moved = stack.copy();
                    moved.setCount(toMove);
                    roboBeeSlot.set(moved);
                } else {
                    targetStack.grow(toMove);
                    roboBeeSlot.setChanged();
                }
                stack.shrink(toMove);
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                ItemStack result = stack.copy();
                result.setCount(toMove);
                return result;
            }
            return ItemStack.EMPTY;
        }
        // Move Speed Upgrade from Player Inventory
        else if (stack.getItem() == CMPItems.SPEED_UPGRADE.get()) {
            Slot upgradeSlot = slots.get(SPEED_UPGRADE_SLOT);
            if (upgradeSlot.getItem().isEmpty()) {
                ItemStack moved = stack.split(1);
                upgradeSlot.set(moved);
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                return moved;
            }
            return ItemStack.EMPTY;
        }
        // Move Ender Upgrade from Player Inventory
        else if (stack.getItem() == CMPItems.ENDER_UPGRADE.get()) {
            Slot upgradeSlot = slots.get(ENDER_UPGRADE_SLOT);
            if (upgradeSlot.getItem().isEmpty()) {
                ItemStack moved = stack.split(1);
                upgradeSlot.set(moved);
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                return moved;
            }
            return ItemStack.EMPTY;
        }

        return super.quickMoveStack(player, index);
    }

    public int getETA() {
        if (data != null) {
            return data.get(0);
        }
        return -1;
    }

    public boolean isBeeOnTravel() {
        if (data != null && data.get(0) != -1) {
            return data.get(1) == 1;
        }
        return false;
    }

    public boolean hasSpeedUpgrade() {
        if (contentHolder instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
            return advancedBeePortBlockEntity.hasSpeedUpgrade();
        }
        return false;
    }

    public boolean hasEnderUpgrade() {
        if (contentHolder instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
            return advancedBeePortBlockEntity.hasEnderUpgrade();
        }
        return false;
    }

    /**
     * Custom slot handler for upgrade items.
     */
    private static class UpgradeSlotHandler extends SlotItemHandler {
        private final net.minecraft.world.item.Item validItem;

        public UpgradeSlotHandler(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition, net.minecraft.world.item.Item validItem) {
            super(itemHandler, index, xPosition, yPosition);
            this.validItem = validItem;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() == validItem && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
