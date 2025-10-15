package algorithms

import data.GanttSlice
import data.Process
import data.ProcessMetrics
import data.SimulationMetrics



private data class ProcessState(
    val process: Process,
    var remainingBurst: Int,
    var hasArrived: Boolean = false,
    var startTime: Int = -1,
    var lastRunTime: Int = 0,
    val initialBurst: Int = process.burstTime
) {
    val id: String = process.id
    val arrivalTime: Int = process.arrivalTime
    val priority: Int = process.priority
}

fun runFCFS(processes: List<Process>): Pair<List<GanttSlice>, Map<String, ProcessMetrics>> {
    if (processes.isEmpty()) return Pair(emptyList(), emptyMap())
    val sortedProcesses = processes.sortedWith(
        compareBy<Process> { it.arrivalTime }.thenBy { it.id }
    )

    var currentTime = 0
    val ganttChart = mutableListOf<GanttSlice>()
    val results = mutableMapOf<String, ProcessMetrics>()

    sortedProcesses.forEach { p ->
        val startTime = maxOf(currentTime, p.arrivalTime)
        val finishTime = startTime + p.burstTime

        ganttChart.add(GanttSlice(
            processId = p.id,
            start = startTime,
            end = finishTime
        ))

        results[p.id] = ProcessMetrics(
            completionTime = finishTime,
            turnaroundTime = finishTime - p.arrivalTime,
            waitingTime = startTime - p.arrivalTime
        )
        currentTime = finishTime
    }

    return Pair(ganttChart, results)
}


fun runRoundRobin(processes: List<Process>, quantum: Int): Pair<List<GanttSlice>, Map<String, ProcessMetrics>> {
    if (processes.isEmpty() || quantum <= 0) return Pair(emptyList(), emptyMap())

    val tempProcesses = processes.map { ProcessState(it, it.burstTime) }
        .sortedBy { it.arrivalTime }

    val readyQueue = ArrayDeque<ProcessState>()
    val ganttChart = mutableListOf<GanttSlice>()
    val results = mutableMapOf<String, ProcessMetrics>()
    var currentTime = 0
    var processIndex = 0
    val numProcesses = processes.size

    while (results.size < numProcesses) {

        while (processIndex < numProcesses && tempProcesses[processIndex].arrivalTime <= currentTime) {
            val pState = tempProcesses[processIndex]
            if (!pState.hasArrived) {
                readyQueue.add(pState)
                pState.hasArrived = true
            }
            processIndex++
        }

        if (readyQueue.isEmpty()) {
            if (processIndex < numProcesses) {
                currentTime = tempProcesses[processIndex].arrivalTime
                continue
            } else {
                break
            }
        }

        val currentProcessState = readyQueue.removeFirst()
        val p = currentProcessState.process

        val executionTime = minOf(currentProcessState.remainingBurst, quantum)

        val startTime = currentTime
        currentTime += executionTime
        currentProcessState.remainingBurst -= executionTime

        if (currentProcessState.startTime == -1) {
            currentProcessState.startTime = startTime
        }

        ganttChart.add(GanttSlice(p.id, startTime, currentTime))

        val processesAddedThisCycle = mutableListOf<ProcessState>()
        while (processIndex < numProcesses && tempProcesses[processIndex].arrivalTime <= currentTime) {
            val pState = tempProcesses[processIndex]
            if (!pState.hasArrived) {
                processesAddedThisCycle.add(pState)
                pState.hasArrived = true
            }
            processIndex++
        }
        readyQueue.addAll(processesAddedThisCycle)

        if (currentProcessState.remainingBurst > 0) {
            readyQueue.add(currentProcessState)
        } else {
            val completionTime = currentTime
            results[p.id] = ProcessMetrics(
                completionTime = completionTime,
                turnaroundTime = completionTime - p.arrivalTime,
                waitingTime = (completionTime - p.arrivalTime) - currentProcessState.initialBurst
            )
        }
    }

    return Pair(ganttChart.toList(), results)
}

fun runSJF(processes: List<Process>): Pair<List<GanttSlice>, Map<String, ProcessMetrics>> {
    if (processes.isEmpty()) return Pair(emptyList(), emptyMap())

    val pendingProcesses = processes.toMutableList()
    val finishedProcesses = mutableListOf<Process>()
    val ganttChart = mutableListOf<GanttSlice>()
    val results = mutableMapOf<String, ProcessMetrics>()
    var currentTime = 0

    while (finishedProcesses.size < processes.size) {
        val arrivedProcesses = pendingProcesses.filter { it.arrivalTime <= currentTime }
        if (arrivedProcesses.isNotEmpty()) {
            val nextProcess = arrivedProcesses.minWithOrNull(
                compareBy<Process> { it.burstTime }
                    .thenBy { it.arrivalTime }
                    .thenBy { it.id }
            )!!

            val startTime = currentTime
            val finishTime = startTime + nextProcess.burstTime

            ganttChart.add(GanttSlice(
                processId = nextProcess.id,
                start = startTime,
                end = finishTime
            ))

            results[nextProcess.id] = ProcessMetrics(
                completionTime = finishTime,
                turnaroundTime = finishTime - nextProcess.arrivalTime,
                waitingTime = startTime - nextProcess.arrivalTime
            )

            currentTime = finishTime
            pendingProcesses.remove(nextProcess)
            finishedProcesses.add(nextProcess)

        } else {
            // CPU is idle: Advance time to the arrival of the next process
            val nextArrival = pendingProcesses.minOfOrNull { it.arrivalTime }
            if (nextArrival != null) {
                currentTime = nextArrival
            } else {
                break
            }
        }
    }

    return Pair(ganttChart, results)
}

// --- SRTF (Preemptive SJF) ---

/**
 * Executes the Shortest Remaining Time First (SRTF) scheduling algorithm.
 * Preemptive, priority is based on remaining burst time.
 */
fun runSRTF(processes: List<Process>): Pair<List<GanttSlice>, Map<String, ProcessMetrics>> {
    if (processes.isEmpty()) return Pair(emptyList(), emptyMap())

    val tempProcesses = processes.map { ProcessState(it, it.burstTime) }.sortedBy { it.arrivalTime }
    val activeProcesses = tempProcesses.toMutableList()
    val results = mutableMapOf<String, ProcessMetrics>()
    val ganttSlices = mutableListOf<GanttSlice>()
    var currentTime = 0

    var lastExecutedProcessId: String? = null

    while (results.size < processes.size) {
        // 1. Find all arrived and incomplete processes
        val readyQueue = activeProcesses
            .filter { it.arrivalTime <= currentTime && it.remainingBurst > 0 }
            .toMutableList()

        if (readyQueue.isEmpty()) {
            // CPU is idle. Advance time to the next process arrival.
            val nextArrival = activeProcesses.minOfOrNull { if (it.remainingBurst > 0) it.arrivalTime else Int.MAX_VALUE }
            if (nextArrival != null && nextArrival > currentTime) {
                currentTime = nextArrival
                continue
            } else {
                break // No more active processes to run
            }
        }

        // 2. Select the process with the shortest remaining burst time (SRTF)
        val currentProcess = readyQueue.minWithOrNull(
            compareBy<ProcessState> { it.remainingBurst }
                .thenBy { it.arrivalTime } // Tie-break: earlier arrival
                .thenBy { it.id } // Tie-break: ID
        )!!

        // 3. Handle preemption/Gantt chart update
        if (currentProcess.id != lastExecutedProcessId) {
            // New process is running, or a different process is resuming
            ganttSlices.add(GanttSlice(currentProcess.id, currentTime, currentTime + 1))
        } else {
            // Same process is continuing, extend the last slice
            ganttSlices.last().end = currentTime + 1
        }

        // 4. Update state
        if (currentProcess.startTime == -1) {
            currentProcess.startTime = currentTime
        }
        currentProcess.remainingBurst--
        lastExecutedProcessId = currentProcess.id
        currentTime++

        // 5. Check for completion
        if (currentProcess.remainingBurst == 0) {
            val completionTime = currentTime
            results[currentProcess.id] = ProcessMetrics(
                completionTime = completionTime,
                turnaroundTime = completionTime - currentProcess.arrivalTime,
                waitingTime = (completionTime - currentProcess.arrivalTime) - currentProcess.initialBurst
            )
        }
    }
    return Pair(ganttSlices, results)
}

// --- Priority (Non-Preemptive) ---

/**
 * Executes the Non-Preemptive Priority scheduling algorithm.
 * Priority (lower number is higher priority).
 */
fun runPriorityNonPreemptive(processes: List<Process>): Pair<List<GanttSlice>, Map<String, ProcessMetrics>> {
    if (processes.isEmpty()) return Pair(emptyList(), emptyMap())

    val pendingProcesses = processes.toMutableList()
    val finishedProcesses = mutableListOf<Process>()
    val ganttChart = mutableListOf<GanttSlice>()
    val results = mutableMapOf<String, ProcessMetrics>()
    var currentTime = 0

    while (finishedProcesses.size < processes.size) {
        val arrivedProcesses = pendingProcesses.filter { it.arrivalTime <= currentTime }

        if (arrivedProcesses.isNotEmpty()) {
            // Select the next process: highest priority (lowest number), tie-break by arrival time, then ID
            val nextProcess = arrivedProcesses.minWithOrNull(
                compareBy<Process> { it.priority }
                    .thenBy { it.arrivalTime }
                    .thenBy { it.id }
            )!!

            val startTime = currentTime
            val finishTime = startTime + nextProcess.burstTime

            ganttChart.add(GanttSlice(
                processId = nextProcess.id,
                start = startTime,
                end = finishTime
            ))

            results[nextProcess.id] = ProcessMetrics(
                completionTime = finishTime,
                turnaroundTime = finishTime - nextProcess.arrivalTime,
                waitingTime = startTime - nextProcess.arrivalTime
            )

            currentTime = finishTime
            pendingProcesses.remove(nextProcess)
            finishedProcesses.add(nextProcess)

        } else {
            // CPU is idle: Advance time to the arrival of the next process
            val nextArrival = pendingProcesses.minOfOrNull { it.arrivalTime }
            if (nextArrival != null) {
                currentTime = nextArrival
            } else {
                break
            }
        }
    }

    return Pair(ganttChart, results)
}

// --- Priority (Preemptive) ---

/**
 * Executes the Preemptive Priority scheduling algorithm.
 * Priority (lower number is higher priority). Preemption occurs on arrival of a higher priority process.
 */
fun runPriorityPreemptive(processes: List<Process>): Pair<List<GanttSlice>, Map<String, ProcessMetrics>> {
    if (processes.isEmpty()) return Pair(emptyList(), emptyMap())

    val tempProcesses = processes.map { ProcessState(it, it.burstTime) }.sortedBy { it.arrivalTime }
    val activeProcesses = tempProcesses.toMutableList()
    val results = mutableMapOf<String, ProcessMetrics>()
    val ganttSlices = mutableListOf<GanttSlice>()
    var currentTime = 0

    var lastExecutedProcessId: String? = null

    while (results.size < processes.size) {
        // 1. Find all arrived and incomplete processes
        val readyQueue = activeProcesses
            .filter { it.arrivalTime <= currentTime && it.remainingBurst > 0 }
            .toMutableList()

        if (readyQueue.isEmpty()) {
            // CPU is idle. Advance time to the next process arrival.
            val nextArrival = activeProcesses.minOfOrNull { if (it.remainingBurst > 0) it.arrivalTime else Int.MAX_VALUE }
            if (nextArrival != null && nextArrival > currentTime) {
                currentTime = nextArrival
                continue
            } else {
                break // No more active processes to run
            }
        }

        // 2. Select the process with the highest priority (lowest number)
        val currentProcess = readyQueue.minWithOrNull(
            compareBy<ProcessState> { it.priority }
                .thenBy { it.arrivalTime } // Tie-break: FCFS
                .thenBy { it.id } // Tie-break: ID
        )!!

        // 3. Handle preemption/Gantt chart update
        if (currentProcess.id != lastExecutedProcessId) {
            // New process is running, or a different process is resuming
            ganttSlices.add(GanttSlice(currentProcess.id, currentTime, currentTime + 1))
        } else {
            // Same process is continuing, extend the last slice
            ganttSlices.last().end = currentTime + 1
        }

        // 4. Update state
        if (currentProcess.startTime == -1) {
            currentProcess.startTime = currentTime
        }
        currentProcess.remainingBurst--
        lastExecutedProcessId = currentProcess.id
        currentTime++

        // 5. Check for completion
        if (currentProcess.remainingBurst == 0) {
            val completionTime = currentTime
            results[currentProcess.id] = ProcessMetrics(
                completionTime = completionTime,
                turnaroundTime = completionTime - currentProcess.arrivalTime,
                waitingTime = (completionTime - currentProcess.arrivalTime) - currentProcess.initialBurst
            )
        }
    }
    return Pair(ganttSlices, results)
}


fun calculateAverages(metrics: Map<String, ProcessMetrics>): SimulationMetrics {
    if (metrics.isEmpty()) return SimulationMetrics()

    val totalWait = metrics.values.sumOf { it.waitingTime }
    val totalTurnaround = metrics.values.sumOf { it.turnaroundTime }
    val count = metrics.size

    return SimulationMetrics(
        averageWaitingTime = totalWait.toDouble() / count,
        averageTurnaroundTime = totalTurnaround.toDouble() / count
    )
}
