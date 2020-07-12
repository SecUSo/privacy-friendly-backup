package org.secuso.privacyfriendlybackup.ui.application

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_backup_overview.*
import org.secuso.privacyfriendlybackup.BackupApplication
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.ui.DisplayMenuItemActivity
import org.secuso.privacyfriendlybackup.ui.MainActivity
import org.secuso.privacyfriendlybackup.ui.MainActivity.Companion.FILTER
import org.secuso.privacyfriendlybackup.ui.MainActivity.Companion.SELECTED_MENU_ITEM
import org.secuso.privacyfriendlybackup.ui.common.Mode


class ApplicationOverviewFragment : Fragment(), ApplicationAdapter.ManageListAdapterCallback {

    // ui
    private lateinit var viewModel: ApplicationOverviewViewModel
    private lateinit var adapter : ApplicationAdapter
    private lateinit var toolbar : Toolbar
    private lateinit var appBar : AppBarLayout

    private var oldMode : Mode = Mode.NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        savedInstanceState ?: return

        val savedOldMode = savedInstanceState.getString("oldMode")
        oldMode = if(savedOldMode != null) Mode.valueOf(savedOldMode) else oldMode
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_overview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ApplicationOverviewViewModel::class.java)
        adapter = ApplicationAdapter(requireContext(), this)

        toolbar = requireActivity().findViewById(R.id.toolbar)
        appBar = requireActivity().findViewById(R.id.app_bar)

        fragment_backup_overview_list.adapter = adapter
        fragment_backup_overview_list.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        viewModel.appLiveData.observe(viewLifecycleOwner) { data ->
            adapter.setData(data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("oldMode", oldMode.name)
    }

    private fun playColorAnimation(colorFrom: Int, colorTo: Int, applyAnimation : (a : ValueAnimator) -> Unit = this::defaultFadeAnimation) : Animator {
        val colorAnimation: ValueAnimator = ValueAnimator.ofArgb(colorFrom, colorTo)
        colorAnimation.duration = 250
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_sort -> {
                // TODO: to implement sorting - just swap the comparator :)
            }
            else -> return false
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val activity = requireActivity()

        if(isLargeTablet()) {
            if(activity is DisplayMenuItemActivity && !isPortrait()) {
                activity.finish()
            } else if(activity is MainActivity && isPortrait()){
                val intent = activity.intent.apply {
                    putExtra(MainActivity.SELECTED_MENU_ITEM, MainActivity.MenuItem.MENU_MAIN_BACKUP_OVERVIEW.ordinal)
                }
                activity.finish()
                activity.startActivity(intent)
            }
        }
    }

    private fun isRtl(): Boolean {
        return resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    private fun isLargeTablet(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun isXLargeTablet(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    private fun isPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    override fun onItemClick(view: View, packageName: String, job : BackupJob?, menuItemId: Int?) {
        when(menuItemId) {
            R.id.menu_backup -> viewModel.createBackupForPackage(packageName)
            R.id.menu_cancel_jobs -> viewModel.cancelRunningJobs(packageName)
            R.id.menu_restore_most_recent_backup -> viewModel.restoreRecentBackup(packageName)
            R.id.menu_manage_backups -> gotoBackups(packageName)
        }
        if(job != null) {
            (requireActivity().application as BackupApplication).schedulePeriodicWork()
        }
    }

    fun gotoBackups(packageName : String) {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            putExtra(SELECTED_MENU_ITEM, MainActivity.MenuItem.MENU_MAIN_BACKUP_OVERVIEW.ordinal)
            putExtra(FILTER, packageName)
        }
        startActivity(intent)
    }
}