package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import data.FragmentationStats
import data.MemoryBlock
import kotlin.math.max

/**
 * Defines the initial, partitioned state of the memory.
 * Total size is 256 KB (50+100+76+30).
 */
private val initialMemoryBlocks = listOf(
    MemoryBlock(id = "Free", start = 0, size = 50, isFree = true),
    MemoryBlock(id = "Free", start = 50, size = 100, isFree = true),
    MemoryBlock(id = "Free", start = 150, size = 76, isFree = true),
    MemoryBlock(id = "Free", start = 226, size = 30, isFree = true)
).sortedBy { it.start }


/**
 * Composable function to create and remember the StateHolder instance.
 */
@Composable
fun rememberMemoryManagementStateHolder(): MemoryManagementStateHolder {
    return remember { MemoryManagementStateHolder() }
}

class MemoryManagementStateHolder {
    private val TOTAL_MEMORY = 256 // Total memory size in KB
    private var nextProcessId = 1
    // Tracks the starting address of the last successful allocation for Next-Fit
    private var lastAllocatedBlockStart by mutableStateOf(0)

    // Counter to use instead of platform-specific clock function
    private var allocationSequence = 0L

    // --- State exposed to the UI ---
    var memoryBlocks by mutableStateOf(initialMemoryBlocks)
        private set

    var requestSizeInput by mutableStateOf("10")
        private set

    var allocationAlgorithm by mutableStateOf("First-Fit")
        private set

    var allocationMessage by mutableStateOf("Ready to allocate with partitioned memory.")
        private set

    // All available algorithms
    val algorithms: List<String> = listOf("First-Fit", "Best-Fit", "Worst-Fit", "Next-Fit")

    // Derived state for stats
    val fragmentationStats: State<FragmentationStats> = mutableStateOf(calculateFragmentationStats())

    // --- Control Functions ---

    /**
     * Resets the memory state back to the initial, partitioned configuration.
     */
    fun resetMemory() {
        memoryBlocks = initialMemoryBlocks
        nextProcessId = 1
        lastAllocatedBlockStart = 0
        allocationSequence = 0L
        allocationMessage = "Memory reset to initial partitioned state."
        (fragmentationStats as MutableState).value = calculateFragmentationStats()
    }

    fun setRequestSize(size: String) {
        requestSizeInput = size
    }

    fun setAlgorithm(algorithm: String) {
        allocationAlgorithm = algorithm
        allocationMessage = "Algorithm set to $algorithm. Ready to allocate."
    }

    // --- Core Allocation Logic ---

    fun allocateProcess() {
        val requestedSize = requestSizeInput.toIntOrNull()
        if (requestedSize == null || requestedSize <= 0) {
            allocationMessage = "Invalid request size. Must be > 0."
            return
        }

        // Increment the sequence number for the new block
        allocationSequence++

        val freeBlocks = memoryBlocks.filter { it.isFree }
        val fitBlock = when (allocationAlgorithm) {
            "First-Fit" -> findFirstFit(freeBlocks, requestedSize)
            "Best-Fit" -> findBestFit(freeBlocks, requestedSize)
            "Worst-Fit" -> findWorstFit(freeBlocks, requestedSize)
            "Next-Fit" -> findNextFit(freeBlocks, requestedSize)
            else -> findFirstFit(freeBlocks, requestedSize) // Default fallback
        }

        if (fitBlock == null) {
            allocationMessage = "Allocation failed: No suitable free block found (External Fragmentation)."
            return
        }

        // --- Execute Allocation ---
        val newBlocks = memoryBlocks.toMutableList()
        val index = newBlocks.indexOf(fitBlock)
        if (index == -1) return // Should not happen

        // Process ID for the new block
        val newProcessId = "P$nextProcessId"
        nextProcessId++

        val allocatedSize = requestedSize
        val remainingSize = fitBlock.size - allocatedSize

        if (remainingSize >= 0) {
            // 1. New Allocated Block
            newBlocks[index] = MemoryBlock(
                id = newProcessId,
                start = fitBlock.start,
                size = allocatedSize,
                isFree = false,
                processSize = requestedSize,
                internalFragmentation = 0, // No internal frag in this simple model
                lastAllocatedTime = allocationSequence // Use the sequence counter
            )

            // 2. Remaining Free Block (if any)
            if (remainingSize > 0) {
                newBlocks.add(index + 1, MemoryBlock(
                    id = "Free",
                    start = fitBlock.start + allocatedSize,
                    size = remainingSize,
                    isFree = true
                ))
            }

            // Update the state
            memoryBlocks = newBlocks.toList().sortedBy { it.start }
            allocationMessage = "Allocated $requestedSize KB to $newProcessId using $allocationAlgorithm."

            // Update Next-Fit tracking
            lastAllocatedBlockStart = newBlocks[index].start

        } else {
            allocationMessage = "Allocation failed: Internal error in fit logic."
        }
        (fragmentationStats as MutableState).value = calculateFragmentationStats()
    }

    // --- Deallocation Logic ---

    fun deallocateProcess(id: String) {
        val newBlocks = memoryBlocks.toMutableList()
        val index = newBlocks.indexOfFirst { it.id == id && !it.isFree }

        if (index == -1) {
            allocationMessage = "Deallocation failed: Process $id not found."
            return
        }

        val deallocatedBlock = newBlocks[index]

        // Convert the allocated block back to a free block
        newBlocks[index] = deallocatedBlock.copy(
            id = "Free",
            isFree = true,
            processSize = 0,
            internalFragmentation = 0,
            lastAllocatedTime = 0L // Reset sequence number
        )

        // --- Coalescing (Merging adjacent free blocks) ---
        var didCoalesce = true
        while (didCoalesce) {
            didCoalesce = false
            for (i in 0 until newBlocks.size - 1) {
                val current = newBlocks[i]
                val next = newBlocks[i + 1]

                if (current.isFree && next.isFree) {
                    // Merge them
                    val mergedBlock = current.copy(
                        size = current.size + next.size
                    )
                    newBlocks[i] = mergedBlock
                    newBlocks.removeAt(i + 1)
                    didCoalesce = true
                    break
                }
            }
        }

        memoryBlocks = newBlocks.toList().sortedBy { it.start }
        allocationMessage = "Deallocated process $id. Free space coalesced."
        (fragmentationStats as MutableState).value = calculateFragmentationStats()
    }

    // --- Allocation Algorithms Implementations ---

    private fun findBestFit(freeBlocks: List<MemoryBlock>, requestedSize: Int): MemoryBlock? {
        return freeBlocks
            .filter { it.size >= requestedSize }
            .minByOrNull { it.size }
    }

    private fun findFirstFit(freeBlocks: List<MemoryBlock>, requestedSize: Int): MemoryBlock? {
        return freeBlocks.firstOrNull { it.size >= requestedSize }
    }

    private fun findWorstFit(freeBlocks: List<MemoryBlock>, requestedSize: Int): MemoryBlock? {
        return freeBlocks
            .filter { it.size >= requestedSize }
            .maxByOrNull { it.size }
    }

    private fun findNextFit(freeBlocks: List<MemoryBlock>, requestedSize: Int): MemoryBlock? {
        val sortedBlocks = freeBlocks.sortedBy { it.start }

        // 1. Find the index where the search should begin (start from or after last allocated)
        val startIndex = sortedBlocks.indexOfFirst { it.start >= lastAllocatedBlockStart }.let {
            if (it == -1) 0 else it
        }

        // 2. Search from the starting index to the end
        for (i in startIndex until sortedBlocks.size) {
            val block = sortedBlocks[i]
            if (block.size >= requestedSize) {
                return block
            }
        }

        // 3. Wrap around and search from the beginning up to the starting index
        for (i in 0 until startIndex) {
            val block = sortedBlocks[i]
            if (block.size >= requestedSize) {
                return block
            }
        }

        return null // No fit found
    }

    // --- Metrics Calculation ---

    private fun calculateFragmentationStats(): FragmentationStats {
        val totalFree = memoryBlocks.filter { it.isFree }.sumOf { it.size }
        val internalFrag = memoryBlocks.filter { !it.isFree }.sumOf { it.internalFragmentation }

        // External fragmentation is calculated as the total free space that is NOT in the largest free block.
        val largestFreeBlock = memoryBlocks.filter { it.isFree }.maxOfOrNull { it.size } ?: 0

        val externalFrag = max(0, totalFree - largestFreeBlock)

        return FragmentationStats(
            totalFree = totalFree,
            internal = internalFrag,
            external = externalFrag
        )
    }
}
