
import { useMemo, useEffect, useState } from 'react';
import ReactFlow, {
    useNodesState,
    useEdgesState,
    Position,
    MarkerType,
    type Node,
    type Edge,
    Panel,
    useReactFlow,
    Background,
    Controls,
    ReactFlowProvider
} from 'reactflow';
import 'reactflow/dist/style.css';
import dagre from 'dagre';
import { Group, Tooltip, ActionIcon, SegmentedControl } from '@mantine/core';
import { IconRefresh } from '@tabler/icons-react';
import {
    BPMNStartNode,
    BPMNEndNode,
    BPMNUserTaskNode,
    BPMNServiceTaskNode,
    BPMNCallActivityNode,
    BPMNGatewayNode
} from './CustomNodes';

// Define Prop Interfaces locally to match WorkflowEditor
interface StageAction {
    id?: number;
    actionLabel: string;
    buttonStyle: string;
    targetType: string;
    targetStage?: string;
    postActionStatus?: string;
}

interface StageConfig {
    stageName: string;
    stageCode: string;
    sequenceOrder: number;
    isNestedWorkflow?: boolean;
    nestedWorkflowCode?: string;
    screenCode?: string;
    accessType?: string;
    preEntryHook?: string;
    postEntryHook?: string;
    preExitHook?: string;
    postExitHook?: string;
    allowedActions?: string;
    parallelGrouping?: string;
    slaDurationDays?: number;
    isRuleStage?: boolean;
    ruleKey?: string;
    routingRules?: string;
    actions?: StageAction[];
}

interface WorkflowMaster {
    workflowCode: string;
    workflowName: string;
    slaDurationDays?: number;
}

interface FlowchartVisualizerProps {
    stages: StageConfig[];
    workflow: WorkflowMaster;
}

// Dagre Layouting Logic
const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const getLayoutedElements = (nodes: Node[], edges: Edge[]) => {
    // Top-to-Bottom Layout for "Tree/Flowchart" feel
    dagreGraph.setGraph({ rankdir: 'TB', ranksep: 50, nodesep: 75 });

    nodes.forEach((node) => {
        // Approximate dimensions for layout
        if (node.type === 'bpmnGateway') {
            dagreGraph.setNode(node.id, { width: 40, height: 40 });
        } else if (node.type === 'bpmnStart' || node.type === 'bpmnEnd') {
            dagreGraph.setNode(node.id, { width: 40, height: 40 });
        } else {
            dagreGraph.setNode(node.id, { width: 180, height: 80 });
        }
    });

    edges.forEach((edge) => {
        dagreGraph.setEdge(edge.source, edge.target);
    });

    dagre.layout(dagreGraph);

    nodes.forEach((node) => {
        const nodeWithPosition = dagreGraph.node(node.id);
        node.targetPosition = Position.Top;
        node.sourcePosition = Position.Bottom;

        // Shift slightly to center
        const w = (node.type === 'bpmnGateway' || node.type === 'bpmnStart' || node.type === 'bpmnEnd') ? 20 : 90;
        const h = (node.type === 'bpmnGateway' || node.type === 'bpmnStart' || node.type === 'bpmnEnd') ? 20 : 40;

        node.position = {
            x: nodeWithPosition.x - w,
            y: nodeWithPosition.y - h,
        };
    });

    return { nodes, edges };
};

// Helper to get connected nodes
const getConnectedNodes = (nodeId: string, _nodes: Node[], edges: Edge[]) => {
    const connected = new Set<string>();
    connected.add(nodeId);

    // Direct neighbors only (Incoming and Outgoing)
    edges.forEach(edge => {
        if (edge.source === nodeId) connected.add(edge.target);
        if (edge.target === nodeId) connected.add(edge.source);
    });
    return connected;
};

export function FlowchartVisualizer(props: FlowchartVisualizerProps) {
    return (
        <ReactFlowProvider>
            <FlowchartCanvas {...props} />
        </ReactFlowProvider>
    );
}

function FlowchartCanvas({ stages, workflow }: FlowchartVisualizerProps) {
    const [filterMode, setFilterMode] = useState<string>('ALL'); // ALL, RULES, ACTIONS
    const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
    const { fitView } = useReactFlow();

    const nodeTypes = useMemo(() => ({
        bpmnStart: BPMNStartNode,
        bpmnEnd: BPMNEndNode,
        bpmnUserTask: BPMNUserTaskNode,
        bpmnServiceTask: BPMNServiceTaskNode,
        bpmnCallActivity: BPMNCallActivityNode,
        bpmnGateway: BPMNGatewayNode
    }), []);

    // Transform Stages to Nodes/Edges
    const { initialNodes, initialEdges } = useMemo(() => {
        const nodes: Node[] = [];
        const edges: Edge[] = [];

        // 1. Start Event
        nodes.push({
            id: 'start',
            type: 'bpmnStart',
            data: { label: 'Start' },
            position: { x: 0, y: 0 }
        });

        let previousNodeId = 'start';

        // Sort stages by sequence
        const sortedStages = [...stages].sort((a, b) => a.sequenceOrder - b.sequenceOrder);

        // Group by Sequence
        const groups: StageConfig[][] = [];
        if (sortedStages.length > 0) {
            let currentGroup: StageConfig[] = [];
            let currentSeq = sortedStages[0].sequenceOrder;

            sortedStages.forEach(s => {
                if (s.sequenceOrder !== currentSeq) {
                    groups.push(currentGroup);
                    currentGroup = [];
                    currentSeq = s.sequenceOrder;
                }
                currentGroup.push(s);
            });
            groups.push(currentGroup);
        }

        groups.forEach((group) => {
            if (group.length === 1) {
                // Single Stage
                const stage = group[0];
                const nodeId = stage.stageCode;
                const node = createNode(stage, workflow);
                nodes.push(node);

                edges.push(createEdge(previousNodeId, nodeId, undefined, '#555', 'SEQUENCE'));

                // Routing Logic
                if (stage.routingRules) {
                    try {
                        const rules = JSON.parse(stage.routingRules);
                        if (Array.isArray(rules) && rules.length > 0) {
                            const gatewayId = `split_${nodeId}`;
                            nodes.push({
                                id: gatewayId,
                                type: 'bpmnGateway',
                                data: { label: '?' },
                                position: { x: 0, y: 0 }
                            });
                            edges.push(createEdge(nodeId, gatewayId, undefined, '#555', 'SEQUENCE'));

                            rules.forEach((r: any) => {
                                if (r.targetStageCode) {
                                    edges.push(createEdge(gatewayId, r.targetStageCode, r.condition, '#555', 'RULE'));
                                }
                            });

                            // Default path continues
                            previousNodeId = gatewayId;
                        } else {
                            previousNodeId = nodeId;
                        }
                    } catch (e) {
                        previousNodeId = nodeId;
                    }
                } else {
                    previousNodeId = nodeId;
                }

                if (stage.actions) {
                    stage.actions.forEach(action => {
                        const color = getActionColor(action.buttonStyle);
                        if (action.targetType === 'SPECIFIC' && action.targetStage) {
                            edges.push(createEdge(nodeId, action.targetStage, action.actionLabel, color, 'ACTION'));
                        } else if (action.targetType === 'END') {
                            edges.push(createEdge(nodeId, 'end', action.actionLabel, color, 'ACTION'));
                        }
                    });
                }

            } else {
                // Parallel Split
                const splitId = `split_${group[0].sequenceOrder}`;
                nodes.push({
                    id: splitId,
                    type: 'bpmnGateway',
                    data: { label: '+' },
                    position: { x: 0, y: 0 }
                });
                edges.push(createEdge(previousNodeId, splitId, undefined, '#555', 'SEQUENCE'));

                const joinId = `join_${group[0].sequenceOrder}`;
                nodes.push({
                    id: joinId,
                    type: 'bpmnGateway',
                    data: { label: '+' },
                    position: { x: 0, y: 0 }
                });

                group.forEach(stage => {
                    const nodeId = stage.stageCode;
                    nodes.push(createNode(stage, workflow));
                    edges.push(createEdge(splitId, nodeId, undefined, '#555', 'SEQUENCE'));
                    edges.push(createEdge(nodeId, joinId, undefined, '#555', 'SEQUENCE'));

                    // Action-based Routing in Parallel
                    if (stage.actions) {
                        stage.actions.forEach(action => {
                            const color = getActionColor(action.buttonStyle);
                            if (action.targetType === 'SPECIFIC' && action.targetStage) {
                                edges.push(createEdge(nodeId, action.targetStage, action.actionLabel, color, 'ACTION'));
                            } else if (action.targetType === 'END') {
                                edges.push(createEdge(nodeId, 'end', action.actionLabel, color, 'ACTION'));
                            }
                        });
                    }
                });

                previousNodeId = joinId;
            }
        });

        // 3. End Event
        nodes.push({
            id: 'end',
            type: 'bpmnEnd',
            data: { label: 'End' },
            position: { x: 0, y: 0 }
        });

        edges.push(createEdge(previousNodeId, 'end', undefined, '#555', 'SEQUENCE'));

        return { initialNodes: nodes, initialEdges: edges };
    }, [stages, workflow]);

    // Apply Layout
    const layouted = useMemo(() => getLayoutedElements(initialNodes, initialEdges), [initialNodes, initialEdges]);

    const [nodes, setNodes, onNodesChange] = useNodesState(layouted.nodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(layouted.edges);

    useEffect(() => {
        setNodes(layouted.nodes);
        setEdges(layouted.edges);
    }, [layouted, setNodes, setEdges]);

    // Filter Edges
    const visibleEdges = useMemo(() => {
        if (filterMode === 'ALL') return edges;
        return edges.filter(e => {
            if (filterMode === 'RULES_ONLY') return e.data?.type === 'RULE' || e.data?.type === 'SEQUENCE'; // Keep structure
            if (filterMode === 'ACTIONS_ONLY') return e.data?.type === 'ACTION' || e.data?.type === 'SEQUENCE';
            return true;
        });
    }, [edges, filterMode]);

    // Blur Logic
    const DisplayNodes = useMemo(() => {
        if (!selectedNodeId) return nodes; // No focus

        const connected = getConnectedNodes(selectedNodeId, nodes, edges);
        return nodes.map(node => {
            if (connected.has(node.id)) {
                return { ...node, style: { ...node.style, opacity: 1 } };
            }
            return { ...node, style: { ...node.style, opacity: 0.1, transition: 'opacity 0.2s' } };
        });
    }, [nodes, edges, selectedNodeId]);


    return (
        <div style={{ height: 600, border: '1px solid #dee2e6', borderRadius: 8, background: '#f8f9fa' }}>
            <ReactFlow
                nodes={DisplayNodes}
                edges={visibleEdges}
                nodeTypes={nodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onNodeClick={(_, node) => setSelectedNodeId(node.id)}
                onPaneClick={() => setSelectedNodeId(null)}
                fitView
                attributionPosition="bottom-right"
            >
                <Panel position="top-right">
                    <Group gap="xs">
                        <SegmentedControl
                            size="xs"
                            value={filterMode}
                            onChange={setFilterMode}
                            data={[
                                { label: 'All', value: 'ALL' },
                                { label: 'Rules', value: 'RULES_ONLY' },
                                { label: 'Actions', value: 'ACTIONS_ONLY' }
                            ]}
                        />
                        <Tooltip label="Reset Layout">
                            <ActionIcon variant="light" color="gray" onClick={() => fitView()}>
                                <IconRefresh size={16} />
                            </ActionIcon>
                        </Tooltip>
                    </Group>
                </Panel>
                <Panel position="top-left">
                    <p style={{ margin: 0, fontSize: 12, color: '#888' }}>
                        {selectedNodeId ? 'Focus Mode Active (Click BG to reset)' : 'Click node to focus path'}
                    </p>
                </Panel>
                <Background color="#aaa" gap={16} />
                <Controls />
            </ReactFlow>
        </div>
    );
}

// Helpers
function createNode(stage: StageConfig, workflow: WorkflowMaster): Node {
    let type = 'bpmnUserTask';
    let label = stage.stageName;
    let subLabel = 'User Task';

    if (stage.isNestedWorkflow) {
        type = 'bpmnCallActivity';
        subLabel = `Call: ${stage.nestedWorkflowCode}`;
    } else if (stage.isRuleStage) {
        type = 'bpmnServiceTask'; // Reusing service task style for Rules
        subLabel = `Rule: ${stage.ruleKey}`;
    }

    const hasSla = (stage.slaDurationDays && stage.slaDurationDays > 0) || (workflow.slaDurationDays && workflow.slaDurationDays > 0);

    return {
        id: stage.stageCode,
        type: type,
        data: {
            label: label,
            subLabel: subLabel,
            stage: stage,
            hasBoundary: hasSla
        },
        position: { x: 0, y: 0 }
    };
}

const getActionColor = (style: string) => {
    switch (style) {
        case 'success': return '#40c057';
        case 'danger': return '#fa5252';
        case 'warning': return '#fab005';
        case 'primary': return '#228be6';
        default: return '#555';
    }
};

function createEdge(source: string, target: string, label?: string, color: string = '#555', type: string = 'SEQUENCE'): Edge {
    return {
        id: `e-${source}-${target}-${label || ''}`,
        source: source,
        target: target,
        type: 'default', // Bezier for visibility
        label: label,
        data: { type: type }, // Store type for filtering
        labelStyle: { fill: color, fontWeight: 700 },
        markerEnd: {
            type: MarkerType.ArrowClosed,
            width: 20,
            height: 20,
            color: color
        },
        style: {
            strokeWidth: 2,
            stroke: color
        }
    };
}
