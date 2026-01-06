import { Container, Title, Button, TextInput, Group, Stack, Paper, Select, NumberInput, Modal, Table, ActionIcon, SimpleGrid, Tabs, SegmentedControl, Text, Code, Badge } from '@mantine/core';
import { IconEdit, IconTrash, IconGitBranch, IconUser, IconSettings, IconGavel, IconArrowsSplit, IconSitemap, IconInfoCircle, IconBolt, IconPlug } from '@tabler/icons-react';
import { useForm } from '@mantine/form';
import { useDisclosure } from '@mantine/hooks';
import { useParams, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { BpmnVisualizer } from '../components/bpmn/BpmnVisualizer';
import { FlowchartVisualizer } from '../components/bpmn/FlowchartVisualizer';

// Define types locally or import from a shared types file
interface StageAction {
    id?: number;
    actionLabel: string;
    buttonStyle: string;
    targetType: string;
    targetStage?: string;
    postActionStatus?: string;
    actionType?: string; // COMPLETION, ERROR_TRIGGER
    errorCode?: string; // REWORK_REQUIRED
}

interface StageConfig {
    stageName: string;
    stageCode: string;
    sequenceOrder: number;
    isNestedWorkflow?: boolean;
    nestedWorkflowCode?: string;
    screenCode?: string;
    accessType?: string;
    preEntryHook?: string;
    postEntryHook?: string;
    preExitHook?: string;
    postExitHook?: string;
    actions?: StageAction[]; // Refactored
    parallelGrouping?: string;
    isRuleStage?: boolean;
    ruleKey?: string;
    isServiceTask?: boolean;
    delegateExpression?: string;
    entryCondition?: string;
    // Legacy support for display if needed, but we prefer 'actions'
    allowedActions?: string;
    routingRules?: string; // JSON string
    routingRulesList?: any[]; // UI only
    exceptionRules?: string; // JSON String
    exceptionRulesList?: any[]; // UI only
}



function WorkflowEditor() {
    const { code } = useParams();
    const navigate = useNavigate();
    const [opened, { open, close }] = useDisclosure(false);
    const [stages, setStages] = useState<StageConfig[]>([]);
    const [deletedStages, setDeletedStages] = useState<StageConfig[]>([]);
    const [editingIndex, setEditingIndex] = useState<number | null>(null);
    const [modules, setModules] = useState<string[]>([]);

    useEffect(() => {
        fetch('/api/modules')
            .then(res => res.json())
            .then((data: any[]) => setModules(data.map(m => m.moduleName)))
            .catch(err => {
                console.error("Failed to load modules", err);
                // Fallback
                setModules(['Overall', 'Credit Initiation', 'Financial Spreading', 'Credit Rating', 'Credit Approval', 'Sales', 'HR', 'Finance']);
            });
    }, []);

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
            // screenCode: '', // Removed
            // accessType: 'EDITABLE', // Removed
            preEntryHook: '',
            postEntryHook: '',
            preExitHook: '',
            postExitHook: '',
            actions: [] as StageAction[],
            parallelGrouping: '',
            isRuleStage: false,
            ruleKey: '',
            isServiceTask: false,
            delegateExpression: '',
            entryCondition: '',
            routingRulesList: [] as { condition: string, targetStageCode: string }[],
            exceptionRulesList: [] as { errorCode: string, targetStageCode: string }[]
        }
    });

    // Validations or effects can go here

    const handleStageSubmit = (values: typeof stageForm.values) => {
        if (editingIndex !== null) {
            // Update existing
            const updated = [...stages];
            const payload = {
                ...values,
                routingRules: JSON.stringify(values.routingRulesList)
            };
            updated[editingIndex] = payload;
            setStages(updated);
        } else {
            // Add new
            const payload = {
                ...values,
                routingRules: JSON.stringify(values.routingRulesList)
            };
            setStages([...stages, payload]);
        }
        close();
        stageForm.reset();
        setEditingIndex(null);
    };

    const openAddModal = () => {
        setEditingIndex(null);
        stageForm.reset();
        // Auto-increment sequence order
        const maxSeq = stages.length > 0 ? Math.max(...stages.map(s => s.sequenceOrder)) : 0;
        stageForm.setFieldValue('sequenceOrder', maxSeq + 1);
        open();
    };

    const openEditModal = (index: number) => {
        setEditingIndex(index);
        const stage = stages[index];
        stageForm.setValues({
            ...stage,
            // screenCode: stage.screenCode || '',
            // accessType: stage.accessType || 'EDITABLE',
            preEntryHook: stage.preEntryHook || '',
            postEntryHook: stage.postEntryHook || '',
            preExitHook: stage.preExitHook || '',
            postExitHook: stage.postExitHook || '',
            actions: stage.actions || [],
            parallelGrouping: stage.parallelGrouping || '',
            isRuleStage: stage.isRuleStage || false,
            ruleKey: stage.ruleKey || '',
            isServiceTask: (stage as any).isServiceTask || false,
            delegateExpression: (stage as any).delegateExpression || '',
            entryCondition: stage.entryCondition || '',
            routingRulesList: stage.routingRules ? JSON.parse(stage.routingRules) : [],
            exceptionRulesList: (stage as any).exceptionRules ? JSON.parse((stage as any).exceptionRules) : []
        });
        open();
    };

    const handleDeleteStage = (index: number) => {
        const stage = stages[index];
        // If stage has code/id (persisted), add to deleted list to process on save
        if (stage.stageCode && code) { // Only track if we are editing an existing workflow? Or just if stage itself is old?
            // Actually, the API needs workflow code and stage code.
            // If it's a new stage (not saved yet), we don't need to call delete API.
            // We can check if `code` (workflow param) exists.
            if (code) {
                setDeletedStages([...deletedStages, stage]);
            }
        }
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

                // Mapping save removed as Screen Implementation is deprecated
            }

            // 3. Delete Removed Stages
            for (const del of deletedStages) {
                console.log("Deleting Stage:", del.stageCode);
                try {
                    await fetch(`/api/workflows/${wfCode}/stages/${del.stageCode}`, { method: 'DELETE' });
                } catch (e) {
                    console.error("Failed to delete stage", del.stageCode, e);
                }
            }
            setDeletedStages([]); // Clear after save

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
                        <Tabs.Tab value="diagram" leftSection={<IconGitBranch size={14} />}>BPMN Diagram</Tabs.Tab>
                        <Tabs.Tab value="tree" leftSection={<IconSitemap size={14} />}>Decision Tree</Tabs.Tab>
                    </Tabs.List>

                    <Tabs.Panel value="config">
                        <Stack>
                            <TextInput label="Workflow Name" required {...form.getInputProps('workflowName')} />
                            <TextInput label="Workflow Code" required disabled={!!code} {...form.getInputProps('workflowCode')} />


                            <Select
                                label="Module"
                                data={modules.length > 0 ? modules : ['Overall', 'Credit Initiation', 'Financial Spreading', 'Credit Rating', 'Credit Approval', 'Sales', 'HR', 'Finance']}
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
                                                <Table.Th>Type</Table.Th>
                                                <Table.Th>Detail</Table.Th>
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
                                                    <Table.Td>
                                                        <Stack gap={4}>
                                                            {s.isNestedWorkflow ? <Group gap={5}><IconSettings size={16} /><Text size="xs">Nested</Text></Group> :
                                                                s.isRuleStage ? <Group gap={5}><IconGavel size={16} /><Text size="xs">Rule</Text></Group> :
                                                                    <Group gap={5}><IconUser size={16} /><Text size="xs">User</Text></Group>
                                                            }
                                                            {s.routingRules && s.routingRules !== '[]' && (
                                                                <Group gap={5}>
                                                                    <IconArrowsSplit size={16} color="orange" />
                                                                    <Text size="xs" c="orange" fw={500}>Branch</Text>
                                                                </Group>
                                                            )}
                                                        </Stack>
                                                    </Table.Td>
                                                    <Table.Td>
                                                        {s.isNestedWorkflow ? s.nestedWorkflowCode : (s.isRuleStage ? s.ruleKey : '-')}
                                                    </Table.Td>
                                                    <Table.Td>{s.actions?.map(a => a.actionLabel).join(', ') || s.allowedActions || '-'}</Table.Td>
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

                    <Tabs.Panel value="tree">
                        <FlowchartVisualizer stages={stages} workflow={form.values} />
                    </Tabs.Panel>
                </Tabs>
            </Paper>

            <Modal opened={opened} onClose={close} title={editingIndex !== null ? "Edit Stage" : "Add Stage"} size="xl">
                <form onSubmit={stageForm.onSubmit(handleStageSubmit)}>
                    <Paper withBorder p="xs" mb="md" bg="gray.0">
                        <Group justify="space-between">
                            <div>
                                <Text fw={700} size="lg">{stageForm.values.stageName || 'New Stage'}</Text>
                                <Group gap="xs">
                                    <Code>{stageForm.values.stageCode || 'NO_CODE'}</Code>
                                    <Badge variant="outline">
                                        {stageForm.values.isNestedWorkflow ? 'NESTED' :
                                            stageForm.values.isRuleStage ? 'RULE' :
                                                stageForm.values.isServiceTask ? 'SERVICE' : 'USER'}
                                    </Badge>
                                </Group>
                            </div>
                        </Group>
                    </Paper>

                    <Tabs defaultValue="general">
                        <Tabs.List mb="md">
                            <Tabs.Tab value="general" leftSection={<IconInfoCircle size={14} />}>General</Tabs.Tab>
                            <Tabs.Tab value="config" leftSection={<IconSettings size={14} />}>Configuration</Tabs.Tab>
                            <Tabs.Tab value="actions" leftSection={<IconBolt size={14} />}>Actions</Tabs.Tab>
                            <Tabs.Tab value="routing" leftSection={<IconArrowsSplit size={14} />}>Routing/Branching</Tabs.Tab>
                            <Tabs.Tab value="hooks" leftSection={<IconPlug size={14} />}>Hooks</Tabs.Tab>
                            {stageForm.values.isNestedWorkflow && <Tabs.Tab value="exceptions" leftSection={<IconBolt size={14} />}>Exceptions / Rework</Tabs.Tab>}
                        </Tabs.List>

                        <Tabs.Panel value="general">
                            <Stack>
                                <SimpleGrid cols={2}>
                                    <TextInput label="Stage Name" required {...stageForm.getInputProps('stageName')} />
                                    <TextInput label="Stage Code" required {...stageForm.getInputProps('stageCode')} />
                                </SimpleGrid>
                                <NumberInput label="Sequence Order" required min={1} {...stageForm.getInputProps('sequenceOrder')} />



                            </Stack>
                        </Tabs.Panel>

                        <Tabs.Panel value="config">
                            <Stack>
                                <Paper withBorder p="xs" mb="xs">
                                    <Text size="sm" fw={500} mb={5}>Stage Type</Text>
                                    <SegmentedControl
                                        value={
                                            stageForm.values.isNestedWorkflow ? 'NESTED' :
                                                stageForm.values.isRuleStage ? 'RULE' : 'USER'
                                        }
                                        onChange={(value) => {
                                            stageForm.setFieldValue('isNestedWorkflow', value === 'NESTED');
                                            stageForm.setFieldValue('isRuleStage', value === 'RULE');
                                            stageForm.setFieldValue('isServiceTask', value === 'SERVICE');
                                            if (value !== 'NESTED') stageForm.setFieldValue('nestedWorkflowCode', '');
                                            if (value !== 'RULE') stageForm.setFieldValue('ruleKey', '');
                                            if (value !== 'SERVICE') stageForm.setFieldValue('delegateExpression', '');
                                        }}
                                        data={[
                                            { label: 'User Task', value: 'USER' },
                                            { label: 'Service Task', value: 'SERVICE' },
                                            { label: 'Nested Workflow', value: 'NESTED' },
                                            { label: 'Business Rule', value: 'RULE' },
                                        ]}
                                        fullWidth
                                    />
                                </Paper>

                                {stageForm.values.isNestedWorkflow && (
                                    <TextInput
                                        label="Nested Workflow Code"
                                        placeholder="WORKFLOW_CODE"
                                        required
                                        {...stageForm.getInputProps('nestedWorkflowCode')}
                                    />
                                )}

                                {stageForm.values.isRuleStage && (
                                    <TextInput
                                        label="Rule Key (Decision Table ID)"
                                        placeholder="e.g. RISK_RULE"
                                        description="The Key of the uploaded DMN table"
                                        required
                                        {...stageForm.getInputProps('ruleKey')}
                                    />
                                )}

                                {stageForm.values.isServiceTask && (
                                    <TextInput
                                        label="Delegate Expression"
                                        placeholder="${myDelegate}"
                                        description="Expression resolving to a JavaDelegate bean (e.g. ${testDelegate})"
                                        required
                                        {...stageForm.getInputProps('delegateExpression')}
                                    />
                                )}

                                <Paper withBorder p="xs" mt="sm">
                                    <Text size="sm" fw={500} mb={5}>Entry Rules</Text>
                                    <TextInput
                                        label="Entry Condition (Expression)"
                                        placeholder="${amount > 1000}"
                                        description="Full UEL Expression required. E.g. ${creditScore <= 500}. Wraps the entire condition."
                                        {...stageForm.getInputProps('entryCondition')}
                                    />
                                </Paper>

                                <Paper withBorder p="xs" mt="sm" bg="gray.0">
                                    <Title order={6} mb="xs">Execution Mode</Title>
                                    <TextInput
                                        label="Parallel Group ID"
                                        placeholder="e.g. GRP_APPROVAL"
                                        description="Stages with same Sequence & Group ID will run in parallel"
                                        {...stageForm.getInputProps('parallelGrouping')}
                                    />
                                </Paper>
                            </Stack>
                        </Tabs.Panel>

                        <Tabs.Panel value="hooks">
                            <SimpleGrid cols={2}>
                                <TextInput label="Pre-Entry" placeholder="com.example.Listener" {...stageForm.getInputProps('preEntryHook')} />
                                <TextInput label="Post-Entry" placeholder="com.example.Listener" {...stageForm.getInputProps('postEntryHook')} />
                                <TextInput label="Pre-Exit" placeholder="com.example.Listener" {...stageForm.getInputProps('preExitHook')} />
                                <TextInput label="Post-Exit" placeholder="com.example.Listener" {...stageForm.getInputProps('postExitHook')} />
                            </SimpleGrid>
                        </Tabs.Panel>

                        <Tabs.Panel value="actions">
                            <Stack>
                                <Group justify="space-between">
                                    <Text fw={500}>Stage Actions</Text>
                                    <Button size="xs" variant="light" onClick={() => {
                                        const newAction: StageAction = { actionLabel: 'New Action', buttonStyle: 'primary', targetType: 'NEXT' };
                                        stageForm.insertListItem('actions', newAction);
                                    }}>
                                        + Add Action
                                    </Button>
                                </Group>

                                {stageForm.values.actions?.map((action, idx) => (
                                    <Paper key={idx} withBorder p="xs" mb="xs" bg="gray.0">
                                        <Group justify="space-between" mb="xs">
                                            <Text fw={500} size="sm">Action {idx + 1}</Text>
                                            <ActionIcon color="red" size="sm" onClick={() => stageForm.removeListItem('actions', idx)}>
                                                <IconTrash size={14} />
                                            </ActionIcon>
                                        </Group>
                                        <SimpleGrid cols={2}>
                                            <TextInput label="Label" size="xs" required {...stageForm.getInputProps(`actions.${idx}.actionLabel`)} />
                                            <Select label="Style" size="xs" data={['primary', 'success', 'danger', 'warning', 'default']} {...stageForm.getInputProps(`actions.${idx}.buttonStyle`)} />

                                            <Select
                                                label="Action Type"
                                                size="xs"
                                                data={[
                                                    { label: 'Standard Completion', value: 'COMPLETION' },
                                                    { label: 'Trigger Error / Rework', value: 'ERROR_TRIGGER' }
                                                ]}
                                                {...stageForm.getInputProps(`actions.${idx}.actionType`)}
                                            />

                                            {stageForm.values.actions![idx].actionType === 'ERROR_TRIGGER' ? (
                                                <TextInput
                                                    label="Error Code"
                                                    size="xs"
                                                    placeholder="REWORK_REQUIRED"
                                                    required
                                                    {...stageForm.getInputProps(`actions.${idx}.errorCode`)}
                                                />
                                            ) : (
                                                <Select
                                                    label="Target Type"
                                                    size="xs"
                                                    data={['NEXT', 'SPECIFIC', 'END']}
                                                    {...stageForm.getInputProps(`actions.${idx}.targetType`)}
                                                />
                                            )}

                                            {stageForm.values.actions![idx].actionType !== 'ERROR_TRIGGER' && action.targetType === 'SPECIFIC' && (
                                                <Select
                                                    label="Target Stage"
                                                    size="xs"
                                                    data={stages.filter(s => s.stageCode !== stageForm.values.stageCode).map(s => s.stageCode)}
                                                    {...stageForm.getInputProps(`actions.${idx}.targetStage`)}
                                                />
                                            )}

                                            {stageForm.values.actions![idx].actionType !== 'ERROR_TRIGGER' && (
                                                <TextInput label="Post-Status" size="xs" placeholder="APPROVED" {...stageForm.getInputProps(`actions.${idx}.postActionStatus`)} />
                                            )}
                                        </SimpleGrid>
                                    </Paper>
                                ))}
                                {(!stageForm.values.actions || stageForm.values.actions.length === 0) && (
                                    <Text size="xs" c="dimmed" ta="center" py="sm">No actions configured. Standard completion.</Text>
                                )}
                            </Stack>
                        </Tabs.Panel>

                        <Tabs.Panel value="routing">
                            <Stack>
                                <Paper withBorder p="xs" bg="blue.0" mb="sm">
                                    <Text size="sm">Define logic to branch to different stages based on variables. Rules are evaluated in order.</Text>
                                </Paper>
                                <Group justify="space-between">
                                    <Text fw={500}>Branching Rules</Text>
                                    <Button size="xs" variant="light" onClick={() => {
                                        stageForm.insertListItem('routingRulesList', { condition: '', targetStageCode: '' });
                                    }}>
                                        + Add Rule
                                    </Button>
                                </Group>

                                <Table>
                                    <Table.Thead>
                                        <Table.Tr>
                                            <Table.Th style={{ width: '50%' }}>Condition (ex: ${"${amount > 1000}"})</Table.Th>
                                            <Table.Th>Target Stage</Table.Th>
                                            <Table.Th style={{ width: 50 }}></Table.Th>
                                        </Table.Tr>
                                    </Table.Thead>
                                    <Table.Tbody>
                                        {stageForm.values.routingRulesList?.map((_: any, idx: number) => (
                                            <Table.Tr key={idx}>
                                                <Table.Td>
                                                    <TextInput
                                                        placeholder="${variable == 'value'}"
                                                        {...stageForm.getInputProps(`routingRulesList.${idx}.condition`)}
                                                    />
                                                </Table.Td>
                                                <Table.Td>
                                                    <Select
                                                        placeholder="Select Stage"
                                                        data={stages.filter(s => s.stageCode !== stageForm.values.stageCode).map(s => s.stageCode)}
                                                        {...stageForm.getInputProps(`routingRulesList.${idx}.targetStageCode`)}
                                                    />
                                                </Table.Td>
                                                <Table.Td>
                                                    <ActionIcon color="red" onClick={() => stageForm.removeListItem('routingRulesList', idx)}>
                                                        <IconTrash size={16} />
                                                    </ActionIcon>
                                                </Table.Td>
                                            </Table.Tr>
                                        ))}
                                        {(!stageForm.values.routingRulesList || stageForm.values.routingRulesList.length === 0) && (
                                            <Table.Tr>
                                                <Table.Td colSpan={3} align="center">
                                                    <Text size="xs" c="dimmed">No branching rules. Flow continues to next sequence.</Text>
                                                </Table.Td>
                                            </Table.Tr>
                                        )}
                                    </Table.Tbody>
                                </Table>
                            </Stack>
                        </Tabs.Panel>
                    </Tabs >

                    <Button type="submit" mt="md" fullWidth>{editingIndex !== null ? "Update Stage" : "Add Stage"}</Button>
                </form >
            </Modal >

        </Container >
    );
}

export default WorkflowEditor;
