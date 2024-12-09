package org.polarmeet.grpcdistributedserver.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.stereotype.Service

@Service
class MessageBroker(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val _messageFlow = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 100000
    )
    val messageFlow = _messageFlow.asSharedFlow()

    companion object {
        const val CHANNEL = "realtime-messages"
    }

    init {
        // Subscribe to Redis channel using reactive API
        redisTemplate.listenTo(PatternTopic(CHANNEL))
            .map { message -> message.message }
            .subscribe { message ->
                _messageFlow.tryEmit(message)
            }
    }

    suspend fun publishMessage(message: String) {
        redisTemplate.convertAndSend(CHANNEL, message).awaitSingle()
    }
}