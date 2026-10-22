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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        enableEdgeToEdge()
        editDesign = intent?.getStringExtra(EXTRA_EDIT_DESIGN)
        setContent {
            SimonTheme {
                SimonRoot(editDesign) { editDesign = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_EDIT_DESIGN)?.let { editDesign = it }
    }

    companion object {
        const val EXTRA_EDIT_DESIGN = "edit_design"
    }
}
