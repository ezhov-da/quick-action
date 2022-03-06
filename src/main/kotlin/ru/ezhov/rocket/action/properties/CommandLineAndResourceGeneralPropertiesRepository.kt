package ru.ezhov.rocket.action.properties

import mu.KotlinLogging
import ru.ezhov.rocket.action.notification.NotificationFactory
import ru.ezhov.rocket.action.notification.NotificationType
import java.io.IOException
import java.util.Properties

private val logger = KotlinLogging.logger { }

class CommandLineAndResourceGeneralPropertiesRepository : GeneralPropertiesRepository {

    private val properties: Properties = Properties()

    init {
        try {
            properties.load(this.javaClass.getResourceAsStream("/general.properties"))
        } catch (e: IOException) {
            "Error read general properties"
                .let { text ->
                    logger.error(e) { text }
                    NotificationFactory.notification.show(type = NotificationType.ERROR, text = text)
                }
        }
    }

    override fun asString(name: UsedPropertiesName, default: String): String =
        try {
            System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName)
                ?: default
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            default
        }

    override fun asInteger(name: UsedPropertiesName, default: Int): Int =
        try {
            (System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName))?.toInt()
                ?: default
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            default
        }

    override fun asLong(name: UsedPropertiesName, default: Long): Long =
        try {
            (System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName))?.toLong()
                ?: default
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            default
        }

    override fun asBoolean(name: UsedPropertiesName, default: Boolean): Boolean =
        try {
            (System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName))?.toBoolean()
                ?: default
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            default
        }

    override fun asFloat(name: UsedPropertiesName, default: Float): Float =
        try {
            (System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName))?.toFloat()
                ?: default
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            default
        }

    override fun asStringOrNull(name: UsedPropertiesName): String? =
        try {
            System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName)
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            null
        }

    override fun asIntegerOrNull(name: UsedPropertiesName): Int? =
        try {
            (System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName))?.toInt()
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            null
        }

    override fun asLongOrNull(name: UsedPropertiesName): Long? =
        try {
            (System.getProperty(name.propertyName)
                ?: properties.getProperty(name.propertyName))?.toLong()
        } catch (ex: Exception) {
            logger.warn(ex) { "Error read property=$name" }
            null
        }
}