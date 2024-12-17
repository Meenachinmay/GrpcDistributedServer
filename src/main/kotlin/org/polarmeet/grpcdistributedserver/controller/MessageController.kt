package org.polarmeet.grpcdistributedserver.controller

import org.polarmeet.grpcdistributedserver.service.MessageBroker
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/messages")
class MessageController(private val messageBroker: MessageBroker) {

    @PostMapping("/publish")
    suspend fun publishMessage(@RequestBody message: String): ResponseEntity<String> {
        messageBroker.publishMessage(message)
        return ResponseEntity.ok("Message published successfully")
    }
}