import { useState, useEffect } from 'react';
import { Container, Title, Paper, Switch, Group, Select, Button, LoadingOverlay, Alert } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconInfoCircle } from '@tabler/icons-react';

interface UserLeave {
    id?: number;
    userId: string;
    fromDate: string;
    toDate: string;
    substituteUserId: string;
    active: boolean;
}

export default function AvailabilitySettings() {
    const [userId, setUserId] = useState('');
    const [loading, setLoading] = useState(false);
    const [activeLeave, setActiveLeave] = useState<UserLeave | null>(null);
    const [employees, setEmployees] = useState<any[]>([]);

    const form = useForm({
        initialValues: {
            oooEnabled: false,
            dateRange: [null, null] as [Date | null, Date | null],
            substitute: '',
        },
    });

    useEffect(() => {
        fetchEmployees();
    }, []);

    useEffect(() => {
        if (userId) {
            fetchActiveLeave();
        }
    }, [userId]);

    const fetchEmployees = async () => {
        try {
            const res = await fetch('/api/hrms/employees');
            if (res.ok) {
                const data = await res.json();
                setEmployees(Array.isArray(data) ? data : []);
                // Default to first employee if not set
                if (!userId && data.length > 0) {
                    setUserId(data[0].employeeId);
                }
            }
        } catch (e) {
            console.error("Failed to fetch employees", e);
        }
    };

    const fetchActiveLeave = async () => {
        setLoading(true);
        try {
            const res = await fetch(`http://localhost:8080/api/user-leaves/active/${userId}`);
            if (res.status === 200) {
                const data = await res.json();
                setActiveLeave(data);
                form.setValues({
                    oooEnabled: true,
                    dateRange: [new Date(data.fromDate), new Date(data.toDate)],
                    substitute: data.substituteUserId
                });
            } else {
                setActiveLeave(null);
                form.setValues({ oooEnabled: false, dateRange: [null, null], substitute: '' });
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async (values: typeof form.values) => {
        if (!values.oooEnabled) {
            // Turning off OOO
            if (activeLeave && activeLeave.id) {
                await fetch(`http://localhost:8080/api/user-leaves/${activeLeave.id}`, { method: 'DELETE' });
                notifications.show({ title: 'Success', message: 'You are now marked as Available', color: 'blue' });
                setActiveLeave(null);
            }
            return;
        }

        // Turning ON OOO
        if (!values.dateRange[0] || !values.dateRange[1]) {
            notifications.show({ title: 'Error', message: 'Please select a date range', color: 'red' });
            return;
        }
        if (!values.substitute) {
            notifications.show({ title: 'Error', message: 'Please select a substitute', color: 'red' });
            return;
        }

        // Helper to format as Local ISO without timezone shift
        const formatLocalISO = (date: Date, setEndOfDay = false) => {
            const d = new Date(date);
            if (setEndOfDay) {
                d.setHours(23, 59, 59, 999);
            } else {
                d.setHours(0, 0, 0, 0);
            }
            // Manual ISO formatting to preserve local time
            const year = d.getFullYear();
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            const hours = String(d.getHours()).padStart(2, '0');
            const minutes = String(d.getMinutes()).padStart(2, '0');
            const seconds = String(d.getSeconds()).padStart(2, '0');
            return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
        };

        const payload: UserLeave = {
            userId,
            fromDate: formatLocalISO(values.dateRange[0]),
            toDate: formatLocalISO(values.dateRange[1], true),
            substituteUserId: values.substitute,
            active: true
        };

        try {
            const res = await fetch('http://localhost:8080/api/user-leaves', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (res.ok) {
                const data = await res.json();
                setActiveLeave(data);
                notifications.show({ title: 'Saved', message: 'Out of Office settings saved', color: 'green' });
            }
        } catch (err) {
            notifications.show({ title: 'Error', message: 'Failed to save settings', color: 'red' });
        }
    };

    const employeeOptions = employees.map(e => ({ value: e.employeeId, label: `${e.fullName} (${e.employeeId})` }));
    const substituteOptions = employeeOptions.filter(e => e.value !== userId);

    return (
        <Container size="sm" py="xl">
            <Paper p="xl" withBorder>
                <Title order={2} mb="lg">Availability & Delegation</Title>

                <Select
                    label="Simulate User ID"
                    placeholder="Select User"
                    data={employeeOptions}
                    value={userId}
                    onChange={(v) => setUserId(v || '')}
                    mb="xl"
                    searchable
                />

                <LoadingOverlay visible={loading} />

                <form onSubmit={form.onSubmit(handleSave)}>
                    <Switch
                        label="Enable Out of Office"
                        size="md"
                        mb="md"
                        {...form.getInputProps('oooEnabled', { type: 'checkbox' })}
                    />

                    {form.values.oooEnabled && (
                        <>
                            <Alert icon={<IconInfoCircle size={16} />} title="Delegation Active" color="blue" mb="md">
                                Tasks assigned to you during this period will be automatically redirected to your substitute.
                            </Alert>

                            <DatePickerInput
                                type="range"
                                label="Leave Period"
                                placeholder="Pick dates range"
                                mb="md"
                                required
                                {...form.getInputProps('dateRange')}
                            />

                            <Select
                                label="Substitute User"
                                placeholder="Select a colleague"
                                data={substituteOptions}
                                mb="xl"
                                required
                                searchable
                                {...form.getInputProps('substitute')}
                            />
                        </>
                    )}

                    <Group justify="flex-end">
                        <Button type="submit">Save Settings</Button>
                    </Group>
                </form>
            </Paper>
        </Container>
    );
}
