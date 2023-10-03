package space.outbreak.hatfactory

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.MerchantRecipe

class HatManager(
    private val plugin: HatFactoryPlugin
) : Listener {
    private val hats = mutableMapOf<String, Hat>()
    private val openedInventories = mutableListOf<Inventory>()

    val hatCount get() = hats.size

    fun getHat(name: String): Hat? {
        return hats[name]
    }

    fun getHatNames(): List<String> {
        return hats.keys.toList()
    }

    fun registerHat(hat: Hat) {
        hats[hat.name] = hat
    }

    fun unregisterHat(name: String): Boolean {
        if (hats.remove(name) != null) {
            Bukkit.removeRecipe(NamespacedKey(plugin, name))
            return true
        }
        return false
    }

    fun clearHats() {
        for (hat in hats.values) {
            Bukkit.removeRecipe(NamespacedKey(plugin, hat.name))
        }
        hats.clear()
    }

    fun openStore(player: Player, noSecondIngredient: Boolean = false): InventoryView? {
        val inv = Bukkit.createMerchant(Locale.HAT_STORE_TITLE.comp())
        val recipes = mutableListOf<MerchantRecipe>()
        for (hat in hats.values) {
            val recipe = if (noSecondIngredient) hat.merchantRecipeNoSecondIngredient else hat.merchantRecipe
            if (hat.permissions.isEmpty() || player.hasPermission(hat.obtainPermission)) {
                recipes.add(recipe)
            } else {
                for (perm in hat.permissions) {
                    if (player.hasPermission(perm)) {
                        recipes.add(recipe)
                        break
                    }
                }
            }
        }
        inv.recipes = recipes
        return player.openMerchant(inv, false)
    }

    /**
     * Закрывает все открытые игроками инвентари
     * */
    fun prepareToReload() {
        for (inv in openedInventories)
            inv.close()
        openedInventories.clear()
        clearHats()
    }

    @EventHandler
    fun onInvClose(event: InventoryCloseEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        openedInventories.remove(event.inventory)
    }
}