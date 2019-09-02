
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatSpinner


class MySpinner : AppCompatSpinner {
    private var modalHack: ModalHack
    private val TAG = "MySpinner"

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * An interface which a client of this Spinner could use to receive
     * open/closed events for this Spinner.
     */
    interface OnSpinnerEventsListener {

        /**
         * Callback triggered when the spinner was opened.
         */
        fun onSpinnerOpened(spinner: Spinner)

        /**
         * Callback triggered when the spinner was closed.
         */
        fun onSpinnerClosed(spinner: Spinner)

    }

    private var mListener: OnSpinnerEventsListener? = null
    private var mOpenInitiated = false

    // implement the Spinner constructors that you need
    init {
        modalHack = ModalHack()
    }

    override fun performClick(): Boolean {
        // register that the Spinner was opened so we have a status
        // indicator for when the container holding this Spinner may lose focus
        mOpenInitiated = true
        isSelected = true
        hideKeyboard()
        mListener?.apply { onSpinnerOpened(this@MySpinner) }

        modalHack.beforeShow()
        val performClick = super.performClick()
        modalHack.afterShow()

        return performClick
    }

    // hide the navigation bar when the spinner opens @API 21 - 29(Lollipop to Q)
    private inner class ModalHack {
        // API >= 23
        private var popupWindow: ListPopupWindow? = null

        // API 21 22
        private var popupWindow2122: androidx.appcompat.widget.ListPopupWindow? = null

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) popupWindow = setModal(false)
            else popupWindow2122 = setModal2122(false)
        }

        private fun setModal(b: Boolean): ListPopupWindow? {
            try {
                if (b) performClosedEvent()

                if (!hasNavBar(context.resources)) return null

                Spinner::class.java.getDeclaredField("mPopup").apply {
                    isAccessible = true
                    return (get(this@MySpinner) as ListPopupWindow).also {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                            it.listView?.run {
                                if (onFocusChangeListener == null) setOnFocusChangeListener { _, flag ->
                                    if (!flag) setModal(true)
                                }
                            }
                        }
                        it.isModal = b
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "e = $e")
            }
            return null
        }

        // listView was born
        fun afterShow() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setModal(false)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) mPopupRequestFocus(popupWindow)
            } else {
                setModal2122(false)
                mPopupRequestFocus(popupWindow2122)
            }
        }

        // for API 23 - 27
        fun mPopupRequestFocus(listPopupWindow: ListPopupWindow?) {
            if (listPopupWindow == null) return

            try {
                ListPopupWindow::class.java.getDeclaredField("mPopup").apply {
                    isAccessible = true
                    (get(listPopupWindow) as PopupWindow).let { popupWindow ->
                        popupWindow.contentView?.apply {
                            requestFocus(View.FOCUS_DOWN)
                            setOnFocusChangeListener { _, hasFocus ->
                                if (!popupWindow.isShowing && !hasFocus) setModal(true)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "e = $e")
            }
        }

        // for API 21 22
        fun mPopupRequestFocus(listPopupWindow: androidx.appcompat.widget.ListPopupWindow?) {
            if (listPopupWindow == null) return

            try {
                androidx.appcompat.widget.ListPopupWindow::class.java.getDeclaredField("mPopup").apply {
                    isAccessible = true
                    (get(listPopupWindow) as PopupWindow).let { popupWindow ->
                        popupWindow.contentView?.apply {
                            requestFocus(View.FOCUS_DOWN)
                            setOnFocusChangeListener { _, hasFocus ->
                                if (!popupWindow.isShowing && !hasFocus) setModal2122(true)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "e = $e")
            }
        }

        fun beforeShow() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setModal(false)
            } else {
                setModal2122(false)
            }
        }

        private fun setModal2122(b: Boolean): androidx.appcompat.widget.ListPopupWindow? {
            try {
                if (b) performClosedEvent()

                if (!hasNavBar(context.resources)) return null

                AppCompatSpinner::class.java.getDeclaredField("mPopup").apply {
                    isAccessible = true
                    return (get(this@MySpinner) as androidx.appcompat.widget.ListPopupWindow).apply {
                        isModal = b
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "e = $e")
            }
            return null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasBeenOpened() && hasFocus) {
            performClosedEvent()
        }
    }

    /**
     * Register the listener which will listen for events.
     */
    fun setSpinnerEventsListener(onSpinnerEventsListener: OnSpinnerEventsListener) {
        mListener = onSpinnerEventsListener
    }

    /**
     * Propagate the closed Spinner event to the listener from outside if needed.
     */
    fun performClosedEvent() {
        mOpenInitiated = false
        isSelected = false
        mListener?.apply { onSpinnerClosed(this@MySpinner) }
    }

    /**
     * A boolean flag indicating that the Spinner triggered an open event.
     *
     * @return true for opened Spinner
     */
    fun hasBeenOpened(): Boolean {
        return mOpenInitiated
    }
}