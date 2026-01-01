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
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class AdvancedBeePortMenu extends PackagePortMenu {

    private ContainerData data;

    private static final int ROBO_BEE_SLOT = 54;
    private static final int SPEED_UPGRADE_SLOT = 55;
    private static final int ENDER_UPGRADE_SLOT = 56;

    public AdvancedBeePortMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
        // Client side - create empty data that will receive synced values from server
        this.data = new SimpleContainerData(2);
        this.addDataSlots(this.data);
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
            addSlot(new BeePortBeeStackHandler(advancedBeePortBlockEntity.getRoboBeeInventory(), 0, 12, 60));
            addSlot(new UpgradeSlotHandler(advancedBeePortBlockEntity.getUpgradeInventory(), 0, 140, 58, CMPItems.SPEED_UPGRADE.get(), 8));
            addSlot(new UpgradeSlotHandler(advancedBeePortBlockEntity.getUpgradeInventory(), 1, 158, 58, CMPItems.ENDER_UPGRADE.get(), 1));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return super.quickMoveStack(player, index);
        }

        ItemStack stack = slot.getItem();

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
        } else if (index == SPEED_UPGRADE_SLOT || index == ENDER_UPGRADE_SLOT) {
            if (moveItemStackTo(stack, 18, 54, false)) {
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                return stack.copy();
            }
        } else if (stack.getItem() == CMPItems.ROBO_BEE.get()) {
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
        } else if (stack.getItem() == CMPItems.SPEED_UPGRADE.get()) {
            Slot upgradeSlot = slots.get(SPEED_UPGRADE_SLOT);
            ItemStack targetStack = upgradeSlot.getItem();
            int maxStackSize = 8;
            int space = maxStackSize - (targetStack.isEmpty() ? 0 : targetStack.getCount());

            if (space > 0) {
                int toMove = Math.min(space, stack.getCount());
                if (targetStack.isEmpty()) {
                    ItemStack moved = stack.copy();
                    moved.setCount(toMove);
                    upgradeSlot.set(moved);
                } else {
                    targetStack.grow(toMove);
                    upgradeSlot.setChanged();
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
        } else if (stack.getItem() == CMPItems.ENDER_UPGRADE.get()) {
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
        return data != null && data.get(1) == 1;
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

    public boolean isReturnToSender() {
        if (contentHolder instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
            return advancedBeePortBlockEntity.isReturnToSender();
        }
        return true;
    }

    public BlockPos getBlockPos() {
        if (contentHolder instanceof AdvancedBeePortBlockEntity advancedBeePortBlockEntity) {
            return advancedBeePortBlockEntity.getBlockPos();
        }
        return BlockPos.ZERO;
    }

    private static class UpgradeSlotHandler extends SlotItemHandler {
        private final net.minecraft.world.item.Item validItem;
        private final int maxStackSize;

        public UpgradeSlotHandler(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition, net.minecraft.world.item.Item validItem, int maxStackSize) {
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
