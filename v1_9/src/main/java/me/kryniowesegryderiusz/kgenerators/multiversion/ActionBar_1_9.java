package me.kryniowesegryderiusz.kgenerators.multiversion;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBar_1_9 implements ActionBar {
  public void sendActionBar(Player player, String msg) {
	  player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
  }
}