
import { useMemo } from 'react';
import ReactFlow, {
    Background,
    Controls,
    useNodesState,
    useEdgesState,
    Position,
    MarkerType,
    type Node,
    type Edge
} from 'reactflow';
import 'reactflow/dist/style.css';
import dagre from 'dagre';
import {
    BPMNStartNode,
    BPMNEndNode,
    BPMNUserTaskNode,
    BPMNServiceTaskNode,
    BPMNCallActivityNode,
    BPMNGatewayNode
} from './CustomNodes';

// Define Prop Interfaces locally to match WorkflowEditor
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
}

interface WorkflowMaster {
    workflowCode: string;
    workflowName: string;
    slaDurationDays?: number;
}

interface BpmnVisualizerProps {
    stages: StageConfig[];
    workflow: WorkflowMaster;
}

// Dagre Layouting Logic
const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const getLayoutedElements = (nodes: Node[], edges: Edge[]) => {
    dagreGraph.setGraph({ rankdir: 'LR' }); // Left to Right

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
        node.targetPosition = Position.Left;
        node.sourcePosition = Position.Right;

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

export function BpmnVisualizer({ stages, workflow }: BpmnVisualizerProps) {
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

                edges.push(createEdge(previousNodeId, nodeId));
                previousNodeId = nodeId;
            } else {
                // Parallel Split
                const splitId = `split_${group[0].sequenceOrder}`;
                nodes.push({
                    id: splitId,
                    type: 'bpmnGateway',
                    data: { label: '+' },
                    position: { x: 0, y: 0 }
                });
                edges.push(createEdge(previousNodeId, splitId));

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
                    edges.push(createEdge(splitId, nodeId));
                    edges.push(createEdge(nodeId, joinId));
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

        edges.push(createEdge(previousNodeId, 'end'));

        return { initialNodes: nodes, initialEdges: edges };
    }, [stages, workflow]);

    // Apply Layout
    const layouted = useMemo(() => getLayoutedElements(initialNodes, initialEdges), [initialNodes, initialEdges]);

    const [nodes, , onNodesChange] = useNodesState(layouted.nodes);
    const [edges, , onEdgesChange] = useEdgesState(layouted.edges);

    return (
        <div style={{ height: 600, border: '1px solid #dee2e6', borderRadius: 8 }}>
            <ReactFlow
                nodes={nodes}
                edges={edges}
                nodeTypes={nodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                fitView
            >
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

function createEdge(source: string, target: string): Edge {
    return {
        id: `e-${source}-${target}`,
        source: source,
        target: target,
        type: 'smoothstep',
        markerEnd: { type: MarkerType.ArrowClosed },
        style: { strokeWidth: 2 }
    };
}
