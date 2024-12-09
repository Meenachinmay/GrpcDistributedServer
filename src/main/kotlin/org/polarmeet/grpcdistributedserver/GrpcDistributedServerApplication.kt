package org.polarmeet.grpcdistributedserver

import kotlinx.coroutines.runBlocking
import org.polarmeet.grpcdistributedserver.grpc.StreamingServer
import org.polarmeet.grpcdistributedserver.service.MessageBroker
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import javax.annotation.PostConstruct
import kotlin.system.exitProcess

@SpringBootApplication
class GrpcDistributedServerApplication {

    // Create a component to manage the gRPC server lifecycle
    @Component
    class GrpcServerLifecycle(private val messageBroker: MessageBroker) {
        private lateinit var server: StreamingServer
        private val shutdownLatch = CountDownLatch(1)

        @PostConstruct
        fun initialize() {
            // Initialize system properties for optimal performance
            System.setProperty("io.netty.leakDetection.level", "disabled")
            System.setProperty("io.netty.recycler.maxCapacity", "1000")
            System.setProperty("io.netty.allocator.numHeapArenas",
                Runtime.getRuntime().availableProcessors().toString())

            try {
                // Create the streaming server with the injected MessageBroker
                server = StreamingServer(messageBroker)

                // Add shutdown hook for graceful termination
                Runtime.getRuntime().addShutdownHook(Thread {
                    println("\n📍 Initiating graceful shutdown...")
                    try {
                        shutdownLatch.countDown()
                        println("📍 Server shutdown completed successfully")
                    } catch (e: Exception) {
                        println("❌ Error during shutdown: ${e.message}")
                        e.printStackTrace()
                    }
                })

                println("""
                    |================================================
                    |🚀 Starting High-Performance Streaming Server
                    |================================================
                    |Press Ctrl+C to shutdown
                    |------------------------------------------------
                """.trimMargin())

                // Start the server
                server.start()

            } catch (e: Exception) {
                println("""
                    |❌ Fatal error during server initialization:
                    |   ${e.message}
                    |   Stack trace:
                """.trimMargin())
                e.printStackTrace()
                exitProcess(1)
            }
        }
    }
}

fun main(args: Array<String>): Unit = runBlocking {
    // Start the Spring application
    runApplication<GrpcDistributedServerApplication>(*args)
}