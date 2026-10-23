package com.migux97.simon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.migux97.simon.data.Repository
import com.migux97.simon.ui.SimonRoot
import com.migux97.simon.ui.theme.SimonTheme

class MainActivity : ComponentActivity() {

    private var editDesign by mutableStateOf<String?>(null)
    private var route by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        enableEdgeToEdge()
        editDesign = intent?.getStringExtra(EXTRA_EDIT_DESIGN)
        route = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            SimonTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.migux97.simon.ui.components.LocalAnimationsActive provides com.migux97.simon.ui.components.rememberAnimationsAllowed()
                ) {
                    SimonRoot(editDesign, route) { editDesign = null; route = null }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_EDIT_DESIGN)?.let { editDesign = it }
        intent.getStringExtra(EXTRA_ROUTE)?.let { route = it }
    }

    companion object {
        const val EXTRA_EDIT_DESIGN = "edit_design"
        const val EXTRA_ROUTE = "route"
    }
}
