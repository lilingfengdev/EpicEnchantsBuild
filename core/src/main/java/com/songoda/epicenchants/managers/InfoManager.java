package com.songoda.epicenchants.managers;

import com.songoda.epicenchants.EpicEnchants;
import com.songoda.epicenchants.menus.InfoMenu;
import com.songoda.epicenchants.menus.MainInfoMenu;
import com.songoda.epicenchants.objects.Group;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InfoManager {
    private final Map<Group, InfoMenu> infoMenus;
    private final EpicEnchants instance;
    @Getter private MainInfoMenu mainInfoMenu;

    public InfoManager(EpicEnchants instance) {
        this.instance = instance;
        this.infoMenus = new HashMap<>();
    }

    public Optional<InfoMenu> getMenu(Group group) {
        return Optional.ofNullable(infoMenus.get(group));
    }

    public void loadMenus() {
        mainInfoMenu = new MainInfoMenu(instance, instance.getFileManager().getConfiguration("menus/main-info-menu"));
        instance.getFileManager().getYmlFiles("menus/groups").ifPresent(list -> list.forEach(file -> {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                infoMenus.put(instance.getGroupManager().getValue(config.getString("group"))
                        .orElseThrow(() -> new IllegalArgumentException("Invalid group: " + config.getString("group"))), new InfoMenu(instance, config));
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("Something went wrong loading the menu from file " + file.getName());
                Bukkit.getConsoleSender().sendMessage("Please check to make sure there are no errors in the file.");
                e.printStackTrace();
            }
        }));
    }

}
