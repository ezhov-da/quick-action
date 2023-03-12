package ru.ezhov.rocket.action.application.configuration.ui

import mu.KotlinLogging
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import ru.ezhov.rocket.action.api.RocketActionConfiguration
import ru.ezhov.rocket.action.api.RocketActionConfigurationProperty
import ru.ezhov.rocket.action.api.RocketActionPropertySpec
import ru.ezhov.rocket.action.api.context.icon.AppIcon
import ru.ezhov.rocket.action.api.context.notification.NotificationType
import ru.ezhov.rocket.action.application.configuration.ui.event.ConfigurationUiListener
import ru.ezhov.rocket.action.application.configuration.ui.event.ConfigurationUiObserverFactory
import ru.ezhov.rocket.action.application.configuration.ui.event.model.ConfigurationUiEvent
import ru.ezhov.rocket.action.application.configuration.ui.event.model.RemoveSettingUiEvent
import ru.ezhov.rocket.action.application.domain.model.SettingsModel
import ru.ezhov.rocket.action.application.domain.model.SettingsValueType
import ru.ezhov.rocket.action.application.infrastructure.MutableRocketActionSettings
import ru.ezhov.rocket.action.application.plugin.context.RocketActionContextFactory
import ru.ezhov.rocket.action.application.plugin.manager.domain.RocketActionPluginRepository
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants

private val logger = KotlinLogging.logger {}

class EditorRocketActionSettingsPanel(
    private val rocketActionPluginRepository: RocketActionPluginRepository
) : JPanel(BorderLayout()) {
    private val infoPanel: InfoPanel = InfoPanel()
    private val rocketActionSettingsPanel = RocketActionSettingsPanel()
    private var currentSettings: TreeRocketActionSettings? = null
    private var callback: SavedRocketActionSettingsPanelCallback? = null
    private val testPanel: TestPanel =
        TestPanel(rocketActionPluginRepository = rocketActionPluginRepository) {
            rocketActionSettingsPanel.create()?.settings
        }

    private fun testAndCreate(): JPanel {
        val panel = JPanel()
        val button = JButton("Сохранить конфигурацию текущего действия")
        button.addActionListener {
            rocketActionSettingsPanel.create()?.let { rs ->
                callback!!.saved(rs)
                RocketActionContextFactory.context.notification()
                    .show(NotificationType.INFO, "Конфигурация текущего действия сохранена")
            } ?: run {
                RocketActionContextFactory.context.notification().show(NotificationType.WARN, "Действие не выбрано")
            }

        }
        panel.add(button)
        return panel
    }

    fun show(settings: TreeRocketActionSettings, callback: SavedRocketActionSettingsPanelCallback) {
        if (stubPanel != null) {
            this.remove(stubPanel)
            stubPanel = null
            this.add(basicPanel)
            this.revalidate()
            this.repaint()
        }
        testPanel.clearTest()
        currentSettings = settings
        this.callback = callback
        val configuration: RocketActionConfiguration? =
            rocketActionPluginRepository.by(settings.settings.type)
                ?.configuration(RocketActionContextFactory.context)
        val configurationDescription = configuration?.let {
            configuration.description()
        }
        infoPanel.refresh(
            type = settings.settings.type,
            rocketActionId = settings.settings.id,
            description = configurationDescription
        )
        val settingsFinal = settings.settings.settings
        val values = settingsFinal
            .map { settingsModel ->
                val property = configuration
                    ?.let { conf ->
                        conf
                            .properties()
                            .firstOrNull { p: RocketActionConfigurationProperty? ->
                                p!!.key().value == settingsModel.name
                            }
                    }
                Value(
                    key = settingsModel.name,
                    value = settingsModel.value,
                    property = property,
                    valueType = settingsModel.valueType,
                )
            }
            .toMutableList() +
            configuration // ищем те свойства, что добавились
                ?.properties()
                ?.filter { p: RocketActionConfigurationProperty ->
                    settingsFinal.firstOrNull { it.name == p.key().value } == null
                }
                ?.map { p: RocketActionConfigurationProperty? ->
                    Value(
                        key = p!!.key().value,
                        value = "",
                        property = p,
                        valueType = null,
                    )
                }.orEmpty()

        rocketActionSettingsPanel
            .setRocketActionConfiguration(
                values
                    .sortedWith(
                        compareByDescending<Value> { it.property?.isRequired() }
                            .thenBy { it.property?.name() }
                    )
            )
    }

    private data class Value(
        val key: String,
        val value: String,
        val property: RocketActionConfigurationProperty?,
        val valueType: SettingsValueType?,
    )

    private class InfoPanel : JPanel() {
        private val textFieldInfo = JTextField().apply { isEditable = false }
        private val labelDescription = JLabel()

        init {
            layout = BorderLayout()
            add(textFieldInfo, BorderLayout.NORTH)
            add(labelDescription, BorderLayout.CENTER)
        }

        fun refresh(type: String, rocketActionId: String, description: String?) {
            textFieldInfo.text = "type: $type id: $rocketActionId"
            description?.let {
                labelDescription.text = description
            }
            removeAll()
            HandlerPanel.of(rocketActionId)
                ?.let { hp ->
                    add(
                        JPanel(BorderLayout()).apply {
                            add(textFieldInfo, BorderLayout.CENTER)
                            add(hp, BorderLayout.EAST)
                        },
                        BorderLayout.NORTH
                    )
                    add(labelDescription, BorderLayout.CENTER)
                }
                ?: run {
                    add(textFieldInfo, BorderLayout.NORTH)
                    add(labelDescription, BorderLayout.CENTER)
                }
        }
    }

    private inner class RocketActionSettingsPanel : JPanel() {
        private val settingPanels = mutableListOf<SettingPanel>()
        private var values: List<Value>? = null

        init {
            this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        fun setRocketActionConfiguration(list: List<Value>) {
            removeAll()
            settingPanels.clear()
            this.values = list
            list
                .forEach { v: Value ->
                    val panel = SettingPanel(v)
                    this.add(panel)
                    settingPanels.add(panel)
                }
            this.add(Box.createVerticalBox())
            repaint()
            revalidate()
        }

        fun create(): TreeRocketActionSettings? = currentSettings?.let { rs ->
            TreeRocketActionSettings(
                configuration = rs.configuration,
                settings = MutableRocketActionSettings(
                    id = rs.settings.id,
                    type = rs.settings.type,
                    settings = settingPanels.map { panel -> panel.value() }.toMutableList(),
                    actions = rs.settings.actions.toMutableList()
                )
            )
        }
    }

    private class SettingPanel(private val value: Value) : JPanel() {
        private var valueCallback: () -> Pair<String, SettingsValueType?> = { Pair("", null) }

        init {
            this.layout = BorderLayout()
            this.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            value.property
                ?.let { property ->
                    val labelName = JLabel(property.name())
                    val labelDescription = JLabel(RocketActionContextFactory.context.icon().by(AppIcon.INFO))
                    labelDescription.toolTipText = property.description()

                    val topPanel = JPanel()
                    topPanel.layout = BoxLayout(topPanel, BoxLayout.X_AXIS)
                    topPanel.border = BorderFactory.createEmptyBorder(0, 0, 1, 0)
                    topPanel.add(labelName)
                    if (property.isRequired()) {
                        topPanel.add(JLabel("*").apply { foreground = Color.RED })
                    }
                    topPanel.add(labelDescription)

                    val centerPanel = JPanel(BorderLayout())
                    when (val configProperty = property.property()) {
                        is RocketActionPropertySpec.StringPropertySpec -> {
                            val plainText = JRadioButton("Простой текст").apply { }
                            val mustacheTemplate = JRadioButton("Шаблон Mustache")
                            val groovyTemplate = JRadioButton("Шаблон Groovy")

                            when (value.valueType) {
                                SettingsValueType.PLAIN_TEXT -> plainText.isSelected = true
                                SettingsValueType.MUSTACHE_TEMPLATE -> mustacheTemplate.isSelected = true
                                SettingsValueType.GROOVY_TEMPLATE -> groovyTemplate.isSelected = true
                                else -> plainText.isSelected = true
                            }

                            ButtonGroup().apply {
                                add(plainText)
                                add(mustacheTemplate)
                                add(groovyTemplate)
                            }

                            centerPanel.add(JPanel().apply {
                                add(plainText)
                                add(mustacheTemplate)
                                add(groovyTemplate)
                            }, BorderLayout.NORTH)

                            centerPanel.add(
                                RTextScrollPane(
                                    RSyntaxTextArea()
                                        .also { tp ->
                                            tp.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE
                                            valueCallback = {
                                                Pair(
                                                    first = tp.text,
                                                    second = when {
                                                        plainText.isSelected -> SettingsValueType.PLAIN_TEXT
                                                        mustacheTemplate.isSelected -> SettingsValueType.MUSTACHE_TEMPLATE
                                                        groovyTemplate.isSelected -> SettingsValueType.GROOVY_TEMPLATE
                                                        else -> SettingsValueType.PLAIN_TEXT
                                                    }
                                                )
                                            }
                                            if (property.isRequired() && value.value.isEmpty()) {
                                                tp.text = configProperty.defaultValue ?: ""
                                            } else {
                                                tp.text = value.value
                                            }
                                        }
                                ),
                                BorderLayout.CENTER
                            )
                        }

                        is RocketActionPropertySpec.BooleanPropertySpec -> {
                            centerPanel.add(JScrollPane(
                                JCheckBox()
                                    .also { cb ->
                                        cb.isSelected = value.value.toBoolean()
                                        valueCallback = { Pair(cb.isSelected.toString(), null) }
                                    }
                            ), BorderLayout.CENTER)
                        }

                        is RocketActionPropertySpec.ListPropertySpec -> {
                            val default = configProperty.defaultValue.orEmpty()
                            val selectedValues = configProperty.valuesForSelect
                            if (!selectedValues.contains(default)) {
                                selectedValues.toMutableList().add(default)
                            }
                            val list = JComboBox(selectedValues.toTypedArray())
                            list.selectedItem =
                                if (selectedValues.contains(value.value)) {
                                    value.value
                                } else {
                                    default
                                }
                            centerPanel.add(JScrollPane(
                                list
                                    .also { l ->
                                        valueCallback = { Pair(l.selectedItem.toString(), null) }
                                    }
                            ), BorderLayout.CENTER)
                        }

                        is RocketActionPropertySpec.IntPropertySpec -> {
                            val default = value.value.toIntOrNull()
                                ?: configProperty.defaultValue?.toIntOrNull()
                                ?: 0
                            centerPanel.add(
                                JSpinner(SpinnerNumberModel(default, configProperty.min, configProperty.max, 1))
                                    .also {
                                        valueCallback = { Pair(it.model.value.toString(), null) }
                                    },
                                BorderLayout.CENTER
                            )
                        }
                    }

                    this.add(topPanel, BorderLayout.NORTH)
                    this.add(centerPanel, BorderLayout.CENTER)
                } ?: run {
                val text = "Обнаружено незарегистрированное свойство '${value.key}:${value.value}' " +
                    "description=${value.property?.description()}"
                logger.warn { text }
                RocketActionContextFactory.context.notification().show(
                    type = NotificationType.WARN,
                    text = text
                )
            }
        }

        fun value(): SettingsModel = SettingsModel(
            name = value.key,
            value = valueCallback().first,
            valueType = valueCallback().second

        )
    }

    private var stubPanel: JPanel? = null
    private var basicPanel: JPanel = JPanel(BorderLayout())

    init {
        basicPanel.add(infoPanel, BorderLayout.NORTH)
        basicPanel.add(JScrollPane(rocketActionSettingsPanel), BorderLayout.CENTER)

        val southPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(testPanel)
            add(testAndCreate())
        }
        basicPanel.add(southPanel, BorderLayout.SOUTH)

        add(basicPanel, BorderLayout.CENTER)

        val editorPanel = this
        ConfigurationUiObserverFactory.observer.register(object : ConfigurationUiListener {
            override fun action(event: ConfigurationUiEvent) {
                if (event is RemoveSettingUiEvent && event.countChildrenRoot == 0) {
                    stubPanel = JPanel(BorderLayout()).apply {
                        add(
                            JLabel("Создайте первое действие").apply {
                                horizontalTextPosition = SwingConstants.CENTER
                                horizontalAlignment = SwingConstants.CENTER
                                verticalTextPosition = SwingConstants.CENTER
                                verticalAlignment = SwingConstants.CENTER
                            },
                            BorderLayout.CENTER
                        )
                    }
                    editorPanel.remove(basicPanel)
                    editorPanel.add(stubPanel, BorderLayout.CENTER)
                    editorPanel.revalidate()
                    editorPanel.repaint()
                }
            }
        })
    }
}
