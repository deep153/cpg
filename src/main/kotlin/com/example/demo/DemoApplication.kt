package com.example.demo

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage
import de.fraunhofer.aisec.cpg.persistence.persist
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.http.content.*
import freemarker.cache.ClassTemplateLoader
import org.neo4j.driver.*
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("DemoApplication")

data class AnalyzeRequest(
    val code: String,
    val page: Int = 1
)

data class AnalyzeResponse(
    val result: String? = null,
    val error: String? = null,
    val graphData: GraphData? = null
)

fun main() {
    logger.info("Starting server...")
    try {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
        logger.info("Server started successfully")
    } catch (e: Exception) {
        logger.error("Error starting server: ${e.message}", e)
    }
}

fun Application.module() {
    logger.info("Configuring application module...")
    try {
        install(FreeMarker) {
            logger.info("Configuring FreeMarker...")
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        }

        install(ContentNegotiation) {
            logger.info("Configuring ContentNegotiation...")
            jackson()
        }

        routing {
            logger.info("Configuring routes...")
            
            // Add static file serving
            static("/") {
                logger.info("Configuring static file serving...")
                resources("static")
            }

            get("/") {
                logger.info("Handling root request...")
                try {
                    call.respond(FreeMarkerContent("index.ftl", mapOf("code" to "")))
                } catch (e: Exception) {
                    logger.error("Error serving template: ${e.message}", e)
                    throw e
                }
            }

            post("/analyze") {
                try {
                    val request = call.receive<AnalyzeRequest>()

                    if (request.code.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, AnalyzeResponse(error = "Code cannot be empty"))
                        return@post
                    }

                    // Create temporary files for Rust and LLVM IR
                    val rustFile = File.createTempFile("temp", ".rs")
                    val llvmFile = File.createTempFile("temp", ".ll")
                    rustFile.writeText(request.code)

                    try {
                        // Compile Rust to LLVM IR
                        logger.info("Compiling Rust code to LLVM IR...")
                        val process = ProcessBuilder()
                            .command("rustc", "--emit=llvm-ir", rustFile.absolutePath, "-o", llvmFile.absolutePath)
                            .redirectErrorStream(true)
                            .start()

                        val exitCode = process.waitFor()
                        if (exitCode != 0) {
                            val error = process.inputStream.bufferedReader().readText()
                            logger.error("Compilation failed: $error")
                            call.respond(HttpStatusCode.BadRequest, AnalyzeResponse(
                                error = "Rust compilation failed: $error"
                            ))
                            return@post
                        }

                        logger.info("Compilation successful, running CPG analysis...")

                        // Configure and run CPG analysis with LLVM IR
                        val config = TranslationConfiguration.builder()
                            .sourceLocations(listOf(llvmFile))
                            .defaultPasses()
                            .registerLanguage<LLVMIRLanguage>()
                            .build()

                        val analyzer = TranslationManager.builder()
                            .config(config)
                            .build()


                        val result = analyzer.analyze().get()

                        // Use CPG's built-in Neo4j persistence
                        try {
                            logger.info("Persisting translation result to Neo4j...")
//                            val neo4jConfig = Neo4jConfig.getInstance()
                            GraphDatabase.driver(
                                "neo4j://localhost:7687",
                                AuthTokens.basic("neo4j", "password")
                            ).use { driver ->

                                val session = driver.session()

                                with(session) {
                                    session.run("MATCH (n) DETACH DELETE n").consume()
                                    result.persist()
                                }
                            }
                            logger.info("Successfully persisted translation result to Neo4j")
                        } catch (e: Exception) {
                            logger.error("Failed to persist to Neo4j: ${e.message}", e)
                            // Continue with the response even if Neo4j persistence fails
                        }

                        // Get graph data for UI visualization
                        //val completeGraphData = GraphConverter.getCompleteGraphData(result)

                        call.respond(AnalyzeResponse(
                            result = "Analysis completed successfully.",
                            graphData = null
                        ))
                    } catch (e: Exception) {
                        logger.error("Analysis error ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, AnalyzeResponse(
                            error = "Analysis error: ${e.message}"
                        ))
                    } finally {
                        rustFile.delete()
                        llvmFile.delete()
                    }
                } catch (e: Exception) {
                    logger.error("Invalid request: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, AnalyzeResponse(
                        error = "Invalid request: ${e.message}"
                    ))
                }
            }
        }
        logger.info("Application module configured successfully")
    } catch (e: Exception) {
        logger.error("Error configuring application module: ${e.message}", e)
    }
}

 