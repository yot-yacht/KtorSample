package jp.me.samples.ktorsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ktor.client.*
import io.ktor.application.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.post
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.serialization
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create Test Server on App
        val server = embeddedServer(
            Netty,
            port = 8080,
            module = { mymodule() }
        ).apply {
            start(wait = false)
        }

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            engine {
                connectTimeout = 100_000
                socketTimeout = 100_000
            }
        }

        runBlocking {
            val message = client.post<HelloWorld>("http://127.0.0.1:8080/") {
                contentType(ContentType.Application.Json)
                body = HelloWorld("world")
            }

            android.util.Log.e("TAG", "CLIENT: Message from the server: $message")

            client.close()
            server.stop(1L, 1L, TimeUnit.SECONDS)
        }
    }

    @Serializable
    data class HelloWorld(val hello: String)

    private fun Application.mymodule() {
        install(ContentNegotiation) {
            serialization()
        }

        routing {
            post("/") {
                val helloWorld = call.receive<HelloWorld>()
                val json = Json(JsonConfiguration.Stable)
                val message = json.stringify(HelloWorld.serializer(), helloWorld)
                println("SERVER: Message from the client: $message")
                call.respond(HelloWorld(hello = "response"))
            }
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
        }
    }
}
