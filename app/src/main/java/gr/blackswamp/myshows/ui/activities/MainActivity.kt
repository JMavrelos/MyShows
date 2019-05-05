package gr.blackswamp.myshows.ui.activities


import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.fragments.ListFragment
import gr.blackswamp.myshows.ui.fragments.DisplayFragment
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val SHOW_DETAIL = "show_detail"
    }

    private lateinit var viewModel: MainViewModel
    //region bindings
    private lateinit var base: View
    private var tabletMode: Boolean = false
    private lateinit var loading: View
    private lateinit var inputBlock: View
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this, ViewModelProvider.AndroidViewModelFactory(application)).get(MainViewModel::class.java)
        setUpBindings()
        setUpObservers()
        if (savedInstanceState == null) {
            if (tabletMode) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.list, ListFragment.newInstance(), ListFragment.TAG)
                    .commit()
            } else {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.content, ListFragment.newInstance(), ListFragment.TAG)
                    .commit()
            }
        }
    }

    private fun setUpBindings() {
        base = findViewById(R.id.base)
        tabletMode = (findViewById<View>(R.id.content) == null)
        loading = findViewById(R.id.loading)
        inputBlock = findViewById(R.id.input_block)
    }

    private fun setUpObservers() {
        viewModel.error.observe(this, Observer { showError(it) })
        viewModel.loading.observe(this, Observer { showLoading(it) })
        viewModel.show.observe(this, Observer { gotoShow(it) })
    }

    private fun gotoShow(it: ShowDetailVO?) {
        val fragment = supportFragmentManager.findFragmentByTag(DisplayFragment.TAG)
        if (it == null && fragment != null) {
            supportFragmentManager.popBackStack(SHOW_DETAIL, POP_BACK_STACK_INCLUSIVE)
        } else if (it != null && tabletMode) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.show, DisplayFragment.newInstance(), DisplayFragment.TAG)
                .addToBackStack(SHOW_DETAIL)
                .commit()
        } else if (it != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, DisplayFragment.newInstance(), DisplayFragment.TAG)
                .addToBackStack(SHOW_DETAIL)
                .commit()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.findFragmentByTag(DisplayFragment.TAG) != null) {
            viewModel.exitDisplay()
        } else {
            finish()
        }
    }

    private fun showError(message: String?) {
        if (message != null) {
            val snackbar = Snackbar.make(base, message, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction(R.string.dismiss) {
                snackbar.dismiss()
            }.show()
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loading.visibility = VISIBLE
            inputBlock.visibility = VISIBLE
        } else {
            loading.visibility = GONE
            inputBlock.visibility = GONE
        }
    }
}
