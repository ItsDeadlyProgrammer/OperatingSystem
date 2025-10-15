package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Home() {

    var currentScreen by remember { mutableStateOf("main") }

    when (currentScreen) {
        "main" -> OSStudyMainScreen(
            onNavigateToScheduling = { currentScreen = "scheduling" },
            onNavigateToDeadlock = { currentScreen = "deadlock" },
            onNavigateToMemory = { currentScreen = "memory" }
        )
        "scheduling" -> ProcessSchedulingScreen(
            onBack = { currentScreen = "main" }
        )
        "deadlock" -> DeadlockScreen(
            onBack = { currentScreen = "main" }
        )
        "memory" -> MemoryManagementScreen(
            onBack = { currentScreen = "main" }
        )
    }
}

@Composable
fun OSStudyMainScreen(
    onNavigateToScheduling: () -> Unit,
    onNavigateToDeadlock: () -> Unit,
    onNavigateToMemory: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

    ) {

        Text(
            text = "Let's Study OS",
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            lineHeight = 60.sp,
            modifier = Modifier.padding(bottom = 40.dp)
        )


        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopicButton(
                title = "Process Scheduling",
                onClick = onNavigateToScheduling,
                backgroundColor = Color(0xFF1E88E5) // Blue color
            )

            TopicButton(
                title = "Deadlock",
                onClick = onNavigateToDeadlock,
                backgroundColor = Color(0xFFD32F2F) // Red color
            )

            TopicButton(
                title = "Memory Management",
                onClick = onNavigateToMemory,
                backgroundColor = Color(0xFF4CAF50) // Green color
            )


        }
    }
}


@Composable
private fun TopicButton(
    title: String,
    onClick: () -> Unit,
    backgroundColor: Color
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessSchedulingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Scheduling") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon("back","To Previous Screen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ProcessSchedulingScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlockScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deadlock") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                       Icon("back","To Previous Screen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Deadlock()
        }
    }
}
