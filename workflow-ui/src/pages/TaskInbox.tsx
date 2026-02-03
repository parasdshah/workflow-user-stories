
import { Container, Title, Accordion, Badge, Group, Text, Loader, Button, Tabs, ActionIcon, Modal, Code, Tooltip, TextInput } from '@mantine/core';
import { IconCheck, IconX, IconCode } from '@tabler/icons-react';
import React, { useState, useEffect } from 'react';
import { useDisclosure } from '@mantine/hooks';

interface CaseDto {
    caseId: string;
    workflowCode: string;
    startTime: string;
    processVariables?: any;
    workflowName?: string;
    startUserId?: string;
}

interface TaskDto {
    taskId: string;
    stageName: string;
    assignee: string;
    createdTime: string;
    candidateGroups?: string[];
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
    parentCaseId?: string;
    parentWorkflowCode?: string;
    parentWorkflowName?: string;
    processVariables?: any;
}

function TaskInbox() {
    const [activeTab, setActiveTab] = useState<string | null>('active');
    const [cases, setCases] = useState<CaseDto[]>([]);
    const [tasks, setTasks] = useState<{ [key: string]: TaskDto[] }>({});
    const [loadingTasks, setLoadingTasks] = useState<{ [key: string]: boolean }>({});

    // History State
    const [history, setHistory] = useState<HistoricStageDto[]>([]);
    const [loadingHistory, setLoadingHistory] = useState(false);

    // Variables Modal
    const [opened, { open, close }] = useDisclosure(false);
    const [selectedVariables, setSelectedVariables] = useState<string>('{}');

    const handleViewVariables = (e: React.MouseEvent, variables: any) => {
        e.stopPropagation(); // Prevent Accordion toggle
        setSelectedVariables(JSON.stringify(variables || {}, null, 2));
        open();
    };

    const [filters, setFilters] = useState({
        workflowCode: '',
        initiator: '',
        cpId: '',
        candidateGroup: ''
    });

    const handleFilterChange = (key: string, value: string) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    useEffect(() => {
        const queryParams = new URLSearchParams();
        if (filters.workflowCode) queryParams.append('workflowCode', filters.workflowCode);
        if (filters.cpId) queryParams.append('cpId', filters.cpId);
        if (filters.candidateGroup) queryParams.append('candidateGroup', filters.candidateGroup);

        if (activeTab === 'active') {
            if (filters.initiator) queryParams.append('initiator', filters.initiator);
            fetch(`/api/runtime/cases?${queryParams.toString()}`)
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status}`);
                    return res.json();
                })
                .then(data => {
                    if (Array.isArray(data)) {
                        setCases(data);
                    } else {
                        console.error("API returned non-array for cases:", data);
                        setCases([]);
                    }
                })
                .catch(err => {
                    console.error("Error fetching cases:", err);
                    setCases([]);
                });
        } else if (activeTab === 'history') {
            setLoadingHistory(true);
            const userId = "user"; // Hardcoded for simplified auth context
            queryParams.append('userId', userId);

            fetch(`/api/runtime/cases/tasks/history?${queryParams.toString()}`)
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
    }, [activeTab, filters]);

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
    const groupedHistory = Array.isArray(history) ? history.reduce((acc, stage) => {
        const groupKey = stage.parentCaseId || stage.caseId;

        if (!groupKey) return acc;
        if (!acc[groupKey]) {
            acc[groupKey] = [];
        }
        acc[groupKey].push(stage);
        return acc;
    }, {} as { [key: string]: HistoricStageDto[] }) : {};

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>Task Inbox</Title>
                <Group>
                    <TextInput
                        placeholder="Workflow Code"
                        size="xs"
                        value={filters.workflowCode}
                        onChange={(e) => handleFilterChange('workflowCode', e.currentTarget.value)}
                    />
                    {activeTab === 'active' && (
                        <>
                            <TextInput
                                placeholder="Initiator"
                                size="xs"
                                value={filters.initiator}
                                onChange={(e) => handleFilterChange('initiator', e.currentTarget.value)}
                            />
                            <TextInput
                                placeholder="Queue / Group"
                                size="xs"
                                value={filters.candidateGroup}
                                onChange={(e) => handleFilterChange('candidateGroup', e.currentTarget.value)}
                            />
                        </>
                    )}
                    <TextInput
                        placeholder="CP ID"
                        size="xs"
                        value={filters.cpId}
                        onChange={(e) => handleFilterChange('cpId', e.currentTarget.value)}
                    />
                </Group>
            </Group>

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
                                <Accordion.Item key={c.caseId} value={c.caseId}>
                                    <Accordion.Control>
                                        <Group justify="space-between" pr="md">
                                            <Text fw={500}>{c.workflowCode} (ID: {c.caseId})</Text>
                                            <Group>
                                                <Text size="sm" c="dimmed">Started: {new Date(c.startTime).toLocaleString()}</Text>
                                                <Badge color="green">Active</Badge>
                                                <Tooltip label="View Variables">
                                                    <ActionIcon variant="light" color="blue" onClick={(e) => handleViewVariables(e, c.processVariables)}>
                                                        <IconCode size={18} />
                                                    </ActionIcon>
                                                </Tooltip>
                                                <Button size="xs" variant="light" component="a" href={`/cases/${c.caseId}`}>View Case</Button>
                                            </Group>
                                        </Group>
                                    </Accordion.Control>
                                    <Accordion.Panel>
                                        <Title order={5} mb="sm">Current Stages (Tasks)</Title>
                                        {loadingTasks[c.caseId] && <Loader size="sm" />}
                                        {!loadingTasks[c.caseId] && tasks[c.caseId]?.length === 0 && <Text size="sm">No active tasks (or case ended).</Text>}
                                        {!loadingTasks[c.caseId] && tasks[c.caseId]?.map(t => (
                                            <Group key={t.taskId} mb="xs" p="xs" style={{ border: '1px solid #eee', borderRadius: '4px' }}>
                                                <Text size="sm" fw={500}>{t.stageName}</Text>
                                                {t.assignee ? (
                                                    <Badge variant="outline" size="sm">{t.assignee}</Badge>
                                                ) : (
                                                    <Badge variant="filled" color="orange" size="sm">
                                                        {t.candidateGroups && t.candidateGroups.length > 0
                                                            ? `Queue: ${t.candidateGroups.join(', ')}`
                                                            : 'Unassigned'}
                                                    </Badge>
                                                )}
                                                <Text size="xs" c="dimmed">Created: {new Date(t.createdTime).toLocaleString()}</Text>
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
                                                <div>
                                                    <Text fw={500}>
                                                        {firstTask.parentCaseId
                                                            ? (firstTask.parentWorkflowName || firstTask.parentWorkflowCode || 'Composite Case')
                                                            : (firstTask.stageName ? `Case: ${firstTask.workflowCode}` : (firstTask.workflowCode || 'Unknown Workflow'))}
                                                    </Text>
                                                    <Text size="xs" c="dimmed">ID: {caseId}</Text>
                                                </div>
                                                <Group>
                                                    <Badge color="gray">Ended/History</Badge>
                                                    <Tooltip label="View Variables">
                                                        <ActionIcon variant="light" color="blue" onClick={(e) => handleViewVariables(e, firstTask.processVariables)}>
                                                            <IconCode size={18} />
                                                        </ActionIcon>
                                                    </Tooltip>
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

            <Modal opened={opened} onClose={close} title="Process Variables" size="lg">
                <Code block>{selectedVariables}</Code>
            </Modal>
        </Container>
    );
}

class ErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean, error: any }> {
    constructor(props: any) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: any) {
        return { hasError: true, error };
    }

    componentDidCatch(error: any, errorInfo: any) {
        console.error("TaskInbox Error:", error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            return (
                <Container py="xl">
                    <Title c="red">Something went wrong in Task Inbox</Title>
                    <Code block mt="md" color="red">
                        {this.state.error?.toString()}
                    </Code>
                    <Button mt="md" onClick={() => window.location.reload()}>Reload Page</Button>
                </Container>
            );
        }

        return this.props.children;
    }
}

export default function TaskInboxWithBoundary() {
    return (
        <ErrorBoundary>
            <TaskInbox />
        </ErrorBoundary>
    );
}
