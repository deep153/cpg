package com.example.demo.config

import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.persistence.persist
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.config.*
import org.neo4j.driver.GraphDatabase
import org.slf4j.LoggerFactory

/**
 * Configuration class for Neo4j connection settings.
 * Loads settings from .env file, environment variables, and application.conf as fallback.
 * 
 * To use this configuration:
 * 1. Create a .env file in the project root
 * 2. Add the following variables (customize the values):
 *    NEO4J_URL=bolt://localhost:7687
 *    NEO4J_USERNAME=neo4j
 *    NEO4J_PASSWORD=your_password
 *    NEO4J_BATCH_SIZE=1000
 *    NEO4J_TIMEOUT_SECONDS=300
 */
class Neo4jConfig {
    private val logger = LoggerFactory.getLogger(Neo4jConfig::class.java)
    private val config = HoconApplicationConfig(ConfigFactory.load())
    private val dotenv = dotenv {
        // Don't throw if .env file is missing
        ignoreIfMissing = true
    }

    private val PROTOCOL = "neo4j://"

    private val DEFAULT_HOST = "localhost"
    private val DEFAULT_PORT = 7474
    private val DEFAULT_USER_NAME = "neo4j"
    private val DEFAULT_PASSWORD = "password"
    private val DEFAULT_SAVE_DEPTH = -1
    private val DEFAULT_MAX_COMPLEXITY = -1

    val url: String
        get() = getConfigValue(
            envKey = "NEO4J_URL",
            configPath = "neo4j.url",
            defaultValue = "localhost:7474"
        )

    val username: String
        get() = getConfigValue(
            envKey = "NEO4J_USERNAME",
            configPath = "neo4j.username",
            defaultValue = "neo4j"
        )

    val password: String
        get() = getConfigValue(
            envKey = "NEO4J_PASSWORD",
            configPath = "neo4j.password",
            defaultValue = "password"
        )

    val batchSize: Int
        get() = getConfigValue(
            envKey = "NEO4J_BATCH_SIZE",
            configPath = "neo4j.batch-size",
            defaultValue = "1000"
        ).toInt()

    val timeoutSeconds: Long
        get() = getConfigValue(
            envKey = "NEO4J_TIMEOUT_SECONDS",
            configPath = "neo4j.timeout-seconds",
            defaultValue = "300"
        ).toLong()

//    fun pushToNeo4j(translationResult: TranslationResult) {
//        val session = connect()
//        with(session) {
//            executeWrite { tx -> tx.run("MATCH (n) DETACH DELETE n").consume() }
//            translationResult.persist()
//        }
//        session.close()
//    }
//
//    fun connect(): org.neo4j.driver.Session {
//        val driver =
//            GraphDatabase.driver(
//                "${PROTOCOL}$DEFAULT_HOST:$DEFAULT_PORT",
//                org.neo4j.driver.AuthTokens.basic(DEFAULT_USER_NAME, DEFAULT_PASSWORD),
//            )
//        driver.verifyConnectivity()
//        return driver.session()
//    }

    private fun getConfigValue(envKey: String, configPath: String, defaultValue: String): String {
        // Priority: Environment Variable > .env file > application.conf > default value
        return System.getenv(envKey)
            ?: dotenv[envKey]
            ?: try { config.property(configPath).getString() } catch (e: Exception) { null }
            ?: defaultValue
    }

    companion object {
        private var instance: Neo4jConfig? = null

        @Synchronized
        fun getInstance(): Neo4jConfig {
            if (instance == null) {
                instance = Neo4jConfig()
            }
            return instance!!
        }
    }
} 