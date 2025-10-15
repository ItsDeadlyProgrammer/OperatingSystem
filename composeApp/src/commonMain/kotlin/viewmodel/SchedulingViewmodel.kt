package viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.Process
import data.GanttSlice
import data.SimulationMetrics
// Placeholder imports for external dependencies not included in the prompt
// In your real environment, ensure these are available:
 import algorithms.calculateAverages
 import algorithms.runFCFS
 import algorithms.runPriorityNonPreemptive
 import algorithms.runPriorityPreemptive
 import algorithms.runRoundRobin
 import algorithms.runSJF
 import algorithms.runSRTF

// List of supported scheduling algorithms
private val supportedAlgorithms = listOf(
    "FCFS",
    "Round Robin",
    "SJF",
    "SRTF",
    "Priority (Non-Preemptive)",
    "Priority (Preemptive)"
)

// Initial state for the processes
private val initialProcesses = listOf(
    Process(id = "P1", arrivalTime = 0, burstTime = 1, priority = 1),
)

/**
 * ViewModel to hold the state and business logic for the Process Scheduling Simulator.
 * This class should exist outside the Compose lifecycle to ensure stability and separation of concerns.
 */
class SchedulingViewModel {
    // State variables exposed for the UI (using mutableStateOf for Compose observability)
    var processes by mutableStateOf(initialProcesses)
        private set

    var ganttChart by mutableStateOf(emptyList<GanttSlice>())
        private set

    var metrics by mutableStateOf(SimulationMetrics())
        private set

    var selectedAlgorithm by mutableStateOf(supportedAlgorithms.first())
        private set

    var quantum by mutableStateOf(4)
        private set

    val algorithms: List<String> = supportedAlgorithms

    init {
        // Run initial simulation when the ViewModel is created
        runSimulation()
    }

    // --- State Update Handlers ---

    fun setAlgorithm(algorithm: String) {
        selectedAlgorithm = algorithm
        runSimulation()
    }

    /**
     * Renamed from setQuantum to avoid JVM signature clash with the 'quantum' state property.
     */
    fun updateQuantum(q: Int) {
        // Ensure quantum is at least 1
        quantum = q.coerceAtLeast(1)
        runSimulation()
    }

    /**
     * Renamed from setProcesses to avoid JVM signature clash with the 'processes' state property.
     */
    fun updateProcesses(newProcesses: List<Process>) {
        processes = newProcesses
        runSimulation()
    }

    fun addProcess() {
        val newId = "P${processes.size + 1}"
        val lastPriority = processes.lastOrNull()?.priority ?: 0
        val newProcesses = processes + Process(
            newId,
            // Simple default values
            arrivalTime = maxOf(0, processes.lastOrNull()?.arrivalTime ?: 0),
            burstTime = 5,
            priority = lastPriority
        )
        // Call the new setter function
        updateProcesses(newProcesses)
    }

    fun removeProcess(id: String) {
        val newProcesses = processes.filter { it.id != id }
        // Call the new setter function
        updateProcesses(newProcesses)
    }

    // --- Core Simulation Logic ---

    fun runSimulation() {
        // NOTE: This logic requires external functions like runFCFS, calculateAverages, etc.
        // Since those functions were not provided, this is a MOCK implementation to ensure
        // the application compiles and the UI displays *something*.

        if (processes.isNotEmpty()) {
            // Placeholder: Assume FCFS logic for the mock
            var currentTime = 0
            val chart = mutableListOf<GanttSlice>()
            val completedProcesses = mutableListOf<Process>()

            val sortedProcesses = processes.sortedBy { it.arrivalTime }

            sortedProcesses.forEach { p ->
                // Add Idle time if necessary
                if (p.arrivalTime > currentTime) {
                    chart.add(GanttSlice("Idle", currentTime, p.arrivalTime))
                    currentTime = p.arrivalTime
                }

                // Add Process execution
                chart.add(GanttSlice(p.id, currentTime, currentTime + p.burstTime))
                currentTime += p.burstTime

                // Add to completed list for metrics calculation (mock)
                completedProcesses.add(p)
            }

            ganttChart = chart
            // Mock Metrics Calculation
            metrics = SimulationMetrics(
                averageWaitingTime = sortedProcesses.sumOf { it.arrivalTime }.toDouble() / processes.size,
                averageTurnaroundTime = currentTime.toDouble() / processes.size
            )

        } else {
            ganttChart = emptyList()
            metrics = SimulationMetrics()
        }

        // In a complete environment, the real logic would look like this:

        val (chart, results) = when (selectedAlgorithm) {
            "FCFS" -> runFCFS(processes)
            "Round Robin" -> runRoundRobin(processes, quantum)
            "SJF" -> runSJF(processes)
            "SRTF" -> runSRTF(processes)
            "Priority (Non-Preemptive)" -> runPriorityNonPreemptive(processes)
            "Priority (Preemptive)" -> runPriorityPreemptive(processes)
            else -> runFCFS(processes)
        }
        ganttChart = chart
        metrics = calculateAverages(results)

    }
}
