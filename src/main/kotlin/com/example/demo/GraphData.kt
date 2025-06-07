package com.example.demo

data class GraphNode(
    val id: String,
    val label: String,
    val type: String,
    val properties: Map<String, Any?> = mapOf()
)

data class GraphEdge(
    val id: String,
    val from: String,
    val to: String,
    val label: String,
    val type: String
)

data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val totalNodes: Int,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int
) 