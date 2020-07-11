package org.secuso.privacyfriendlybackup.ui.backup

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_backup_overview.*
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.ui.MainActivity
import org.secuso.privacyfriendlybackup.ui.MainActivity.Companion.SELECTED_MENU_ITEM
import org.secuso.privacyfriendlybackup.ui.common.Mode

class BackupOverviewFragment : Fragment(),
    FilterableBackupAdapter.ManageListAdapterCallback,
    SearchView.OnQueryTextListener {

    companion object {
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

    // ui
    private lateinit var toolbar : Toolbar
    private lateinit var appBar : AppBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        savedInstanceState ?: return

        val savedOldMode = savedInstanceState.getString("oldMode")
        oldMode = if(savedOldMode != null) Mode.valueOf(savedOldMode) else oldMode

        currentDeleteCount = savedInstanceState.getInt("currentDeleteCount")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_overview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this).get(BackupOverviewViewModel::class.java)
        adapter =
            FilterableBackupAdapter(
                requireContext(),
                this
            )

        toolbar = requireActivity().findViewById(R.id.toolbar)
        appBar = requireActivity().findViewById(R.id.app_bar)

        fragment_backup_overview_list.adapter = adapter
        fragment_backup_overview_list.layoutManager = when {
            isXLargeTablet() -> {
                GridLayoutManager(context,if (isPortrait()) 2 else 3,GridLayoutManager.VERTICAL,false)
            }
            isLargeTablet() -> {
                GridLayoutManager(context,if (isPortrait()) 1 else 2,GridLayoutManager.VERTICAL,false)
            }
            else -> {
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            }
        }

        fab.setOnClickListener {
            if(Mode.DELETE.isActiveIn(viewModel.getCurrentMode())) {

                val builder = AlertDialog.Builder(requireContext()).apply {
                    setTitle(R.string.dialog_delete_confimation_title)
                    setMessage(R.string.dialog_delete_confimation_message)
                    setNegativeButton(R.string.dialog_delete_confimation_negative, null)
                    setPositiveButton(R.string.dialog_delete_confimation_positive) { dialog, _ ->
                        viewModel.deleteData(adapter.getDeleteList())
                        dialog.dismiss()
                        onDisableDeleteMode()
                    }
                }
                builder.show()

            }
        }

        viewModel.backupLiveData.observe(viewLifecycleOwner) { data ->
            adapter.setData(data)
        }

        viewModel.filteredBackupLiveData.observe(viewLifecycleOwner) { data ->
            adapter.setFilteredData(data)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentDeleteCount", currentDeleteCount)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_manage_lists, menu)

        toolbarDeleteIcon = menu.findItem(R.id.action_delete)
        searchIcon = menu.findItem(R.id.action_search)
        selectAllIcon = menu.findItem(R.id.action_select_all)

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

        if(Mode.DELETE.isActiveIn(viewModel.getCurrentMode())) {
            onEnableDeleteMode()
        } else {
            onDisableDeleteMode()
        }

        viewModel.filterLiveData.observe(viewLifecycleOwner) { text ->
            val searchView = (searchIcon?.actionView as SearchView?)
            searchView?.setQuery(text, false)
            searchView?.requestFocus()
        }
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
                Toast.makeText(requireContext(), "action_search", Toast.LENGTH_SHORT).show()
            }
            R.id.action_select_all -> {
                if (currentDeleteCount > 0) {
                    adapter.deselectAll()
                } else if (currentDeleteCount == 0) {
                    adapter.selectAll()
                }
            }
            R.id.action_sort -> {
                viewModel.insertTestData()
                // TODO: to implement sorting - just swap the comparator :)
            }
            else -> return false
        }
        return true
    }

    override fun onDeleteCountChanged(count: Int) {
        currentDeleteCount = count
        when(count) {
            0 -> selectAllIcon?.setIcon(R.drawable.ic_check_box_outline_blank_24)
            adapter.completeData.size -> selectAllIcon?.setIcon(R.drawable.ic_check_box_24)
            else -> selectAllIcon?.setIcon(R.drawable.ic_indeterminate_check_box_24)
        }
    }

    override fun onEnableDeleteMode() {
        viewModel.enableMode(Mode.DELETE)

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24)

        adapter.enableDeleteMode()

        toolbarDeleteIcon?.isVisible = false
        selectAllIcon?.isVisible = true

        fab.show()
    }

    fun onDisableDeleteMode() {
        viewModel.disableMode(Mode.DELETE)

        if(!isPortrait() && isXLargeTablet()) {
            toolbar.navigationIcon = null
        }

        toolbarDeleteIcon?.isVisible = true
        selectAllIcon?.isVisible = false

        fab.hide()
        adapter.disableDeleteMode()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val activity = requireActivity()

        if(isLargeTablet()) {
            if(activity is BackupOverviewActivity && !isPortrait()) {
                activity.finish()
            } else if(activity is MainActivity && isPortrait()){
                val intent = activity.intent.apply {
                    putExtra(SELECTED_MENU_ITEM, MainActivity.MenuItem.MENU_MAIN_BACKUP_OVERVIEW.ordinal)
                }
                activity.finish()
                activity.startActivity(intent)
            }
        }
    }

    override fun onItemClick(id: Long) {
        TODO("Not yet implemented")
    }

    @SuppressLint("PrivateResource")
    private fun animateSearchToolbar(numberOfMenuIcon: Int, containsOverflow: Boolean, show: Boolean) : Animator {
        //appBar.setBackgroundColor(ContextCompat.getColor(requireContext(), viewModel.getCurrentMode().color))

        if (show) {
            toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), viewModel.getCurrentMode().color))
        }

        val width: Int = toolbar.width -
                (if (containsOverflow) resources.getDimensionPixelSize(R.dimen.abc_action_button_min_width_overflow_material) else 0) -
                resources.getDimensionPixelSize(R.dimen.abc_action_button_min_width_material) * numberOfMenuIcon / 2

        val createCircularReveal = ViewAnimationUtils.createCircularReveal(
            toolbar,
            if (isRtl()) toolbar.width - width else width,
            toolbar.height / 2,
            if(show) 0.0f else width.toFloat(),
            if(show) width.toFloat() else 0.0f
        )

        createCircularReveal.duration = 250

        if(!show) {
            createCircularReveal.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    activity?.runOnUiThread {
                        toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), viewModel.getCurrentMode().color))
                        searchIcon?.isVisible = true
                    }
                }
            })
        }

        return createCircularReveal
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

    override fun onQueryTextSubmit(query: String?): Boolean {
        viewModel.setFilterText(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setFilterText(newText)
        return true
    }

}