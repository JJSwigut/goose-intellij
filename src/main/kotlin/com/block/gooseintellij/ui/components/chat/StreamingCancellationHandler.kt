package com.block.gooseintellij.ui.components.chat

import kotlinx.coroutines.Job

/**
 * Handles cancellation of streaming operations and manages streaming state
 */
class StreamingCancellationHandler {

    private var currentJob: Job? = null
    private val cancellationCallbacks = mutableListOf<() -> Unit>()

    /**
     * Starts a new streaming operation, cancelling any existing one
     * @param streamingJob The new streaming job to track
     */
    fun startStreaming(streamingJob: Job) {
        cancelCurrent()
        currentJob = streamingJob
    }

    /**
     * Cancels the current streaming operation if any
     */
    fun cancelCurrent() {
        currentJob?.cancel()
        currentJob = null
        
        // Notify all registered callbacks
        cancellationCallbacks.forEach { it() }
        cancellationCallbacks.clear()
    }

    /**
     * Checks if there's an active streaming operation
     * @return true if streaming is active, false otherwise
     */
    fun isStreaming(): Boolean {
        return currentJob?.isActive == true
    }

    /**
     * Registers a callback to be called when streaming is cancelled
     * @param callback The callback function to execute on cancellation
     */
    fun onCancellation(callback: () -> Unit) {
        cancellationCallbacks.add(callback)
    }

    /**
     * Cleans up all resources and cancels any active streaming
     */
    fun cleanup() {
        cancelCurrent()
        cancellationCallbacks.clear()
    }

    /**
     * Gets the current streaming job for direct access if needed
     * @return The current streaming job or null if none active
     */
    fun getCurrentJob(): Job? = currentJob
}