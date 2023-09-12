package space.outbreak.hatfactory

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class HatFactoryCommands(
    private val plugin: HatFactoryPlugin
) {
    private val manager: HatManager get() = plugin.hatManager

    init {
        CommandAPICommand("hats")
            .withSubcommands(
                CommandAPICommand("reload")
                    .withPermission(Permissions.RELOAD)
                    .executes(CommandExecutor { sender, _ ->
                        plugin.reload()
                        Locale.RELOADED.send(sender, "hats-count" to manager.hatCount)
                    }),
                CommandAPICommand("give")
                    .withPermission(Permissions.GIVE)
                    .withArguments(
                        StringArgument("hat").replaceSuggestions(ArgumentSuggestions.strings {
                            plugin.hatManager.getHatNames().toTypedArray()
                        })
                    )
                    .withOptionalArguments(
                        PlayerArgument("player"),
                        IntegerArgument("amount")
                    )
                    .executes(CommandExecutor { sender, args ->
                        give(
                            sender,
                            args.getOptional("player").orElse(null) as Player?,
                            args.get("hat") as String,
                            args.getOptional("amount").orElse(1) as Int
                        )
                    }),
                CommandAPICommand("store")
                    .withPermission(Permissions.STORE)
                    .executesPlayer(PlayerCommandExecutor { player, _ -> manager.openStore(player) })
            )
            .register()
    }

    private fun give(sender: CommandSender, targetArg: Player?, hatName: String, amount: Int) {
        var toOther = false

        val target: Player = if (targetArg == null) {
            if (sender !is Player)
                throw CommandAPI.failWithString("Target player is required")
            sender
        } else {
            if (sender is Player) {
                toOther = targetArg.uniqueId != sender.uniqueId
            }
            targetArg
        }

        val hat = manager.getHat(hatName) ?: throw CommandAPI.failWithString(
            LegacyComponentSerializer.legacySection()
                .serialize(Locale.UNKNOWN_HAT.comp("hat-name" to hatName))
        )

        target.inventory.addItem(hat.getItemStack().apply { this.amount = amount })

        (if (toOther) Locale.HAT_GIVEN_TO_OTHER else Locale.HAT_GIVEN).send(
            sender,
            "player" to target.name,
            "hat-name" to hatName,
            "hat-amount" to amount,
            "hat-displayname" to hat.getItemStack().displayName()
        )
    }
}