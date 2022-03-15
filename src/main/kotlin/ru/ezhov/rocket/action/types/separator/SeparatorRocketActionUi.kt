package ru.ezhov.rocket.action.types.separator

import ru.ezhov.rocket.action.api.RocketAction
import ru.ezhov.rocket.action.api.RocketActionConfigurationProperty
import ru.ezhov.rocket.action.api.RocketActionConfigurationPropertyKey
import ru.ezhov.rocket.action.api.RocketActionSettings
import ru.ezhov.rocket.action.api.RocketActionType
import ru.ezhov.rocket.action.icon.AppIcon
import ru.ezhov.rocket.action.icon.IconRepositoryFactory
import ru.ezhov.rocket.action.types.AbstractRocketAction
import java.awt.Component
import javax.swing.Icon
import javax.swing.JSeparator

class SeparatorRocketActionUi : AbstractRocketAction() {
    override fun create(settings: RocketActionSettings): RocketAction =
        object : RocketAction {
            override fun contains(search: String): Boolean = false

            override fun isChanged(actionSettings: RocketActionSettings): Boolean = false

            override fun component(): Component = JSeparator()
        }

    override fun asString(): List<RocketActionConfigurationPropertyKey> = emptyList()

    override fun type(): RocketActionType = RocketActionType { "SEPARATOR" }

    override fun name(): String = "Разделитель"

    override fun description(): String = "Разделитель"

    override fun icon(): Icon? = IconRepositoryFactory.repository.by(AppIcon.MINUS)

    override fun properties(): List<RocketActionConfigurationProperty> = emptyList()
}