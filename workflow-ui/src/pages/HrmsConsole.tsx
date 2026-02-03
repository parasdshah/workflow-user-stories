import { Container, Title, Tabs, Table, Button, Group, Modal, Select, TextInput, NumberInput, Paper, Text, Stack } from '@mantine/core';
import { IconUsers, IconDatabase, IconPlus } from '@tabler/icons-react';
import { useDisclosure } from '@mantine/hooks';
import { useEffect, useState } from 'react';
import { useForm } from '@mantine/form';

export function HrmsConsole() {
    const [activeTab, setActiveTab] = useState<string | null>('matrix');
    return (
        <Container size="xl" py="xl">
            <Title order={2} mb="lg">HRMS Management Console (Matrix Sandbox)</Title>

            <Paper withBorder p="md">
                <Tabs value={activeTab} onChange={setActiveTab}>
                    <Tabs.List>
                        <Tabs.Tab value="matrix" leftSection={<IconUsers size={14} />}>Matrix Assignments</Tabs.Tab>
                        <Tabs.Tab value="references" leftSection={<IconDatabase size={14} />}>Reference Data</Tabs.Tab>
                    </Tabs.List>

                    <Tabs.Panel value="matrix" pt="xs">
                        <MatrixAssignmentsPanel />
                    </Tabs.Panel>

                    <Tabs.Panel value="references" pt="xs">
                        <ReferenceDataPanel />
                    </Tabs.Panel>
                </Tabs>
            </Paper>
        </Container>
    );
}

function MatrixAssignmentsPanel() {
    const [assignments, setAssignments] = useState<any[]>([]);
    const [opened, { open, close }] = useDisclosure(false);

    // Lookups
    const [employees, setEmployees] = useState<any[]>([]);
    const [roles, setRoles] = useState<any[]>([]);
    const [regions, setRegions] = useState<any[]>([]);
    const [segments, setSegments] = useState<any[]>([]);
    const [subSegments, setSubSegments] = useState<any[]>([]);

    const fetchData = async () => {
        try {
            const [aRes, eRes, rRes, regRes, sRes, ssRes] = await Promise.all([
                fetch('/api/hrms/assignments'),
                fetch('/api/hrms/employees'),
                fetch('/api/hrms/roles'),
                fetch('/api/hrms/regions'),
                fetch('/api/hrms/segments'),
                fetch('/api/hrms/sub-segments')
            ]);

            setAssignments(aRes.ok ? await aRes.json() : []);
            setEmployees(eRes.ok ? await eRes.json() : []);
            setRoles(rRes.ok ? await rRes.json() : []);
            setRegions(regRes.ok ? await regRes.json() : []);
            setSegments(sRes.ok ? await sRes.json() : []);
            setSubSegments(ssRes.ok ? await ssRes.json() : []);
        } catch (e) {
            console.error("Failed to load Matrix Data", e);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const form = useForm({
        initialValues: {
            employeeId: '',
            roleCode: '',
            regionId: '',
            segmentId: '',
            subSegmentId: '',
            limit: 0
        }
    });

    const handleSubmit = async (values: typeof form.values) => {
        const payload = {
            employee: { employeeId: values.employeeId },
            role: { roleCode: values.roleCode },
            scopeRegion: { regionId: values.regionId },
            scopeSegment: values.segmentId ? { segmentId: parseInt(values.segmentId) } : null,
            scopeSubSegment: values.subSegmentId ? { subSegmentId: parseInt(values.subSegmentId) } : null,
            approvalLimit: values.limit,
            currencyCode: 'USD',
            denomination: 'ACTUALS'
        };

        await fetch('/api/hrms/assignments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        close();
        form.reset();
        fetchData(); // Refresh
    };

    return (
        <Stack>
            <Group justify="flex-end">
                <Button leftSection={<IconPlus size={14} />} onClick={open}>Add Assignment</Button>
            </Group>
            <Table striped highlightOnHover withTableBorder>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Employee</Table.Th>
                        <Table.Th>Role</Table.Th>
                        <Table.Th>Scope Region</Table.Th>
                        <Table.Th>Scope Segment</Table.Th>
                        <Table.Th>Approval Limit</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {assignments.map((row) => (
                        <Table.Tr key={row.assignmentId}>
                            <Table.Td>{row.employee?.fullName} ({row.employee?.employeeId})</Table.Td>
                            <Table.Td>{row.role?.roleName}</Table.Td>
                            <Table.Td>{row.scopeRegion?.regionName}</Table.Td>
                            <Table.Td>
                                {row.scopeSegment?.segmentName || row.scopeSubSegment?.subSegmentName ? (
                                    <Text size="sm">{row.scopeSegment?.segmentName} {row.scopeSubSegment ? `> ${row.scopeSubSegment.subSegmentName}` : ''}</Text>
                                ) : (
                                    <Text size="sm" c="dimmed">Global</Text>
                                )}
                            </Table.Td>
                            <Table.Td>{row.approvalLimit} {row.currencyCode}</Table.Td>
                        </Table.Tr>
                    ))}
                </Table.Tbody>
            </Table>

            <Modal opened={opened} onClose={close} title="New Matrix Assignment">
                <form onSubmit={form.onSubmit(handleSubmit)}>
                    <Stack>
                        <Select
                            label="Employee"
                            data={employees.map(e => ({ value: e.employeeId, label: `${e.fullName} (${e.employeeId})` }))}
                            searchable
                            required
                            {...form.getInputProps('employeeId')}
                        />
                        <Select
                            label="Role"
                            data={roles.map(r => ({ value: r.roleCode, label: r.roleName }))}
                            required
                            {...form.getInputProps('roleCode')}
                        />
                        <Select
                            label="Scope Region"
                            data={regions.map(r => ({ value: String(r.regionId), label: r.regionName }))}
                            searchable
                            required
                            {...form.getInputProps('regionId')}
                        />
                        <Select
                            label="Scope Segment"
                            data={segments.map(s => ({ value: String(s.segmentId), label: s.segmentName }))}
                            placeholder="Optional (All Segments)"
                            clearable
                            {...form.getInputProps('segmentId')}
                        />
                        <Select
                            label="Scope Sub-Segment"
                            data={subSegments
                                .filter(ss => !form.values.segmentId || String(ss.businessSegment?.segmentId) === form.values.segmentId)
                                .map(ss => ({ value: String(ss.subSegmentId), label: ss.subSegmentName }))}
                            placeholder="Optional (All Sub-Segments)"
                            disabled={!form.values.segmentId}
                            clearable
                            {...form.getInputProps('subSegmentId')}
                        />
                        <NumberInput
                            label="Approval Limit (USD)"
                            required
                            {...form.getInputProps('limit')}
                        />
                        <Button type="submit">Save Assignment</Button>
                    </Stack>
                </form>
            </Modal>
        </Stack>
    );
}

function ReferenceDataPanel() {
    const [roles, setRoles] = useState<any[]>([]);

    // Add Role Logic
    const [roleModalOpened, { open: openRole, close: closeRole }] = useDisclosure(false);
    const roleForm = useForm({
        initialValues: { roleCode: '', roleName: '' }
    });

    const fetchRefs = async () => {
        try {
            // Fetch only if API is reachable
            const res = await fetch('/api/hrms/roles');
            if (res.ok) {
                const data = await res.json();
                setRoles(Array.isArray(data) ? data : []);
            }
        } catch (e) { console.error(e); }
    };

    useEffect(() => { fetchRefs(); }, []);

    const handleAddRole = async (values: typeof roleForm.values) => {
        try {
            const res = await fetch('/api/hrms/roles', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...values, baseAuthorityLimit: 0, baseCurrency: 'USD' })
            });

            if (res.ok) {
                closeRole();
                roleForm.reset();
                fetchRefs();
            } else {
                alert("Failed to create role. Backend may be unreachable. Check Proxy settings or Backend status.");
                console.error("Create Role Failed", res.status, res.statusText);
            }
        } catch (e) {
            alert("Network Error: Failed to create role.");
            console.error(e);
        }
    };

    return (
        <Stack>
            <Group justify="space-between">
                <Text fw={500}>Roles</Text>
                <Button size="xs" variant="light" onClick={openRole}>+ Add Role</Button>
            </Group>
            <Table mb="xl" withTableBorder>
                <Table.Thead><Table.Tr><Table.Th>Code</Table.Th><Table.Th>Name</Table.Th></Table.Tr></Table.Thead>
                <Table.Tbody>
                    {roles.map(r => (
                        <Table.Tr key={r.roleCode}>
                            <Table.Td>{r.roleCode}</Table.Td>
                            <Table.Td>{r.roleName}</Table.Td>
                        </Table.Tr>
                    ))}
                </Table.Tbody>
            </Table>

            <Modal opened={roleModalOpened} onClose={closeRole} title="Add Role">
                <form onSubmit={roleForm.onSubmit(handleAddRole)}>
                    <Stack>
                        <TextInput label="Role Code" required {...roleForm.getInputProps('roleCode')} />
                        <TextInput label="Role Name" required {...roleForm.getInputProps('roleName')} />
                        <Button type="submit">Create Role</Button>
                    </Stack>
                </form>
            </Modal>

            <Text c="dimmed" size="sm">Regions and Products can be managed similarly (Not implemented in Sandbox).</Text>
        </Stack>
    );
}
