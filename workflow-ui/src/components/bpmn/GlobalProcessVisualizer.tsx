
import { useEffect, useState, useCallback } from 'react';
import ReactFlow, {
    Background,
    Controls,
    useNodesState,
    useEdgesState,
    Panel,
    useReactFlow,
    ReactFlowProvider
} from 'reactflow';
import 'reactflow/dist/style.css';
import dagre from 'dagre';
import { Button, LoadingOverlay, Alert } from '@mantine/core';
import { IconRefresh, IconAlertCircle } from '@tabler/icons-react';
import {
    BPMNStartNode,
    BPMNEndNode,
    BPMNUserTaskNode,
    BPMNServiceTaskNode,
    BPMNCallActivityNode,
    BPMNGatewayNode,
    BPMNGroupNode
} from './CustomNodes';

interface GraphDTO {
    nodes: any[];
    edges: any[];
}

interface Props {
    workflowCode: string;
}

const nodeTypes = {
    bpmnStart: BPMNStartNode,
    bpmnEnd: BPMNEndNode,
    bpmnUserTask: BPMNUserTaskNode,
    bpmnServiceTask: BPMNServiceTaskNode,
    bpmnCallActivity: BPMNCallActivityNode,
    bpmnGateway: BPMNGatewayNode,
    bpmnGroup: BPMNGroupNode
};

export function GlobalProcessVisualizer({ workflowCode }: Props) {
    return (
        <ReactFlowProvider>
            <GlobalCanvas workflowCode={workflowCode} />
        </ReactFlowProvider>
    );
}

function GlobalCanvas({ workflowCode }: Props) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [nodes, setNodes, onNodesChange] = useNodesState([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState([]);
    const { fitView } = useReactFlow();

    const fetchGraph = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/workflows/${workflowCode}/global-graph`);
            if (!res.ok) throw new Error("Failed to fetch graph");
            const data: GraphDTO = await res.json();

            // Transform nodes to ensure data.label exists (Backend doesn't put label in data for Start/End)
            data.nodes = data.nodes.map(node => ({
                ...node,
                data: {
                    ...(node.data || {}),
                    label: node.label
                }
            }));

            // Layout Logic
            const { nodes: layoutedNodes, edges: layoutedEdges } = layoutGraphRecursively(data.nodes, data.edges);

            setNodes(layoutedNodes);
            setEdges(layoutedEdges);

            setTimeout(() => fitView(), 100);

        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [workflowCode, fitView, setNodes, setEdges]);

    useEffect(() => {
        fetchGraph();
    }, [fetchGraph]);

    return (
        <div style={{ height: 700, border: '1px solid #dee2e6', borderRadius: 8, background: '#f8f9fa', position: 'relative' }}>
            <LoadingOverlay visible={loading} />
            {error && (
                <div style={{ position: 'absolute', top: 10, left: 10, right: 10, zIndex: 10 }}>
                    <Alert icon={<IconAlertCircle size={16} />} title="Error" color="red">
                        {error}
                    </Alert>
                </div>
            )}

            <ReactFlow
                nodes={nodes}
                edges={edges}
                nodeTypes={nodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                fitView
                minZoom={0.1}>
                <Panel position="top-right">
                    <Button
                        leftSection={<IconRefresh size={16} />}
                        variant="light"
                        size="xs"
                        onClick={fetchGraph}>
                        Refresh Map
                    </Button>
                </Panel>
                <Background color="#aaa" gap={16} />
                <Controls />
            </ReactFlow>
        </div>
    );
}

// --- Recursive Layout Logic ---

// Helper: Group nodes by parentId
function groupByParent(nodes: any[]) {
    const groups: Record<string, any[]> = {};
    const roots: any[] = [];

    nodes.forEach(n => {
        const pid = n.parentId;
        if (!pid) {
            roots.push(n);
        } else {
            if (!groups[pid]) groups[pid] = [];
            groups[pid].push(n);
        }
    });
    return { roots, groups };
}

function layoutGraphRecursively(rawNodes: any[], rawEdges: any[]) {
    const { roots, groups } = groupByParent(rawNodes);

    // We start processing from the root level, but to get sizes, we need strict bottom-up.
    // However, knowing the hierarchy is static from `parentId`, we can define a function:
    // processLevel(parentId) -> returns { width, height, nodes }

    // Map to store calculated sizes of Group Nodes
    const groupSizes: Record<string, { width: number, height: number }> = {};

    // Recursive function to layout a specific container
    // Returns the modified nodes for this level AND all children (flattened list)
    function processLevel(levelNodes: any[]): any[] {
        let allProcessedNodes: any[] = [];

        // 1. First, process any node in this level that IS a group (has children)
        // We need to calculate its size BEFORE laying out this level.
        levelNodes.forEach(node => {
            if (node.type === 'bpmnGroup') {
                const children = groups[node.id] || [];

                if (children.length > 0) {
                    // Recurse!
                    const childrenProcessed = processLevel(children);
                    allProcessedNodes = [...allProcessedNodes, ...childrenProcessed];

                    // After recursing, we know the bounding box of children (relative to 0,0)
                    // But wait, Dagre layout inside processLevel sets positions relative to 0,0.
                    // We need to find the max width/height used by children to set the Group Size.

                    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
                    childrenProcessed.forEach(c => {
                        // Only consider direct children for bbox? 
                        // Actually all descendants are relative to their immediate parent in ReactFlow if parentId is set.
                        // So we only look at direct children's positions.
                        if (c.parentId === node.id) {
                            minX = Math.min(minX, c.position.x);
                            minY = Math.min(minY, c.position.y);
                            maxX = Math.max(maxX, c.position.x + c.width || c.position.x + 150); // Fallback width
                            maxY = Math.max(maxY, c.position.y + c.height || c.position.y + 80);
                        }
                    });

                    const padding = 40;
                    const width = (maxX - minX) + padding * 2;
                    const height = (maxY - minY) + padding * 2;

                    groupSizes[node.id] = { width: Math.max(width, 200), height: Math.max(height, 150) };

                    // Also, we might need to shift children if minX < 0? 
                    // Dagre usually starts at 0,0.
                } else {
                    groupSizes[node.id] = { width: 200, height: 100 }; // Empty group default
                }
            }
        });

        // 2. Now Layout *THIS* level using Dagre
        const g = new dagre.graphlib.Graph();
        g.setGraph({ rankdir: 'LR', nodesep: 50, ranksep: 50 });
        g.setDefaultEdgeLabel(() => ({}));

        levelNodes.forEach(node => {
            // Determine dimensions
            let w = 150, h = 80;
            if (node.type === 'bpmnStart' || node.type === 'bpmnEnd' || node.type === 'bpmnGateway') {
                w = 40; h = 40;
            } else if (node.type === 'bpmnGroup') {
                // Use calculated size
                const size = groupSizes[node.id] || { width: 250, height: 150 };
                w = size.width;
                h = size.height;
            }
            g.setNode(node.id, { width: w, height: h });
        });

        // Add edges relevant to this level (source and target both in levelNodes)
        const nodeIds = new Set(levelNodes.map(n => n.id));
        rawEdges.forEach(edge => {
            if (nodeIds.has(edge.source) && nodeIds.has(edge.target)) {
                g.setEdge(edge.source, edge.target);
            }
        });

        dagre.layout(g);

        const currentLevelProcessed = levelNodes.map(node => {
            const pos = g.node(node.id);
            const w = g.node(node.id).width;
            const h = g.node(node.id).height;

            return {
                ...node,
                position: {
                    x: pos.x - w / 2, // Dagre gives center
                    y: pos.y - h / 2
                },
                style: { width: w, height: h } // Explicit size for Groups
            };
        });

        return [...allProcessedNodes, ...currentLevelProcessed];
    }

    // Start processing at roots
    const finalNodes = processLevel(roots);

    // Edges don't need change, ReactFlow handles them by ID.
    // But we need to convert EdgeDTO to ReactFlow Edge format
    const finalEdges = rawEdges.map(e => ({
        ...e,
        markerEnd: { type: 'arrowclosed' },
        style: { stroke: '#555', strokeWidth: 2 }
    }));

    return { nodes: finalNodes, edges: finalEdges };
}
