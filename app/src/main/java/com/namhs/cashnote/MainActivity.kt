package com.namhs.cashnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.namhs.cashnote.presentation.CashNoteApp
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashNoteTheme {
                CashNoteApp()
            }
        }
    }
}
