package org.polarmeet.grpcdistributedserver.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.processors.PublishProcessor
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.ReactiveSubscription
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.test.DefaultAsserter.fail

class MessageBrokerTest {
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var messageBroker: MessageBroker

    @BeforeEach
    fun setup() {
        redisTemplate = mockk<ReactiveRedisTemplate<String, String>> {
            // Mock publish operation
            every {
                convertAndSend(MessageBroker.CHANNEL, any())
            } returns Mono.just(1L)

            // Mock subscribe operation with a finite stream
            every {
                listenTo(any<PatternTopic>())
            } returns Flux.empty()
        }

        messageBroker = MessageBroker(redisTemplate)
    }

    @Test
    fun `should publish message to Redis`() = runBlocking {
        // Using withTimeout to ensure test completion
        withTimeout(5000) {
            // Given
            val testMessage = "Test message"

            // When
            messageBroker.publishMessage(testMessage)

            // Then
            verify(exactly = 1) {
                redisTemplate.convertAndSend(MessageBroker.CHANNEL, testMessage)
            }
        }
    }

    @Test
    fun `should receive messages from Redis subscription`() = runBlocking {
        // Given
        val testMessage = "Test message"

        // Create a Subject that will let us control message emission precisely
        val messageProcessor = PublishProcessor.create<ReactiveSubscription.ChannelMessage<String, String>>()

        // Create our channel message mock
        val channelMessage = mockk<ReactiveSubscription.ChannelMessage<String, String>> {
            every { message } returns testMessage
            every { channel } returns ByteBuffer.wrap(MessageBroker.CHANNEL.toByteArray()).toString()
        }

        // Create our Redis template mock that will use our controlled processor
        val testTemplate = mockk<ReactiveRedisTemplate<String, String>> {
            every {
                listenTo(any<PatternTopic>())
            } returns Flux.from(messageProcessor)

            every {
                convertAndSend(any(), any())
            } returns Mono.just(1L)
        }

        // Create our message broker with the mocked template
        val testBroker = MessageBroker(testTemplate)

        // Launch a coroutine to collect messages
        val job = launch {
            // We'll use take(1) to automatically complete after receiving one message
            testBroker.messageFlow
                .take(1)
                .collect { message ->
                    assertEquals(testMessage, message)
                }
        }

        // Give the broker a moment to set up its subscription
        delay(100)

        // Emit our test message
        messageProcessor.onNext(channelMessage)

        // Wait for collection to complete with a timeout
        withTimeout(1000) {
            job.join()
        }
    }
}