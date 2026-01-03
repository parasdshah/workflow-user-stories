import { Container, Title, Table, Badge, Text, Group, ActionIcon, Modal, Button, TextInput } from '@mantine/core';
import { IconTrash, IconRotateClockwise } from '@tabler/icons-react';
import { useState, useEffect } from 'react';
import { notifications } from '@mantine/notifications';

interface DeploymentDto {
    id: string;
    name: string;
    deploymentTime: string;
    status: string; // ACTIVE, SUSPENDED
}

function DeploymentHistory() {
    const [deployments, setDeployments] = useState<DeploymentDto[]>([]);
    const [selectedDeployment, setSelectedDeployment] = useState<DeploymentDto | null>(null);

    // Undeploy State
    const [undeployModalOpen, setUndeployModalOpen] = useState(false);

    // Rollback State
    const [rollbackModalOpen, setRollbackModalOpen] = useState(false);
    const [rollbackInput, setRollbackInput] = useState('');

    const fetchDeployments = () => {
        fetch('/api/deployments')
            .then(res => res.json())
            .then(data => setDeployments(data))
            .catch(err => console.error("Error fetching deployments:", err));
    };

    useEffect(() => {
        fetchDeployments();
    }, []);

    const handleUndeployClick = (d: DeploymentDto) => {
        setSelectedDeployment(d);
        setUndeployModalOpen(true);
    };

    const confirmUndeploy = () => {
        if (!selectedDeployment) return;
        fetch(`/api/deployments/${selectedDeployment.id}`, { method: 'DELETE' })
            .then(res => {
                if (!res.ok) throw new Error('Failed to undeploy');
                notifications.show({ title: 'Success', message: 'Deployment undeployed', color: 'green' });
                fetchDeployments();
            })
            .catch(err => notifications.show({ title: 'Error', message: err.message, color: 'red' }))
            .finally(() => {
                setUndeployModalOpen(false);
                setSelectedDeployment(null);
            });
    };

    const handleRollbackClick = (d: DeploymentDto) => {
        setSelectedDeployment(d);
        setRollbackInput('');
        setRollbackModalOpen(true);
    };

    const confirmRollback = () => {
        if (!selectedDeployment) return;
        if (rollbackInput !== 'ROLLBACK') return;

        fetch(`/api/deployments/${selectedDeployment.id}/rollback`, { method: 'POST' })
            .then(res => {
                if (!res.ok) throw new Error('Failed to rollback');
                notifications.show({ title: 'Success', message: 'Rollback successful (New version created)', color: 'green' });
                fetchDeployments();
            })
            .catch(err => notifications.show({ title: 'Error', message: err.message, color: 'red' }))
            .finally(() => {
                setRollbackModalOpen(false);
                setSelectedDeployment(null);
            });
    };

    return (
        <Container size="lg" py="xl">
            <Title order={2} mb="lg">Deployment History</Title>
            <Table>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>ID</Table.Th>
                        <Table.Th>Name (Workflow Code)</Table.Th>
                        <Table.Th>Date</Table.Th>
                        <Table.Th>Status</Table.Th>
                        <Table.Th>Actions</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {deployments.map((d) => {
                        const isSuspended = d.status === 'SUSPENDED';
                        return (
                            <Table.Tr key={d.id} style={{ opacity: isSuspended ? 0.6 : 1, backgroundColor: isSuspended ? '#f8f9fa' : undefined }}>
                                <Table.Td>{d.id}</Table.Td>
                                <Table.Td>{d.name}</Table.Td>
                                <Table.Td>{new Date(d.deploymentTime).toLocaleString()}</Table.Td>
                                <Table.Td>
                                    <Badge color={isSuspended ? 'gray' : 'green'}>{isSuspended ? 'Undeployed' : 'Deployed'}</Badge>
                                </Table.Td>
                                <Table.Td>
                                    <Group gap="xs">
                                        <ActionIcon
                                            color="blue"
                                            variant="light"
                                            onClick={() => handleRollbackClick(d)}
                                            title="Rollback"
                                            disabled={isSuspended}
                                        >
                                            <IconRotateClockwise size={16} />
                                        </ActionIcon>
                                        <ActionIcon
                                            color="red"
                                            variant="light"
                                            onClick={() => handleUndeployClick(d)}
                                            title="Undeploy"
                                            disabled={isSuspended}
                                        >
                                            <IconTrash size={16} />
                                        </ActionIcon>
                                    </Group>
                                </Table.Td>
                            </Table.Tr>
                        );
                    })}
                    {deployments.length === 0 && (
                        <Table.Tr>
                            <Table.Td colSpan={5} align="center"><Text c="dimmed">No deployments found.</Text></Table.Td>
                        </Table.Tr>
                    )}
                </Table.Tbody>
            </Table>

            {/* Undeploy Modal */}
            <Modal opened={undeployModalOpen} onClose={() => setUndeployModalOpen(false)} title="Confirm Undeploy">
                <Text size="sm">Are you sure you want to undeploy <b>{selectedDeployment?.name}</b>?</Text>
                <Text size="xs" c="dimmed" mt="xs">This action may cascade and delete active process instances associated with this deployment.</Text>
                <Group justify="flex-end" mt="xl">
                    <Button variant="default" onClick={() => setUndeployModalOpen(false)}>Cancel</Button>
                    <Button color="red" onClick={confirmUndeploy}>Undeploy</Button>
                </Group>
            </Modal>

            {/* Rollback Modal */}
            <Modal opened={rollbackModalOpen} onClose={() => setRollbackModalOpen(false)} title="Confirm Rollback">
                <Text size="sm" mb="md">Type <b>ROLLBACK</b> to confirm reverting to version from {selectedDeployment ? new Date(selectedDeployment.deploymentTime).toLocaleDateString() : ''}.</Text>
                <TextInput
                    placeholder="ROLLBACK"
                    value={rollbackInput}
                    onChange={(e) => setRollbackInput(e.target.value)}
                    error={rollbackInput && rollbackInput !== 'ROLLBACK' ? 'Must match exactly' : null}
                />
                <Group justify="flex-end" mt="xl">
                    <Button variant="default" onClick={() => setRollbackModalOpen(false)}>Cancel</Button>
                    <Button disabled={rollbackInput !== 'ROLLBACK'} onClick={confirmRollback}>Rollback</Button>
                </Group>
            </Modal>
        </Container>
    );
}

export default DeploymentHistory;
