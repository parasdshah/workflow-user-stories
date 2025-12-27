import { Timeline, Text, Badge, Card, Button, Group, Collapse, Loader } from '@mantine/core';
import { IconCheck, IconCircleDashed, IconPlayerPlay, IconX, IconChevronDown, IconChevronRight } from '@tabler/icons-react';
import { useState, useEffect } from 'react';

interface StageDTO {
    stageCode: string;
    stageName: string;
    taskId?: string;
    status: string; // ACTIVE, COMPLETED
    assignee?: string;
    createdTime?: string;
    endTime?: string;
    dueDate?: string;
    allowedActions?: string;
    actionTaken?: string;
    subProcessInstanceId?: string;
}

function NestedTimeline({ caseId }: { caseId: string }) {
    const [stages, setStages] = useState<StageDTO[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch(`/api/runtime/cases/${caseId}/stages`)
            .then(res => res.json())
            .then(data => setStages(data))
            .catch(err => console.error(err))
            .finally(() => setLoading(false));
    }, [caseId]);

    if (loading) return <Loader size="xs" />;

    return (
        <Timeline active={stages.findIndex(s => s.status === 'ACTIVE')} bulletSize={18} lineWidth={1} style={{ marginTop: 10, marginBottom: 10 }}>
            {stages.map((stage, i) => (
                <Timeline.Item
                    key={i}
                    bullet={stage.status === 'COMPLETED' ? <IconCheck size={10} /> : (stage.status === 'ACTIVE' ? <IconPlayerPlay size={10} /> : <IconCircleDashed size={10} />)}
                    color={stage.status === 'COMPLETED' ? 'green' : (stage.status === 'ACTIVE' ? 'blue' : 'gray')}
                    title={<Text size="sm">{stage.stageName}</Text>}
                >
                    {stage.status === 'COMPLETED' && stage.actionTaken && (
                        <Badge size="xs" color={stage.actionTaken === 'REJECT' ? 'red' : 'green'}>{stage.actionTaken}</Badge>
                    )}
                    <Text c="dimmed" size="xs">
                        Assignee: {stage.assignee || 'Unassigned'}
                    </Text>
                    <Text size="xs" mt={4} c="dimmed">
                        Created: {stage.createdTime ? new Date(stage.createdTime).toLocaleString() : '-'}
                    </Text>
                    {stage.endTime && (
                        <Text size="xs" mt={4} c="dimmed">
                            Completed: {new Date(stage.endTime).toLocaleString()}
                        </Text>
                    )}
                </Timeline.Item>
            ))}
        </Timeline>
    );
}

interface CaseTimelineProps {
    stages: StageDTO[];
    onAction?: (taskId: string, outcome?: string) => void;
}

export function CaseTimeline({ stages, onAction }: CaseTimelineProps) {
    // Sort logic handled by backend, but safe to verify if needed

    const getBullet = (status: string) => {
        if (status === 'COMPLETED') return (
            <div style={{ backgroundColor: 'green', borderRadius: '50%', color: 'white', width: 20, height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <IconCheck size={14} />
            </div>
        );
        if (status === 'ACTIVE') return <IconPlayerPlay size={14} />;
        return <IconCircleDashed size={14} />;
    };

    const getColor = (status: string) => {
        if (status === 'COMPLETED') return 'green';
        if (status === 'ACTIVE') return 'blue';
        return 'gray';
    };

    const [expandedSubProcesses, setExpandedSubProcesses] = useState<{ [key: string]: boolean }>({});

    const toggleSubProcess = (processId: string) => {
        setExpandedSubProcesses(prev => ({
            ...prev,
            [processId]: !prev[processId]
        }));
    };

    return (
        <Card withBorder padding="xl" radius="md">
            <Text size="lg" fw={500} mb="xl">Case Progress</Text>
            <Timeline active={stages.findIndex(s => s.status === 'ACTIVE')} bulletSize={24} lineWidth={2}>
                {stages.map((stage, index) => (
                    <Timeline.Item
                        key={`${stage.stageCode}-${index}`}
                        bullet={getBullet(stage.status)}
                        color={getColor(stage.status)}
                        title={stage.stageName}
                    >
                        <Text c="dimmed" size="sm">
                            Assignee: {stage.assignee || 'Unassigned'}
                        </Text>
                        <Text size="xs" mt={4}>
                            Created: {stage.createdTime ? new Date(stage.createdTime).toLocaleString() : '-'}
                        </Text>
                        {stage.endTime && (
                            <Text size="xs" mt={4}>
                                Completed: {new Date(stage.endTime).toLocaleString()}
                            </Text>
                        )}
                        {stage.status === 'COMPLETED' && stage.actionTaken && (
                            <Badge
                                color={stage.actionTaken === 'REJECT' ? 'red' : 'green'}
                                size="sm"
                                mt={4}
                                leftSection={stage.actionTaken === 'REJECT' ? <IconX size={12} /> : <IconCheck size={12} />}
                            >
                                Action: {stage.actionTaken}
                            </Badge>
                        )}
                        {stage.status === 'ACTIVE' && (
                            <Group mt="xs">
                                <Badge color="blue" size="xs">In Progress</Badge>
                                {stage.taskId && onAction && (
                                    <>
                                        {stage.allowedActions ? (
                                            stage.allowedActions.replace(/[\[\]"]/g, '').split(',').map(action => (
                                                <Button
                                                    key={action.trim()}
                                                    size="xs"
                                                    variant="outline"
                                                    color={action.trim() === 'REJECT' ? 'red' : 'blue'}
                                                    onClick={() => onAction(stage.taskId!, action.trim())}
                                                >
                                                    {action.trim()}
                                                </Button>
                                            ))
                                        ) : (
                                            <Button size="xs" variant="outline" onClick={() => onAction(stage.taskId!)}>
                                                Complete
                                            </Button>
                                        )}
                                    </>
                                )}
                            </Group>
                        )}

                        {stage.subProcessInstanceId && (
                            <div style={{ marginTop: 8 }}>
                                <Button
                                    variant="subtle"
                                    size="xs"
                                    onClick={() => toggleSubProcess(stage.subProcessInstanceId!)}
                                    leftSection={expandedSubProcesses[stage.subProcessInstanceId!] ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                                >
                                    {expandedSubProcesses[stage.subProcessInstanceId!] ? 'Hide Sub-process' : 'View Sub-process'}
                                </Button>
                                <Collapse in={expandedSubProcesses[stage.subProcessInstanceId!]}>
                                    <Card withBorder padding="sm" mt="xs" bg="gray.0">
                                        <Text size="xs" fw={500} c="dimmed" mb="xs">Sub-workflow: {stage.subProcessInstanceId}</Text>
                                        {expandedSubProcesses[stage.subProcessInstanceId!] && (
                                            <NestedTimeline caseId={stage.subProcessInstanceId!} />
                                        )}
                                    </Card>
                                </Collapse>
                            </div>
                        )}
                    </Timeline.Item>
                ))}
            </Timeline>
        </Card>
    );
}
