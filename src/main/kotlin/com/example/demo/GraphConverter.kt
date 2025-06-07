package com.example.demo

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.edges.edges
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.nodes
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID
import kotlin.jvm.javaClass
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class GraphConverter {
    companion object {
        private val logger = LoggerFactory.getLogger("GraphConverter")
        private val nodeIdCounter = AtomicInteger(0)
        private const val DEFAULT_PAGE_SIZE = 100

        /**
         * Gets complete graph data without pagination
         */
        fun getCompleteGraphData(result: TranslationResult): GraphData {
            val allNodes = result.nodes.toList()
            val totalNodes = allNodes.size
            
            logger.info("Converting all $totalNodes nodes")

            val nodes = allNodes.map { node: Node ->
                val originalId = node.id.toString()
                GraphNode(
                    id = node.id.toString(),
                    label = originalId,
                    type = node.javaClass.simpleName,
                    properties = mapOf(
                        "code" to node.code,
                        "location" to node.location?.toString()
                    )
                )
            }

            val edges = mutableListOf<GraphEdge>()

            val edgeIdCounter = AtomicInteger(0)

            // Process all edges
            allNodes.forEach { node ->

                val edgeId = "e${edgeIdCounter.incrementAndGet()}"

                // Add edges only for nodes that are in our current page
                processEdges(node, node.id.toString(), edges, edgeId)

//                node.edges.forEach { edge ->
//                    val fromId = node.id.toString()
//                    val toId = edge.end.id.toString()
//
//                    edges.add(GraphEdge(
//                        id = "e${edgeId++}",
//                        from = fromId,
//                        to = toId,
//                        label = edge.javaClass.simpleName,
//                        type = edge.javaClass.simpleName
//                    ))
//                }
            }

            logger.info("total nodes: $totalNodes, edges: ${edges.size}")
            return GraphData(
                nodes = nodes,
                edges = edges,
                totalNodes = totalNodes,
                hasMore = false,
                page = 1,
                pageSize = totalNodes
            )
        }

//        fun convertToGraphData(result: TranslationResult, page: Int = 1, pageSize: Int = DEFAULT_PAGE_SIZE): GraphData {
//            val edgeIdCounter = AtomicInteger(0)
//            val nodes = mutableListOf<GraphNode>()
//            val edges = mutableListOf<GraphEdge>()
//            val nodeIdMap = mutableMapOf<String, String>()
//            val allNodes = result.nodes.toList()
//            val totalNodes = allNodes.size
//
//            // Calculate pagination
//            val start = (page - 1) * pageSize
//            val end = minOf(start + pageSize, totalNodes)
//            val hasMore = end < totalNodes
//
//            logger.info("Converting nodes $start to $end of $totalNodes total nodes")
//
//            // Create a set of nodes in the current page for quick lookup
//            val currentPageNodes = allNodes.subList(start, end).map {
//                it.id.toString()
//            }.toSet()
//
//            // Process only the nodes for the current page
//            allNodes.subList(start, end).forEach { node: Node ->
//                val originalId = node.id.toString()
////                val newId = "n${nodeIdCounter.incrementAndGet()}"
////                nodeIdMap[originalId] = newId
//
//                nodes.add(
//                    GraphNode(
//                        id = originalId,
//                        label = node.name.toString(),
//                        type = node.javaClass.simpleName,
//                        properties = mapOf(
//                            "code" to node.code,
//                            "location" to node.location?.toString()
//                        )
//                    )
//                )
//
//                val edgeId = "e${edgeIdCounter.incrementAndGet()}"
//
//                // Add edges only for nodes that are in our current page
//                processEdges(node, originalId, edges, edgeId, currentPageNodes)
//            }
//
//            return GraphData(
//                nodes = nodes,
//                edges = edges,
//                totalNodes = totalNodes,
//                hasMore = hasMore,
//                page = page,
//                pageSize = pageSize
//            )
//        }

        private fun processEdges(
            node: Node,
            nodeId: String,
            edges: MutableList<GraphEdge>,
            edgeId: String,
        ) {
            // Process AST edges
            node.astChildren.forEach { child ->
                addEdge(nodeId, child, "AST", "ast", edges, edgeId)
            }

            // Process DFG edges
            node.nextDFG.forEach { dfgNode ->
                addEdge(nodeId, dfgNode, "DFG", "dfg", edges, edgeId)
            }

            // Process EOG edges
            node.nextEOG.forEach { eogNode ->
                addEdge(nodeId, eogNode, "EOG", "eog",  edges, edgeId)
            }
        }

        private fun addEdge(
            fromId: String,
            toNode: Node,
            label: String,
            type: String,
            edges: MutableList<GraphEdge>,
            edgeId: String,
        ) {
            val toOriginalId = toNode.id.toString()

            edges.add(
                GraphEdge(
                    id = edgeId,
                    from = fromId,
                    to = toOriginalId,
                    label = label,
                    type = type
                )
            )
        }
    }
} 