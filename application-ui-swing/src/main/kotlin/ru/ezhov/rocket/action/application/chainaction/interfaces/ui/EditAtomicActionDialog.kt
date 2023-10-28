package ru.ezhov.rocket.action.application.chainaction.interfaces.ui

import net.miginfocom.swing.MigLayout
import ru.ezhov.rocket.action.application.chainaction.application.ChainActionExecutorService
import ru.ezhov.rocket.action.application.chainaction.application.ChainActionService
import ru.ezhov.rocket.action.application.chainaction.domain.model.AtomicAction
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.KeyStroke

class EditAtomicActionDialog(
    private val chainActionExecutorService: ChainActionExecutorService,
    private val chainActionService: ChainActionService,
) : JDialog() {
    private val contentPane = JPanel(MigLayout(/*"debug"*/))
    private val buttonOK: JButton = JButton("Create")
    private val buttonCancel: JButton? = JButton("Cancel")

    private val nameTextField: JTextField = JTextField()
    private val nameLabel: JLabel = JLabel("Name:").apply { labelFor = nameTextField }

    private val descriptionTextPane: JTextPane = JTextPane()
    private val descriptionLabel: JLabel = JLabel("Description:").apply { labelFor = descriptionTextPane }

    private val engineLabel: JLabel = JLabel("Engine:")
    private val kotlinEngine: JRadioButton = JRadioButton("Kotlin")
    private val groovyEngine: JRadioButton = JRadioButton("Groovy")

    private val sourceLabel: JLabel = JLabel("Source:")
    private val textSource: JRadioButton = JRadioButton("Text")
    private val fileSource: JRadioButton = JRadioButton("File")


    private val dataTextPane: JTextPane = JTextPane()
    private val dataLabel: JLabel = JLabel("Data:").apply { labelFor = dataTextPane }

    init {
        kotlinEngine.isSelected = true
        val engineButtonGroup = ButtonGroup()
        engineButtonGroup.add(kotlinEngine)
        engineButtonGroup.add(groovyEngine)

        textSource.isSelected = true
        val sourceButtonGroup = ButtonGroup()
        sourceButtonGroup.add(fileSource)
        sourceButtonGroup.add(textSource)

        contentPane.add(nameLabel)
        contentPane.add(nameTextField, "grow, span 2, wrap, width max")

        contentPane.add(descriptionLabel, "wrap")
        contentPane.add(JScrollPane(descriptionTextPane), "span, grow, shrink 50, hmin 18%")

        contentPane.add(engineLabel)
        contentPane.add(kotlinEngine)
        contentPane.add(groovyEngine, "wrap")

        contentPane.add(sourceLabel)
        contentPane.add(textSource)
        contentPane.add(fileSource, "wrap")

        contentPane.add(dataLabel, "wrap")
        contentPane.add(JScrollPane(dataTextPane), "span, grow, shrink, height max")

        contentPane.add(buttonOK, "cell 2 7, split 2, align right")
        contentPane.add(buttonCancel)

        setContentPane(contentPane)
        isModal = true
        getRootPane().defaultButton = buttonOK
        buttonOK.addActionListener { onOK() }
        buttonCancel!!.addActionListener { onCancel() }

        // call onCancel() when cross is clicked
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                onCancel()
            }
        })

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
            { onCancel() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )

        setLocationRelativeTo(null)
        setSize(700, 500)
    }

    private fun onOK() {
        // add your code here
        dispose()
    }

    private fun onCancel() {
        // add your code here if necessary
        dispose()
    }

    private var action: AtomicAction? = null

    fun setAtomicAction(action: AtomicAction) {
        this.action = action
    }
}