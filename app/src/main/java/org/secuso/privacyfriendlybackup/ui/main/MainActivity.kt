package org.secuso.privacyfriendlybackup.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.widget.NestedScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.ui.application.ApplicationOverviewFragment
import org.secuso.privacyfriendlybackup.ui.backup.BackupOverviewFragment
import org.secuso.privacyfriendlybackup.ui.common.DisplayMenuItemActivity
import org.secuso.privacyfriendlybackup.ui.encryption.EncryptionSettingsFragment
import org.secuso.privacyfriendlybackup.ui.help.HelpFragment

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [DisplayMenuItemActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class MainActivity : AppCompatActivity() {

    enum class MenuItem(
        @DrawableRes _imageRes: Int,
        @StringRes _titleRes: Int,
        _fragment: Class<out Fragment>,
        _activity: Class<out Activity>
    ) {
        MENU_MAIN_BACKUP_OVERVIEW(R.drawable.ic_backup_24, R.string.menu_main_backup, BackupOverviewFragment::class.java, DisplayMenuItemActivity::class.java),
        MENU_MAIN_APPS(R.drawable.ic_apps_24, R.string.menu_main_apps, ApplicationOverviewFragment::class.java, DisplayMenuItemActivity::class.java),
        MENU_MAIN_ENCRYPTION(R.drawable.ic_encryption_24, R.string.menu_main_encryption, EncryptionSettingsFragment::class.java, DisplayMenuItemActivity::class.java),
        //MENU_MAIN_SETTINGS(R.drawable.ic_settings_24, R.string.menu_main_settings, SettingsFragment::class.java, DisplayMenuItemActivity::class.java),
        MENU_MAIN_HELP(R.drawable.ic_help_outline_24, R.string.menu_main_help, HelpFragment::class.java, DisplayMenuItemActivity::class.java),
        MENU_MAIN_ABOUT(R.drawable.ic_about_24, R.string.menu_main_about, AboutFragment::class.java, DisplayMenuItemActivity::class.java);

        val imageRes = _imageRes
        val titleRes = _titleRes
        val fragment = _fragment
        val activity = _activity
    }

    companion object {
        const val TAG = "PFA MainActivity"


        const val BACKUP_ID = "BACKUP_ID"
        const val SELECTED_MENU_ITEM = "SELECTED_MENU_ITEM"
        const val FILTER = "FILTER"
        const val TWOPANE = "TWOPANE"

        val DEFAULT_NAVIGATION_ITEM =
            MenuItem.MENU_MAIN_APPS

        val items : List<MenuItem> = listOf(
            MenuItem.MENU_MAIN_APPS,
            MenuItem.MENU_MAIN_BACKUP_OVERVIEW,
            MenuItem.MENU_MAIN_ENCRYPTION,
            //MenuItem.MENU_MAIN_SETTINGS,
            MenuItem.MENU_MAIN_HELP,
            MenuItem.MENU_MAIN_ABOUT
        )
    }

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    private var adapter: SimpleItemRecyclerViewAdapter? = null
    private var currentActiveMenuItem: MenuItem? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent()")

        navigateFromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate()")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = title

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        if (findViewById<NestedScrollView>(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        setupRecyclerView(findViewById(R.id.item_list))

        navigateFromIntent(intent)
    }

    private fun navigateFromIntent(intent : Intent?) {
        Log.d(TAG, "navigateFromIntent($intent)")
        if(intent != null && intent.extras != null && intent.extras!!.containsKey(SELECTED_MENU_ITEM)) {
            val menuItem = MenuItem.valueOf(intent.extras!!.getString(
                SELECTED_MENU_ITEM, DEFAULT_NAVIGATION_ITEM.name))
            handleNavigation(menuItem, intent.extras)
        } else if(twoPane){
            handleNavigation(DEFAULT_NAVIGATION_ITEM)
        }
    }

    fun handleNavigation(menuItem : MenuItem, extras : Bundle? = null) {
        val fragment = menuItem.fragment
        val activity = menuItem.activity


        if (twoPane) {
            highlightActiveMenuItem(menuItem)

            val f = fragment.newInstance()
            if(extras != null) {
                f.arguments = extras.apply {
                    putBoolean(TWOPANE, twoPane)
                }
            }

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.item_detail_container, f)
                .commit()
        } else {
            val intent = Intent(this, activity).apply {
                if(extras != null) {
                    putExtras(extras)
                }
                putExtra(SELECTED_MENU_ITEM, menuItem.name)
                putExtra(TWOPANE, twoPane)
            }
            startActivity(intent)
        }
    }

    private fun highlightActiveMenuItem(menuItem: MenuItem) {
        currentActiveMenuItem = menuItem
        adapter?.highlightMenuItem(currentActiveMenuItem)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        adapter = SimpleItemRecyclerViewAdapter(
            this,
            items,
            twoPane
        )
        recyclerView.adapter = adapter
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: MainActivity,
        private val values: List<MenuItem>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private var activeMenuItem : MenuItem = MenuItem.MENU_MAIN_APPS
        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as MenuItem

                parentActivity.handleNavigation(item)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_main_menu, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.imageView.setImageResource(item.imageRes)
            holder.imageView.setColorFilter(ContextCompat.getColor(parentActivity, R.color.colorAccent))
            holder.contentView.setText(item.titleRes)

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }

            val color = if(twoPane && item == activeMenuItem) {
                ContextCompat.getColor(parentActivity, R.color.lightgrey)
            } else {
                ContextCompat.getColor(parentActivity, R.color.transparent)
            }
            holder.background.setBackgroundColor(color)
        }

        override fun getItemCount() = values.size

        fun highlightMenuItem(currentActiveMenuItem: MenuItem?) {
            currentActiveMenuItem ?: return
            activeMenuItem = currentActiveMenuItem
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val background: LinearLayout = view.findViewById(R.id.background)
            val imageView: ImageView = view.findViewById(R.id.image)
            val contentView: TextView = view.findViewById(R.id.content)
        }
    }
}