

import { Container, Title, Accordion, Badge, Group, Text, Loader, Button } from '@mantine/core';
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

function TaskInbox() {
    const [cases, setCases] = useState<CaseDto[]>([]);
    const [tasks, setTasks] = useState<{ [key: string]: TaskDto[] }>({});
    const [loadingTasks, setLoadingTasks] = useState<{ [key: string]: boolean }>({});

    useEffect(() => {
        fetch('/api/runtime/cases')
            .then(res => res.json())
            .then(data => setCases(data))
            .catch(err => console.error("Error fetching cases:", err));
    }, []);

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

    return (
        <Container size="lg" py="xl">
            <Title order={2} mb="lg">Task Inbox (Active Cases)</Title>

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
        </Container>
    );
}

export default TaskInbox;
