import { Container, Title, Button, TextInput, Group, Stack, Paper, Select, NumberInput, Modal, Table, Checkbox, ActionIcon, SimpleGrid, Tabs } from '@mantine/core';
import { IconEdit, IconTrash, IconGitBranch } from '@tabler/icons-react';
import { useForm } from '@mantine/form';
import { useDisclosure } from '@mantine/hooks';
import { useParams, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { BpmnVisualizer } from '../components/bpmn/BpmnVisualizer';

// Define types locally or import from a shared types file
interface StageConfig {
    stageName: string;
    stageCode: string;
    sequenceOrder: number;
    isNestedWorkflow?: boolean;
    nestedWorkflowCode?: string;
    // UI only fields for mapping
    screenCode?: string;
    accessType?: string;
    // Hooks
    preEntryHook?: string;
    postEntryHook?: string; // New
    preExitHook?: string;   // New
    postExitHook?: string;
    allowedActions?: string; // e.g. "APPROVE,REJECT"
    parallelGrouping?: string; // New
}

interface ScreenDefinition {
    screenCode: string;
    description: string;
}

function WorkflowEditor() {
    const { code } = useParams();
    const navigate = useNavigate();
    const [opened, { open, close }] = useDisclosure(false);
    const [stages, setStages] = useState<StageConfig[]>([]);
    const [editingIndex, setEditingIndex] = useState<number | null>(null);
    const [screens, setScreens] = useState<ScreenDefinition[]>([]);

    const form = useForm({
        initialValues: {
            id: null as number | null,
            workflowName: '',
            workflowCode: '',
            associatedModule: '',
            slaDurationDays: 0,
        },
    });

    const stageForm = useForm({
        initialValues: {
            stageName: '',
            stageCode: '',
            sequenceOrder: 1,
            isNestedWorkflow: false,
            nestedWorkflowCode: '',
            screenCode: '',
            accessType: 'EDITABLE',
            preEntryHook: '',
            postEntryHook: '',
            preExitHook: '',
            postExitHook: '',
            allowedActions: '',
            parallelGrouping: ''
        }
    });

    useEffect(() => {
        // Fetch available screens
        fetch('/api/screens')
            .then(res => {
                if (!res.ok) throw new Error("Failed to fetch screens");
                return res.json();
            })
            .then(data => {
                if (Array.isArray(data)) {
                    setScreens(data);
                } else {
                    console.error("Expected array of screens but got:", data);
                    setScreens([]);
                }
            })
            .catch(err => console.error("Failed to load screens", err));
    }, []);

    const handleStageSubmit = (values: typeof stageForm.values) => {
        if (editingIndex !== null) {
            // Update existing
            const updated = [...stages];
            updated[editingIndex] = values;
            setStages(updated);
        } else {
            // Add new
            setStages([...stages, values]);
        }
        close();
        stageForm.reset();
        setEditingIndex(null);
    };

    const openAddModal = () => {
        setEditingIndex(null);
        stageForm.reset();
        open();
    };

    const openEditModal = (index: number) => {
        setEditingIndex(index);
        const stage = stages[index];
        stageForm.setValues({
            ...stage,
            screenCode: stage.screenCode || '',
            accessType: stage.accessType || 'EDITABLE',
            preEntryHook: stage.preEntryHook || '',
            postEntryHook: stage.postEntryHook || '',
            preExitHook: stage.preExitHook || '',
            postExitHook: stage.postExitHook || '',
            allowedActions: stage.allowedActions || '',
            parallelGrouping: stage.parallelGrouping || ''
        });

        // Fetch existing mapping if we don't have it locally (e.g. page reload)
        // But for now, we rely on what's in 'stages' state.
        // If 'stages' came from API, it lacks 'screenCode'. We might need to fetch it.
        // Optimization: Lazy fetch mapping when opening modal if missing.
        if (code && !stage.screenCode) {
            fetch(`/api/stages/${stage.stageCode}/mapping`)
                .then(res => {
                    if (res.status === 204) return null; // No content
                    return res.json();
                })
                .then(mapping => {
                    if (mapping) {
                        stageForm.setFieldValue('screenCode', mapping.screenCode);
                        stageForm.setFieldValue('accessType', mapping.accessType);
                        // Update local state too so we don't fetch again
                        const updated = [...stages];
                        updated[index] = { ...stage, screenCode: mapping.screenCode, accessType: mapping.accessType };
                        setStages(updated);
                    }
                })
                .catch(err => console.error("Error fetching mapping", err));
        }

        open();
    };

    const handleDeleteStage = (index: number) => {
        setStages(stages.filter((_, i) => i !== index));
    };

    useEffect(() => {
        if (code) {
            console.log("Fetching workflow details for:", code);
            // Fetch Workflow
            fetch(`/api/workflows/${code}`)
                .then(res => {
                    if (!res.ok) throw new Error(`Workflow Fetch Failed: ${res.statusText}`);
                    return res.json();
                })
                .then(data => {
                    console.log("Loaded Workflow:", data);
                    form.setValues({
                        id: data.id,
                        workflowName: data.workflowName,
                        workflowCode: data.workflowCode,
                        associatedModule: data.associatedModule,
                        slaDurationDays: data.slaDurationDays || 0,
                    });
                })
                .catch(err => {
                    console.error("Failed to load workflow", err);
                    alert(`Error loading workflow: ${err.message}`);
                });

            // Fetch Stages
            fetch(`/api/workflows/${code}/stages`)
                .then(res => {
                    if (!res.ok) throw new Error(`Stages Fetch Failed: ${res.statusText}`);
                    return res.json();
                })
                .then(data => {
                    console.log("Loaded Stages:", data);
                    setStages(data);
                })
                .catch(err => {
                    console.error("Failed to load stages", err);
                    alert(`Error loading stages: ${err.message}`);
                });
        }
    }, [code]);

    const handleSave = async () => {
        // 1. Save Workflow
        const wfBody = {
            ...form.values,
            workflowCode: code || form.values.workflowCode // Use param code if edit, else form
        };
        console.log("Saving Workflow Payload:", wfBody);

        try {
            const wfRes = await fetch('/api/workflows', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(wfBody)
            });

            if (!wfRes.ok) {
                const errText = await wfRes.text();
                console.error("Workflow Save Failed:", errText);
                alert(`Failed to save workflow: ${wfRes.status} - ${errText}`);
                return;
            }

            const savedWf = await wfRes.json();
            console.log("Workflow Saved Success:", savedWf);

            // 2. Save Stages
            const wfCode = savedWf.workflowCode; // Use returned code to be safe
            for (const stage of stages) {
                const stageBody = { ...stage, workflowCode: wfCode };
                // Remove UI-only fields before sending to Stage API
                const { screenCode, accessType, ...apiStageBody } = stageBody;

                console.log("Saving Stage Payload:", apiStageBody);
                const stageRes = await fetch(`/api/workflows/${wfCode}/stages`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(apiStageBody)
                });

                if (!stageRes.ok) {
                    const stageErr = await stageRes.text();
                    console.error("Stage Save Failed:", stageErr);
                    alert(`Failed to save stage ${stage.stageCode}: ${stageErr}`);
                    continue;
                }

                // 3. Save Mapping (if screenCode is set)
                if (stage.screenCode) {
                    console.log("Saving Mapping for stage:", stage.stageCode);
                    const mappingRes = await fetch(`/api/stages/${stage.stageCode}/mapping`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            stageCode: stage.stageCode,
                            screenCode: stage.screenCode,
                            accessType: stage.accessType || 'EDITABLE'
                        })
                    });
                    if (!mappingRes.ok) {
                        console.error("Mapping Save Failed");
                        // alert(`Failed to save mapping for ${stage.stageCode}`);
                    }
                }
            }

            alert('Saved successfully!');
            navigate('/');
        } catch (error: any) {
            console.error("Unexpected Error in handleSave:", error);
            alert(`Unexpected error saving workflow: ${error.message}`);
        }
    };

    return (
        <Container size="md" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>{code ? 'Edit Workflow' : 'Create Workflow'}</Title>
                <Group>
                    <Button variant="default" onClick={() => navigate('/')}>Cancel</Button>
                    <Button onClick={handleSave}>Save</Button>
                </Group>
            </Group>

            <Paper withBorder p="md">
                <Tabs defaultValue="config">
                    <Tabs.List mb="md">
                        <Tabs.Tab value="config" leftSection={<IconEdit size={14} />}>Configuration</Tabs.Tab>
                        <Tabs.Tab value="diagram" leftSection={<IconGitBranch size={14} />}>Diagram</Tabs.Tab>
                    </Tabs.List>

                    <Tabs.Panel value="config">
                        <Stack>
                            <TextInput label="Workflow Name" required {...form.getInputProps('workflowName')} />
                            <TextInput label="Workflow Code" required disabled={!!code} {...form.getInputProps('workflowCode')} />
                            <Select
                                label="Module"
                                data={['Overall', 'Credit Initiation', 'Financial Spreading', 'Credit Rating', 'Credit Approval', 'Sales', 'HR', 'Finance']}
                                {...form.getInputProps('associatedModule')}
                            />
                            <NumberInput
                                label="SLA (Days)"
                                min={0}
                                decimalScale={1}
                                step={0.5}
                                {...form.getInputProps('slaDurationDays')}
                            />
                        </Stack>

                        {code && (
                            <Stack mt="xl">
                                <Group justify="space-between">
                                    <Title order={3}>Stages</Title>
                                    <Button variant="light" onClick={openAddModal}>Add Stage</Button>
                                </Group>

                                <Paper withBorder p="sm">
                                    <Table>
                                        <Table.Thead>
                                            <Table.Tr>
                                                <Table.Th>Sequence</Table.Th>
                                                <Table.Th>Name</Table.Th>
                                                <Table.Th>Code</Table.Th>
                                                <Table.Th>Is Nested?</Table.Th>
                                                <Table.Th>Nested Code</Table.Th>
                                                <Table.Th>Screen</Table.Th>
                                                <Table.Th>Actions</Table.Th>
                                                <Table.Th>Actions</Table.Th>
                                            </Table.Tr>
                                        </Table.Thead>
                                        <Table.Tbody>
                                            {stages.sort((a, b) => a.sequenceOrder - b.sequenceOrder).map((s, index) => (
                                                <Table.Tr key={s.stageCode}>
                                                    <Table.Td>{s.sequenceOrder}</Table.Td>
                                                    <Table.Td>{s.stageName}</Table.Td>
                                                    <Table.Td>{s.stageCode}</Table.Td>
                                                    <Table.Td>{s.isNestedWorkflow ? 'Yes' : 'No'}</Table.Td>
                                                    <Table.Td>{s.nestedWorkflowCode || '-'}</Table.Td>
                                                    <Table.Td>{s.screenCode || '-'}</Table.Td>
                                                    <Table.Td>{s.allowedActions || '-'}</Table.Td>
                                                    <Table.Td>
                                                        <Group gap="xs">
                                                            <ActionIcon variant="subtle" color="blue" onClick={() => openEditModal(index)}>
                                                                <IconEdit size={16} />
                                                            </ActionIcon>
                                                            <ActionIcon variant="subtle" color="red" onClick={() => handleDeleteStage(index)}>
                                                                <IconTrash size={16} />
                                                            </ActionIcon>
                                                        </Group>
                                                    </Table.Td>
                                                </Table.Tr>
                                            ))}
                                        </Table.Tbody>
                                    </Table>
                                </Paper>
                            </Stack>
                        )}
                    </Tabs.Panel>

                    <Tabs.Panel value="diagram">
                        <BpmnVisualizer stages={stages} workflow={form.values} />
                    </Tabs.Panel>
                </Tabs>
            </Paper>

            <Modal opened={opened} onClose={close} title={editingIndex !== null ? "Edit Stage" : "Add Stage"} size="lg">
                <form onSubmit={stageForm.onSubmit(handleStageSubmit)}>
                    <Stack>
                        <SimpleGrid cols={2}>
                            <TextInput label="Stage Name" required {...stageForm.getInputProps('stageName')} />
                            <TextInput label="Stage Code" required {...stageForm.getInputProps('stageCode')} />
                            <NumberInput label="Sequence Order" required min={1} {...stageForm.getInputProps('sequenceOrder')} />
                            <Group align="flex-end">
                                <Checkbox
                                    label="Is Nested Workflow?"
                                    {...stageForm.getInputProps('isNestedWorkflow', { type: 'checkbox' })}
                                />
                            </Group>
                        </SimpleGrid>

                        {/* Parallel Configuration */}
                        <Paper withBorder p="xs" bg="gray.0">
                            <Title order={6} mb="xs">Execution Mode</Title>
                            <Group align="flex-end">
                                <TextInput
                                    label="Parallel Group ID"
                                    placeholder="e.g. GROUP_A"
                                    description="Assign same ID & Sequence to run in parallel"
                                    style={{ flex: 1 }}
                                    {...stageForm.getInputProps('parallelGrouping')}
                                />
                            </Group>
                        </Paper>

                        {stageForm.values.isNestedWorkflow && (
                            <TextInput
                                label="Nested Workflow Code"
                                placeholder="e.g. SUB_PROCESS_01"
                                required
                                {...stageForm.getInputProps('nestedWorkflowCode')}
                            />
                        )}

                        {!stageForm.values.isNestedWorkflow && (
                            <>
                                <SimpleGrid cols={2}>
                                    <Select
                                        label="Screen Implementation"
                                        placeholder="Select Screen"
                                        data={screens.map(s => ({ value: s.screenCode, label: `${s.description} (${s.screenCode})` }))}
                                        {...stageForm.getInputProps('screenCode')}
                                    />
                                    <Select
                                        label="Access Type"
                                        data={['EDITABLE', 'READ_ONLY']}
                                        {...stageForm.getInputProps('accessType')}
                                    />
                                </SimpleGrid>

                                <Title order={5} mt="sm">Hooks (Flowable Listeners)</Title>
                                <SimpleGrid cols={2}>
                                    <TextInput label="Pre-Entry Class (ExecutionListener: start)" placeholder="com.example.MyStartListener" {...stageForm.getInputProps('preEntryHook')} />
                                    <TextInput label="Post-Entry Class (TaskListener: create)" placeholder="com.example.MyCreateListener" {...stageForm.getInputProps('postEntryHook')} />
                                    <TextInput label="Pre-Exit Class (TaskListener: complete)" placeholder="com.example.MyCompleteListener" {...stageForm.getInputProps('preExitHook')} />
                                    <TextInput label="Post-Exit Class (ExecutionListener: end)" placeholder="com.example.MyEndListener" {...stageForm.getInputProps('postExitHook')} />
                                </SimpleGrid>

                                <Title order={5} mt="sm">Outcome Actions</Title>
                                <TextInput
                                    label="Allowed Actions"
                                    placeholder="e.g. APPROVE,REJECT,ON-HOLD (comma separated)"
                                    description="Leave empty for default 'Complete' action"
                                    {...stageForm.getInputProps('allowedActions')}
                                />
                            </>
                        )}

                        <Button type="submit">{editingIndex !== null ? "Update" : "Add"}</Button>
                    </Stack>
                </form>
            </Modal>

        </Container>
    );
}

export default WorkflowEditor;
