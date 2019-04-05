package com.songoda.epicenchants.menus;

import com.songoda.epicenchants.EpicEnchants;
import com.songoda.epicenchants.objects.Enchant;
import com.songoda.epicenchants.objects.Group;
import com.songoda.epicenchants.utils.objects.FastInv;
import com.songoda.epicenchants.utils.objects.ItemBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.songoda.epicenchants.objects.Placeholder.of;
import static com.songoda.epicenchants.utils.single.GeneralUtils.color;
import static java.util.Arrays.stream;

public class InfoMenu extends FastInv {
    public InfoMenu(EpicEnchants instance, FileConfiguration config) {
        super(config.getInt("rows") * 9, color(config.getString("title")));

        Group group = instance.getGroupManager().getValue(config.getString("group")).orElseThrow(() -> new IllegalArgumentException("Invalid group: " + config.getString("group")));

        Set<Integer> slots;

        if (config.getString("slots").equalsIgnoreCase("ALL_SLOTS")) {
            slots = IntStream.range(0, config.getInt("rows") * 9).boxed().collect(Collectors.toSet());
        } else {
            String[] split = config.getString("slots").split(",");
            slots = stream(split, 0, split.length).filter(StringUtils::isNumeric).map(Integer::parseInt).collect(Collectors.toSet());
        }

        Iterator<Enchant> enchantIterator = instance.getEnchantManager().getEnchants(group).iterator();
        slots.stream().filter(slot -> enchantIterator.hasNext()).forEach(slot -> {
            Enchant enchant = enchantIterator.next();

            String whitelist = instance.getItemGroup().getGroup(enchant.getItemWhitelist())
                    .map(s -> StringUtils.capitalize(s.getName().toLowerCase()))
                    .orElse(String.join(", ", enchant.getItemWhitelist().stream().map(Material::toString).collect(Collectors.toSet())));

            addItem(slot, new ItemBuilder(config.getConfigurationSection("enchant-item"),
                    of("group_color", enchant.getGroup().getColor()),
                    of("enchant", enchant.getIdentifier()),
                    of("max_level", enchant.getMaxLevel()),
                    of("applicable_to", whitelist),
                    of("enchant", enchant.getIdentifier()),
                    of("description", enchant.getDescription().stream().map(s -> config.getString("enchant-item.description-color") + s).collect(Collectors.toList()))).build());
        });

        if (config.isConfigurationSection("contents"))
            config.getConfigurationSection("contents").getKeys(false)
                    .stream()
                    .map(s -> "contents." + s)
                    .map(config::getConfigurationSection)
                    .forEach(section -> addItem(section.getInt("slot"), new ItemBuilder(section).build()));
    }
}
