package ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GanttSlice
import data.Process
import data.SimulationMetrics
import viewmodel.SchedulingViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessSchedulingScreen(

    viewModel: SchedulingViewModel = remember { SchedulingViewModel() }
) {

    val processes = viewModel.processes
    val ganttChart = viewModel.ganttChart
    val metrics = viewModel.metrics
    val selectedAlgorithm = viewModel.selectedAlgorithm
    val quantum = viewModel.quantum

    val scrollState = rememberScrollState()

    MaterialTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(5.dp,16.dp,5.dp,5.dp)
            .verticalScroll(scrollState) // Apply vertical scroll
        ) {
            Text(
                "OS Scheduling Simulator",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AlgorithmSelectionPanel(
                selectedAlgorithm = selectedAlgorithm,
                algorithms = viewModel.algorithms,
                onAlgorithmChange = viewModel::setAlgorithm,
                quantum = quantum,
                onQuantumChange = viewModel::updateQuantum,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            ProcessInputPanel(
                processes = processes,
                onProcessesChange = viewModel::updateProcesses,
                onAddProcess = viewModel::addProcess,
                onRemoveProcess = viewModel::removeProcess,
                currentAlgorithm = selectedAlgorithm
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Simulation Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnimatedVisibility(ganttChart.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    MetricDisplay(metrics)
                    Spacer(Modifier.height(16.dp))
                    GanttChartVisualization(ganttChart)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmSelectionPanel(
    selectedAlgorithm: String,
    algorithms: List<String>,
    onAlgorithmChange: (String) -> Unit,
    quantum: Int,
    onQuantumChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedAlgorithm,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scheduling Algorithm") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        algorithms.forEach { algorithm ->
                            DropdownMenuItem(
                                text = { Text(algorithm) },
                                onClick = {
                                    onAlgorithmChange(algorithm)
                                    expanded = false
                                }
                            )
                        }
                    }
                }


                if (selectedAlgorithm == "Round Robin") {
                    OutlinedTextField(
                        value = quantum.toString(),
                        onValueChange = { newValue ->
                            // Only allow positive integers
                            val newQuantum = newValue.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            onQuantumChange(newQuantum)
                        },
                        label = { Text("Time Quantum (q)") },
                        modifier = Modifier.width(150.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                } else {
                    Spacer(Modifier.width(150.dp))
                }
            }
        }
    }
}

@Composable
fun ProcessInputPanel(
    processes: List<Process>,
    onProcessesChange: (List<Process>) -> Unit,
    onAddProcess: () -> Unit,
    onRemoveProcess: (String) -> Unit,
    currentAlgorithm: String
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp, 10.dp, 10.dp, 10.dp)) {
            Text(
                "Process Parameters",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val showPriority = currentAlgorithm.contains("Priority")

            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID", Modifier.width(40.dp), style = MaterialTheme.typography.labelMedium)
                Text("Arrival (A)", Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium)
                Text("Burst (B)", Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium)

                if (showPriority) {
                    Text("Priority (P)", Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(48.dp))
            }


            LazyColumn(Modifier.heightIn(max = 200.dp)) {
                itemsIndexed(processes) { index, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.id, Modifier.width(40.dp))

                        // Helper function to safely parse and update an Int field
                        fun updateProcess(p: Process, processes: List<Process>, onProcessesChange: (List<Process>) -> Unit, update: (Process) -> Process) {
                            val updatedList = processes.toMutableList()
                            val currentProcess = updatedList.find { it.id == p.id } ?: return

                            val safeIndex = updatedList.indexOf(currentProcess)
                            if (safeIndex != -1) {
                                updatedList[safeIndex] = update(currentProcess)
                                onProcessesChange(updatedList)
                            }
                        }

                        var arrivalTextState by remember(p.arrivalTime) {
                            mutableStateOf(TextFieldValue(p.arrivalTime.toString()))
                        }

                        OutlinedTextField(
                            value = arrivalTextState.text,
                            onValueChange = { newValue ->
                                arrivalTextState = arrivalTextState.copy(text = newValue)
                                updateProcess(p, processes, onProcessesChange) { process ->
                                    process.copy(arrivalTime = newValue.toIntOrNull() ?: 0)
                                }
                            },
                            label = { Text("Arrival") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        arrivalTextState = arrivalTextState.copy(
                                            selection = TextRange(0, arrivalTextState.text.length)
                                        )
                                    }
                                },
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        var burstTextState by remember(p.burstTime) {
                            mutableStateOf(TextFieldValue(p.burstTime.toString()))
                        }
                        OutlinedTextField(
                            value = burstTextState.text,
                            onValueChange = { newValue ->
                                burstTextState = burstTextState.copy(text = newValue)
                                updateProcess(p, processes, onProcessesChange) { process ->
                                    val newBurst = newValue.toIntOrNull() ?: 1
                                    process.copy(burstTime = newBurst.coerceAtLeast(1))
                                }
                            },
                            label = { Text("Burst") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        burstTextState = burstTextState.copy(
                                            selection = TextRange(0, burstTextState.text.length)
                                        )
                                    }
                                },
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (showPriority) {
                            var priorityTextState by remember(p.priority) {
                                mutableStateOf(TextFieldValue(p.priority.toString()))
                            }

                            OutlinedTextField(
                                value = priorityTextState.text,
                                onValueChange = { newValue ->
                                    priorityTextState = priorityTextState.copy(text = newValue)
                                    updateProcess(p, processes, onProcessesChange) { process ->
                                        process.copy(priority = newValue.toIntOrNull() ?: 0)
                                    }
                                },
                                label = { Text("Priority") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            priorityTextState = priorityTextState.copy(
                                                selection = TextRange(0, priorityTextState.text.length)
                                            )
                                        }
                                    },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        IconButton(onClick = { onRemoveProcess(p.id) }) {
                            Icon("close", "Remove")
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Start) {
                Button(
                    onClick = onAddProcess,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon("add", "Add")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Process")
                }
            }
        }
    }
}

@Composable
fun MetricDisplay(metrics: SimulationMetrics) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        Card(Modifier.weight(1f).padding(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Avg Waiting Time",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "${metrics.averageWaitingTime.toFormattedString()} units", // Using fixed extension (FIX)
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.tertiary)
                )
            }
        }
        Card(Modifier.weight(1f).padding(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Avg Turnaround Time",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${metrics.averageTurnaroundTime.toFormattedString()} units",
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun GanttChartVisualization(ganttChart: List<GanttSlice>) {
    val textMeasurer = rememberTextMeasurer()

    val totalTime = remember(ganttChart) {
        ganttChart.lastOrNull()?.end ?: 1
    }

    val processColorMap = remember {
        mapOf(
            "P1" to Color(0xFFE57373), // Red
            "P2" to Color(0xFF4DB6AC), // Teal
            "P3" to Color(0xFFBA68C8), // Purple
            "P4" to Color(0xFFFFB74D), // Orange
            "P5" to Color(0xFF64B5F6), // Blue
            "P6" to Color(0xFF81C784), // Green
            "Idle" to Color(0xFFEEEEEE) // Grey/Off-white for idle
        )
    }

    val canvasHeight = 80.dp
    val timeLabelHeight = 20.dp
    val textStyle = TextStyle(fontSize = 12.sp, color = Color.Black)

    Card(Modifier.fillMaxWidth().height(canvasHeight + timeLabelHeight + 16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
            val chartWidth = size.width
            val chartHeight = size.height - timeLabelHeight.toPx()

            val unitWidth = if (totalTime > 0) chartWidth / totalTime else chartWidth
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(0f, 0f),
                size = Size(chartWidth, chartHeight)
            )

            ganttChart.forEach { slice ->
                val startX = slice.start * unitWidth
                val endX = slice.end * unitWidth
                val sliceWidth = endX - startX

                val color = processColorMap[slice.processId] ?: Color.Gray

                drawRect(
                    color = color,
                    topLeft = Offset(startX, 0f),
                    size = Size(sliceWidth, chartHeight)
                )

                if (sliceWidth > 30f) {
                    val textLayoutResult = textMeasurer.measure(slice.processId, textStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            startX + (sliceWidth - textLayoutResult.size.width) / 2f,
                            (chartHeight - textLayoutResult.size.height) / 2f
                        ),
                        color = if (slice.processId == "Idle") Color.Black else Color.White
                    )
                }
            }

            drawTimeMarkers(
                totalTime = totalTime,
                unitWidth = unitWidth,
                chartHeight = chartHeight,
                textMeasurer = textMeasurer,
                textStyle = textStyle
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTimeMarkers(
    totalTime: Int,
    unitWidth: Float,
    chartHeight: Float,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
) {
    drawLine(
        color = Color.Black,
        start = Offset(0f, 0f),
        end = Offset(0f, chartHeight + 5.dp.toPx()),
        strokeWidth = 2f
    )
    drawText(
        textLayoutResult = textMeasurer.measure("0", textStyle),
        topLeft = Offset(0f, chartHeight + 8.dp.toPx())
    )

    for (time in 1..totalTime) {
        val x = time * unitWidth

        // Draw line marker
//        drawLine(
//            color = Color.Black,
//            start = Offset(x, 0f),
//            end = Offset(x, chartHeight + 5.dp.toPx()),
//            strokeWidth = 2f
//        )

        val text = time.toString()
        val textLayoutResult = textMeasurer.measure(text, textStyle)

        val textX = if (time == totalTime) {
            x - textLayoutResult.size.width
        } else {
            x - textLayoutResult.size.width / 2f
        }

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(textX.coerceAtLeast(0f), chartHeight + 8.dp.toPx()),
        )
    }


}


@Composable
fun Icon(name: String, contentDescription: String? = null) {
    val vp = loadVectorPainter(name)
    vp.Draw(contentDescription)
}
