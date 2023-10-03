package space.outbreak.hatfactory

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.apache.commons.text.StringSubstitutor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

enum class Locale {
    RELOADED,
    UNKNOWN_HAT,
    HAT_GIVEN,
    HAT_GIVEN_TO_OTHER,
    HAT_STORE_TITLE,
    STONECUTTER_LOST,
    ;

    private val key: String = toString().replace("__", ".").replace("_", "-").lowercase()

    fun comp(vararg replacing: Pair<String, Any>): Component {
        return process(config.getString(key, key)!!, *replacing)
    }

    fun send(audience: Audience, vararg replacing: Pair<String, Any>) {
        audience.sendMessage(comp(*replacing))
    }

    fun sendActionBar(audience: Audience, vararg replacing: Pair<String, Any>) {
        audience.sendActionBar(comp(*replacing))
    }

    companion object {
        /**
         * Оборачивает компонент в компонент с явно отключенным курсивом.
         * Может быть полезно, чтобы убирать курсив из описаний и названий предметов.
         * */
        fun deitalize(comp: Component): Component {
            val dn = Component.empty().decoration(TextDecoration.ITALIC, false)
            return dn.children(mutableListOf(comp))
        }

        var mm: MiniMessage = MiniMessage.miniMessage()
            private set

        private lateinit var config: FileConfiguration

        val placeholders = mutableMapOf<String, String>()

        fun load(config: YamlConfiguration) {
            Companion.config = config

            val placeholdersSection = config.getConfigurationSection("placeholders")
            if (placeholdersSection != null) {
                for (p in placeholdersSection.getKeys(false)) {
                    placeholders[p] = placeholdersSection.getString(p) ?: "null"
                }
            }
        }

        private fun replaceAll(str: String, map: Map<String, Any>): String {
            val substitutor = StringSubstitutor(map, "%", "%", '\\')
            return substitutor.replace(str)
        }

        /**
         * Парсит строку формата MiniMessage в компонент.
         *
         * @param text строка для перевода в компонент
         * @param replacing плейсхолдеры для замены, где ключ - имя плейсхолдера
         *  без %. Значение - Component либо любой другой объект. Компоненты будут
         *  вставлены, используя TextReplacementConfig (медленно),
         *  объекты любого другого типа - просто переведены в строку и
         *  заменены (оптимизированно)
         * */
        fun process(text: String, vararg replacing: Pair<String, Any>): Component {
            val mapComps = mutableMapOf<String, Component>()
            val mapStrings = HashMap<String, Any>(placeholders)

            for (pair in replacing) {
                if (pair.second is Component)
                    mapComps[pair.first] = pair.second as Component
                else
                    mapStrings[pair.first] = pair.second
            }

            var comp = mm.deserialize(replaceAll(text, mapStrings))

            for (entry in mapComps.iterator()) {
                comp = comp.replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral("%${entry.key}%")
                        .replacement(entry.value)
                        .build()
                )
            }
            return comp
        }
    }
}

