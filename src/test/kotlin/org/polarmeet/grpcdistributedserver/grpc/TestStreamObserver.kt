package org.polarmeet.grpcdistributedserver.grpc

import io.grpc.stub.StreamObserver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * A test helper class that implements StreamObserver and provides methods to verify streaming responses.
 * This class helps us track the completion, errors, and received messages during gRPC streaming tests.
 */
class TestStreamObserver<T> : StreamObserver<T> {
    private val latch = CountDownLatch(1)
    private val error = AtomicReference<Throwable>()
    private val values = mutableListOf<T>()
    private var completed = false

    override fun onNext(value: T) {
        synchronized(values) {
            values.add(value)
        }
    }

    override fun onError(t: Throwable) {
        error.set(t)
        latch.countDown()
    }

    override fun onCompleted() {
        completed = true
        latch.countDown()
    }

    /**
     * Waits for the stream to complete or timeout
     * @param timeout how long to wait
     * @param unit the time unit for the timeout
     * @return true if the stream completed, false if it timed out
     */
    fun await(timeout: Long, unit: TimeUnit): Boolean {
        return latch.await(timeout, unit)
    }

    /**
     * Checks if the stream encountered any errors
     */
    fun hasError(): Boolean {
        return error.get() != null
    }

    /**
     * Gets the error if one occurred
     */
    fun getError(): Throwable? {
        return error.get()
    }

    /**
     * Gets all received values
     */
    fun getValues(): List<T> {
        synchronized(values) {
            return values.toList()
        }
    }

    /**
     * Checks if the stream completed successfully
     */
    fun isCompleted(): Boolean {
        return completed
    }
}