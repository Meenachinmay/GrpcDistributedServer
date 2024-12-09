package org.polarmeet.grpcdistributedserver.controller

import org.polarmeet.grpcdistributedserver.service.MessageBroker
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux

@RestController
class SSEController(private val messageBroker: MessageBroker) {

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamMessages(): Flux<String> {
        return messageBroker.messageFlow
            .map { message ->
                """
                data: $message
                
                """
            }
            .asFlux()
    }
}