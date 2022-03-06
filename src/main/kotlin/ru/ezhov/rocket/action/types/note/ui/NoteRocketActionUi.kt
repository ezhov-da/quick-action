package ru.ezhov.rocket.action.types.note.ui

import mu.KotlinLogging
import ru.ezhov.rocket.action.BaseDialog
import ru.ezhov.rocket.action.api.RocketAction
import ru.ezhov.rocket.action.api.RocketActionConfigurationProperty
import ru.ezhov.rocket.action.api.RocketActionConfigurationPropertyKey
import ru.ezhov.rocket.action.api.RocketActionSettings
import ru.ezhov.rocket.action.api.RocketActionType
import ru.ezhov.rocket.action.icon.AppIcon
import ru.ezhov.rocket.action.icon.IconRepositoryFactory
import ru.ezhov.rocket.action.icon.IconService
import ru.ezhov.rocket.action.notification.NotificationFactory
import ru.ezhov.rocket.action.notification.NotificationType
import ru.ezhov.rocket.action.types.AbstractRocketAction
import ru.ezhov.rocket.action.types.note.application.NoteApplicationService
import ru.ezhov.rocket.action.types.note.infrastructure.NoteRepositoryFactory
import java.awt.Component
import java.io.File
import java.nio.file.Path
import javax.swing.JMenu
import javax.swing.SwingWorker
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private val logger = KotlinLogging.logger {}

class NoteRocketActionUi : AbstractRocketAction() {
    override fun create(settings: RocketActionSettings): RocketAction? = run {

        settings.settings()[LABEL]
            ?.takeIf { it.isNotEmpty() }
            ?.let { label ->
                settings.settings()[PATH]
                    ?.let { pathConfig ->
                        val path = pathConfig.ifEmpty { File("").absolutePath }
                        val description = settings.settings()[DESCRIPTION]?.takeIf { it.isNotEmpty() } ?: label
                        val menu = JMenu(label)
                        settings.settings()[ICON_URL].let { icon ->
                            menu.icon = IconService().load(
                                iconUrl = icon ?: "",
                                defaultIcon = IconRepositoryFactory.repository.by(AppIcon.CLIPBOARD)
                            )
                        }
                        menu.toolTipText = description

                        runDialog(Path(path))

                        object : RocketAction {
                            override fun contains(search: String): Boolean = false

                            override fun isChanged(actionSettings: RocketActionSettings): Boolean =
                                !(settings.id() == actionSettings.id() &&
                                    settings.settings() == actionSettings.settings())

                            override fun component(): Component = menu
                        }
                    }
            }
    }

    private fun checkAndCopyDb(path: String): File {
        val fileName = "notes.s3db"
        val fileDb = File(path, fileName)
        if (!fileDb.exists()) {
            this.javaClass.getResourceAsStream("/$fileName")?.use { ins ->
                fileDb.writeBytes(ins.readAllBytes())

                logger.debug { "Notes DB coped to '${fileDb.absolutePath}'" }
            }
        } else {
            logger.debug { "Notes DB '${fileDb.absolutePath}' already exists" }
        }

        return fileDb
    }

    private fun runDialog(pathToDb: Path) {
        object : SwingWorker<NoteApplicationService, Any>() {
            override fun doInBackground(): NoteApplicationService = run {
                val dbFile = checkAndCopyDb(pathToDb.absolutePathString())

                NoteApplicationService(
                    NoteRepositoryFactory.sqLiteNoteRepository(dbFile)
                )
            }

            override fun done() {
                try {
                    NoteDialog(
                        noteApplicationService = this.get(),
                        owner = BaseDialog.dialog
                    ).isVisible = true
                } catch (ex: Exception) {
                    logger.warn(ex) { "Error when load notes by DB path '${pathToDb.absolutePathString()}'" }
                    NotificationFactory.notification.show(
                        NotificationType.WARN,
                        "Ошибка создания \"Заметок\""
                    )
                }
            }
        }.execute()
    }

    override fun type(): RocketActionType = RocketActionType { TYPE }

    override fun description(): String = "Хранение небольших заметок, ссылок, кусков кода. С тегами и поиском"

    override fun asString(): List<RocketActionConfigurationPropertyKey> = listOf(LABEL)

    override fun properties(): List<RocketActionConfigurationProperty> {
        return listOf(
            createRocketActionProperty(key = LABEL, name = LABEL.value, description = "Заголовок", required = true, default = "Заметки"),
            createRocketActionProperty(key = DESCRIPTION, name = DESCRIPTION.value, description = "Описание", required = false),
            createRocketActionProperty(key = ICON_URL, name = ICON_URL.value, description = "URL иконки", required = false),
            createRocketActionProperty(
                key = PATH,
                name = PATH.value,
                description = "Путь хранения сохраняемых данных",
                required = true,
                default = File("").absolutePath),
        )
    }

    override fun name(): String = "Заметки"

    companion object {
        const val TYPE = "NOTE"
        private val LABEL = RocketActionConfigurationPropertyKey("label")
        private val ICON_URL = RocketActionConfigurationPropertyKey("iconUrl")
        private val DESCRIPTION = RocketActionConfigurationPropertyKey("description")
        private val PATH = RocketActionConfigurationPropertyKey("path")
    }
}