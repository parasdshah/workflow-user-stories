import { useEffect, useState } from 'react';
import { Container, Title, Paper, Table, Text, Badge, ActionIcon, Group, Collapse, Box, Button, MultiSelect } from '@mantine/core';
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

interface GroupWorkloadDTO {
    groupId: string;
    groupName: string;
    pendingCount: number;
    tasks: TaskSummaryDTO[];
}

export default function GroupWorkload() {
    const [data, setData] = useState<GroupWorkloadDTO[]>([]);
    const [loading, setLoading] = useState(false);
    const [expandedRows, setExpandedRows] = useState<string[]>([]);
    const [groups, setGroups] = useState<string[]>([]);
    const [roleOptions, setRoleOptions] = useState<{ value: string; label: string }[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchRoles();
    }, []);

    const fetchRoles = async () => {
        try {
            const res = await fetch('/api/hrms/roles');
            if (res.ok) {
                const json = await res.json();
                if (Array.isArray(json)) {
                    setRoleOptions(json.map((r: any) => ({ value: r.roleCode, label: `${r.roleName} (${r.roleCode})` })));
                }
            }
        } catch (e) {
            console.error("Failed to fetch roles", e);
        }
    };

    const fetchData = async () => {
        if (groups.length === 0) {
            setData([]);
            return;
        }

        setLoading(true);
        try {
            const query = groups.map(g => `groupIds=${encodeURIComponent(g)}`).join('&');
            // FIX: Controller is mapped to /api/runtime/cases, so full path is /api/runtime/cases/workload/groups
            const res = await fetch(`/api/runtime/cases/workload/groups?${query}`);
            if (res.ok) {
                const json = await res.json();
                setData(json);
            } else {
                console.error("Failed to fetch group workload");
                setData([]);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const toggleRow = (groupId: string) => {
        setExpandedRows(prev =>
            prev.includes(groupId) ? prev.filter(id => id !== groupId) : [...prev, groupId]
        );
    };

    const rows = data.map((group) => {
        const isExpanded = expandedRows.includes(group.groupId);

        return (
            <>
                <Table.Tr key={group.groupId} style={{ cursor: 'pointer', backgroundColor: isExpanded ? '#f8f9fa' : undefined }} onClick={() => toggleRow(group.groupId)}>
                    <Table.Td>
                        <Group>
                            <ActionIcon variant="subtle" size="sm" color="gray">
                                {isExpanded ? <IconChevronDown size={16} /> : <IconChevronRight size={16} />}
                            </ActionIcon>
                            <Text fw={500}>{group.groupName}</Text>
                        </Group>
                    </Table.Td>
                    <Table.Td>
                        <Badge size="lg" variant="light" color={group.pendingCount > 5 ? 'red' : 'blue'}>
                            {group.pendingCount} Cases
                        </Badge>
                    </Table.Td>
                </Table.Tr>
                <Table.Tr key={`${group.groupId}-details`}>
                    <Table.Td colSpan={2} p={0}>
                        <Collapse in={isExpanded}>
                            <Box p="md" bg="gray.0" style={{ borderBottom: '1px solid #dee2e6' }}>
                                {group.tasks.length === 0 ? (
                                    <Text c="dimmed" size="sm" ta="center">No active tasks</Text>
                                ) : (
                                    <Table verticalSpacing="xs" striped highlightOnHover>
                                        <Table.Thead>
                                            <Table.Tr>
                                                <Table.Th>Case ID</Table.Th>
                                                <Table.Th>Workflow</Table.Th>
                                                <Table.Th>Stage</Table.Th>
                                                <Table.Th>Created At</Table.Th>
                                                <Table.Th>Action</Table.Th>
                                            </Table.Tr>
                                        </Table.Thead>
                                        <Table.Tbody>
                                            {group.tasks.map(task => (
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
                                                        <Button size="xs" variant="filled" color="blue" ml="xs" onClick={(e) => {
                                                            e.stopPropagation();
                                                            fetch(`/api/runtime/cases/${task.caseId}/tasks/${task.taskId}/claim?userId=user`, { method: 'POST' })
                                                                .then(res => {
                                                                    if (res.ok) fetchData();
                                                                    else alert('Failed to claim task');
                                                                });
                                                        }}>
                                                            Claim
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
                <Title order={2}>Group Queue Dashboard</Title>
                <Group style={{ flexGrow: 1, maxWidth: '600px' }}>
                    <MultiSelect
                        label="Select Pools / Groups"
                        placeholder="Select groups"
                        data={roleOptions}
                        searchable
                        nothingFoundMessage="No groups found"
                        value={groups}
                        onChange={setGroups}
                        style={{ flexGrow: 1 }}
                    />
                    <Button variant="filled" onClick={fetchData} loading={loading} mt={24}>Load Workload</Button>
                </Group>
            </Group>

            <Paper withBorder shadow="sm" radius="md">
                <Table verticalSpacing="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Group Name</Table.Th>
                            <Table.Th>Pending Workload</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {data.length === 0 && !loading && (
                            <Table.Tr>
                                <Table.Td colSpan={2}>
                                    <Text p="xl" ta="center" c="dimmed">
                                        {groups.length === 0 ? "Select groups to view workload." : "No workload found for selected groups."}
                                    </Text>
                                </Table.Td>
                            </Table.Tr>
                        )}
                        {rows}
                    </Table.Tbody>
                </Table>
            </Paper>
        </Container>
    );
}
