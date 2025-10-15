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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Edge
import data.GraphState
import data.Node
import data.ResourceData
import data.SafetyResult
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


// --- 2. DEADLOCK ANALYSIS LOGIC (Banker's Safety Check Style) ---

/**
 * Executes a simplified safety check, similar to the core algorithm in the Banker's approach.
 * It determines if there is a safe sequence in which all processes can finish given the current
 * resource allocations and requests.
 * * FIX: Reworked the calculation of allocatedMap and availableMap to correctly handle the
 * R -> P allocation edge direction for the RAG model.
 */
fun runSafetyCheck(processes: List<String>, resources: List<ResourceData>, edges: List<Edge>): SafetyResult {
    if (processes.isEmpty()) return SafetyResult(true)

    val resourceIds = resources.map { it.id }

    // allocatedMap: P -> R -> count (How many instances of R is P holding)
    val allocatedMap = mutableMapOf<String, MutableMap<String, Int>>()
    // requestMap: P -> R -> count (How many instances of R is P requesting)
    val requestMap = mutableMapOf<String, MutableMap<String, Int>>()

    // 1. Tally Allocations (R -> P) and Requests (P -> R)
    edges.forEach { edge ->
        val fromId = edge.fromId
        val toId = edge.toId

        if (edge.isRequest) {
            // P -> R (Request): Process requests Resource
            if (processes.contains(fromId) && resourceIds.contains(toId)) {
                requestMap.getOrPut(fromId) { mutableMapOf() }.let {
                    it[toId] = it.getOrElse(toId) { 0 } + 1
                }
            }
        } else {
            // R -> P (Allocation): Resource is allocated to Process
            if (resourceIds.contains(fromId) && processes.contains(toId)) {
                // We map this as P (toId) holding R (fromId)
                allocatedMap.getOrPut(toId) { mutableMapOf() }.let {
                    it[fromId] = it.getOrElse(fromId) { 0 } + 1
                }
            }
        }
    }

    // 2. Calculate Initial Available Resources (Work Vector)
    val totalMap = resources.associate { it.id to it.totalInstances }
    val availableMap = totalMap.toMutableMap()

    // Subtract ALLOCATED resources from total to get AVAILABLE
    allocatedMap.values.forEach { allocationsHeldByProcess ->
        allocationsHeldByProcess.forEach { (rId, count) ->
            availableMap[rId] = availableMap.getOrElse(rId) { 0 } - count
        }
    }

    // 3. Safety Algorithm Execution
    val work = availableMap.toMutableMap()
    val finish = processes.associateWith { false }.toMutableMap()
    val safeSequence = mutableListOf<String>()

    var processFound = true
    while (processFound) {
        processFound = false
        for (pId in processes) {
            if (!finish[pId]!!) {
                // Check if process pId's request <= Work (current available pool)
                val currentRequest = requestMap[pId] ?: emptyMap()

                val canSatisfy = currentRequest.all { (rId, count) ->
                    (work[rId] ?: 0) >= count
                }

                if (canSatisfy) {
                    // Process pId can run: pretend it runs and releases its resources
                    val currentAllocation = allocatedMap[pId] ?: emptyMap()
                    currentAllocation.forEach { (rId, count) ->
                        work[rId] = work.getOrElse(rId) { 0 } + count // Add released resources back to work
                    }
                    finish[pId] = true
                    safeSequence.add(pId)
                    processFound = true
                }
            }
        }
    }

    // 4. Check Result
    val isSafe = finish.all { it.value }

    return if (isSafe) {
        SafetyResult(true, safeSequence)
    } else {
        SafetyResult(false)
    }
}

// --- 3. UI COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Deadlock() {
    // State management for the graph structure
    var processes by remember { mutableStateOf(listOf("P1")) }
    var resources by remember { mutableStateOf(listOf(
        ResourceData("R1", 1), // R1 is single instance
    )) }
    var edges by remember { mutableStateOf<List<Edge>>(listOf()) }

    // Derived state for graph visualization and detection
    val nodes: List<Node> by remember(processes, resources) {
        derivedStateOf {
            processes.map { Node(it, true) } + resources.map { Node(it.id, false) }
        }
    }

    // Run the Safety Check
    val safetyResult: SafetyResult by remember(processes, resources, edges) {
        derivedStateOf {
            runSafetyCheck(processes, resources, edges)
        }
    }

    // Changed to vertical scroll state for better mobile support
    val verticalScrollState = rememberScrollState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deadlock & Safety Graph", style = TextStyle(fontSize = 20.sp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        // FIX: Changed main layout from Row (horizontal) to Column (vertical)
        // to prevent layout overflow and crashes on narrow screens.
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(verticalScrollState) // Use vertical scroll
                .padding(16.dp), // Main padding applied here
        ) {
            // Left Panel: Graph Controls
            Column(
                // FIX: Removed fixed width and fillMaxHeight. Now takes full width.
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Safety Status Indicator
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (safetyResult.isSafe) Color(0xFF4CAF50) else Color(0xFFF44336),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (safetyResult.isSafe) "SYSTEM IS SAFE" else "SYSTEM IS UNSAFE",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        )
                        val sequenceText = if (safetyResult.isSafe) {
                            "Safe Sequence: <${safetyResult.safeSequence.joinToString(", ")}>"
                        } else {
                            "No Safe Sequence Found"
                        }
                        Text(sequenceText)
                    }
                }

                // Node and Edge Management
                NodeManagementPanel(
                    title = "Processes (P)",
                    nodes = processes.map { it to 0 }, // 0 instances for P
                    onAdd = { processes = processes + "P${processes.size + 1}" },
                    onRemove = { id ->
                        processes = processes.filter { it != id }
                        edges = edges.filter { it.fromId != id && it.toId != id }
                    },
                    onInstancesChange = { _, _ -> /* N/A for processes */ }
                )
                Spacer(Modifier.height(8.dp))
                NodeManagementPanel(
                    title = "Resources (R)",
                    nodes = resources.map { it.id to it.totalInstances },
                    onAdd = { resources = resources + ResourceData("R${resources.size + 1}", 1) },
                    onRemove = { id ->
                        resources = resources.filter { it.id != id }
                        edges = edges.filter { it.fromId != id && it.toId != id }
                    },
                    onInstancesChange = { id, instances ->
                        resources = resources.map { if (it.id == id) it.copy(totalInstances = instances) else it }
                    },
                    isResource = true
                )
                Spacer(Modifier.height(16.dp))
                EdgeManagementPanel(nodes = nodes, resources = resources, edges = edges, onEdgesChange = { edges = it })
            }

            Spacer(Modifier.height(16.dp)) // Separator

            // Right Panel: Graph Visualization
            Card(
                // FIX: Removed weight, set fixed height for stable Canvas drawing in a Column.
                modifier = Modifier.fillMaxWidth().height(400.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                ResourceAllocationGraph(
                    graphState = GraphState(nodes, resources, edges),
                    safetyResult = safetyResult // <-- Passed the result here
                )
            }
        }
    }
}

@Composable
fun NodeManagementPanel(
    title: String,
    nodes: List<Pair<String, Int>>, // Pair of ID and Instances
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onInstancesChange: (String, Int) -> Unit,
    isResource: Boolean = false
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                IconButton(onClick = onAdd) {
                    // FIX: Using standard Material Icon
                    Icon("add","Add")
                }
            }
            Spacer(Modifier.height(8.dp))

            // Table Header
            Row(Modifier.fillMaxWidth()) {
                Text("ID", Modifier.width(40.dp))
                if (isResource) {
                    // Explicitly name modifier to resolve potential ambiguity
                    Text(text = "Total Instances", modifier = Modifier.weight(1f).padding(start = 8.dp))
                }
                Spacer(Modifier.width(48.dp)) // For the remove button
            }

            // Node List
            LazyColumn(Modifier.heightIn(max = 100.dp)) {
                items(nodes) { (id, instances) ->
                    // 1. Introduce local state for TextFieldValue, which holds both text and selection info
                    var instanceTextState by remember(instances) {
                        mutableStateOf(TextFieldValue(instances.toString()))
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(id, Modifier.width(40.dp))

                        if (isResource) {
                            OutlinedTextField(
                                // 2. Bind the value to the local state
                                value = instanceTextState.text,
                                onValueChange = { newValue ->
                                    // Update local state first
                                    instanceTextState = instanceTextState.copy(text = newValue)

                                    // Update the external model
                                    val newInstances = newValue.toIntOrNull() ?: 1
                                    onInstancesChange(id, newInstances.coerceAtLeast(1))
                                },
                                label = { Text("Instances", fontSize = 10.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    // 3. CRITICAL: Use onFocusChanged to select all text when the field gains focus
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            instanceTextState = instanceTextState.copy(
                                                selection = TextRange(0, instanceTextState.text.length)
                                            )
                                        }
                                    },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        IconButton(onClick = { onRemove(id) }, modifier = Modifier.size(24.dp)) {
                            Icon("close", contentDescription = "Remove")
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun EdgeManagementPanel(
    nodes: List<Node>,
    resources: List<ResourceData>,
    edges: List<Edge>,
    onEdgesChange: (List<Edge>) -> Unit
) {
    val validNodes = nodes.map { it.id }
    var fromId by remember { mutableStateOf(nodes.firstOrNull()?.id ?: "") }
    var toId by remember { mutableStateOf(nodes.getOrNull(1)?.id ?: "") }
    var isRequest by remember { mutableStateOf(true) } // Request (P->R) or Allocation (R->P)

    // Calculate current allocations *per resource* for limit check
    val currentResourceAllocations = remember(edges) {
        edges.filter { !it.isRequest } // Allocation edges (R -> P)
            .groupBy { it.fromId } // Group by Resource ID (R1, R2, etc.)
            .mapValues { it.value.size } // Count the number of allocation edges from that resource
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Add/Remove Edge (Unit)", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(
                    selected = isRequest,
                    onClick = { isRequest = true },
                    label = { Text("Request (P → R)") }
                )
                FilterChip(
                    selected = !isRequest,
                    onClick = { isRequest = false },
                    label = { Text (text= "Allocation (R → P)")}
                )
            }

            // Node Selectors
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DropdownSelector(
                    label = "From",
                    selectedId = fromId,
                    nodes = validNodes,
                    onSelect = { fromId = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Icon("right","Right Arrow")
                Spacer(Modifier.width(8.dp))
                DropdownSelector(
                    label = "To",
                    selectedId = toId,
                    nodes = validNodes,
                    onSelect = { toId = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Add/Remove Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        if (fromId in validNodes && toId in validNodes) {
                            val newEdge = Edge(fromId, toId, isRequest)
                            val fromNode = nodes.find { it.id == fromId }
                            val toNode = nodes.find { it.id == toId }
                            val resourceData = resources.find { it.id == fromId } ?: resources.find { it.id == toId }

                            val isValid = when {
                                fromNode == null || toNode == null || resourceData == null -> false
                                newEdge.isRequest && (!fromNode.isProcess || toNode.isProcess) -> false // P -> R (Must be Process -> Resource)
                                !newEdge.isRequest && (fromNode.isProcess || !toNode.isProcess) -> false // R -> P (Must be Resource -> Process)
                                !newEdge.isRequest -> {
                                    // Check resource limits for allocation (R -> P)
                                    // The resource ID is 'fromId' for Allocation (R -> P)
                                    val currentAllocated = currentResourceAllocations.getOrElse(fromId) { 0 }
                                    currentAllocated < resourceData.totalInstances
                                }
                                else -> true
                            }

                            if (isValid && newEdge !in edges) {
                                onEdgesChange(edges + newEdge)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("Add Edge")
                }

                Button(
                    onClick = {
                        val edgeToRemove = Edge(fromId, toId, isRequest)
                        onEdgesChange(edges.filter { it != edgeToRemove })
                    },
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D))
                ) {
                    Text("Remove")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Current Edges: ${edges.size}", style = TextStyle(fontSize = 12.sp, color = Color.Gray))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    selectedId: String,
    nodes: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier // New modifier parameter added here
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier // Applied the modifier here
    ) {
        OutlinedTextField(
            value = selectedId,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            nodes.forEach { nodeId ->
                DropdownMenuItem(
                    text = { Text(nodeId) },
                    onClick = {
                        onSelect(nodeId)
                        expanded = false
                    }
                )
            }
        }
    }
}


// --- 4. VISUALIZATION ---

@Composable
fun ResourceAllocationGraph(graphState: GraphState, safetyResult: SafetyResult) {
    val nodeMap = remember(graphState.nodes) { graphState.nodes.associateBy { it.id }.toMutableMap() }
    val resourceDataMap = remember(graphState.resources) { graphState.resources.associateBy { it.id } }
    val textMeasurer = rememberTextMeasurer()

    // Calculate instance counts for visualization
    val allocatedCounts = remember(graphState.edges) {
        graphState.edges.filter { !it.isRequest }.groupBy { it.fromId }.mapValues { it.value.size }
    }

    val availableCounts = remember(graphState.resources, allocatedCounts) {
        graphState.resources.associate { it.id to (it.totalInstances - allocatedCounts.getOrElse(it.id) { 0 }) }
    }

    // Use a fixed random seed for predictable node placement in each session
    val random = remember { Random(123) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val padding = 60f
        val radius = 25f
        val resourceSize = Size(radius * 2, radius * 2)

        // 1. Calculate Node Positions (Simple distributed layout)
        val processNodes = graphState.nodes.filter { it.isProcess }
        val resourceNodes = graphState.nodes.filter { !it.isProcess }

        // Place Processes on the left side
        processNodes.forEachIndexed { index, node ->
            val yPos = padding + (height - 2 * padding) * (index.toFloat() + 0.5f) / processNodes.size.toFloat()
            nodeMap[node.id] = node.copy(position = Offset(padding + random.nextFloat() * 20f, yPos + random.nextFloat() * 10f - 5f))
        }

        // Place Resources on the right side
        resourceNodes.forEachIndexed { index, node ->
            val yPos = padding + (height - 2 * padding) * (index.toFloat() + 0.5f) / resourceNodes.size.toFloat()
            nodeMap[node.id] = node.copy(position = Offset(width - padding - random.nextFloat() * 20f, yPos + random.nextFloat() * 10f - 5f))
        }

        // 2. Draw Edges
        graphState.edges.forEach { edge ->
            val fromNode = nodeMap[edge.fromId] ?: return@forEach
            val toNode = nodeMap[edge.toId] ?: return@forEach

            // No specific cycle highlighting, rely on Safety Check
            drawRAGEdge(
                from = fromNode,
                to = toNode,
                isRequest = edge.isRequest,
                drawScope = this,
                processRadius = radius,
                resourceSize = resourceSize
            )
        }

        // 3. Draw Nodes and Labels
        nodeMap.values.forEach { node ->
            val isDeadlocked = !safetyResult.isSafe // Use safety result for highlighting
            val borderColor = if (isDeadlocked) Color.Red else Color.Black
            val fillColor = if (node.isProcess) Color(0xFFC8E6C9) else Color(0xFFBBDEFB)

            if (node.isProcess) {
                // Process (Circle)
                drawCircle(
                    color = fillColor,
                    radius = radius,
                    center = node.position
                )
                drawCircle(
                    color = borderColor,
                    radius = radius,
                    center = node.position,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            } else {
                // Resource (Rectangle)
                drawRect(
                    color = fillColor,
                    topLeft = node.position - Offset(resourceSize.width / 2, resourceSize.height / 2),
                    size = resourceSize
                )
                drawRect(
                    color = borderColor,
                    topLeft = node.position - Offset(resourceSize.width / 2, resourceSize.height / 2),
                    size = resourceSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                // Draw resource instances inside the rectangle
                val rData = resourceDataMap[node.id]
                if (rData != null) {
                    val total = rData.totalInstances
                    val available = availableCounts.getOrElse(node.id) { 0 }
                    val allocated = total - available

                    val instanceWidth = 6f
                    val instanceSpacing = 4f
                    val startOffset = node.position - Offset(resourceSize.width / 2, resourceSize.height / 2) + Offset(instanceSpacing, instanceSpacing)

                    // Draw the instance boxes
                    for (i in 0 until total) {
                        val isAvailable = available > i
                        val color = if (isAvailable) Color(0xFF00796B) else Color(0xFFD32F2F) // Teal for available, Red for allocated

                        drawRect(
                            color = color,
                            topLeft = startOffset + Offset(
                                (i % 2).toFloat() * (instanceWidth + instanceSpacing),
                                (i / 2).toFloat() * (instanceWidth + instanceSpacing)
                            ),
                            size = Size(instanceWidth, instanceWidth)
                        )
                    }
                }
            }

            // Draw Node Label
            drawText(
                textMeasurer = textMeasurer,
                text = node.id,
                style = TextStyle(fontSize = 12.sp, color = Color.Black),
                topLeft = node.position - Offset(radius / 2f, 5f)
            )
        }
    }
}

// Helper function to draw RAG edges with arrowheads
private fun drawRAGEdge(
    from: Node,
    to: Node,
    isRequest: Boolean,
    drawScope: DrawScope,
    processRadius: Float,
    resourceSize: Size,
) = with(drawScope) {
    val arrowSize = 10f
    val edgeColor = if (isRequest) Color.Blue else Color.DarkGray
    val strokeWidth = 2f

    val startPos = from.position
    val endPos = to.position

    // Calculate angle of the line
    val angle = atan2(endPos.y - startPos.y, endPos.x - startPos.x)
    val dx = cos(angle)
    val dy = sin(angle)

    // Determine the distance to offset from the start/end point based on node shape/size
    val startOffset = if (from.isProcess) processRadius else resourceSize.width / 2
    val endOffset = if (to.isProcess) processRadius else resourceSize.width / 2

    // Apply distance offset to start/end points for drawing outside the nodes
    val drawnStart = Offset(
        startPos.x + dx * startOffset,
        startPos.y + dy * startOffset
    )
    val drawnEnd = Offset(
        endPos.x - dx * endOffset,
        endPos.y - dy * endOffset
    )

    // Draw the main line
    drawLine(
        color = edgeColor,
        start = drawnStart,
        end = drawnEnd,
        strokeWidth = strokeWidth
    )

    // Draw the arrowhead at the 'drawnEnd' point
    // Using PI from kotlin.math
    val arrowPoint1 = Offset(
        drawnEnd.x - cos(angle - PI.toFloat() / 6) * arrowSize,
        drawnEnd.y - sin(angle - PI.toFloat() / 6) * arrowSize
    )
    val arrowPoint2 = Offset(
        drawnEnd.x - cos(angle + PI.toFloat() / 6) * arrowSize,
        drawnEnd.y - sin(angle + PI.toFloat() / 6) * arrowSize
    )

    drawLine(edgeColor, drawnEnd, arrowPoint1, strokeWidth = strokeWidth)
    drawLine(edgeColor, drawnEnd, arrowPoint2, strokeWidth = strokeWidth)
}

