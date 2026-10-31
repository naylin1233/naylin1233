package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthManager
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.RecapMMTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check contract with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            RecapMMTheme(darkTheme = viewModel.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: MainViewModel) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val coroutineScope = rememberCoroutineScope()

    // Listen to Firebase Auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        HomeScreen(
            viewModel = viewModel,
            onSignOut = {
                currentUser = null
            }
        )
    }
}
