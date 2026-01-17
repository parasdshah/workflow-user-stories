import { useEffect, useState } from 'react';
import { Container, Title, Paper, Table, Text, Badge, ActionIcon, Group, Collapse, Box, Button, Loader } from '@mantine/core';
import { IconChevronRight, IconChevronDown, IconBriefcase } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';

interface TaskSummaryDTO {
    taskId: string;
    caseId: string;
    stageName: string;
    stageCode: string;
    createdTime: string;
    dueDate?: string;
    workflowName: string;
}

interface UserWorkloadDTO {
    userId: string;
    userName: string;
    pendingCount: number;
    tasks: TaskSummaryDTO[];
}

export default function UserWorkload() {
    const [data, setData] = useState<UserWorkloadDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [expandedRows, setExpandedRows] = useState<string[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const res = await fetch('/api/runtime/stats/workload');
            if (res.ok) {
                const json = await res.json();
                setData(json);
            } else {
                console.error("Failed to fetch workload stats");
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const toggleRow = (userId: string) => {
        setExpandedRows(prev =>
            prev.includes(userId) ? prev.filter(id => id !== userId) : [...prev, userId]
        );
    };

    if (loading) return <Container py="xl"><Loader /></Container>;

    const rows = data.map((user) => {
        const isExpanded = expandedRows.includes(user.userId);

        return (
            <>
                <Table.Tr key={user.userId} style={{ cursor: 'pointer', backgroundColor: isExpanded ? '#f8f9fa' : undefined }} onClick={() => toggleRow(user.userId)}>
                    <Table.Td>
                        <Group>
                            <ActionIcon variant="subtle" size="sm" color="gray">
                                {isExpanded ? <IconChevronDown size={16} /> : <IconChevronRight size={16} />}
                            </ActionIcon>
                            <Text fw={500}>{user.userName}</Text>
                            <Text size="xs" c="dimmed">({user.userId})</Text>
                        </Group>
                    </Table.Td>
                    <Table.Td>
                        <Badge size="lg" variant="light" color={user.pendingCount > 5 ? 'red' : 'blue'}>
                            {user.pendingCount} Cases
                        </Badge>
                    </Table.Td>
                </Table.Tr>
                <Table.Tr key={`${user.userId}-details`}>
                    <Table.Td colSpan={2} p={0}>
                        <Collapse in={isExpanded}>
                            <Box p="md" bg="gray.0" style={{ borderBottom: '1px solid #dee2e6' }}>
                                {user.tasks.length === 0 ? (
                                    <Text c="dimmed" size="sm" ta="center">No active tasks</Text>
                                ) : (
                                    <Table verticalSpacing="xs" striped highlightOnHover>
                                        <Table.Thead>
                                            <Table.Tr>
                                                <Table.Th>Case ID</Table.Th>
                                                <Table.Th>Workflow</Table.Th>
                                                <Table.Th>Stage</Table.Th>
                                                <Table.Th>Assigned At</Table.Th>
                                                <Table.Th>Action</Table.Th>
                                            </Table.Tr>
                                        </Table.Thead>
                                        <Table.Tbody>
                                            {user.tasks.map(task => (
                                                <Table.Tr key={task.taskId}>
                                                    <Table.Td>
                                                        <Text size="sm" fw={500}>{task.caseId}</Text>
                                                    </Table.Td>
                                                    <Table.Td><Text size="sm">{task.workflowName}</Text></Table.Td>
                                                    <Table.Td><Badge variant="dot" size="sm">{task.stageName}</Badge></Table.Td>
                                                    <Table.Td><Text size="sm">{new Date(task.createdTime).toLocaleString()}</Text></Table.Td>
                                                    <Table.Td>
                                                        <Button leftSection={<IconBriefcase size={14} />} size="xs" variant="default" onClick={(e) => {
                                                            e.stopPropagation();
                                                            navigate(`/cases/${task.caseId}`);
                                                        }}>
                                                            View Case
                                                        </Button>
                                                    </Table.Td>
                                                </Table.Tr>
                                            ))}
                                        </Table.Tbody>
                                    </Table>
                                )}
                            </Box>
                        </Collapse>
                    </Table.Td>
                </Table.Tr>
            </>
        );
    });

    return (
        <Container size="xl" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>User Workload Dashboard</Title>
                <Button variant="light" onClick={fetchData}>Refresh</Button>
            </Group>

            <Paper withBorder shadow="sm" radius="md">
                <Table verticalSpacing="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>User Name</Table.Th>
                            <Table.Th>Pending Workload</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{rows}</Table.Tbody>
                </Table>
                {data.length === 0 && (
                    <Text p="xl" ta="center" c="dimmed">No active users found.</Text>
                )}
            </Paper>
        </Container>
    );
}
