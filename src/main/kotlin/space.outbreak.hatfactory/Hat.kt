package space.outbreak.hatfactory

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe

class Hat(
    val name: String,
    private val item: ItemStack,
    val firstRecipeItem: ItemStack,
    val secondRecipeItem: ItemStack?,
    val permissions: List<String>,
) {
    fun getItemStack(): ItemStack {
        return item.clone()
    }

    val obtainPermission = Permissions.OBTAIN_HAT.replace("%hat%", name)

    val merchantRecipe: MerchantRecipe by lazy {
        MerchantRecipe(
            getItemStack(),
            0,
            99999999,
            false,
        ).apply {
            this.addIngredient(firstRecipeItem)
            if (secondRecipeItem != null)
                this.addIngredient(secondRecipeItem)
        }
    }
}