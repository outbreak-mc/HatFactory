package space.outbreak.hatfactory

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory

class HatInventoryEventListener(
    private val plugin: HatFactoryPlugin
) : Listener {
    private val manager: HatManager get() = plugin.hatManager

    private val playersToBackToStonecutter = mutableMapOf<Player, Inventory>()

    /**
     * @return true, если камнерез, к которому был привязан инвентарь stonecutterInv,
     * всё ещё доступен для игрока player. Возвращает false, если камнерез
     * слишком далеко или уничтожен.
     * */
    private fun checkStonecutter(player: Player, stonecutterInv: Inventory): Boolean {
        val loc = stonecutterInv.location
        if (loc != null)
            return loc.block.type == Material.STONECUTTER && loc.distance(player.location) < 3
        return false
    }

    @EventHandler
    fun onClickHatMenuInStonecutter(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.STONECUTTER) return
        val currentItem = event.currentItem ?: return

        // Слот 1 в инвентаре STONECUTTER это то, что получаем на выходе
        if (event.rawSlot != 1) {
            // Если попытаться переместить шапку в слот, с клиентом
            // происходят какие-то глюки, а наша сборка и вовсе вылетает
            if (currentItem.isHat())
                event.isCancelled = true
            return
        }

        if (currentItem.type != plugin.hatMenuOpenItem.type) return
        if (currentItem.itemMeta.hasCustomModelData() != plugin.hatMenuOpenItem.itemMeta.hasCustomModelData()) return
        if (plugin.hatMenuOpenItem.itemMeta.hasCustomModelData() && currentItem.itemMeta.customModelData != plugin.hatMenuOpenItem.itemMeta.customModelData) return

        val player = event.whoClicked as Player

        playersToBackToStonecutter[event.whoClicked as Player] = event.clickedInventory ?: return
        event.isCancelled = true
        player.closeInventory()
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            manager.openStore(player)
        }, 1L)

    }

    @EventHandler
    fun onBlockPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (!event.isBlockInHand) return
        val inv = event.player.inventory
        if (inv.itemInMainHand.isHat() || inv.itemInOffHand.isHat())
            event.isCancelled = true
    }

    @EventHandler
    fun onClickMerchant(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        if (event.rawSlot != 2) return
        val player = event.whoClicked as Player
        val stonecutterInv = playersToBackToStonecutter[player] ?: return
        if (!checkStonecutter(player, stonecutterInv)) {
            event.isCancelled = true
            player.closeInventory()
            Locale.STONECUTTER_LOST.sendActionBar(player)
        }
    }

    @EventHandler
    fun onStonecutterBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.STONECUTTER) return
        for (entry in playersToBackToStonecutter.entries) {
            val inv = entry.value
            val invLoc = inv.location ?: continue
            val player = entry.key
            if (invLoc == event.block.location) {
                inv.close()
                player.closeInventory()
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    player.closeInventory()
                }, 1L)
                playersToBackToStonecutter.remove(player)
                Locale.STONECUTTER_LOST.sendActionBar(player)
                break
            }
        }
    }

    @EventHandler
    fun onInvClose(event: InventoryCloseEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val player = event.player as Player
        val stonecutterInv = playersToBackToStonecutter[player] ?: return

        if (checkStonecutter(player, stonecutterInv)) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.openInventory(stonecutterInv)
                playersToBackToStonecutter.remove(player)
            }, 1L)
        }
    }

    fun prepareToReload() {
        for (inv in playersToBackToStonecutter.values)
            inv.close()
        playersToBackToStonecutter.clear()
    }
}