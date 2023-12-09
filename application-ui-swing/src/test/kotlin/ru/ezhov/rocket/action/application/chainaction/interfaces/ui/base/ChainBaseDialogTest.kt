package ru.ezhov.rocket.action.application.chainaction.interfaces.ui.base

import io.mockk.every
import io.mockk.mockk
import ru.ezhov.rocket.action.application.TestUtilsFactory
import ru.ezhov.rocket.action.application.chainaction.application.ActionExecutorService
import ru.ezhov.rocket.action.application.chainaction.application.AtomicActionService
import ru.ezhov.rocket.action.application.chainaction.application.ChainActionService
import ru.ezhov.rocket.action.application.chainaction.infrastructure.ActionExecutorImpl
import ru.ezhov.rocket.action.application.chainaction.infrastructure.JsonAtomicActionRepository
import ru.ezhov.rocket.action.application.chainaction.infrastructure.JsonChainActionRepository
import ru.ezhov.rocket.action.application.engine.application.EngineFactory
import ru.ezhov.rocket.action.application.variables.application.VariablesDto
import javax.swing.SwingUtilities
import javax.swing.UIManager

internal class ChainBaseDialogTest


fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ex: Throwable) {
            //
        }
        val chainActionRepository = JsonChainActionRepository(TestUtilsFactory.objectMapper)
        val atomicActionService = AtomicActionService(JsonAtomicActionRepository(TestUtilsFactory.objectMapper))
        val chainActionService = ChainActionService(chainActionRepository)
        val dialog = ChainBaseDialog(
            actionExecutorService = ActionExecutorService(
                actionExecutor = ActionExecutorImpl(
                    engineFactory = EngineFactory(),
                    variablesApplication = mockk {
                        every { all() } returns VariablesDto(
                            key = "",
                            variables = emptyList()
                        )
                    },
                    atomicActionService = atomicActionService
                ),
                chainActionService = chainActionService,
                atomicActionService = atomicActionService,
            ),
            actionExecutor = mockk(),
            chainActionService = chainActionService,
            atomicActionService = atomicActionService,
            configurationApplication = mockk(),
        )
        dialog.isVisible = true
    }
}

