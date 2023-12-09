package ru.ezhov.rocket.action.application.chainaction.interfaces.ui.base

import ru.ezhov.rocket.action.application.applicationConfiguration.application.ConfigurationApplication
import ru.ezhov.rocket.action.application.chainaction.application.ActionExecutorService
import ru.ezhov.rocket.action.application.chainaction.application.AtomicActionService
import ru.ezhov.rocket.action.application.chainaction.application.ChainActionService
import ru.ezhov.rocket.action.application.chainaction.domain.ActionExecutor
import ru.ezhov.rocket.action.application.chainaction.interfaces.ui.ChainConfigurationFrame
import ru.ezhov.rocket.action.application.icon.infrastructure.IconRepository
import java.awt.BorderLayout
import javax.swing.JDialog

class ChainBaseDialog(
    actionExecutor: ActionExecutor,
    actionExecutorService: ActionExecutorService,
    chainActionService: ChainActionService,
    atomicActionService: AtomicActionService,
    configurationApplication: ConfigurationApplication,
    iconRepository: IconRepository,
) : JDialog() {

    private val chainBasePanel: ChainBasePanel = ChainBasePanel(
        movableComponent = this,
        actionExecutorService = actionExecutorService,
        chainActionService = chainActionService,
        atomicActionService = atomicActionService,
        configurationApplication = configurationApplication,
    )

    private val chainConfigurationFrame = ChainConfigurationFrame(
        actionExecutorService = actionExecutorService,
        chainActionService = chainActionService,
        atomicActionService = atomicActionService,
        actionExecutor = actionExecutor,
        iconRepository = iconRepository,
    )

    init {
        setSize(200, 120)
        isAlwaysOnTop = true
        isUndecorated = true
        opacity = 0.7F // TODO ezhov test
        add(chainBasePanel, BorderLayout.CENTER)
        setLocationRelativeTo(null)
    }

    fun showDialog() {
        isVisible = true
    }
}
