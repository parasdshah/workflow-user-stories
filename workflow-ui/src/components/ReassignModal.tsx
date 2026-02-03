import { Modal, Select, Textarea, Button, Group, Text } from '@mantine/core';
import { useState, useEffect } from 'react';
import { notifications } from '@mantine/notifications';

interface ReassignModalProps {
    opened: boolean;
    onClose: () => void;
    caseId: string;
    taskId: string;
    taskName: string;
    currentAssignee?: string;
    onSuccess: () => void;
}

export default function ReassignModal({ opened, onClose, caseId, taskId, taskName, currentAssignee, onSuccess }: ReassignModalProps) {
    const [users, setUsers] = useState<{ value: string; label: string }[]>([]);
    const [selectedUser, setSelectedUser] = useState<string | null>(null);
    const [reason, setReason] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (opened) {
            fetchUsers();
            setReason('');
            setSelectedUser(null);
        }
    }, [opened]);

    const fetchUsers = async () => {
        try {
            // Using /api/hrms/employees as it is standard
            const empRes = await fetch('/api/hrms/employees');
            if (empRes.ok) {
                const data = await empRes.json();
                if (Array.isArray(data)) {
                    setUsers(data.map((u: any) => ({ value: u.employeeId, label: `${u.fullName} (${u.employeeId})` })));
                } else {
                    console.warn("User API returned non-array", data);
                    setUsers([]);
                }
            }
        } catch (e) {
            console.error("Failed to load users", e);
        }
    };

    const handleReassign = async () => {
        if (!selectedUser || !reason.trim()) return;

        setLoading(true);
        try {
            const res = await fetch(`/api/runtime/cases/${caseId}/tasks/${taskId}/reassign?adminUserId=admin`, { // Hardcoded admin for now, or use context
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    newAssignee: selectedUser,
                    reason: reason
                })
            });

            if (res.ok) {
                notifications.show({
                    title: 'Success',
                    message: 'Task successfully reassigned',
                    color: 'green'
                });
                onSuccess();
                onClose();
            } else {
                const err = await res.text();
                notifications.show({
                    title: 'Error',
                    message: err || 'Failed to reassign task',
                    color: 'red'
                });
            }
        } catch (e) {
            notifications.show({
                title: 'Error',
                message: 'Network error',
                color: 'red'
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <Modal opened={opened} onClose={onClose} title="Reassign Task">
            <Text size="sm" mb="md">
                Reassigning <b>{taskName}</b> (Case: {caseId})<br />
                Current Assignee: {currentAssignee || "Unassigned (Group Queue)"}
            </Text>

            <Select
                label="New Assignee"
                placeholder="Select user"
                data={users}
                value={selectedUser}
                onChange={setSelectedUser}
                searchable
                mb="md"
                required
            />

            <Textarea
                label="Reason for Reassignment"
                placeholder="Why is this tasks being reassigned?"
                value={reason}
                onChange={(e) => setReason(e.currentTarget.value)}
                minRows={3}
                mb="xl"
                required
            />

            <Group justify="flex-end">
                <Button variant="default" onClick={onClose}>Cancel</Button>
                <Button color="red" onClick={handleReassign} loading={loading} disabled={!selectedUser || !reason.trim()}>
                    Confirm Reassignment
                </Button>
            </Group>
        </Modal>
    );
}
