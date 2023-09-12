package space.outbreak.hatfactory

import org.bukkit.inventory.ItemStack

fun ItemStack.isHat(): Boolean {
    return itemMeta != null && itemMeta.persistentDataContainer.has(HatFactoryPlugin.hatNamespaceKey)
}