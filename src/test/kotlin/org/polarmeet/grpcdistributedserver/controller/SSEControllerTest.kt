package org.polarmeet.grpcdistributedserver.controller

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.polarmeet.grpcdistributedserver.service.MessageBroker
import reactor.test.StepVerifier
import java.time.Duration

class SSEControllerTest {
    private val messageBroker = mockk<MessageBroker>()
    private val controller = SSEController(messageBroker)

    @Test
    fun `should stream messages correctly`() {
        // Create a SharedFlow that we can control directly
        val sharedFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 10)

        // Configure our mock to return our controlled SharedFlow
        every { messageBroker.messageFlow } returns sharedFlow

        // Get the result stream from our controller
        val result = controller.streamMessages()

        // Create our StepVerifier with a more controlled approach
        StepVerifier.create(result)
            .then {
                // Emit our first message
                runBlocking { sharedFlow.emit("message1") }
            }
            // Verify the first message arrives correctly
            .expectNext("data: message1\n\n")
            .then {
                // Emit our second message
                runBlocking { sharedFlow.emit("message2") }
            }
            // Verify the second message arrives correctly
            .expectNext("data: message2\n\n")
            // Cancel the stream since we're done testing
            .thenCancel()
            // Complete the verification with a reasonable timeout
            .verify(Duration.ofSeconds(2))
    }
}