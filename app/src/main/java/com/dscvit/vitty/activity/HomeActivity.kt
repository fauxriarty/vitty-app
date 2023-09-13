package com.dscvit.vitty.activity

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.dscvit.vitty.R
import com.dscvit.vitty.databinding.ActivityHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : FragmentActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)


        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.allRequestFragment || destination.id == R.id.friendFragment || destination.id == R.id.searchFragment || destination.id == R.id.navigation_requests) {

                binding.navView.visibility = View.GONE
            } else {

                binding.navView.visibility = View.VISIBLE
            }
        }
    }


}
