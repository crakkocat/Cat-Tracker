package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.CatRepository
import com.example.ui.CatTrackerViewModel
import com.example.ui.DashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local persistence (Room Database)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CatRepository(database.catDao())
        
        // Retrieve or build the ViewModel
        val viewModel: CatTrackerViewModel by viewModels {
            CatTrackerViewModel.provideFactory(repository)
        }

        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
