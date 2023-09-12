package space.outbreak.hatfactory

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.StonecuttingRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException

class HatFactoryPlugin : JavaPlugin() {
    object Fields {
        const val defaultIngredient = "default-ingredient"
        const val storeOpenMenuItem = "store-open-menu-item"
        const val ingredient1 = "ingredient-1"
        const val ingredient2 = "ingredient-2"
        const val permissions = "permissions"
        const val item = "item"
        const val displayname = "displayname"
        const val customModelData = "custom-model-data"
        const val lore = "lore"
        const val amount = "amount"
    }

    val hatManager: HatManager = HatManager(this)
    val menuItemStonecuttingRecipeNamespaceKey = NamespacedKey(this, "hatfactory-stonecutting-recipe")
    private val hatFactoryEventListener = HatInventoryEventListener(this)

    companion object {
        val hatNamespaceKey = NamespacedKey("hatfactory", "hat")
    }

    lateinit var hatMenuOpenItem: ItemStack
        private set

    lateinit var hatMenuMainIngredient: ItemStack
        private set

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).shouldHookPaperReload(true).silentLogs(true))
    }

    private fun getConfigFile(filename: String): File {
        val file = File(dataFolder, filename)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            saveResource(filename, false)
        }
        return file
    }

    private fun loadYaml(filename: String): YamlConfiguration {
        val file = getConfigFile(filename)
        val config = YamlConfiguration()
        try {
            config.load(file)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
        }
        return config
    }

    private fun loadLocale() {
        Locale.load(loadYaml("locale.yml"))
    }

    private fun parseItemStack(section: ConfigurationSection, path: String): ItemStack? {
        if (section.isString(path)) {
            return try {
                ItemStack(Material.getMaterial(section.getString(path)!!)!!)
            } catch (e: NullPointerException) {
                null
            }
        }

        val conf = section.getConfigurationSection(path) ?: return null

        val materialName: String = conf.getString("material")?.uppercase() ?: return null
        val material = Material.getMaterial(materialName) ?: return null

        val item = ItemStack(material, conf.getInt(Fields.amount, 1))
        val meta = item.itemMeta

        val lore = mutableListOf<Component>()
        for (loreLineRaw in conf.getString(Fields.lore, "")!!.split("\n")) {
            if (loreLineRaw.isEmpty())
                continue
            val loreLine = Locale.deitalize(Locale.process(loreLineRaw))
            val plainText = PlainTextComponentSerializer.plainText().serialize(loreLine)
            if (plainText.isEmpty())
                continue
            lore.add(loreLine.children(mutableListOf(Locale.process(loreLineRaw))))
        }
        if (lore.size > 0)
            meta.lore(lore)

        val displaynameRaw = conf.getString(Fields.displayname)
        if (displaynameRaw != null)
            meta.displayName(Locale.deitalize(Locale.process(displaynameRaw)))

        if (conf.contains(Fields.customModelData))
            meta.setCustomModelData(conf.getInt(Fields.customModelData))

        item.setItemMeta(meta)
        return item
    }

    private fun loadHats() {
        val defaultIngredient = parseItemStack(config, Fields.defaultIngredient)
        val hatsConfig = loadYaml("hats.yml")

        for (hatName in hatsConfig.getKeys(false)) {
            val conf = hatsConfig.getConfigurationSection(hatName)!!

            val item = parseItemStack(conf, Fields.item)
            if (item == null) {
                logger.severe("Unable to load item for hat '$hatName'")
                continue
            }
            val meta = item.itemMeta
            meta.persistentDataContainer.set(hatNamespaceKey, PersistentDataType.BOOLEAN, true)
            item.setItemMeta(meta)

            var ingredient1 = parseItemStack(conf, Fields.ingredient1)
            if (ingredient1 == null) {
                if (defaultIngredient == null) {
                    logger.severe(
                        "Unable to load main ingredient for hat '$hatName': " +
                                "Both '$Fields.ingredient1' and '${Fields.defaultIngredient}' are null"
                    )
                    continue
                }
                ingredient1 = defaultIngredient
            }

            hatManager.registerHat(
                Hat(
                    hatName,
                    item,
                    ingredient1,
                    parseItemStack(conf, Fields.ingredient2),
                    conf.getStringList(Fields.permissions),
                )
            )
        }
    }

    private fun loadHatMenuRecipe() {
        Bukkit.removeRecipe(menuItemStonecuttingRecipeNamespaceKey)

        val mainHatIngredient = parseItemStack(config, Fields.defaultIngredient)
        if (mainHatIngredient == null) {
            logger.severe("Unable to load hat main item: invalid '${Fields.defaultIngredient}'!")
            return
        }
        hatMenuMainIngredient = mainHatIngredient

        val tmpHatMenuSourceItem = parseItemStack(config, Fields.storeOpenMenuItem)
        if (tmpHatMenuSourceItem == null) {
            logger.severe("Unable to load hat menu open item: invalid '${Fields.storeOpenMenuItem}'!")
            return
        }
        hatMenuOpenItem = tmpHatMenuSourceItem

        Bukkit.addRecipe(
            StonecuttingRecipe(
                menuItemStonecuttingRecipeNamespaceKey,
                hatMenuOpenItem,
                ExactChoice(hatMenuMainIngredient)
            )
        )
    }

    override fun onEnable() {
        saveDefaultConfig()
        loadLocale()
        loadHatMenuRecipe()
        loadHats()
        CommandAPI.onEnable()
        Bukkit.getPluginManager().registerEvents(hatFactoryEventListener, this)
        Bukkit.getPluginManager().registerEvents(hatManager, this)
        HatFactoryCommands(this)
    }

    override fun onDisable() {
        Bukkit.removeRecipe(menuItemStonecuttingRecipeNamespaceKey)
        HandlerList.unregisterAll(hatFactoryEventListener)
        CommandAPI.onDisable()
    }

    fun reload() {
        hatManager.prepareToReload()
        hatFactoryEventListener.prepareToReload()
        loadHatMenuRecipe()
        saveDefaultConfig()
        loadLocale()
        loadHats()
    }
}