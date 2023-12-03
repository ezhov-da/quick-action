package ru.ezhov.rocket.action.application.chainaction.interfaces.ui.configuration.actions

import net.miginfocom.swing.MigLayout
import ru.ezhov.rocket.action.application.chainaction.application.AtomicActionService
import ru.ezhov.rocket.action.application.chainaction.application.ChainActionService
import ru.ezhov.rocket.action.application.chainaction.domain.event.AtomicActionCreatedDomainEvent
import ru.ezhov.rocket.action.application.chainaction.domain.event.AtomicActionDeletedDomainEvent
import ru.ezhov.rocket.action.application.chainaction.domain.event.AtomicActionUpdatedDomainEvent
import ru.ezhov.rocket.action.application.chainaction.domain.model.AtomicAction
import ru.ezhov.rocket.action.application.chainaction.domain.model.AtomicActionEngine
import ru.ezhov.rocket.action.application.chainaction.domain.model.AtomicActionSource
import ru.ezhov.rocket.action.application.chainaction.domain.model.ContractType
import ru.ezhov.rocket.action.application.chainaction.interfaces.ui.ChainsSelectPanel
import ru.ezhov.rocket.action.application.chainaction.interfaces.ui.CreateAndEditAtomicActionDialog
import ru.ezhov.rocket.action.application.chainaction.interfaces.ui.CreateAndEditChainActionDialog
import ru.ezhov.rocket.action.application.chainaction.interfaces.ui.renderer.AtomicActionListCellRenderer
import ru.ezhov.rocket.action.application.event.domain.DomainEvent
import ru.ezhov.rocket.action.application.event.domain.DomainEventSubscriber
import ru.ezhov.rocket.action.application.event.infrastructure.DomainEventFactory
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeListener
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JMenu
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane

class ActionsConfigurationPanel(
    private val atomicActionService: AtomicActionService,
    private val chainActionService: ChainActionService,
    private val createAndEditChainActionDialog: CreateAndEditChainActionDialog,
) : JPanel(MigLayout()) {
    private val sortActionPanelConfiguration = SortActionPanelConfiguration()
    private val searchActionPanelConfiguration = SearchActionPanelConfiguration()

    private val buttonCreateAction: JButton = JButton("Create atomic action")
    private val buttonCreateChainFromAction: JButton =
        JButton("Create chain from atomic action").apply { isEnabled = false }
    private val buttonDuplicate: JButton = JButton("Duplicate").apply { isEnabled = false }
    private val buttonEditAction: JButton = JButton("Edit").apply { isEnabled = false }
    private val buttonDeleteAction: JButton = JButton("Delete").apply { isEnabled = false }

    private val allListActionsModel = DefaultListModel<AtomicAction>()
    private val allListActions = JList(allListActionsModel)

    private val createAndEditAtomicActionDialog = CreateAndEditAtomicActionDialog(
        atomicActionService = atomicActionService
    )

    init {
        allListActions.cellRenderer = AtomicActionListCellRenderer(chainActionService)

        allListActions.addListSelectionListener {
            allListActions.selectedValue?.let {
                buttonEditAction.isEnabled = true
                buttonDeleteAction.isEnabled = true
                buttonCreateChainFromAction.isEnabled = true
                buttonDuplicate.isEnabled = true
            }
        }

        allListActions.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    allListActionsModel.getElementAt(allListActions.locationToIndex(e.point))?.let {
                        showPopupMenu(element = it, event = e)
                    }
                }
            }
        })

        buttonEditAction.addActionListener {
            allListActions.selectedValue?.let {
                createAndEditAtomicActionDialog.showEditDialog(it)
            }
        }

        buttonDeleteAction.addActionListener {
            allListActions.selectedValue?.let {
                atomicActionService.deleteAtomic(it.id)
            }
        }

        buttonCreateChainFromAction.addActionListener {
            allListActions.selectedValue?.let {
                createAndEditChainActionDialog.showCreateDialogWith(it)
            }
        }

        buttonDuplicate.addActionListener {
            allListActions.selectedValue?.let {
                val action = it.duplicate()
                atomicActionService.addAtomic(action)
                createAndEditAtomicActionDialog.showEditDialog(action)
            }
        }

        DomainEventFactory.subscriberRegistrar.subscribe(object : DomainEventSubscriber {
            override fun handleEvent(event: DomainEvent) {
                when (event) {
                    is AtomicActionCreatedDomainEvent -> {
                        fillList()
                    }

                    is AtomicActionDeletedDomainEvent -> {
                        val iterator = allListActionsModel.elements()
                        while (iterator.hasMoreElements()) {
                            val action = iterator.nextElement()
                            if (action.id == event.id) {
                                allListActionsModel.removeElement(action)
                                break
                            }
                        }

                        fillList()
                    }

                    is AtomicActionUpdatedDomainEvent -> {
                        val iterator = allListActionsModel.elements()
                        while (iterator.hasMoreElements()) {
                            val action = iterator.nextElement()
                            if (action.id == event.atomicAction.id) {
                                val index = allListActionsModel.indexOf(action)

                                allListActionsModel.set(index, event.atomicAction)
                                break
                            }
                        }

                        fillList()
                    }
                }
            }

            override fun subscribedToEventType(): List<Class<*>> = listOf(
                AtomicActionCreatedDomainEvent::class.java,
                AtomicActionDeletedDomainEvent::class.java,
                AtomicActionUpdatedDomainEvent::class.java,
            )
        })


        val propertyChangeListener = PropertyChangeListener {
            if (
                it.propertyName == SortActionPanelConfiguration.SORT_INFO_PROPERTY_NAME ||
                it.propertyName == SearchActionPanelConfiguration.SEARCH_ACTION_PROPERTY_NAME
            ) {
                fillList()
            }
        }

        fillList()

        sortActionPanelConfiguration.addPropertyChangeListener(propertyChangeListener)
        searchActionPanelConfiguration.addPropertyChangeListener(propertyChangeListener)

        val panelAtomicAction = JPanel(MigLayout())
        panelAtomicAction.add(buttonCreateAction, "split 5")
        panelAtomicAction.add(buttonCreateChainFromAction)
        panelAtomicAction.add(buttonEditAction)
        panelAtomicAction.add(buttonDuplicate)
        panelAtomicAction.add(buttonDeleteAction, "wrap")
        panelAtomicAction.add(JScrollPane(allListActions), "height max, width max")

        buttonCreateAction.apply { addActionListener { createAndEditAtomicActionDialog.showCreateDialog() } }

        border = BorderFactory.createTitledBorder("Actions")

        add(sortActionPanelConfiguration, "wrap, width 100%")
        add(searchActionPanelConfiguration, "wrap, width 100%")
        add(panelAtomicAction, "width 100%")
    }

    private fun fillList() {
        val sortInfo = sortActionPanelConfiguration.sortInfo()
        val searchAction = searchActionPanelConfiguration.searchAction()

        fillActions(sortInfo, searchAction)
    }

    private fun fillActions(sortInfo: SortInfo, searchAction: SearchAction) {
        val atomics = atomicActionService.atomics()

        val sortedAtomics = when (sortInfo.sortField) {
            SortField.NAME -> when (sortInfo.direction) {
                Direction.ASC -> atomics.sortedBy { it.name }
                Direction.DESC -> atomics.sortedByDescending { it.name }
            }

            SortField.ENGINE -> when (sortInfo.direction) {
                Direction.ASC -> atomics.sortedBy { it.engine }
                Direction.DESC -> atomics.sortedByDescending { it.engine }
            }

            SortField.CONTRACT -> when (sortInfo.direction) {
                Direction.ASC -> atomics.sortedBy { it.contractType }
                Direction.DESC -> atomics.sortedByDescending { it.contractType }
            }

            SortField.SOURCE -> when (sortInfo.direction) {
                Direction.ASC -> atomics.sortedBy { it.source }
                Direction.DESC -> atomics.sortedByDescending { it.source }
            }
        }

        val filterByText = when (searchAction) {
            is SearchAction.SearchInfo -> {
                if (searchAction.text.isEmpty()) {
                    sortedAtomics
                } else {
                    sortedAtomics
                        .filter {
                            it.name.lowercase().contains(searchAction.text.lowercase()) ||
                                it.description.lowercase().contains(searchAction.text.lowercase()) ||
                                it.data.lowercase().contains(searchAction.text.lowercase())
                        }
                }
            }

            is SearchAction.Reset -> sortedAtomics
        }

        var filterByConditions = filterByText
        searchAction.conditions.forEach { condition ->
            when (condition) {
                SearchAction.SearchCondition.IN_OUT -> filterByConditions =
                    filterByConditions.filter { it.contractType == ContractType.IN_OUT }

                SearchAction.SearchCondition.IN_UNIT -> filterByConditions =
                    filterByConditions.filter { it.contractType == ContractType.IN_UNIT }

                SearchAction.SearchCondition.UNIT_OUT -> filterByConditions =
                    filterByConditions.filter { it.contractType == ContractType.UNIT_OUT }

                SearchAction.SearchCondition.UNIT_UNIT -> filterByConditions =
                    filterByConditions.filter { it.contractType == ContractType.UNIT_UNIT }

                SearchAction.SearchCondition.KOTLIN -> filterByConditions =
                    filterByConditions.filter { it.engine == AtomicActionEngine.KOTLIN }

                SearchAction.SearchCondition.GROOVY -> filterByConditions =
                    filterByConditions.filter { it.engine == AtomicActionEngine.GROOVY }

                SearchAction.SearchCondition.TEXT -> filterByConditions =
                    filterByConditions.filter { it.source == AtomicActionSource.TEXT }

                SearchAction.SearchCondition.FILE -> filterByConditions =
                    filterByConditions.filter { it.source == AtomicActionSource.FILE }
            }
        }

        allListActionsModel.removeAllElements()
        filterByConditions.forEach {
            allListActionsModel.addElement(it)
        }
    }

    private fun showPopupMenu(element: AtomicAction, event: MouseEvent) {
        val chains = chainActionService.usageAction(element.id)

        val popup = JPopupMenu()
        popup.add(JMenu("Usage").apply {
            add(
                ChainsSelectPanel(
                    chains = chains,
                    atomicActionService = atomicActionService,
                    selectChainCallback = { chain -> createAndEditChainActionDialog.showEditDialog(chain) }
                )
            )
        })

        popup.show(allListActions, event.x, event.y)
    }
}
