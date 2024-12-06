package org.polarmeet.grpcdistributedserver.model

// Basic message type for streaming
data class StreamMessage(
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Request and Response classes matching our protobuf definitions
data class StreamRequest(
    val clientId: String
)