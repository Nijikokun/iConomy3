package com.nijiko.coelho.iConomy.entity;

import com.nijiko.coelho.iConomy.iConomy;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;

public class iPlayerListener extends PlayerListener {

    public iPlayerListener() {
    }

    @Override
    public void onPlayerJoin(PlayerEvent event) {
        Player player = event.getPlayer();

        if (!iConomy.getBank().hasAccount(player.getName())) {
            System.out.println("[iConomy] Player didn't have an account, Creating...");
            iConomy.getBank().addAccount(player.getName());
        } else {
            System.out.println("[iConomy] Player had an account!");
        }
    }
}
