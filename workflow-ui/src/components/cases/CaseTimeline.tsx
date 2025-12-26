import { Timeline, Text, Badge, Card, Button, Group } from '@mantine/core';
import { IconCheck, IconCircleDashed, IconPlayerPlay } from '@tabler/icons-react';

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
                            <Badge color={stage.actionTaken === 'REJECT' ? 'red' : 'green'} size="sm" mt={4}>
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
                    </Timeline.Item>
                ))}
            </Timeline>
        </Card>
    );
}
