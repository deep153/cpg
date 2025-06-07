import { NVL } from '@neo4j-nvl/base';

let nvl = null;
let currentPage = 1;
let currentData = null;
let currentNodes = null
let currentEdges = null
let currentZoom = 1.0;

function showLoading(message = 'Analyzing code...') {
    document.getElementById('loading').style.display = 'block';
    document.getElementById('loading').textContent = message;
}

function hideLoading() {
    document.getElementById('loading').style.display = 'none';
}

function showError(message) {
    const resultMessage = document.getElementById('result-message');
    resultMessage.textContent = message;
    resultMessage.className = 'result-message error';
    resultMessage.style.display = 'block';
}

function updatePagination() {
    if (!currentData) return;

    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const pageInfo = document.getElementById('page-info');
    const stats = document.getElementById('stats');

    prevBtn.disabled = currentPage === 1;
    nextBtn.disabled = !currentData.hasMore;
    pageInfo.textContent = 'Page ' + currentPage;
    stats.textContent = 'Showing ' + currentData.nodes.length + ' of ' + currentData.totalNodes + ' nodes';
}

function updateZoomLevel() {
    const zoomLevelElement = document.getElementById('zoom-level');
    zoomLevelElement.textContent = `${Math.round(currentZoom * 100)}%`;
}

function zoomIn() {
    if (nvl) {
        currentZoom = Math.min(currentZoom * 1.2, 3.0); // Max zoom 300%
        nvl.setZoom(currentZoom);
        updateZoomLevel();
    }
}

function zoomOut() {
    if (nvl) {
        currentZoom = Math.max(currentZoom / 1.2, 0.3); // Min zoom 30%
        nvl.setZoom(currentZoom);
        updateZoomLevel();
    }
}

function initialize() {
    console.log("Initializing visualization module");
    const analyzeButton = document.getElementById('analyze-button');
    const codeEditor = document.getElementById('code-editor');
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const zoomInButton = document.querySelector('.zoom-button:last-child');
    const zoomOutButton = document.querySelector('.zoom-button:first-child');

    // Create full screen button
    const fullScreenButton = document.createElement('button');
    fullScreenButton.innerHTML = '⛶';  // Unicode full screen icon
    fullScreenButton.className = 'full-screen-button';
    fullScreenButton.title = 'View Full Screen';
    document.getElementById('visualization').parentElement.appendChild(fullScreenButton);

    // Create overlay elements
    const overlay = document.createElement('div');
    overlay.className = 'visualization-overlay';
    overlay.style.display = 'none';
    
    const overlayContent = document.createElement('div');
    overlayContent.className = 'visualization-overlay-content';
    
    const closeButton = document.createElement('button');
    closeButton.innerHTML = '×';
    closeButton.className = 'overlay-close-button';

    // Create zoom controls for full screen
    const fullScreenControls = document.createElement('div');
    fullScreenControls.className = 'full-screen-controls';
    
    const fullScreenZoomOutButton = document.createElement('button');
    fullScreenZoomOutButton.innerHTML = '−';
    fullScreenZoomOutButton.className = 'zoom-button';
    fullScreenZoomOutButton.title = 'Zoom Out';
    
    const fullScreenZoomLevel = document.createElement('span');
    fullScreenZoomLevel.className = 'zoom-level';
    fullScreenZoomLevel.textContent = '100%';
    
    const fullScreenZoomInButton = document.createElement('button');
    fullScreenZoomInButton.innerHTML = '+';
    fullScreenZoomInButton.className = 'zoom-button';
    fullScreenZoomInButton.title = 'Zoom In';
    
    fullScreenControls.appendChild(fullScreenZoomOutButton);
    fullScreenControls.appendChild(fullScreenZoomLevel);
    fullScreenControls.appendChild(fullScreenZoomInButton);
    
    const fullScreenVisualization = document.createElement('div');
    fullScreenVisualization.id = 'full-screen-visualization';
    
    overlayContent.appendChild(closeButton);
    overlayContent.appendChild(fullScreenControls);
    overlayContent.appendChild(fullScreenVisualization);
    overlay.appendChild(overlayContent);
    document.body.appendChild(overlay);

    // Add event listeners for full screen functionality
    let fullScreenNvl = null;  // Separate instance for full screen
    let fullScreenZoom = 1.0;  // Track zoom level for full screen

    function updateFullScreenZoomLevel() {
        fullScreenZoomLevel.textContent = `${Math.round(fullScreenZoom * 100)}%`;
    }

    function fullScreenZoomIn() {
        if (nvl) {
            fullScreenZoom = Math.min(fullScreenZoom * 1.2, 3.0); // Max zoom 300%
            nvl.setZoom(fullScreenZoom);
            updateFullScreenZoomLevel();
        }
    }

    function fullScreenZoomOut() {
        if (nvl) {
            fullScreenZoom = Math.max(fullScreenZoom / 1.2, 0.3); // Min zoom 30%
            nvl.setZoom(fullScreenZoom);
            updateFullScreenZoomLevel();
        }
    }

    function closeFullScreen() {
        overlay.style.display = 'none';
        let normalZoom = Math.max(1.0 / 1.2, 0.3); // Min zoom 30%
        nvl.destroy();
        initializeVisualization(currentData);
    }

    fullScreenButton.addEventListener('click', () => {
        overlay.style.display = 'flex';
        fullScreenZoom = 1.0;  // Reset zoom level
        updateFullScreenZoomLevel();
        if (currentData) {

            if(nvl){
                nvl.destroy()
            }

            renderGraph(fullScreenVisualization, currentNodes, currentEdges)

            // fullScreenNvl = new NVL(fullScreenVisualization, currentData.nodes, currentData.edges, {
            //     nodeStyle: nvl.getCurrentOptions().nodeStyle,
            //     relationshipStyle: nvl.getCurrentOptions().relationshipStyle,
            //     layout: nvl.getCurrentOptions().layout,
            //     initialZoom: 1.0,
            //     minZoom: 0.1,
            //     maxZoom: 3.0,
            //     allowDynamicMinZoom: true,
            //     renderer: 'canvas',
            //     callbacks: {
            //         onLayoutDone: () => {
            //             if (currentData.nodes.length > 0) {
            //                 fullScreenNvl.fit(currentData.nodes.map(node => node.id));
            //             }
            //         }
            //     }
            // });
        }
    });

    // Add zoom control event listeners
    fullScreenZoomInButton.addEventListener('click', fullScreenZoomIn);
    fullScreenZoomOutButton.addEventListener('click', fullScreenZoomOut);
    closeButton.addEventListener('click', closeFullScreen)

    // Add mouse wheel zoom support for full screen
    fullScreenVisualization.addEventListener('wheel', (event) => {
        if (event.ctrlKey || event.metaKey) {
            event.preventDefault();
            if (event.deltaY < 0) {
                fullScreenZoomIn();
            } else {
                fullScreenZoomOut();
            }
        }
    });

    // Update styles to include full screen controls
    styles.textContent += `
        .full-screen-controls {
            position: absolute;
            top: 10px;
            left: 50%;
            transform: translateX(-50%);
            display: flex;
            align-items: center;
            background: white;
            padding: 5px;
            border-radius: 4px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            z-index: 1001;
        }

        .full-screen-controls .zoom-button {
            background: white;
            border: 1px solid #ccc;
            border-radius: 4px;
            padding: 5px 10px;
            margin: 0 5px;
            cursor: pointer;
            font-size: 16px;
        }

        .full-screen-controls .zoom-button:hover {
            background: #f0f0f0;
        }

        .full-screen-controls .zoom-level {
            margin: 0 10px;
            font-size: 14px;
            color: #333;
        }
    `;

    if (!analyzeButton || !codeEditor) {
        console.error("Required elements not found!");
        return;
    }

    analyzeButton.addEventListener('click', async () => {
        console.log("Analyze button clicked");
        const code = codeEditor.value.trim();
        if (!code) {
            showError('Please enter some code to analyze.');
            return;
        }

        try {
            analyzeButton.disabled = true;
            analyzeButton.textContent = 'Analyzing...';
            await loadPage(1);
        } catch (error) {
            showError('An error occurred while analyzing the code.');
            console.error('Error:', error);
        } finally {
            analyzeButton.disabled = false;
            analyzeButton.textContent = 'Analyze Code';
        }
    });

    if (prevBtn) prevBtn.addEventListener('click', loadPreviousPage);
    if (nextBtn) nextBtn.addEventListener('click', loadNextPage);

    if (zoomInButton) {
        zoomInButton.addEventListener('click', zoomIn);
    }

    if (zoomOutButton) {
        zoomOutButton.addEventListener('click', zoomOut);
    }

    // Add mouse wheel zoom support
    const visualizationContainer = document.getElementById('visualization');
    if (visualizationContainer) {
        visualizationContainer.addEventListener('wheel', (event) => {
            if (event.ctrlKey || event.metaKey) { // Only zoom when Ctrl/Cmd is pressed
                event.preventDefault();
                if (event.deltaY < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }
        });
    }

    console.log("Visualization module initialized");
}

// Add styles to the document
const styles = document.createElement('style');
styles.textContent = `
    .full-screen-button {
        position: absolute;
        top: 10px;
        right: 10px;
        background: #fff;
        border: 1px solid #ccc;
        border-radius: 4px;
        padding: 5px 10px;
        cursor: pointer;
        font-size: 16px;
        z-index: 100;
    }

    .full-screen-button:hover {
        background: #f0f0f0;
    }

    .visualization-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.8);
        display: none;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    }

    .visualization-overlay-content {
        background: white;
        padding: 20px;
        border-radius: 8px;
        position: relative;
        width: 90vw;
        height: 90vh;
        display: flex;
        flex-direction: column;
    }

    .overlay-close-button {
        position: absolute;
        top: 10px;
        right: 10px;
        background: none;
        border: none;
        font-size: 24px;
        cursor: pointer;
        color: #333;
    }

    .overlay-close-button:hover {
        color: #000;
    }

    #full-screen-visualization {
        flex: 1;
        width: 100%;
        height: calc(100% - 40px);
        margin-top: 40px;
    }

    .full-screen-controls {
        position: absolute;
        top: 10px;
        left: 50%;
        transform: translateX(-50%);
        display: flex;
        align-items: center;
        background: white;
        padding: 5px;
        border-radius: 4px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        z-index: 1001;
    }

    .full-screen-controls .zoom-button {
        background: white;
        border: 1px solid #ccc;
        border-radius: 4px;
        padding: 5px 10px;
        margin: 0 5px;
        cursor: pointer;
        font-size: 16px;
    }

    .full-screen-controls .zoom-button:hover {
        background: #f0f0f0;
    }

    .full-screen-controls .zoom-level {
        margin: 0 10px;
        font-size: 14px;
        color: #333;
    }
`;
document.head.appendChild(styles);

function initializeVisualization(graphData, customContainer = null) {
    const container = customContainer || document.getElementById('visualization');
    
    if (!container) {
        console.error('Visualization container not found');
        return;
    }

    console.log('Raw graph data:', {
        nodes: graphData.nodes.length,
        edges: graphData.edges.length,
        sampleNode: graphData.nodes[0],
        sampleEdge: graphData.edges[0]
    });

    // First create a map of all nodes for quick lookup
    const nodesMap = new Map();

    // Validate and log node data
    const nodes = graphData.nodes.map((node, index) => {
        if (!node.id) {
            console.error('Node missing ID:', node);
            return null;
        }
        
        // Set color based on node type
        let nodeColor;
        switch(node.type) {
            // Top-level nodes (teal)
            case 'TranslationResult':
            case 'Component':
                nodeColor = '#20B2AA';
                break;
            
            // Declarations (blue)
            case 'TranslationUnitDeclaration':
            case 'RecordDeclaration':
            case 'FieldDeclaration':
            case 'ConstructorDeclaration':
            case 'ParameterDeclaration':
            case 'VariableDeclaration':
                nodeColor = '#4169E1';
                break;
            
            // Expressions (orange)
            case 'ConstructExpression':
            case 'SubscriptExpression':
            case 'MemberExpression':
            case 'CastExpression':
                nodeColor = '#FFA500';
                break;
            
            // Problem nodes (red)
            case 'ProblemExpression':
                nodeColor = '#FF4444';
                break;
            
            // Operators (purple)
            case 'UnaryOperator':
                nodeColor = '#9370DB';
                break;
            
            // References (green)
            case 'Reference':
                nodeColor = '#32CD32';
                break;
            
            // Literals (yellow)
            case 'Literal':
                nodeColor = '#FFD700';
                break;
            
            // Default (gray)
            default:
                nodeColor = '#808080';
        }
        
        const nvlNode = {
            id: node.id,
            color: nodeColor,
            size: (node.type === 'TranslationResult' || node.type === 'Component') ? 40 : 30,
            caption: node.label || node.type,
            properties: {
                ...node.properties,
                label: node.label || node.type,
                type: node.type
            }
        };
        
        console.log(`Node ${node.id} configuration:`, nvlNode);
        
        nodesMap.set(node.id, nvlNode);
        return nvlNode;
    }).filter(node => node !== null);

    console.log('Node map contents:', {
        size: nodesMap.size,
        sampleKeys: Array.from(nodesMap.keys()).slice(0, 5)
    });

    // Then create relationships only between existing nodes
    const relationships = graphData.edges.map((edge, index) => {
        const fromNode = nodesMap.get(edge.from);
        const toNode = nodesMap.get(edge.to);
        
        // console.log(`Processing edge ${index}:`, {
        //     edge,
        //     fromNode: fromNode ? 'exists' : 'missing',
        //     toNode: toNode ? 'exists' : 'missing',
        //     fromId: edge.from,
        //     toId: edge.to
        // });

        if (!fromNode || !toNode) {
            console.warn(`Skipping edge ${edge.id} due to missing nodes:`, {
                edge,
                fromNodeExists: !!fromNode,
                toNodeExists: !!toNode,
                fromId: edge.from,
                toId: edge.to,
                availableNodeIds: Array.from(nodesMap.keys()).slice(0, 10)
            });
            return null;
        }

        return {
            id: edge.id,
            from: edge.from,
            to: edge.to,
        };
    }).filter(rel => rel !== null);

    // Destroy existing visualization if any
    if (nvl) {
        nvl.destroy();
    }

    console.log(nodes)
    console.log(relationships)

    currentNodes = nodes
    currentEdges = relationships

    renderGraph(container, nodes, relationships)
}

function renderGraph(container, nodes, relationships) {
    // Create new visualization
    nvl = new NVL(container, nodes, relationships, {
        nodeStyle: {
            defaultStyle: {
                fontColor: '#000000',
                caption: {
                    visible: true,
                    fontSize: 12,
                    color: '#000000'
                }
            }
        },
        relationshipStyle: {
            defaultStyle: {
                width: 1,
                color: '#666666',
                arrow: true
            }
        },
        layout: {
            name: 'force',
            options: {
                strength: -1000,
                distanceMin: 100,
                distanceMax: 200,
                gravity: 0.1,
                friction: 0.9
            }
        },
        initialZoom: 1.0,
        minZoom: 0.1,
        maxZoom: 3.0,
        allowDynamicMinZoom: true,
        renderer: 'canvas',
        callbacks: {
            onLayoutDone: () => {
                if (nodes.length > 0) {
                    nvl.fit(nodes.map(node => node.id));
                }
            }
        }
    });

    // Debug log the node types we have
    console.log('Node Types Summary:', {
        nodeCount: nodes.length,
        relationshipCount: relationships.length,
        nodeTypes: [...new Set(nodes.map(n => n.properties.type))]
    });

    // Function to center the graph
    function fitGraph() {
        if (nvl && nodes.length > 0) {
            nvl.fit(nodes.map(node => node.id));
        }
    }

    // Center graph after initialization
    setTimeout(fitGraph, 500);
}

async function loadPage(page) {
    showLoading('Loading graph data...');
    try {
        const code = document.getElementById('code-editor').value.trim();
        // console.log('Making API call with code:', code);
        
        const response = await fetch('/analyze', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                code: code,
                page: page
            })
        });

        console.log('Response status:', response.status);

        if (!response.ok) {
            const errorData = await response.text();
            console.error('Server error:', errorData);
            throw new Error(`Analysis failed: ${response.status} ${errorData}`);
        }

        const data = await response.json();
        console.log('Received data:', data);

        if (data.error) {
            throw new Error(data.error);
        }

        currentData = data.graphData;
        currentPage = page;
        initializeVisualization(currentData);
        updatePagination();
    } catch (error) {
        console.error('Error loading page:', error);
        showError(error.message || 'An error occurred while analyzing the code');
    } finally {
        hideLoading();
    }
}

function loadNextPage() {
    if (currentData && currentData.hasMore) {
        loadPage(currentPage + 1);
    }
}

function loadPreviousPage() {
    if (currentPage > 1) {
        loadPage(currentPage - 1);
    }
}

// Call initialize when the module is loaded
initialize();

// Export the necessary functions
export default {
    loadPage,
    loadNextPage,
    loadPreviousPage
}; 