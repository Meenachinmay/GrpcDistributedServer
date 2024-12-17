package org.polarmeet.grpcdistributedserver.controller

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.polarmeet.grpcdistributedserver.service.MessageBroker
import org.springframework.http.HttpStatus

class MessageControllerTest {
    private val messageBroker = mockk<MessageBroker>()
    private val controller = MessageController(messageBroker)

    @Test
    fun `should publish message successfully`() = runTest {
        // Given
        val message = "test message"
        coEvery { messageBroker.publishMessage(any()) } returns Unit

        // When
        val response = controller.publishMessage(message)

        // Then
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body == "Message published successfully")
        coVerify(exactly = 1) { messageBroker.publishMessage(message) }
    }
}