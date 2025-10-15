package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.FragmentationStats
import data.MemoryBlock
import viewmodel.MemoryManagementStateHolder
import viewmodel.rememberMemoryManagementStateHolder
import kotlin.math.max
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryManagementScreen(onBack: () -> Unit) {
    val stateHolder = rememberMemoryManagementStateHolder()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Management") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Panel: Controls and Stats (now takes full width)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Allocation Controls ---
                AllocationControlsCard(stateHolder)

                Spacer(Modifier.height(16.dp))

                // --- Fragmentation Stats ---
                FragmentationStatsCard(stateHolder.fragmentationStats.value)

                Spacer(Modifier.height(16.dp))

                // --- Deallocation/Processes List ---
                ProcessListCard(
                    blocks = stateHolder.memoryBlocks,
                    onDeallocate = stateHolder::deallocateProcess
                )
            }

            // Bottom Panel: Visualization
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Fixed height for visualization
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                MemoryVisualizer(stateHolder.memoryBlocks)
            }
        }
    }
}

@Composable
fun AllocationControlsCard(stateHolder: MemoryManagementStateHolder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Allocation Controls", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

            AlgorithmSelector(
                selected = stateHolder.allocationAlgorithm,
                options = stateHolder.algorithms,
                onSelect = stateHolder::setAlgorithm
            )
            Spacer(Modifier.height(8.dp))


            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = stateHolder.requestSizeInput,
                    onValueChange = stateHolder::setRequestSize,
                    label = { Text("Request Size (KB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = stateHolder::allocateProcess,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Allocate")
                }
            }
            Spacer(Modifier.height(12.dp))

            // NEW RESET BUTTON
            Button(
                onClick = stateHolder::resetMemory,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Memory to Partitioned State")
            }

            Spacer(Modifier.height(8.dp))
            Text(stateHolder.allocationMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmSelector(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Allocation Algorithm") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FragmentationStatsCard(stats: FragmentationStats) {
    // Colors are safely accessed inside this @Composable function
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Fragmentation Analysis", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.height(4.dp))
            StatRow(label = "Total Free Space", value = "${stats.totalFree} KB", color = secondaryColor)
            StatRow(label = "Internal Fragmentation", value = "${stats.internal} KB", color = errorColor)
            StatRow(label = "External Fragmentation", value = "${stats.external} KB", color = errorColor)
            Text("External fragmentation is the unusable free space scattered across non-contiguous blocks.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = color))
    }
}

@Composable
fun ProcessListCard(blocks: List<MemoryBlock>, onDeallocate: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Processes in Memory", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(Modifier.heightIn(max = 200.dp)) {
                val processes = blocks.filter { !it.isFree }
                if (processes.isEmpty()) {
                    item { Text("No processes allocated.", color = Color.Gray) }
                } else {
                    items(processes) { block ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(block.id, fontWeight = FontWeight.Bold)
                                Text("${block.size} KB (Start: ${block.start} KB)", style = MaterialTheme.typography.bodySmall)
                                if (block.internalFragmentation > 0) {
                                    // Error color is safely accessed here
                                    Text("Frag: ${block.internalFragmentation} KB",
                                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Button(
                                onClick = { onDeallocate(block.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Red deallocate button
                            ) {
                                Text("Free")
                            }
                        }
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryVisualizer(memoryBlocks: List<MemoryBlock>) {
    val textMeasurer = rememberTextMeasurer()
    remember { Random(42) }

    // Colors are safely accessed inside this @Composable function
    val freeBlockColor = MaterialTheme.colorScheme.surfaceVariant
    val freeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val timelineColor = MaterialTheme.colorScheme.onSurface
    val fragmentationColor = MaterialTheme.colorScheme.error

    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        val totalMemory = 256

        if (totalMemory == 0) return@Canvas

        val scaleFactor = height / totalMemory.toFloat()
        var currentY = 0f

        memoryBlocks.forEach { block ->
            val blockHeight = block.size.toFloat() * scaleFactor
            val color = if (block.isFree) {
                freeBlockColor
            } else {
                // Generate a consistent, theme-appropriate color based on block ID hash
                val hash = block.id.hashCode()
                val r = ((hash % 100) / 100f).coerceIn(0.4f, 0.9f)
                val g = (((hash / 2) % 100) / 100f).coerceIn(0.4f, 0.9f)
                val b = (((hash / 3) % 100) / 100f).coerceIn(0.4f, 0.9f)
                Color(r, g, b).copy(alpha = 0.8f) // Use a generated color for processes
            }

            val contentTextColor = if (block.isFree) freeTextColor else Color.Black

            drawRect(
                color = color,
                topLeft = Offset(0f, currentY),
                size = Size(width, blockHeight)
            )

            drawRect(
                color = timelineColor.copy(alpha = 0.6f),
                topLeft = Offset(0f, currentY),
                size = Size(width, blockHeight),
                style = Stroke(width = 1f)
            )

            val label = if (block.isFree) {
                "FREE (${block.size} KB)"
            } else {
                "${block.id}: ${block.size} KB"
            }

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = TextStyle(fontSize = 14.sp, color = contentTextColor, fontWeight = FontWeight.Medium),
                topLeft = Offset(10f, currentY + max(5f, blockHeight / 2 - 12f))
            )

            // Timeline text (Start address)
            drawText(
                textMeasurer = textMeasurer,
                text = "${block.start} KB",
                style = TextStyle(fontSize = 10.sp, color = timelineColor, fontWeight = FontWeight.Bold),
                topLeft = Offset(width - 50f, currentY + 5f)
            )

            // Timeline text (End address)
            if (currentY + blockHeight > 5f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${block.start + block.size} KB",
                    style = TextStyle(fontSize = 10.sp, color = timelineColor, fontWeight = FontWeight.Bold),
                    topLeft = Offset(width - 50f, currentY + blockHeight - 15f)
                )
            }

            // Internal Fragmentation Visualization
            if (!block.isFree && block.internalFragmentation > 0) {
                val fragHeight = block.internalFragmentation.toFloat() * scaleFactor
                drawRect(
                    color = fragmentationColor.copy(alpha = 0.6f), // Error color for Fragmentation
                    topLeft = Offset(0f, currentY + blockHeight - fragHeight),
                    size = Size(width, fragHeight)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "Internal Frag (${block.internalFragmentation} KB)",
                    style = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold),
                    topLeft = Offset(10f, currentY + blockHeight - fragHeight + 5f)
                )
            }

            currentY += blockHeight
        }
    }
}
