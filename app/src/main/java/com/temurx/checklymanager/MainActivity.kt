package com.temurx.checklymanager

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.FirebaseApp
import com.temurx.checklymanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostFragment.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Hide bottom nav on login screen
            binding.bottomNav.visibility =
                if (destination.id == R.id.loginScreen) View.GONE else View.VISIBLE

            // Sync top-level destination selection
            when (destination.id) {
                R.id.dashboardFragment ->
                    binding.bottomNav.menu.findItem(R.id.dashboardFragment).isChecked = true
                R.id.homeFragment,
                R.id.staffDetailFragment,
                R.id.addTaskFragment,
                R.id.taskDetailFragment,
                R.id.addStaffFragment ->
                    binding.bottomNav.menu.findItem(R.id.homeFragment).isChecked = true
                R.id.profileFragment ->
                    binding.bottomNav.menu.findItem(R.id.profileFragment).isChecked = true
            }
        }
    }
}
