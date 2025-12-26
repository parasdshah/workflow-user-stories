import { Container, Title, Button, Group, Table } from '@mantine/core';
import { IconPlus, IconPlayerPlay } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { StartCaseModal } from '../components/cases/StartCaseModal';

interface WorkflowMaster {
    id: number;
    workflowName: string;
    workflowCode: string;
    associatedModule: string;
}

function WorkflowList() {
    const navigate = useNavigate();
    const [workflows, setWorkflows] = useState<WorkflowMaster[]>([]);
    const [startModalOpen, setStartModalOpen] = useState(false);
    const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowMaster | null>(null);

    useEffect(() => {
        fetch('/api/workflows')
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch workflows');
                return res.json();
            })
            .then(data => setWorkflows(data))
            .catch(err => console.error("Error fetching workflows:", err));
    }, []);

    const handleOpenStartModal = (wf: WorkflowMaster) => {
        setSelectedWorkflow(wf);
        setStartModalOpen(true);
    };

    return (
        <Container size="xl" py="xl">

            <Group justify="space-between" mb="lg">
                <Title order={2}>Workflows</Title>
                <Button leftSection={<IconPlus size={16} />} onClick={() => navigate('/create')}>Create Workflow</Button>
            </Group>

            <Table>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>Code</Table.Th>
                        <Table.Th>Module</Table.Th>
                        <Table.Th>Actions</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {workflows.map((wf) => (
                        <Table.Tr key={wf.workflowCode}>
                            <Table.Td>{wf.workflowName}</Table.Td>
                            <Table.Td>{wf.workflowCode}</Table.Td>
                            <Table.Td>{wf.associatedModule}</Table.Td>
                            <Table.Td>
                                <Group gap="xs">
                                    <Button variant="subtle" size="xs" onClick={() => navigate(`/edit/${wf.workflowCode}`)}>Edit</Button>
                                    <Button variant="light" size="xs" onClick={() => navigate(`/preview/${wf.workflowCode}`)}>Preview/Deploy</Button>
                                    <Button
                                        variant="filled"
                                        color="blue"
                                        size="xs"
                                        leftSection={<IconPlayerPlay size={14} />}
                                        onClick={() => handleOpenStartModal(wf)}
                                    >
                                        Start
                                    </Button>
                                </Group>
                            </Table.Td>
                        </Table.Tr>
                    ))}
                    {workflows.length === 0 && (
                        <Table.Tr>
                            <Table.Td colSpan={4} align="center">No workflows found. Create one!</Table.Td>
                        </Table.Tr>
                    )}
                </Table.Tbody>
            </Table>

            {selectedWorkflow && (
                <StartCaseModal
                    opened={startModalOpen}
                    onClose={() => setStartModalOpen(false)}
                    workflowCode={selectedWorkflow.workflowCode}
                    workflowName={selectedWorkflow.workflowName}
                />
            )}
        </Container>
    );
}

export default WorkflowList;
