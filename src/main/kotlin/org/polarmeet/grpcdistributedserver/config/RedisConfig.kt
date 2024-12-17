package org.polarmeet.grpcdistributedserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    @Primary
    fun reactiveRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, String> {
        val serializer = StringRedisSerializer()

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, String>()
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}