import { Container, Title, Table, Group, TextInput, Select, Button, Pagination, Modal, Text, Code, ScrollArea } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useDisclosure } from '@mantine/hooks';
import { IconSearch, IconEye } from '@tabler/icons-react';
import { useState, useEffect } from 'react';

interface AuditTrail {
    id: number;
    entityName: string;
    entityId: string;
    action: string;
    changedBy: string;
    changedAt: string;
    changes: string;
}

interface AuditLogResponse {
    content: AuditTrail[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

export function AuditLog() {
    const [logs, setLogs] = useState<AuditTrail[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [activePage, setActivePage] = useState(1);

    // Filters
    const [entityName, setEntityName] = useState('');
    const [action, setAction] = useState('');
    const [changedBy, setChangedBy] = useState('');
    const [dateRange, setDateRange] = useState<[Date | null, Date | null]>([null, null]);

    // Modal
    const [opened, { open, close }] = useDisclosure(false);
    const [selectedLog, setSelectedLog] = useState<AuditTrail | null>(null);

    const fetchLogs = () => {
        const params = new URLSearchParams();
        params.append('page', (activePage - 1).toString());
        params.append('size', '10');

        if (entityName) params.append('entityName', entityName);
        if (action) params.append('action', action);
        if (changedBy) params.append('changedBy', changedBy);
        if (dateRange[0]) params.append('startDate', dateRange[0].toISOString());
        if (dateRange[1]) params.append('endDate', dateRange[1].toISOString());

        fetch(`/api/audit-logs?${params.toString()}`)
            .then(res => {
                if (!res.ok) throw new Error(`API Error: ${res.statusText}`);
                return res.json();
            })
            .then((data: AuditLogResponse) => {
                setLogs(data.content || []); // Safely handle undefined content
                setTotalPages(data.totalPages || 0);
            })
            .catch(err => {
                console.error("Failed to fetch logs:", err);
                setLogs([]); // Ensure logs is not undefined on error
            });
    };

    useEffect(() => {
        fetchLogs();
    }, [activePage]);

    const handleViewDetails = (log: AuditTrail) => {
        setSelectedLog(log);
        open();
    };

    return (
        <Container size="xl" py="xl">
            <Title order={2} mb="lg">Audit Logs</Title>

            <Group mb="md" align="end">
                <Select
                    label="Entity Type"
                    placeholder="All Entities"
                    data={['WorkflowMaster', 'StageConfig', 'ScreenDefinition']}
                    value={entityName}
                    onChange={(val) => setEntityName(val || '')}
                    clearable
                />
                <Select
                    label="Action"
                    placeholder="All Actions"
                    data={['CREATE', 'UPDATE', 'DELETE']}
                    value={action}
                    onChange={(val) => setAction(val || '')}
                    clearable
                />
                <TextInput
                    label="User"
                    placeholder="Search user"
                    value={changedBy}
                    onChange={(e) => setChangedBy(e.currentTarget.value)}
                />
                <DatePickerInput
                    type="range"
                    label="Date Range"
                    placeholder="Pick dates"
                    value={dateRange}
                    onChange={(val) => setDateRange(val as [Date | null, Date | null])}
                    clearable
                />
                <Button leftSection={<IconSearch size={16} />} onClick={() => { setActivePage(1); fetchLogs(); }}>Search</Button>
            </Group>

            <Table striped highlightOnHover>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Timestamp</Table.Th>
                        <Table.Th>Entity</Table.Th>
                        <Table.Th>ID</Table.Th>
                        <Table.Th>Action</Table.Th>
                        <Table.Th>User</Table.Th>
                        <Table.Th>Details</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {logs.map((log) => (
                        <Table.Tr key={log.id}>
                            <Table.Td>{new Date(log.changedAt).toLocaleString()}</Table.Td>
                            <Table.Td>{log.entityName}</Table.Td>
                            <Table.Td>{log.entityId}</Table.Td>
                            <Table.Td>{log.action}</Table.Td>
                            <Table.Td>{log.changedBy}</Table.Td>
                            <Table.Td>
                                <Button variant="subtle" size="xs" onClick={() => handleViewDetails(log)}>
                                    <IconEye size={16} />
                                </Button>
                            </Table.Td>
                        </Table.Tr>
                    ))}
                    {logs.length === 0 && <Table.Tr><Table.Td colSpan={6} align="center">No logs found</Table.Td></Table.Tr>}
                </Table.Tbody>
            </Table>

            <Group justify="center" mt="lg">
                <Pagination total={totalPages} value={activePage} onChange={setActivePage} />
            </Group>

            <Modal opened={opened} onClose={close} title="Audit Details" size="lg">
                <ScrollArea h={400}>
                    {selectedLog && (
                        <>
                            <Text size="sm" fw={500} mb="xs">
                                {selectedLog.action} on {selectedLog.entityName} ({selectedLog.entityId}) by {selectedLog.changedBy} at {new Date(selectedLog.changedAt).toLocaleString()}
                            </Text>
                            <Code block>
                                {selectedLog.changes ? JSON.stringify(JSON.parse(selectedLog.changes), null, 2) : 'No changes recorded'}
                            </Code>
                        </>
                    )}
                </ScrollArea>
            </Modal>
        </Container>
    );
}

export default AuditLog;
