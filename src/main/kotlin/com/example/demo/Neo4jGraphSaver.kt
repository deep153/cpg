package com.example.demo

import com.example.demo.config.Neo4jConfig
import org.neo4j.driver.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.min

class Neo4jGraphSaver(
    private val config: Neo4jConfig = Neo4jConfig.getInstance()
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(Neo4jGraphSaver::class.java)
    private val driver: Driver = GraphDatabase.driver(
        config.url,
        AuthTokens.basic(config.username, config.password),
        Config.builder()
            .withConnectionTimeout(30, TimeUnit.SECONDS)
            .withMaxTransactionRetryTime(config.timeoutSeconds, TimeUnit.SECONDS)
            .build()
    )

    /**
     * Saves graph data to Neo4j in batches with proper error handling and retries
     */
    fun saveGraphData(graphData: GraphData) {
        try {
            clearExistingData()
            processNodes(graphData.nodes)
            processEdges(graphData.edges)
            logger.info("Successfully saved graph data to Neo4j")
        } catch (e: Exception) {
            logger.error("Failed to save graph data to Neo4j", e)
            throw e
        }
    }

    private fun clearExistingData() {
        driver.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE).build()).use { session ->
            session.executeWrite { tx ->
                tx.run("MATCH (n) DETACH DELETE n").consume()
            }
        }
    }

    private fun processNodes(nodes: List<GraphNode>) {
        val totalNodes = nodes.size
        val batchSize = config.batchSize
        logger.info("Processing $totalNodes nodes in batches of $batchSize")

        for (i in nodes.indices step batchSize) {
            val endIndex = min(i + batchSize, totalNodes)
            val batch = nodes.subList(i, endIndex)
            
            driver.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE).build()).use { session ->
                session.executeWrite { tx ->
                    val query = """
                        UNWIND ${'$'}nodes AS node
                        CREATE (n:Node)
                        SET n = node
                    """
                    val params = mapOf("nodes" to batch.map { node ->
                        buildMap {
                            put("id", node.id)
                            put("label", node.label)
                            put("type", node.type)
                            // Only include non-null properties
                            node.properties?.filterValues { value -> 
                                value != null && value.toString() != "NO_VALUE" 
                            }?.let { validProps ->
                                if (validProps.isNotEmpty()) {
                                    put("properties", validProps)
                                }
                            }
                        }
                    })
                    tx.run(query, params).consume()
                    logger.debug("Processed nodes batch (${i + 1} to $endIndex of $totalNodes)")
                }
            }
        }
        logger.info("Completed processing all nodes")
    }

    private fun processEdges(edges: List<GraphEdge>) {
        val totalEdges = edges.size
        val batchSize = config.batchSize
        logger.info("Processing $totalEdges edges in batches of $batchSize")

        for (i in edges.indices step batchSize) {
            val endIndex = min(i + batchSize, totalEdges)
            val batch = edges.subList(i, endIndex)
            
            driver.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE).build()).use { session ->
                session.executeWrite { tx ->
                    val query = """
                        UNWIND ${'$'}edges AS edge
                        MATCH (from:Node {id: edge.from})
                        MATCH (to:Node {id: edge.to})
                        CREATE (from)-[r:RELATES {
                            id: edge.id,
                            label: edge.label,
                            type: edge.type
                        }]->(to)
                    """
                    val params = mapOf("edges" to batch.map { edge ->
                        mapOf(
                            "id" to edge.id,
                            "from" to edge.from,
                            "to" to edge.to,
                            "label" to edge.label,
                            "type" to edge.type
                        )
                    })
                    tx.run(query, params).consume()
                    logger.debug("Processed edges batch (${i + 1} to $endIndex of $totalEdges)")
                }
            }
        }
        logger.info("Completed processing all edges")
    }

    private fun GraphNode.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "label" to label,
        "type" to type,
        "properties" to (properties ?: emptyMap())
    )

    private fun GraphEdge.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "from" to from,
        "to" to to,
        "label" to label,
        "type" to type
    )

    override fun close() {
        driver.close()
    }

    companion object {
        /**
         * Creates a Neo4jGraphSaver instance and uses it to save the graph data
         */
        fun saveGraph(
            graphData: GraphData,
            config: Neo4jConfig = Neo4jConfig.getInstance()
        ) {
            Neo4jGraphSaver(config).use { saver ->
                saver.saveGraphData(graphData)
            }
        }
    }
} 