package ru.ezhov.rocket.action.plugin.showimage

import mu.KotlinLogging
import org.jdesktop.swingx.JXImageView
import org.jdesktop.swingx.JXPanel
import ru.ezhov.rocket.action.api.RocketAction
import ru.ezhov.rocket.action.api.RocketActionConfiguration
import ru.ezhov.rocket.action.api.RocketActionConfigurationProperty
import ru.ezhov.rocket.action.api.RocketActionConfigurationPropertyKey
import ru.ezhov.rocket.action.api.RocketActionFactoryUi
import ru.ezhov.rocket.action.api.RocketActionPlugin
import ru.ezhov.rocket.action.api.RocketActionSettings
import ru.ezhov.rocket.action.api.RocketActionType
import ru.ezhov.rocket.action.api.context.RocketActionContext
import ru.ezhov.rocket.action.api.context.icon.AppIcon
import ru.ezhov.rocket.action.api.support.AbstractRocketAction
import ru.ezhov.rocket.action.ui.utils.swing.common.toImage
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Image
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.concurrent.ExecutionException
import javax.imageio.ImageIO
import javax.swing.AbstractAction
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JToolBar
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.WindowConstants

private val logger = KotlinLogging.logger {}

class ShowImageRocketActionUi : AbstractRocketAction(), RocketActionPlugin {
    private var actionContext: RocketActionContext? = null

    override fun factory(context: RocketActionContext): RocketActionFactoryUi = this
        .apply {
            actionContext = context
        }

    override fun configuration(context: RocketActionContext): RocketActionConfiguration = this
        .apply {
            actionContext = context
        }

    override fun create(settings: RocketActionSettings, context: RocketActionContext): RocketAction? =
        settings.settings()[IMAGE_URL]?.takeIf { it.isNotEmpty() }?.let { imageUrl ->
            val label = settings.settings()[LABEL]?.takeIf { it.isNotEmpty() } ?: imageUrl
            val description = settings.settings()[DESCRIPTION]?.takeIf { it.isNotEmpty() } ?: imageUrl

            val menu = JMenu(label)
            menu.toolTipText = description
            menu.icon = ImageIcon(this.javaClass.getResource("/icons/load_16x16.gif"))
            LoadImageWorker(imageUrl = imageUrl, menu = menu, settings = settings).execute()

            object : RocketAction {
                override fun contains(search: String): Boolean =
                    label.contains(search, ignoreCase = true)
                        .or(description.contains(search, ignoreCase = true))

                override fun isChanged(actionSettings: RocketActionSettings): Boolean =
                    !(settings.id() == actionSettings.id() &&
                        settings.settings() == actionSettings.settings())

                override fun component(): Component = menu
            }
        }

    override fun type(): RocketActionType = RocketActionType { "SHOW_IMAGE" }

    override fun description(): String = "Показать изображения *.png и *.jpg"

    override fun asString(): List<RocketActionConfigurationPropertyKey> = listOf(LABEL, IMAGE_URL)

    override fun properties(): List<RocketActionConfigurationProperty> {
        return listOf(
            createRocketActionProperty(key = LABEL, name = LABEL.value, description = "Заголовок", required = false),
            createRocketActionProperty(key = DESCRIPTION, name = DESCRIPTION.value, description = "Описание", required = false),
            createRocketActionProperty(key = IMAGE_URL, name = IMAGE_URL.value, description = "URL изображения", required = true)
        )
    }

    override fun name(): String = "Показать изображение *.png и *.jpg"

    private inner class LoadImageWorker(
        private val imageUrl: String,
        private val menu: JMenu, private val settings: RocketActionSettings
    ) : SwingWorker<Image?, String?>() {
        private var cachedImage: File? = null

        @Throws(Exception::class)
        override fun doInBackground(): Image? {
            val url = imageUrl
            val file = actionContext!!.cache().get(URL(url))
            return file?.let { f -> cachedImage = f; ImageIO.read(f) }
        }

        override fun done() {
            menu.icon = actionContext!!.icon().by(AppIcon.IMAGE)
            try {
                val component: Component
                val originalImageUrl = settings.settings()[IMAGE_URL]
                if (originalImageUrl != null) {
                    component = ImagePanel(originalUrl = originalImageUrl, image = this.get(), cachedImage = cachedImage!!)
                } else {
                    val panel = JPanel()
                    panel.add(JLabel(imageUrl))
                    component = panel
                }
                menu.add(component)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ImagePanel(originalUrl: String, image: Image?, cachedImage: File) : JXPanel(BorderLayout()) {
        init {
            val dimension = Toolkit.getDefaultToolkit().screenSize
            val widthNew = (dimension.width * 0.5).toInt()
            val heightNew = (dimension.height * 0.5).toInt()
            val newDimension = Dimension(widthNew, heightNew)
            val toolBar = JToolBar()
            toolBar.isFloatable = false
            toolBar.add(object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    SwingUtilities.invokeLater {
                        val frame = JFrame(cachedImage.absolutePath)
                        frame.iconImage = actionContext!!.icon().by(AppIcon.ROCKET_APP).toImage()
                        frame.add(ImagePanel(originalUrl = originalUrl, image = image, cachedImage = cachedImage))
                        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                        frame.setSize((dimension.width * 0.8).toInt(), (dimension.height * 0.8).toInt())
                        frame.setLocationRelativeTo(null)
                        frame.isVisible = true
                    }
                }

                init {
                    putValue(NAME, "Открыть в отдельном окне")
                }
            })
            preferredSize = newDimension
            maximumSize = newDimension
            minimumSize = newDimension
            val panelImage = JXPanel(BorderLayout())
            val imageView = JXImageView()
            imageView.image = image
            imageView.autoscrolls = true
            panelImage.add(imageView, BorderLayout.CENTER)
            add(toolBar, BorderLayout.NORTH)
            add(panelImage, BorderLayout.CENTER)
            val slider = JSlider(1, 100, 100)
            toolBar.add(slider)
            slider.addChangeListener { imageView.scale = slider.value / 100.0 }

            val panelBottom = JPanel()
            panelBottom.layout = BoxLayout(panelBottom, BoxLayout.Y_AXIS)
            val cachedLabel = JLabel("Cached: ${cachedImage.absolutePath}")
                .also { label ->
                    label.addMouseListener(openFileMouseListener(label, cachedImage))
                }
            panelBottom.add(cachedLabel)

            try {
                URI.create(originalUrl)
            } catch (ex: Exception) {
                logger.warn { "Invalid URI='$originalUrl'" }
                null
            }
                ?.let { uri ->
                    val originalLabel = JLabel("Original: $originalUrl")
                        .also { label ->
                            label.addMouseListener(openUriMouseListener(label, uri))
                        }
                    panelBottom.add(originalLabel)
                }

            add(panelBottom, BorderLayout.SOUTH)
        }
    }

    private fun openFileMouseListener(label: JLabel, file: File) = object : MouseAdapter() {
        override fun mouseReleased(e: MouseEvent) {
            SwingUtilities.invokeLater {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(file)
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                    }
                }
            }
        }

        override fun mouseEntered(e: MouseEvent) {
            SwingUtilities.invokeLater { label.foreground = Color.BLUE }
        }

        override fun mouseExited(e: MouseEvent) {
            SwingUtilities.invokeLater { label.foreground = JLabel().foreground }
        }
    }

    private fun openUriMouseListener(label: JLabel, uri: URI) = object : MouseAdapter() {
        override fun mouseReleased(e: MouseEvent) {
            SwingUtilities.invokeLater {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(uri)
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                    }
                }
            }
        }

        override fun mouseEntered(e: MouseEvent) {
            SwingUtilities.invokeLater { label.foreground = Color.BLUE }
        }

        override fun mouseExited(e: MouseEvent) {
            SwingUtilities.invokeLater { label.foreground = JLabel().foreground }
        }
    }

    override fun icon(): Icon? = actionContext!!.icon().by(AppIcon.IMAGE)

    companion object {
        private val LABEL = RocketActionConfigurationPropertyKey("label")
        private val DESCRIPTION = RocketActionConfigurationPropertyKey("description")
        private val IMAGE_URL = RocketActionConfigurationPropertyKey("imageUrl")
    }
}
