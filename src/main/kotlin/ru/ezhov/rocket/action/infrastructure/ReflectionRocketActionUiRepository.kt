package ru.ezhov.rocket.action.infrastructure

import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import ru.ezhov.rocket.action.api.RocketActionFactoryUi
import ru.ezhov.rocket.action.api.RocketActionType
import ru.ezhov.rocket.action.domain.RocketActionUiRepository
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

class ReflectionRocketActionUiRepository : RocketActionUiRepository {
    private var list: MutableList<RocketActionFactoryUi> = mutableListOf()
    private fun load() {
        logger.debug { "Rocket action UI load started" }
        val measureTimeMillis = measureTimeMillis {
            val reflections = Reflections(
                ConfigurationBuilder()
                    .addUrls(ClasspathHelper.forPackage("ru.ezhov.rocket.action.types"))
                    .setScanners(Scanners.SubTypes)
            )
            val classes = reflections.getSubTypesOf(RocketActionFactoryUi::class.java)
            for (aClass in classes) {
                try {
                    if (!Modifier.isAbstract(aClass.modifiers)) {
                        list.add(aClass.getConstructor().newInstance() as RocketActionFactoryUi)
                    }
                } catch (e: InstantiationException) {
                    logger.error(e) { "Error when load ui" }
                } catch (e: IllegalAccessException) {
                    logger.error(e) { "Error when load ui" }
                }
            }

        }
        logger.debug { "Rocket action UI load completed. count=${list.size} time=$measureTimeMillis" }
    }

    override fun all(): List<RocketActionFactoryUi> {
        if (list.isEmpty()) {
            load()
        }
        return list
    }

    override fun by(type: RocketActionType): RocketActionFactoryUi? =
        all().firstOrNull { r: RocketActionFactoryUi -> r.type().value() == type.value() }
}