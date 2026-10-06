package com.levinhocall.gitagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levinhocall.gitagent.ui.theme.AurixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AurixTheme {
                val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(applicationContext))
                ChatScreen(model = viewModel)
            }
        }
    }
}
