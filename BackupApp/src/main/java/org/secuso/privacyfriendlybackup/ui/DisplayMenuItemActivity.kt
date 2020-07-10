package org.secuso.privacyfriendlybackup.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.secuso.privacyfriendlybackup.R

/**
 * An activity representing a single Item detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [MainActivity].
 */
class DisplayMenuItemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_menu_item)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don"t need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = try {
                    MainActivity.MenuItem.valueOf(intent.getStringExtra(MainActivity.SELECTED_MENU_ITEM)!!).fragment.newInstance()
                } catch (e: IllegalArgumentException) {
                    null
                } catch (e: NullPointerException) {
                    null
                }

            if(fragment != null) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.container, fragment, intent.getStringExtra(MainActivity.SELECTED_MENU_ITEM)!!)
                    .commit()
            } else {
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {

                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back

                navigateUpTo(Intent(this, MainActivity::class.java))

                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val fragment: Fragment? = supportFragmentManager.findFragmentByTag(MainActivity.MenuItem.MENU_MAIN_ENCRYPTION.name)
        if(fragment != null && fragment.isVisible) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }
}