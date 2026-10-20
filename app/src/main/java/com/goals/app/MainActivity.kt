package com.goals.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goals.app.ui.GoalsAppRoot
import com.goals.app.ui.GoalsViewModel
import com.goals.app.ui.theme.GoalsTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val vm: GoalsViewModel by viewModels()
    private val openGoal = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            GoalsTheme(state.settings.theme, state.settings.density, state.settings.dynamicColor) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    GoalsAppRoot(vm, state, BuildConfig.VERSION_NAME, openGoal.receiveAsFlow())
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(EXTRA_GOAL_ID)?.let { openGoal.trySend(it) }
    }

    companion object {
        const val EXTRA_GOAL_ID = "goalId"
    }
}
