import { useState, useEffect } from 'react';
import { Container, Title, Paper, Table, Group, Select, Button, Text, ActionIcon } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconTrash, IconUser, IconCalendar } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';

interface UserLeave {
    id: number;
    userId: string;
    fromDate: string;
    toDate: string;
    substituteUserId: string;
    active: boolean;
}

export default function LeaveManagement() {
    const [leaves, setLeaves] = useState<UserLeave[]>([]);
    const [employees, setEmployees] = useState<any[]>([]);

    // Filters
    const [filterEmployee, setFilterEmployee] = useState<string | null>(null);
    const [filterDates, setFilterDates] = useState<[Date | null, Date | null]>([null, null]);

    useEffect(() => {
        fetchData();
        fetchEmployees();
    }, []);

    const fetchData = async () => {
        try {
            const res = await fetch('/api/user-leaves');
            if (res.ok) {
                setLeaves(await res.json());
            }
        } catch (e) {
            notifications.show({ title: 'Error', message: 'Failed to fetch leaves', color: 'red' });
        }
    };

    const fetchEmployees = async () => {
        try {
            const res = await fetch('/api/hrms/employees');
            if (res.ok) {
                setEmployees(await res.json());
            }
        } catch (e) { console.error(e); }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Are you sure you want to delete this leave?')) return;
        try {
            await fetch(`/api/user-leaves/${id}`, { method: 'DELETE' });
            setLeaves(prev => prev.filter(l => l.id !== id));
            notifications.show({ title: 'Deleted', message: 'Leave record removed', color: 'blue' });
        } catch (e) {
            notifications.show({ title: 'Error', message: 'Failed to delete leave', color: 'red' });
        }
    };

    // Filter Logic
    const filteredLeaves = leaves.filter(l => {
        // Employee Filter
        if (filterEmployee && l.userId !== filterEmployee) return false;

        // Date Filter (Overlap Check)
        if (filterDates[0] && filterDates[1]) {
            const rangeStart = filterDates[0].getTime();
            const rangeEnd = filterDates[1].getTime();
            const leaveStart = new Date(l.fromDate).getTime();
            const leaveEnd = new Date(l.toDate).getTime();

            // Check if ranges overlap
            if (leaveEnd < rangeStart || leaveStart > rangeEnd) return false;
        }

        return true;
    });

    const getEmployeeName = (id: string) => {
        const emp = employees.find(e => e.employeeId === id);
        return emp ? `${emp.fullName} (${id})` : id;
    };

    return (
        <Container size="lg" py="xl">
            <Paper p="md" withBorder>
                <Title order={2} mb="lg">Leave Management</Title>

                <Group mb="lg" align="end">
                    <Select
                        label="Filter by Employee"
                        placeholder="All Employees"
                        data={employees.map(e => ({ value: e.employeeId, label: `${e.fullName} (${e.employeeId})` }))}
                        value={filterEmployee}
                        onChange={setFilterEmployee}
                        searchable
                        clearable
                        leftSection={<IconUser size={16} />}
                        style={{ minWidth: 250 }}
                    />
                    <DatePickerInput
                        type="range"
                        label="Filter by Date Range"
                        placeholder="Pick dates"
                        value={filterDates}
                        onChange={(val: any) => setFilterDates(val)}
                        leftSection={<IconCalendar size={16} />}
                        allowSingleDateInRange
                        clearable
                        style={{ minWidth: 250 }}
                    />
                    <Button variant="outline" onClick={() => { setFilterEmployee(null); setFilterDates([null, null]); }}>
                        Clear Filters
                    </Button>
                </Group>

                <Table striped highlightOnHover withTableBorder>
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Employee</Table.Th>
                            <Table.Th>From</Table.Th>
                            <Table.Th>To</Table.Th>
                            <Table.Th>Substitute</Table.Th>
                            <Table.Th>Status</Table.Th>
                            <Table.Th>Actions</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {filteredLeaves.length > 0 ? filteredLeaves.map(leave => (
                            <Table.Tr key={leave.id}>
                                <Table.Td fw={500}>{getEmployeeName(leave.userId)}</Table.Td>
                                <Table.Td>{new Date(leave.fromDate).toLocaleDateString()}</Table.Td>
                                <Table.Td>{new Date(leave.toDate).toLocaleDateString()}</Table.Td>
                                <Table.Td>{getEmployeeName(leave.substituteUserId)}</Table.Td>
                                <Table.Td>
                                    <Text c={leave.active ? 'green' : 'dimmed'}>
                                        {leave.active ? 'Active' : 'Inactive'}
                                    </Text>
                                </Table.Td>
                                <Table.Td>
                                    <ActionIcon color="red" variant="subtle" onClick={() => handleDelete(leave.id)}>
                                        <IconTrash size={16} />
                                    </ActionIcon>
                                </Table.Td>
                            </Table.Tr>
                        )) : (
                            <Table.Tr>
                                <Table.Td colSpan={6} align="center">
                                    <Text c="dimmed">No leave records found</Text>
                                </Table.Td>
                            </Table.Tr>
                        )}
                    </Table.Tbody>
                </Table>
            </Paper>
        </Container>
    );
}
