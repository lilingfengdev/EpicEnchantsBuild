package com.songoda.epicenchants.hooks;

import com.songoda.ultimatebottles.UltimateBottles;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.Optional;

@Getter
public class HookManager {
    private FactionsHook factionsHook;
    private UltimateBottles ultimateBottles;

    public void setup() {
        ultimateBottles = Bukkit.getPluginManager().isPluginEnabled("UltimateBottles") ? (UltimateBottles) Bukkit.getPluginManager().getPlugin("UltimateBottles") : null;
    }

    public Optional<UltimateBottles> getUltimateBottles() {
        return Optional.ofNullable(ultimateBottles);
    }
}
