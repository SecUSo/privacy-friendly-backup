package org.secuso.privacyfriendlybackup.ui.application

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_backup_overview.*
import org.secuso.privacyfriendlybackup.BackupApplication
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.ui.common.BaseFragment
import org.secuso.privacyfriendlybackup.ui.common.DisplayMenuItemActivity
import org.secuso.privacyfriendlybackup.ui.main.MainActivity
import org.secuso.privacyfriendlybackup.ui.main.MainActivity.Companion.FILTER
import org.secuso.privacyfriendlybackup.ui.main.MainActivity.Companion.SELECTED_MENU_ITEM
import org.secuso.privacyfriendlybackup.ui.common.Mode


class ApplicationOverviewFragment : BaseFragment(), ApplicationAdapter.ManageListAdapterCallback {

    // ui
    private lateinit var viewModel: ApplicationOverviewViewModel
    private lateinit var adapter : ApplicationAdapter

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

        setTitle(R.string.fragment_title_application_overview)

        viewModel = ViewModelProvider(this).get(ApplicationOverviewViewModel::class.java)
        adapter = ApplicationAdapter(requireContext(), this, viewLifecycleOwner)

        fragment_backup_overview_list.adapter = adapter
        fragment_backup_overview_list.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        viewModel.appLiveData.observe(viewLifecycleOwner) { data ->
            adapter.setData(data)

            (requireActivity().application as BackupApplication).schedulePeriodicWork()
        }
    }

    override fun onBackPressed() {
        val activity = activity

        if(activity != null && activity is DisplayMenuItemActivity) {
            activity.pressBack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("oldMode", oldMode.name)
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

    private fun gotoBackups(packageName : String) {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK
            putExtra(SELECTED_MENU_ITEM, MainActivity.MenuItem.MENU_MAIN_BACKUP_OVERVIEW.name)
            putExtra(FILTER, packageName)
        }
        startActivity(intent)

        // if we are in singlepane mode - close current activity
        if(arguments?.getBoolean(MainActivity.TWOPANE) == false) {
            requireActivity().finish()
        }
    }
}