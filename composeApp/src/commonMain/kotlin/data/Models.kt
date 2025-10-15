package data

import androidx.compose.ui.geometry.Offset

data class Process(
    val id: String,
    val arrivalTime: Int,
    val burstTime: Int,
    val priority: Int = 0
)

data class GanttSlice(
    val processId: String,
    val start: Int,
    var end: Int
)


data class ProcessMetrics(
    val completionTime: Int,
    val turnaroundTime: Int,
    val waitingTime: Int
)


data class SimulationMetrics(
    val averageWaitingTime: Double = 0.0,
    val averageTurnaroundTime: Double = 0.0
)

//  DEADLOCK

data class ResourceData(val id: String, val totalInstances: Int)

data class Node(
    val id: String,
    val isProcess: Boolean,
    var position: Offset = Offset.Zero
)

data class Edge(
    val fromId: String,
    val toId: String,
    val isRequest: Boolean
)

data class GraphState(
    val nodes: List<Node>,
    val resources: List<ResourceData>,
    val edges: List<Edge>
)

data class SafetyResult(
    val isSafe: Boolean,
    val safeSequence: List<String> = emptyList()
)

// MEMORY MANAGEMENT

data class MemoryBlock(
    val id: String,
    val start: Int,
    val size: Int,
    val isFree: Boolean,
    val processSize: Int = 0,
    val internalFragmentation: Int = 0,
    val lastAllocatedTime: Long = 0L
)


data class FragmentationStats(
    val totalFree: Int = 0,
    val internal: Int = 0,
    val external: Int = 0
)
