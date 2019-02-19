package com.songoda.epicenchants.menus;

import com.songoda.epicenchants.EpicEnchants;
import com.songoda.epicenchants.enums.ItemType;
import com.songoda.epicenchants.objects.Enchant;
import com.songoda.epicenchants.utils.objects.FastInv;
import com.songoda.epicenchants.utils.objects.ItemBuilder;
import de.tr7zw.itemnbtapi.NBTCompound;
import de.tr7zw.itemnbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.songoda.epicenchants.enums.ItemType.*;
import static com.songoda.epicenchants.objects.Placeholder.of;
import static com.songoda.epicenchants.utils.single.GeneralUtils.*;
import static java.util.Arrays.stream;

public class TinkererMenu extends FastInv {
    private final Map<Integer, Integer> slotMap;
    private final EpicEnchants instance;
    private final FileConfiguration config;

    public TinkererMenu(EpicEnchants instance, FileConfiguration config) {
        super(config.getInt("rows") * 9, color(config.getString("title")));

        this.slotMap = getSlotMap(config);
        this.instance = instance;
        this.config = config;

        if (config.isConfigurationSection("fill")) {
            fill(new ItemBuilder(config.getConfigurationSection("fill")).build());
        }

        config.getConfigurationSection("contents").getKeys(false)
                .stream()
                .map(s -> "contents." + s)
                .map(config::getConfigurationSection)
                .forEach(section -> {
                    addItem(getSlots(section.getString("slot")), new ItemBuilder(section).build(), event -> {
                        if (section.getName().equalsIgnoreCase("accept-left") || section.getName().equalsIgnoreCase("accept-right")) {
                            slotMap.values().stream().map(slot -> getInventory().getItem(slot)).filter(Objects::nonNull).forEach(event.getPlayer().getInventory()::addItem);
                            slotMap.keySet().forEach(slot -> getInventory().clear(slot));
                            event.getPlayer().closeInventory();
                            instance.getAction().perform(event.getPlayer(), "tinkerer.accepted");
                            return;
                        }

                        if (section.getName().equalsIgnoreCase("deposit-all")) {
                            int count = (int) stream(event.getPlayer().getInventory().getContents()).filter(i -> isTinkerable(i) != NONE).count();

                            if (count == 0) {
                                instance.getAction().perform(event.getPlayer(), "tinkerer.no-items");
                                return;
                            }

                            Inventory inventory = event.getPlayer().getInventory();

                            int amount = 0;

                            for (int i = 0; i < inventory.getSize(); i++) {
                                ItemStack itemStack = inventory.getItem(i);
                                ItemType itemType = isTinkerable(itemStack);

                                if (itemType == NONE) {
                                    continue;
                                }

                                for (int j = 0; j < itemStack.getAmount(); j++) {
                                    if (!handleItem(itemStack, itemType)) {
                                        continue;
                                    }

                                    amount++;

                                    if (itemStack.getAmount() > 1) {
                                        itemStack.setAmount(itemStack.getAmount() - 1);
                                        inventory.setItem(i, itemStack);
                                        continue;
                                    }

                                    inventory.clear(i);
                                }
                            }

                            instance.getAction().perform(event.getPlayer(), "tinkerer.deposited-all", of("amount", amount));
                        }
                    });
                });

        // Player clicked an item in tinkerer
        onClick(event -> {
            if (event.getEvent().getClickedInventory() == null) {
                return;
            }

            int slot = event.getSlot();

            if (!slotMap.keySet().contains(slot)) {
                return;
            }

            if (getInventory().getItem(slot) != null && getInventory().getItem(slot).getType() != Material.AIR) {
                event.getPlayer().getInventory().addItem(getInventory().getItem(slot));
                getInventory().clear(slot);
                getInventory().clear(slotMap.get(slot));
            }
        });

        // Player clicked his own inv
        onClick(event -> {
            if (event.getEvent().getClickedInventory() == null || event.getEvent().getClickedInventory().getType() != InventoryType.PLAYER) {
                return;
            }

            ItemStack itemStack = event.getItem();
            ItemType itemType = isTinkerable(itemStack);

            if (itemType == NONE) {
                return;
            }

            if (handleItem(itemStack, itemType)) {
                if (itemStack.getAmount() > 1) {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                    return;
                }

                event.getEvent().getClickedInventory().clear(event.getEvent().getSlot());
            }
        });

        onClose(event -> slotMap.keySet().stream().filter(s -> getInventory().getItem(s) != null).forEach(s -> {
            instance.getAction().perform(event.getPlayer(), "tinkerer.cancelled");
            event.getPlayer().getInventory().addItem(getInventory().getItem(s));
        }));
    }

    private ItemType isTinkerable(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return NONE;
        }

        NBTItem nbtItem = new NBTItem(itemStack);

        if (nbtItem.hasKey("book-item")) {
            return BOOK;
        }

        if (!instance.getHookManager().getUltimateBottles().isPresent()) {
            return NONE;
        }

        if (!itemStack.getEnchantments().isEmpty() || (nbtItem.getCompound("enchants") != null && !nbtItem.getCompound("enchants").getKeys().isEmpty())) {
            if (getExpAmount(itemStack) == 0) {
                return NONE;
            }

            return ENCHANTED;
        }

        return NONE;
    }

    private boolean handleItem(ItemStack itemStack, ItemType itemType) {
        Optional<Map.Entry<Integer, Integer>> emptySlot = slotMap.entrySet().stream().filter(slot -> getInventory().getItem(slot.getKey()) == null || getInventory().getItem(slot.getKey()).getType() == Material.AIR).findFirst();

        if (!emptySlot.isPresent()) {
            return false;
        }

        ItemStack finalItemStack = itemStack.clone();
        finalItemStack.setAmount(1);

        addItem(emptySlot.get().getKey(), finalItemStack);

        switch (itemType) {
            case BOOK:
                getInventory().setItem(emptySlot.get().getValue(), instance.getSpecialItems().getSecretDust(new NBTItem(finalItemStack)));
                break;
            case ENCHANTED:
                getInventory().setItem(emptySlot.get().getValue(), instance.getHookManager().getUltimateBottles().get().createBottle("Tinkerer", getExpAmount(finalItemStack)));
                break;
        }

        return true;
    }

    private LinkedHashMap<Integer, Integer> getSlotMap(FileConfiguration config) {
        return stream(config.getString("slots").split(" ")).map(s -> s.replace(")", "").replace("(", ""))
                .collect(Collectors.toMap(
                        s -> Integer.parseInt(s.split(",")[0]),
                        s -> Integer.parseInt(s.split(",")[1]),
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        LinkedHashMap::new)
                );
    }

    private int getExpAmount(ItemStack itemStack) {
        AtomicInteger total = new AtomicInteger();
        ConfigurationSection section = config.getConfigurationSection("exp-table-per-level");

        itemStack.getEnchantments().forEach((enchantment, level) -> {
            total.addAndGet(section.getInt(enchantment.getName(), section.getInt("DEFAULT")) * level);
        });

        NBTItem nbtItem = new NBTItem(itemStack);

        if (!nbtItem.hasKey("enchants")) {
            return total.get();
        }

        NBTCompound enchantments = nbtItem.getCompound("enchants");

        if (enchantments == null) {
            return total.get();
        }

        enchantments.getKeys().forEach(key -> {
            Enchant enchant = instance.getEnchantManager().getValueUnsafe(key);
            total.addAndGet(section.getInt(enchant.getIdentifier(), enchant.getGroup().getTinkererExp()) * enchantments.getInteger(key));
        });

        return total.get();
    }
}
