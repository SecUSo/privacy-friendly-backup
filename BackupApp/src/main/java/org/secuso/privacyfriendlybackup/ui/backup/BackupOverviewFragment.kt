package org.secuso.privacyfriendlybackup.ui.backup

import android.animation.*
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import kotlinx.android.synthetic.main.fragment_backup_overview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.DIALOG_SKIP_IMPORT_START
import org.secuso.privacyfriendlybackup.ui.common.BaseFragment
import org.secuso.privacyfriendlybackup.ui.common.DisplayMenuItemActivity
import org.secuso.privacyfriendlybackup.ui.common.Mode
import org.secuso.privacyfriendlybackup.ui.inspection.DataInspectionActivity
import org.secuso.privacyfriendlybackup.ui.importbackup.ImportBackupActivity
import org.secuso.privacyfriendlybackup.ui.main.MainActivity.Companion.BACKUP_ID
import org.secuso.privacyfriendlybackup.ui.main.MainActivity.Companion.FILTER


class BackupOverviewFragment : BaseFragment(),
    FilterableBackupAdapter.ManageListAdapterCallback,
    SearchView.OnQueryTextListener {

    companion object {
        const val TAG = "PFA BackupFragment"

        fun newInstance() =
            BackupOverviewFragment()
    }

    private var currentDeleteCount: Int = 0

    private lateinit var viewModel: BackupOverviewViewModel
    private lateinit var adapter : FilterableBackupAdapter

    private var toolbarDeleteIcon: MenuItem? = null
    private var searchIcon: MenuItem? = null
    private var selectAllIcon: MenuItem? = null
    private var oldMode : Mode = Mode.NORMAL

    var predefinedFilter : String? = null
    var highlightSpecificBackup : Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        Log.d(
            TAG,
            "## Backup Overview Fragment created and savedInstanceState is ${if (savedInstanceState == null) "null" else "not null"}"
        )

        savedInstanceState ?: return

        val savedOldMode = savedInstanceState.getString("oldMode")
        oldMode = if(savedOldMode != null) Mode.valueOf(savedOldMode) else oldMode

        currentDeleteCount = savedInstanceState.getInt("currentDeleteCount")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_backup_overview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setTitle(R.string.fragment_title_backup_overview)

        viewModel = ViewModelProvider(this).get(BackupOverviewViewModel::class.java)
        adapter =
            FilterableBackupAdapter(
                requireContext(),
                this
            )

        fragment_backup_overview_list.adapter = adapter
        fragment_backup_overview_list.layoutManager = when {
            isXLargeTablet() -> {
                GridLayoutManager(
                    context,
                    if (isPortrait()) 2 else 3,
                    GridLayoutManager.VERTICAL,
                    false
                )
            }
            isLargeTablet() -> {
                GridLayoutManager(
                    context,
                    if (isPortrait()) 1 else 2,
                    GridLayoutManager.VERTICAL,
                    false
                )
            }
            else -> {
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            }
        }

        fab.setOnClickListener {
            if(Mode.DELETE.isActiveIn(viewModel.getCurrentMode())) {

                val builder = AlertDialog.Builder(requireContext()).apply {
                    setTitle(R.string.dialog_delete_confirmation_title)
                    setMessage(R.string.dialog_delete_confirmation_message)
                    setNegativeButton(R.string.dialog_delete_confirmation_negative, null)
                    setPositiveButton(R.string.dialog_delete_confirmation_positive) { dialog, _ ->
                        viewModel.deleteData(adapter.getDeleteList())
                        dialog.dismiss()
                        onDisableDeleteMode()
                    }
                }
                builder.show()
            }
        }

        Log.d(TAG, "arguments = $arguments")
        predefinedFilter = arguments?.getString(FILTER, "")
        highlightSpecificBackup = arguments?.getLong(BACKUP_ID) ?: -1L
        Log.d(TAG, "predefinedFilter = $predefinedFilter")

        if(!predefinedFilter.isNullOrEmpty()) {
            viewModel.setFilterText(predefinedFilter)
        }

        viewModel.backupLiveData.observe(viewLifecycleOwner) { data ->
            adapter.setData(data)
        }

        viewModel.filteredBackupLiveData.observe(viewLifecycleOwner) { data ->
            adapter.setFilteredData(data)

            playAnimationIfApplicable(data)
            displayNoElementsImage(data.isEmpty())
        }

        viewModel.currentMode.observe(viewLifecycleOwner) { mode ->
            val colorFrom = ContextCompat.getColor(requireContext(), oldMode.color)
            val colorTo = ContextCompat.getColor(requireContext(), mode.color)

            // enabled search
            if(!Mode.SEARCH.isActiveIn(oldMode) && Mode.SEARCH.isActiveIn(mode)) {
                val searchRevealOpen = animateSearchToolbar(42, false, true)
                val colorFade = playColorAnimation(colorFrom, colorTo) {
                    activity?.window?.statusBarColor = it.animatedValue as Int
                }
                val set = AnimatorSet()
                set.playTogether(searchRevealOpen, colorFade)
                set.start()

                // disable search
            } else if(Mode.SEARCH.isActiveIn(oldMode) && !Mode.SEARCH.isActiveIn(mode)) {
                val searchToolbarClose = animateSearchToolbar(2, false, false)
                val colorFade = playColorAnimation(colorFrom, colorTo) {
                    activity?.window?.statusBarColor = it.animatedValue as Int
                }
                val set = AnimatorSet()
                set.playTogether(searchToolbarClose, colorFade)
                set.start()

                // everything else
            } else {
                playColorAnimation(colorFrom, colorTo).start()
            }

            if(Mode.SEARCH.isActiveIn(mode)) {
                searchIcon?.expandActionView()
            } else {
                searchIcon?.collapseActionView()
            }

            if(Mode.DELETE.isActiveIn(mode)) {
                onEnableDeleteMode()
            } else {
                onDisableDeleteMode()
            }

            oldMode = mode
        }
    }

    private fun displayNoElementsImage(show: Boolean) {
        backup_overview_no_entries.visibility = if(show) View.VISIBLE else View.GONE
    }

    private fun playAnimationIfApplicable(data: List<BackupDataStorageRepository.BackupData>) {
        if(data.isNotEmpty() && highlightSpecificBackup != -1L) {
            for (i in data.indices) {
                if(data[i].id == highlightSpecificBackup) {
                    Log.d(TAG, "## found backup in recyclerView at position $i")
                    val highlight = highlightSpecificBackup
                    highlightSpecificBackup = -1L

                    GlobalScope.launch(Dispatchers.Main) {

                        delay(250L)
                        smoothScrollToPosition(i)

                        delay(250L)
                        Log.d(TAG, "## finding viewholder for item id $highlight")
                        val vh = fragment_backup_overview_list.findViewHolderForItemId(highlight)
                        if(vh != null) {
                            (vh as FilterableBackupAdapter.ViewHolder).apply {
                                ObjectAnimator.ofObject(
                                    mCard,
                                    "backgroundColor",
                                    ArgbEvaluator(),
                                    Color.WHITE,
                                    0x0274B2,
                                    Color.WHITE
                                ).apply {
                                    duration = 2000L
                                    startDelay = 250L
                                    start()
                                }
                            }
                        }
                    }
                    break
                }
            }
        }
    }

    private fun smoothScrollToPosition(position: Int) {
        val smoothScroller: SmoothScroller =
            object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
        smoothScroller.targetPosition = position
        fragment_backup_overview_list.layoutManager?.startSmoothScroll(smoothScroller)
    }

    override fun onBackPressed() {
        val currentMode = viewModel.getCurrentMode()
        val activity = activity

        if(activity != null && activity is DisplayMenuItemActivity) {
            when {
                Mode.SEARCH.isActiveIn(currentMode) -> {
                    activity.pressBack()
                }
                Mode.DELETE.isActiveIn(currentMode) -> {
                    onDisableDeleteMode()
                }
                else -> {
                    activity.finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentDeleteCount", currentDeleteCount)
        outState.putString("oldMode", oldMode.name)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_manage_lists, menu)

        toolbarDeleteIcon = menu.findItem(R.id.action_delete)
        searchIcon = menu.findItem(R.id.action_search)
        selectAllIcon = menu.findItem(R.id.action_select_all)


        if(Mode.DELETE.isActiveIn(viewModel.getCurrentMode())) {
            onEnableDeleteMode()
        } else {
            onDisableDeleteMode()
        }

        if(!predefinedFilter.isNullOrEmpty()) {
            searchIcon?.isVisible = false
            return
        }

        val searchView = searchIcon?.actionView as SearchView?
        searchView?.setOnQueryTextListener(this)

        searchIcon?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (searchIcon!!.isActionViewExpanded) {
                    viewModel.disableMode(Mode.SEARCH)
                    viewModel.setFilterText("")
                }
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.enableMode(Mode.SEARCH)
                return true
            }
        })

        if(Mode.SEARCH.isActiveIn(viewModel.getCurrentMode())) {
            searchIcon?.expandActionView()
        } else {
            searchIcon?.collapseActionView()
        }

        viewModel.filterLiveData.observe(viewLifecycleOwner) { text ->
            val searchView = (searchIcon?.actionView as SearchView?)
            searchView?.setQuery(text, false)
            searchView?.requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        searchIcon?.collapseActionView()
        onDisableDeleteMode()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                if (Mode.DELETE.isActiveIn(viewModel.getCurrentMode())) {
                    onDisableDeleteMode()
                } else {
                    return false
                }
            }
            R.id.action_delete -> onEnableDeleteMode()
            R.id.action_search -> {
                /* nothing to do here */
                //Toast.makeText(requireContext(), "action_search", Toast.LENGTH_SHORT).show()
            }
            R.id.action_select_all -> {
                if (currentDeleteCount > 0) {
                    adapter.deselectAll()
                } else if (currentDeleteCount == 0) {
                    adapter.selectAll()
                }
            }
            R.id.action_add -> {
                showImportStartDialog()
            }
            R.id.action_sort -> {
                // viewModel.insertTestData()
                // TODO: to implement sorting - just swap the comparator :)
            }
            else -> return false
        }
        return true
    }

    private fun showImportStartDialog() {
        if(PreferenceManager.getDefaultSharedPreferences(requireActivity()).getBoolean(DIALOG_SKIP_IMPORT_START, false)) {
            startImport()
        } else {
            AlertDialog.Builder(requireActivity()).apply {
                setTitle(R.string.dialog_data_export_start_title)
                setMessage(R.string.dialog_data_export_start_message)
                val view = requireActivity().layoutInflater.inflate(R.layout.dialog_checkbox, null)
                setView(view)
                setPositiveButton(R.string.dialog_data_export_start_confirm) { d, _ ->
                    val checkBox = view.findViewById<CheckBox>(R.id.dialog_checkbox)

                    if (checkBox.isChecked) {
                        PreferenceManager.getDefaultSharedPreferences(requireActivity()).edit()
                            .putBoolean(DIALOG_SKIP_IMPORT_START, true).apply()
                    }

                    startImport()
                    d.dismiss()
                }
                setNegativeButton(R.string.dialog_data_export_start_cancel, null)
            }.create().show()
        }
    }

    private fun startImport() {
        Intent(requireActivity(), ImportBackupActivity::class.java).apply {
            action = ImportBackupActivity.ACTION_OPEN_FILE
            startActivity(this)
        }
    }

    override fun onSelectionCountChanged(count: Int) {
        currentDeleteCount = count
        when(count) {
            0 -> selectAllIcon?.setIcon(R.drawable.ic_check_box_outline_blank_24)
            adapter.completeData.size -> selectAllIcon?.setIcon(R.drawable.ic_check_box_24)
            else -> selectAllIcon?.setIcon(R.drawable.ic_indeterminate_check_box_24)
        }
    }

    override fun onEnableMode(mode: Mode) {
        if()
        viewModel.enableMode(Mode.DELETE)

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24)

        adapter.enableDeleteMode()

        toolbarDeleteIcon?.isVisible = false
        selectAllIcon?.isVisible = true

        fab.show()
    }

    override fun onItemClick(
        id: Long,
        backupData: BackupDataStorageRepository.BackupData,
        view: View
    ) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_popup_backup, popup.menu)
        popup.gravity = Gravity.END
        popup.setOnMenuItemClickListener { item ->
            handlePopupMenuClick(id, backupData, item.itemId)
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun handlePopupMenuClick(
        id: Long,
        backupData: BackupDataStorageRepository.BackupData,
        itemId: Int
    ) {
        when(itemId) {
            R.id.menu_restore -> {
                val builder = AlertDialog.Builder(requireContext())
                builder.setPositiveButton(R.string.restore) { _, _ ->
                    viewModel.restoreBackup(backupData)
                }
                builder.setNegativeButton(R.string.cancel, null)
                builder.setMessage(R.string.dialog_restore_confirmation_message)
                builder.setTitle(R.string.dialog_restore_confirmation_title)
                builder.create().show()
            }
            R.id.menu_inspect -> {
                Intent(requireActivity(), DataInspectionActivity::class.java).let {
                    it.putExtra(DataInspectionActivity.EXTRA_DATA_ID, id)
                    startActivity(it)
                }
            }
            R.id.menu_export -> {
                Intent(requireActivity(), DataInspectionActivity::class.java).let {
                    it.putExtra(DataInspectionActivity.EXTRA_DATA_ID, id)
                    it.putExtra(DataInspectionActivity.EXTRA_EXPORT_DATA, true)
                    startActivity(it)
                }
            }
            else -> {}
        }
    }

    fun onDisableDeleteMode() {
        viewModel.disableMode(Mode.DELETE)

        if(!isPortrait() && isXLargeTablet()) {
            toolbar.navigationIcon = null
        }

        toolbarDeleteIcon?.isVisible = true
        selectAllIcon?.isVisible = false

        fab?.hide()
        adapter.disableDeleteMode()
    }

    @SuppressLint("PrivateResource")
    private fun animateSearchToolbar(
        numberOfMenuIcon: Int,
        containsOverflow: Boolean,
        show: Boolean
    ) : Animator {
        //appBar.setBackgroundColor(ContextCompat.getColor(requireContext(), viewModel.getCurrentMode().color))

        if (show) {
            toolbar.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    viewModel.getCurrentMode().color
                )
            )
        }

        val width: Int = toolbar.width -
                (if (containsOverflow) resources.getDimensionPixelSize(R.dimen.abc_action_button_min_width_overflow_material) else 0) -
                resources.getDimensionPixelSize(R.dimen.abc_action_button_min_width_material) * numberOfMenuIcon / 2

        val createCircularReveal = ViewAnimationUtils.createCircularReveal(
            toolbar,
            if (isRtl()) toolbar.width - width else width,
            toolbar.height / 2,
            if (show) 0.0f else width.toFloat(),
            if (show) width.toFloat() else 0.0f
        )

        createCircularReveal.duration = 250

        if(!show) {
            createCircularReveal.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    activity?.runOnUiThread {
                        toolbar.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                viewModel.getCurrentMode().color
                            )
                        )
                        searchIcon?.isVisible = true
                    }
                }
            })
        }

        return createCircularReveal
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        viewModel.setFilterText(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setFilterText(newText)
        return true
    }

}