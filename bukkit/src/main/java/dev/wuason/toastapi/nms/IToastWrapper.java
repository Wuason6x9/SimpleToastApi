package dev.wuason.toastapi.nms;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface IToastWrapper {
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path);
}
