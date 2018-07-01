package libui

import kotlinx.cinterop.*

/** Represents a top-level window.
 *  Contains one child control that occupies the entirety of the window. */
class Window(
    title: String,
    width: Int,
    height: Int,
    hasMenubar: Boolean = false
) : Control<uiWindow>(uiNewWindow(title, width, height, if (hasMenubar) 1 else 0)),
    Container {

    /** Specify the control to show in content area.
     *  Window can contain only one control, if you need more use layouts like Box or Grid */
    override fun <T : Control<*>> add(widget: T): T {
        uiWindowSetChild(ptr, widget.ctl)
        return widget
    }

    internal var onClose: (Window.() -> Boolean)? = null
    internal var onResize: (Window.() -> Unit)? = null
}

/** Set or return the text to show in window title bar. */
var Window.title: String
    get() = uiWindowTitle(ptr)?.toKString() ?: ""
    set(title) = uiWindowSetTitle(ptr, title)

/** Allow to specify that the window is a frameless one, without borders,
 *  title bar and OS window control widgets. */
var Window.borderless: Boolean
    get() = uiWindowBorderless(ptr) != 0
    set(borderless) = uiWindowSetBorderless(ptr, if (borderless) 1 else 0)

/** Specify if the Window content should have a margin or not. */
var Window.margined: Boolean
    get() = uiWindowMargined(ptr) != 0
    set(margined) = uiWindowSetMargined(ptr, if (margined) 1 else 0)

/** Whether the window should show in fullscreen or not. */
var Window.fullscreen: Boolean
    get() = uiWindowFullscreen(ptr) != 0
    set(fullscreen) = uiWindowSetFullscreen(ptr, if (fullscreen) 1 else 0)

/** Size in pixel of the content area of the window.
 *  Window decoration size are excluded. This mean that if you set window size to 0,0
 *  you still see title bar and OS window buttons. */
var Window.contentSize: SizeInt
    get() = memScoped {
        val width = alloc<IntVar>()
        var height = alloc<IntVar>()
        uiWindowContentSize(ptr, width.ptr, height.ptr)
        SizeInt(width.value, height.value)
    }
    set(size) = uiWindowSetContentSize(ptr, size.width, size.height)

/** Function to be run when window content size change. */
fun Window.onResize(block: Window.() -> Unit) {
    onResize = block
    uiWindowOnContentSizeChanged(ptr, staticCFunction { _, ref -> with(ref.to<Window>()) {
        onResize?.invoke(this)
    }}, ref.asCPointer())
}

/** Function to be run when the user clicks the Window's close button.
 *  Only one function can be registered at a time.
 *  @returns [true] if window is disposed */
fun Window.onClose(block: Window.() -> Boolean) {
    onClose = block
    uiWindowOnClosing(ptr, staticCFunction { _, ref -> with(ref.to<Window>()) {
        if (onClose?.invoke(this) ?: true) 1 else 0
    }}, ref.asCPointer())
}

/** Displays a modal Open File Dialog. */
fun Window.OpenFileDialog(): String? {
    val rawName = uiOpenFile(ptr)
    if (rawName == null) return null
    val strName = rawName.toKString()
    uiFreeText(rawName)
    return strName
}

/** Displays a modal Save File Dialog. */
fun Window.SaveFileDialog(): String? {
    val rawName = uiSaveFile(ptr)
    if (rawName == null) return null
    val strName = rawName.toKString()
    uiFreeText(rawName)
    return strName
}

/** Displays a modal Message Box. */
fun Window.MsgBox(text: String, details: String = "")
    = uiMsgBox(ptr, text, details)

/** Displays a modal Error Message Box. */
fun Window.MsgBoxError(text: String, details: String = "")
    = uiMsgBoxError(ptr, text, details)
