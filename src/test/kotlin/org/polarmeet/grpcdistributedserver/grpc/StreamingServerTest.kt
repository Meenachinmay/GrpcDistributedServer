package org.polarmeet.grpcdistributedserver.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.mockk.mockk
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.polarmeet.grpcdistributedserver.proto.StreamRequest
import org.polarmeet.grpcdistributedserver.proto.StreamResponse
import org.polarmeet.grpcdistributedserver.proto.StreamingServiceGrpc
import org.polarmeet.grpcdistributedserver.service.MessageBroker
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.*
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

class StreamingServerTest {
    private val messageBroker = mockk<MessageBroker>()
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel

    // Helper function to find an available port
    private fun findAvailablePort(): Int {
        return ServerSocket(0).use { socket ->
            socket.localPort
        }
    }

    @BeforeEach
    fun setup() {
        val testPort = findAvailablePort()

        // Get access to the private StreamingServiceImpl class using reflection
        val streamingServerClass = Class.forName("org.polarmeet.grpcdistributedserver.grpc.StreamingServer")
        val serviceImplClass = streamingServerClass.declaredClasses.first { it.simpleName == "StreamingServiceImpl" }

        // Create an instance of StreamingServiceImpl using reflection
        val constructor = serviceImplClass.getDeclaredConstructor(
            CoroutineScope::class.java,
            AtomicInteger::class.java,
            AtomicInteger::class.java,
            AtomicInteger::class.java,
            MessageBroker::class.java
        )
        constructor.isAccessible = true

        val serviceImpl = constructor.newInstance(
            CoroutineScope(Dispatchers.Default),
            AtomicInteger(),
            AtomicInteger(),
            AtomicInteger(),
            messageBroker
        )

        server = ServerBuilder.forPort(testPort)
            .addService(serviceImpl as StreamingServiceGrpc.StreamingServiceImplBase)
            .build()
            .start()

        channel = ManagedChannelBuilder.forAddress("localhost", testPort)
            .usePlaintext()
            .build()
    }

    @AfterEach
    fun tearDown() {
        // Ensure proper cleanup of resources
        runBlocking {
            channel.shutdown()
            server.shutdown()

            withTimeout(5000) {
                while (!channel.isTerminated || !server.isTerminated) {
                    delay(100)
                }
            }
        }
    }

    @Test
    fun `should handle streaming requests successfully`() = runBlocking {
        // Your existing test code remains the same
        val stub = StreamingServiceGrpc.newStub(channel)
        val responseObserver = TestStreamObserver<StreamResponse>()
        val requestObserver = stub.streamData(responseObserver)

        val testMessage = "Test message ${System.currentTimeMillis()}"
        val request = StreamRequest.newBuilder()
            .setData(testMessage)
            .setTimestamp(System.currentTimeMillis())
            .build()

        requestObserver.onNext(request)
        requestObserver.onCompleted()

        assertTrue(
            responseObserver.await(5, TimeUnit.SECONDS),
            "Stream should complete within timeout"
        )

        assertFalse(
            responseObserver.hasError(),
            "Stream should complete without errors: ${responseObserver.getError()?.message ?: ""}"
        )

        val responses = responseObserver.getValues()
        assertTrue(responses.isNotEmpty(), "Should receive at least one response")

        val finalResponse = responses.last()
        assertTrue(finalResponse.success, "Response should indicate success")
        assertEquals(1, finalResponse.totalMessagesProcessed, "Should process exactly one message")
    }
}