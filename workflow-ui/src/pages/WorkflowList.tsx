import { Container, Title, Button, Group, Table, Badge, Indicator, ActionIcon, FileButton, Tooltip } from '@mantine/core';
import { IconPlus, IconPlayerPlay, IconDownload, IconUpload } from '@tabler/icons-react';
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
    interface WorkflowStats {
        workflowCode: string;
        workflowName: string;
        associatedModule: string;
        status: string;
        activeInstances: number;
        completedInstances: number;
    }

    const [stats, setStats] = useState<WorkflowStats[]>([]);
    const [startModalOpen, setStartModalOpen] = useState(false);
    const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowMaster | null>(null);

    useEffect(() => {
        // Fetch Stats (which includes all info we need)
        fetch('/api/workflows/stats')
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch workflow stats');
                return res.json();
            })
            .then(data => setStats(data))
            .catch(err => console.error("Error fetching stats:", err));
    }, []);




    const handleExport = async (code: string) => {
        try {
            const res = await fetch(`/api/workflow/export/${code}`);
            if (!res.ok) throw new Error("Export failed");
            const blob = await res.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `workflow_${code}_${Date.now()}.enc`;
            document.body.appendChild(a);
            a.click();
            a.remove();
        } catch (e) {
            console.error(e);
            alert('Export Failed');
        }
    };

    const handleImport = async (file: File | null) => {
        if (!file) return;
        const formData = new FormData();
        formData.append('file', file);
        try {
            const res = await fetch('/api/workflow/import', { method: 'POST', body: formData });
            if (!res.ok) {
                const errorMsg = await res.text();
                throw new Error(errorMsg || "Import failed");
            }
            alert('Import Successful');
            window.location.reload();
        } catch (e: any) {
            console.error(e);
            alert(`Import Failed: ${e.message}`);
        }
    };

    return (
        <Container size="xl" py="xl">

            <Group justify="space-between" mb="lg">
                <Title order={2}>Workflows</Title>
                <Group>
                    <FileButton onChange={handleImport} accept=".enc">
                        {(props) => <Button {...props} leftSection={<IconUpload size={16} />} variant="default">Import Workflow</Button>}
                    </FileButton>
                    <Button leftSection={<IconPlus size={16} />} onClick={() => navigate('/create')}>Create Workflow</Button>
                </Group>
            </Group>

            <Table>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>Status</Table.Th>
                        <Table.Th>Module</Table.Th>
                        <Table.Th>Active</Table.Th>
                        <Table.Th>Completed</Table.Th>
                        <Table.Th>Actions</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {stats.map((wf) => (
                        <Table.Tr key={wf.workflowCode} style={{ opacity: wf.status === 'DELETED' ? 0.6 : 1 }}>
                            <Table.Td>
                                <Group gap="xs">
                                    <Indicator color={wf.status === 'ACTIVE' ? 'green' : 'gray'} position="middle-start" size={8} offset={-4}>
                                        <span style={{ marginLeft: 10 }}>{wf.workflowName}</span>
                                    </Indicator>
                                </Group>
                            </Table.Td>
                            <Table.Td>
                                <Badge size="xs" color={wf.status === 'ACTIVE' ? 'green' : 'gray'}>{wf.status}</Badge>
                            </Table.Td>
                            <Table.Td>{wf.associatedModule}</Table.Td>
                            <Table.Td>
                                <Badge color="blue" variant="light">{wf.activeInstances}</Badge>
                            </Table.Td>
                            <Table.Td>
                                <Badge color="green" variant="light">{wf.completedInstances}</Badge>
                            </Table.Td>
                            <Table.Td>
                                <Group gap="xs">
                                    <Button variant="subtle" size="xs" onClick={() => navigate(`/edit/${wf.workflowCode}`)}>Edit</Button>
                                    <Tooltip label="Export Configuration">
                                        <ActionIcon variant="light" color="gray" onClick={() => handleExport(wf.workflowCode)}>
                                            <IconDownload size={16} />
                                        </ActionIcon>
                                    </Tooltip>
                                    <Button variant="light" size="xs" onClick={() => navigate(`/preview/${wf.workflowCode}`)}>Preview/Deploy</Button>
                                    <Button
                                        variant="filled"
                                        color="blue"
                                        size="xs"
                                        disabled={wf.status !== 'ACTIVE'}
                                        leftSection={<IconPlayerPlay size={14} />}
                                        onClick={() => {
                                            setSelectedWorkflow({ id: 0, ...wf }); // Quick map to match Master interface
                                            setStartModalOpen(true);
                                        }}
                                    >
                                        Start
                                    </Button>
                                </Group>
                            </Table.Td>
                        </Table.Tr>
                    ))}
                    {stats.length === 0 && (
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
