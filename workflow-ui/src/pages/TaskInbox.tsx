
import { Container, Title, Accordion, Badge, Group, Text, Loader, Button, Tabs } from '@mantine/core';
import { IconCheck, IconX } from '@tabler/icons-react';
import { useState, useEffect } from 'react';

interface CaseDto {
    id: string;
    workflowCode: string;
    startTime: string;
}

interface TaskDto {
    id: string;
    name: string;
    assignee: string;
    created: string;
}

// Reuse or new interface for History Items
interface HistoricStageDto {
    taskId: string;
    stageName: string;
    stageCode: string;
    status: string;
    assignee: string;
    createdTime?: string;
    endTime?: string;
    caseId: string;
    workflowCode: string;
    actionTaken?: string;
}

function TaskInbox() {
    const [activeTab, setActiveTab] = useState<string | null>('active');
    const [cases, setCases] = useState<CaseDto[]>([]);
    const [tasks, setTasks] = useState<{ [key: string]: TaskDto[] }>({});
    const [loadingTasks, setLoadingTasks] = useState<{ [key: string]: boolean }>({});

    // History State
    const [history, setHistory] = useState<HistoricStageDto[]>([]);
    const [loadingHistory, setLoadingHistory] = useState(false);

    useEffect(() => {
        if (activeTab === 'active') {
            fetch('/api/runtime/cases')
                .then(res => res.json())
                .then(data => setCases(data))
                .catch(err => console.error("Error fetching cases:", err));
        } else if (activeTab === 'history') {
            setLoadingHistory(true);
            // TODO: Get real userId. For now "user"
            const userId = "user"; // Or "test-user"
            fetch(`/api/runtime/cases/tasks/history?userId=${userId}`)
                .then(res => {
                    if (!res.ok) throw new Error(`Status: ${res.status}`);
                    return res.json();
                })
                .then(data => {
                    if (Array.isArray(data)) {
                        setHistory(data);
                    } else {
                        console.error("History data is not an array:", data);
                        setHistory([]);
                    }
                })
                .catch(err => {
                    console.error("Error fetching history:", err);
                    setHistory([]);
                })
                .finally(() => setLoadingHistory(false));
        }
    }, [activeTab]);

    const handleAccordionChange = (caseId: string | null) => {
        if (!caseId) return;
        if (tasks[caseId]) return; // Already fetched

        setLoadingTasks(prev => ({ ...prev, [caseId]: true }));
        fetch(`/api/runtime/cases/${caseId}/stages`)
            .then(res => res.json())
            .then(data => {
                setTasks(prev => ({ ...prev, [caseId]: data }));
            })
            .catch(err => console.error(`Error fetching stages for ${caseId}:`, err))
            .finally(() => {
                setLoadingTasks(prev => ({ ...prev, [caseId]: false }));
            });
    };

    // Group history by Case for better view
    // Group history by Case for better view
    const groupedHistory = Array.isArray(history) ? history.reduce((acc, stage) => {
        if (!stage.caseId) return acc; // Skip if no caseId
        if (!acc[stage.caseId]) {
            acc[stage.caseId] = [];
        }
        acc[stage.caseId].push(stage);
        return acc;
    }, {} as { [key: string]: HistoricStageDto[] }) : {};

    return (
        <Container size="lg" py="xl">
            <Title order={2} mb="lg">Task Inbox</Title>

            <Tabs value={activeTab} onChange={setActiveTab}>
                <Tabs.List mb="md">
                    <Tabs.Tab value="active">Active Tasks</Tabs.Tab>
                    <Tabs.Tab value="history">History</Tabs.Tab>
                </Tabs.List>

                <Tabs.Panel value="active">
                    {cases.length === 0 ? (
                        <Text c="dimmed">No active cases found.</Text>
                    ) : (
                        <Accordion onChange={handleAccordionChange}>
                            {cases.map((c) => (
                                <Accordion.Item key={c.id} value={c.id}>
                                    <Accordion.Control>
                                        <Group justify="space-between" pr="md">
                                            <Text fw={500}>{c.workflowCode} (ID: {c.id})</Text>
                                            <Group>
                                                <Text size="sm" c="dimmed">Started: {new Date(c.startTime).toLocaleString()}</Text>
                                                <Badge color="green">Active</Badge>
                                                <Button size="xs" variant="light" component="a" href={`/cases/${c.id}`}>View Case</Button>
                                            </Group>
                                        </Group>
                                    </Accordion.Control>
                                    <Accordion.Panel>
                                        <Title order={5} mb="sm">Current Stages (Tasks)</Title>
                                        {loadingTasks[c.id] && <Loader size="sm" />}
                                        {!loadingTasks[c.id] && tasks[c.id]?.length === 0 && <Text size="sm">No active tasks (or case ended).</Text>}
                                        {!loadingTasks[c.id] && tasks[c.id]?.map(t => (
                                            <Group key={t.id} mb="xs" p="xs" style={{ border: '1px solid #eee', borderRadius: '4px' }}>
                                                <Text size="sm" fw={500}>{t.name}</Text>
                                                <Badge variant="outline" size="sm">{t.assignee || 'Unassigned'}</Badge>
                                                <Text size="xs" c="dimmed">Created: {new Date(t.created).toLocaleString()}</Text>
                                            </Group>
                                        ))}
                                    </Accordion.Panel>
                                </Accordion.Item>
                            ))}
                        </Accordion>
                    )}
                </Tabs.Panel>

                <Tabs.Panel value="history">
                    {loadingHistory && <Loader />}
                    {!loadingHistory && history.length === 0 && <Text c="dimmed">No history found.</Text>}
                    {!loadingHistory && Object.keys(groupedHistory).length > 0 && (
                        <Accordion>
                            {Object.keys(groupedHistory).map(caseId => {
                                const caseTasks = groupedHistory[caseId];
                                const firstTask = caseTasks[0]; // Use for metadata
                                return (
                                    <Accordion.Item key={caseId} value={caseId}>
                                        <Accordion.Control>
                                            <Group justify="space-between" pr="md">
                                                <Text fw={500}>{firstTask.workflowCode || 'Unknown Workflow'} (ID: {caseId})</Text>
                                                <Group>
                                                    <Badge color="gray">Ended/History</Badge>
                                                    <Button size="xs" variant="light" component="a" href={`/cases/${caseId}`}>View Case</Button>
                                                </Group>
                                            </Group>
                                        </Accordion.Control>
                                        <Accordion.Panel>
                                            <Title order={5} mb="sm">Completed Tasks</Title>
                                            {caseTasks.map(t => (
                                                <Group key={t.taskId} mb="xs" p="xs" style={{ border: '1px solid #eee', borderRadius: '4px' }}>
                                                    <Text size="sm" fw={500}>{t.stageName}</Text>
                                                    <Badge variant="outline" size="sm">{t.assignee || 'Unassigned'}</Badge>
                                                    <Text size="xs" c="dimmed">End: {t.endTime ? new Date(t.endTime).toLocaleString() : '-'}</Text>
                                                    <Text size="xs" c="dimmed">End: {t.endTime ? new Date(t.endTime).toLocaleString() : '-'}</Text>
                                                    {t.actionTaken && (
                                                        <Badge
                                                            size="xs"
                                                            color={t.actionTaken === 'REJECT' ? 'red' : 'green'}
                                                            leftSection={t.actionTaken === 'REJECT' ? <IconX size={10} /> : <IconCheck size={10} />}
                                                        >
                                                            {t.actionTaken}
                                                        </Badge>
                                                    )}
                                                </Group>
                                            ))}
                                        </Accordion.Panel>
                                    </Accordion.Item>
                                );
                            })}
                        </Accordion>
                    )}
                </Tabs.Panel>
            </Tabs>
        </Container>
    );
}

export default TaskInbox;
