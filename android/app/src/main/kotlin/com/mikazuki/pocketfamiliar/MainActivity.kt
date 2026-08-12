package com.mikazuki.pocketfamiliar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.mikazuki.pocketfamiliar.ui.screens.HomeScreen
import com.mikazuki.pocketfamiliar.ui.theme.PocketFamiliarTheme
import com.mikazuki.pocketfamiliar.viewmodel.HomeViewModel
import com.mikazuki.pocketfamiliar.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this, HomeViewModelFactory(applicationContext))
            .get(HomeViewModel::class.java)

        setContent {
            PocketFamiliarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the overlay permission state every time the user returns to the app.
        // This handles the case where the user just came back from the system settings screen.
        viewModel.refreshPermissionState()
    }
}
