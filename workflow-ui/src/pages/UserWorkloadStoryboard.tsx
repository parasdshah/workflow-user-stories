import { useEffect, useState } from 'react';
import { Container, Title, Paper, Table, Text, Badge, ActionIcon, Group, Collapse, Box, Button, Loader, TextInput, Pagination, Grid, Card } from '@mantine/core';
import { IconChevronRight, IconChevronDown, IconBriefcase, IconSearch, IconPlayerPlay, IconCheck } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';

interface TaskSummaryDTO {
    taskId: string;
    caseId: string;
    stageName: string;
    stageCode: string;
    createdTime: string;
    dueDate?: string;
    endTime?: string;
    workflowName: string;
    status: string;
}

interface UserStoryboardDTO {
    userId: string;
    userName: string;
    newTasks: TaskSummaryDTO[];
    wipTasks: TaskSummaryDTO[];
    closedTasks: TaskSummaryDTO[];
}

export default function UserWorkloadStoryboard() {
    const [data, setData] = useState<UserStoryboardDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [expandedRows, setExpandedRows] = useState<string[]>([]);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const pageSize = 20;
    const navigate = useNavigate();

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const res = await fetch('/api/runtime/stats/storyboard');
            if (res.ok) {
                const json = await res.json();
                setData(json);
            } else {
                console.error("Failed to fetch storyboard stats");
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

    useEffect(() => {
        setPage(1);
    }, [search]);

    if (loading) return <Container py="xl"><Loader /></Container>;

    const filteredData = data.filter(user =>
        user.userName.toLowerCase().includes(search.toLowerCase()) ||
        user.userId.toLowerCase().includes(search.toLowerCase())
    );

    const paginatedData = filteredData.slice((page - 1) * pageSize, page * pageSize);
    const totalPages = Math.ceil(filteredData.length / pageSize);

    const TaskCard = ({ task, isClosed }: { task: TaskSummaryDTO, isClosed?: boolean }) => (
        <Card shadow="xs" padding="xs" radius="sm" withBorder mb="xs">
            <Text size="sm" fw={500}>{task.caseId}</Text>
            <Text size="xs" c="dimmed">Workflow: {task.workflowName}</Text>
            <Text size="xs">Stage: {task.stageName}</Text>
            {!isClosed && (
                <Text size="xs" c="dimmed">Created: {new Date(task.createdTime).toLocaleDateString()}</Text>
            )}
            {isClosed && (
                <Text size="xs" c="dimmed">Closed: {task.endTime ? new Date(task.endTime).toLocaleDateString() : '-'}</Text>
            )}

            <Group mt="xs" justify="flex-end">
                <Button size="compact-xs" variant="subtle" onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/cases/${task.caseId}`);
                }}>
                    View
                </Button>
            </Group>
        </Card>
    );

    const rows = paginatedData.map((user) => {
        const isExpanded = expandedRows.includes(user.userId);

        return (
            <>
                <Table.Tr key={user.userId} style={{ cursor: 'pointer', backgroundColor: isExpanded ? ('#f8f9fa') : undefined }} onClick={() => toggleRow(user.userId)}>
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
                        <Group gap="xs">
                            <Badge color="blue" variant="dot">{user.newTasks.length} New</Badge>
                            <Badge color="orange" variant="dot">{user.wipTasks.length} WIP</Badge>
                            <Badge color="gray" variant="dot">{user.closedTasks.length} Closed</Badge>
                        </Group>
                    </Table.Td>
                </Table.Tr>
                <Table.Tr key={`${user.userId}-details`}>
                    <Table.Td colSpan={2} p={0}>
                        <Collapse in={isExpanded}>
                            <Box p="md" bg="gray.0" style={{ borderBottom: '1px solid #dee2e6' }}>
                                <Grid>
                                    <Grid.Col span={4}>
                                        <Title order={6} mb="sm" c="blue.7"><IconBriefcase size={14} /> New ({user.newTasks.length})</Title>
                                        {user.newTasks.map(t => <TaskCard key={t.taskId} task={t} />)}
                                        {user.newTasks.length === 0 && <Text size="xs" c="dimmed">No new tasks</Text>}
                                    </Grid.Col>
                                    <Grid.Col span={4} style={{ borderLeft: '1px solid #ced4da', borderRight: '1px solid #ced4da' }}>
                                        <Title order={6} mb="sm" c="orange.7"><IconPlayerPlay size={14} /> In Progress ({user.wipTasks.length})</Title>
                                        {user.wipTasks.map(t => <TaskCard key={t.taskId} task={t} />)}
                                        {user.wipTasks.length === 0 && <Text size="xs" c="dimmed">No WIP tasks</Text>}
                                    </Grid.Col>
                                    <Grid.Col span={4}>
                                        <Title order={6} mb="sm" c="gray.7"><IconCheck size={14} /> Closed ({user.closedTasks.length})</Title>
                                        {user.closedTasks.map(t => <TaskCard key={t.taskId} task={t} isClosed />)}
                                        {user.closedTasks.length === 0 && <Text size="xs" c="dimmed">No closed tasks</Text>}
                                    </Grid.Col>
                                </Grid>
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
                <Title order={2}>Workload Storyboard</Title>
                <Group>
                    <TextInput
                        placeholder="Search user..."
                        leftSection={<IconSearch size={14} />}
                        value={search}
                        onChange={(event) => setSearch(event.currentTarget.value)}
                    />
                    <Button variant="light" onClick={fetchData}>Refresh</Button>
                </Group>
            </Group>

            <Paper withBorder shadow="sm" radius="md">
                <Table verticalSpacing="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>User Name</Table.Th>
                            <Table.Th>Workload Snapshot</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{rows}</Table.Tbody>
                </Table>
                {filteredData.length === 0 && (
                    <Text p="xl" ta="center" c="dimmed">No matching users found.</Text>
                )}
                {totalPages > 1 && (
                    <Group justify="center" p="md" style={{ borderTop: '1px solid #dee2e6' }}>
                        <Pagination total={totalPages} value={page} onChange={setPage} />
                    </Group>
                )}
            </Paper>
        </Container>
    );
}
