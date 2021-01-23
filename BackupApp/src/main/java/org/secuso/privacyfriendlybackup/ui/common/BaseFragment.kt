package org.secuso.privacyfriendlybackup.ui.common

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.ui.main.MainActivity
import org.secuso.privacyfriendlybackup.ui.backup.BackupOverviewFragment
import java.lang.Exception

abstract class BaseFragment : Fragment() {

    val TAG = "PFA BaseFragment"

    protected lateinit var toolbar : Toolbar
    protected lateinit var appBar : AppBarLayout

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbar = requireActivity().findViewById(R.id.toolbar)
        appBar = requireActivity().findViewById(R.id.app_bar)

        if(this !is BackupOverviewFragment) {
            returnToNormalToolbarColor()
        }
    }

    abstract fun onBackPressed()

    private fun returnToNormalToolbarColor() {
        var colorFrom = ContextCompat.getColor(requireContext(), R.color.middlegrey)
        val background = appBar.background
        if(background is ColorDrawable) {
            colorFrom = background.color
        }
        val colorTo = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        playColorAnimation(colorFrom, colorTo, duration = 350).start()
    }

    protected fun setTitle(@StringRes stringRes: Int) {
        if(arguments?.getBoolean(MainActivity.TWOPANE) == false) {
            val title = getString(stringRes)
            toolbar.title = title
            requireActivity().actionBar?.title = title
        }
    }

    protected fun playColorAnimationRes(@ColorRes colorFromId: Int, @ColorRes colorToId: Int, applyAnimation : (a : ValueAnimator) -> Unit = this::defaultFadeAnimation) : Animator {
        val colorFrom = ContextCompat.getColor(requireContext(), colorFromId)
        val colorTo = ContextCompat.getColor(requireContext(), colorToId)

        return playColorAnimation(colorFrom, colorTo, 250, applyAnimation)
    }

    protected fun playColorAnimation(@ColorInt colorFrom: Int, @ColorInt colorTo: Int, duration : Long = 250, applyAnimation : (a : ValueAnimator) -> Unit = this::defaultFadeAnimation) : Animator {
        val colorAnimation: ValueAnimator = ValueAnimator.ofArgb(colorFrom, colorTo)
        colorAnimation.duration = duration
        colorAnimation.addUpdateListener {
            applyAnimation(it)
        }
        return colorAnimation
    }

    private fun defaultFadeAnimation(a : ValueAnimator) {
        appBar.setBackgroundColor(a.animatedValue as Int)
        toolbar.setBackgroundColor(a.animatedValue as Int)
        activity?.window?.statusBarColor = a.animatedValue as Int
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val activity = requireActivity()
        val menuItemName = arguments?.getString(MainActivity.SELECTED_MENU_ITEM)
        val menuItem = if(menuItemName != null) {
            try {
                MainActivity.MenuItem.valueOf(menuItemName)
            } catch (e : Exception) {
                MainActivity.MenuItem.MENU_MAIN_APPS
            }
        } else {
            MainActivity.MenuItem.MENU_MAIN_APPS
        }

        if(isLargeTablet()) {
            if(activity is DisplayMenuItemActivity && !isPortrait()) {
                activity.finish()
            } else if(activity is MainActivity && isPortrait()){
                val intent = activity.intent.apply {
                    putExtra(MainActivity.SELECTED_MENU_ITEM, menuItem.name)
                }
                activity.finish()
                activity.startActivity(intent)
            }
        }
    }

    protected fun isRtl(): Boolean {
        return resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    protected fun isLargeTablet(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    protected fun isXLargeTablet(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    protected fun isPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }
}