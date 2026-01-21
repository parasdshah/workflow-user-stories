import { Timeline, Text, Badge, Card, Button, Group, Collapse, Loader, Modal, JsonInput, Select } from '@mantine/core';
import { IconCheck, IconCircleDashed, IconPlayerPlay, IconX, IconChevronDown, IconChevronRight } from '@tabler/icons-react';
import { useState, useEffect } from 'react';

interface StageDTO {
    stageCode: string;
    stageName: string;
    taskId?: string;
    status: string; // ACTIVE, COMPLETED
    assignee?: string;
    assigneeName?: string;
    createdTime?: string;
    endTime?: string;
    dueDate?: string;
    allowedActions?: string;
    actionTaken?: string;
    subProcessInstanceId?: string;
}

function NestedTimeline({ caseId }: { caseId: string }) {
    const [stages, setStages] = useState<StageDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [expandedSubProcesses, setExpandedSubProcesses] = useState<{ [key: string]: boolean }>({});

    useEffect(() => {
        fetch(`/api/runtime/cases/${caseId}/stages`)
            .then(res => res.json())
            .then(data => {
                console.log("Stages data received:", data);
                setStages(data);
            })
            .catch(err => console.error(err))
            .finally(() => setLoading(false));
    }, [caseId]);

    const toggleSubProcess = (processId: string) => {
        setExpandedSubProcesses(prev => ({
            ...prev,
            [processId]: !prev[processId]
        }));
    };

    if (loading) return <Loader size="xs" />;

    return (
        <Timeline active={stages.findIndex(s => s.status === 'ACTIVE')} bulletSize={18} lineWidth={1} style={{ marginTop: 10, marginBottom: 10 }}>
            {stages.map((stage, i) => (
                <Timeline.Item
                    key={i}
                    bullet={stage.status === 'COMPLETED' ? <IconCheck size={10} /> : (stage.status === 'ACTIVE' ? <IconPlayerPlay size={10} /> : <IconCircleDashed size={10} />)}
                    color={stage.status === 'COMPLETED' ? 'green' : (stage.status === 'ACTIVE' ? 'blue' : 'gray')}
                    title={<Text size="sm">{stage.stageName}</Text>}
                >
                    {stage.status === 'COMPLETED' && stage.actionTaken && (
                        <Badge size="xs" color={stage.actionTaken === 'REJECT' ? 'red' : 'green'}>{stage.actionTaken}</Badge>
                    )}
                    <Text c="dimmed" size="xs">
                        Assignee: {stage.assigneeName || stage.assignee || 'Unassigned'}
                    </Text>
                    <Text size="xs" mt={4} c="dimmed">
                        Created: {stage.createdTime ? new Date(stage.createdTime).toLocaleString() : '-'}
                    </Text>
                    {stage.endTime && (
                        <Text size="xs" mt={4} c="dimmed">
                            Completed: {new Date(stage.endTime).toLocaleString()}
                        </Text>
                    )}

                    {stage.subProcessInstanceId && (
                        <div style={{ marginTop: 8 }}>
                            <Button
                                variant="subtle"
                                size="xs"
                                onClick={() => toggleSubProcess(stage.subProcessInstanceId!)}
                                leftSection={expandedSubProcesses[stage.subProcessInstanceId!] ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                            >
                                {expandedSubProcesses[stage.subProcessInstanceId!] ? 'Hide Sub-process' : 'View Sub-process'}
                            </Button>
                            <Collapse in={expandedSubProcesses[stage.subProcessInstanceId!]}>
                                <Card withBorder padding="sm" mt="xs" bg="gray.0">
                                    <Text size="xs" fw={500} c="dimmed" mb="xs">Sub-workflow: {stage.subProcessInstanceId}</Text>
                                    {expandedSubProcesses[stage.subProcessInstanceId!] && (
                                        <NestedTimeline caseId={stage.subProcessInstanceId!} />
                                    )}
                                </Card>
                            </Collapse>
                        </div>
                    )}
                </Timeline.Item>
            ))}
        </Timeline>
    );
}

interface CaseTimelineProps {
    stages: StageDTO[];
    onAction?: (taskId: string, outcome?: string, variablesJson?: string) => void;
}

export function CaseTimeline({ stages, onAction }: CaseTimelineProps) {
    // Sort logic handled by backend, but safe to verify if needed

    const getBullet = (status: string) => {
        if (status === 'COMPLETED') return (
            <div style={{ backgroundColor: 'green', borderRadius: '50%', color: 'white', width: 20, height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <IconCheck size={14} />
            </div>
        );
        if (status === 'ACTIVE') return <IconPlayerPlay size={14} />;
        return <IconCircleDashed size={14} />;
    };

    const getColor = (status: string) => {
        if (status === 'COMPLETED') return 'green';
        if (status === 'ACTIVE') return 'blue';
        return 'gray';
    };

    const [expandedSubProcesses, setExpandedSubProcesses] = useState<{ [key: string]: boolean }>({});

    const toggleSubProcess = (processId: string) => {
        setExpandedSubProcesses(prev => ({
            ...prev,
            [processId]: !prev[processId]
        }));
    };


    const [modalOpen, setModalOpen] = useState(false);
    const [jsonInput, setJsonInput] = useState('{}');
    const [pendingAction, setPendingAction] = useState<{ taskId: string, outcome?: string, manualReq?: boolean } | null>(null);

    // Manual Assignment State
    const [manualAssignee, setManualAssignee] = useState<string | null>(null);
    const [manualUsers, setManualUsers] = useState<any[]>([]);
    const [manualLoading, setManualLoading] = useState(false);

    const initiateAction = (taskId: string, outcome?: string, allowedActionsJson?: string) => {
        // Check for Manual Assignment Requirement
        let requiresManual = false;
        let groupName = '';

        if (allowedActionsJson && outcome) {
            try {
                const actions = JSON.parse(allowedActionsJson);
                const actionDef = Array.isArray(actions) ? actions.find((a: any) =>
                    (a.value === outcome || a.label === outcome)
                ) : null;

                if (actionDef && actionDef.requiresManualAssignment) {
                    requiresManual = true;
                    groupName = actionDef.assignmentGroup;
                }
            } catch (e) {
                console.warn("Failed to check manual assignment req", e);
            }
        }

        setPendingAction({ taskId, outcome, manualReq: requiresManual });
        setJsonInput('{\n  \n}');
        setManualAssignee(null);

        if (requiresManual) {
            setManualLoading(true);
            // Fetch users for the group
            // Ideally use an API that filters by Role/Group
            // Since we don't have a direct "Get Users By Role" exposed in UI easily, 
            // we might fallback to fetching all (or assume /api/hrms/users?role=X works)
            // Let's assume the standard /api/hrms/users handles basic listing or we use specific endpoint
            fetch(`/api/hrms/employees`)
                .then(res => res.json())
                .then(data => {
                    if (Array.isArray(data)) {
                        // Client-side filtering if 'role' or 'department' matches groupName
                        // If groupName is provided, we try to filter. 
                        // Note: Employee structure might differ, assumign 'role' or 'department' field exists.
                        const filtered = groupName
                            ? data.filter((u: any) => u.roleCode === groupName || u.department === groupName || !groupName)
                            : data;

                        setManualUsers(filtered.map((u: any) => ({ value: u.userId || u.employeeId, label: u.fullName || u.employeeName || u.userId })));
                    } else {
                        setManualUsers([]);
                    }
                })
                .catch(err => {
                    console.error("Failed to load manual assignees", err);
                    setManualUsers([]);
                })
                .finally(() => setManualLoading(false));
        }

        setModalOpen(true);
    };

    const confirmAction = () => {
        if (pendingAction && onAction) {
            let extraVars = {};
            try {
                extraVars = JSON.parse(jsonInput);
            } catch (e) {
                alert("Invalid JSON");
                return;
            }

            if (pendingAction.manualReq) {
                if (!manualAssignee) {
                    alert("Please select an assignee.");
                    return;
                }
                extraVars = { ...extraVars, manualAssignee: manualAssignee };
            }

            onAction(pendingAction.taskId, pendingAction.outcome, JSON.stringify(extraVars));
        }
        setModalOpen(false);
        setPendingAction(null);
    };

    // userMap logic removed as backend provides assigneeName

    return (
        <Card withBorder padding="xl" radius="md">
            <Text size="lg" fw={500} mb="xl">Case Progress</Text>
            <Timeline active={stages.findIndex(s => s.status === 'ACTIVE')} bulletSize={24} lineWidth={2}>
                {stages.map((stage, index) => (
                    <Timeline.Item
                        key={`${stage.stageCode}-${index}`}
                        bullet={getBullet(stage.status)}
                        color={getColor(stage.status)}
                        title={stage.stageName}
                    >
                        {(() => {
                            if (!stage.actionTaken || !stage.allowedActions) return null;
                            try {
                                const actions = JSON.parse(stage.allowedActions);
                                const action = Array.isArray(actions) ? actions.find((a: any) =>
                                    (typeof a === 'string' ? a : (a.value || a.actionLabel)) === stage.actionTaken
                                ) : null;
                                const postStatus = action && typeof action !== 'string' ? action.postStatus : null;

                                if (postStatus) {
                                    return (
                                        <Text size="xs" mt={2} fw={500} c="dimmed">
                                            Status: {postStatus}
                                        </Text>
                                    );
                                }
                            } catch (e) { }
                            return null;
                        })()}

                        <Text c="dimmed" size="sm">
                            Assignee: {stage.assigneeName ? `${stage.assigneeName} (${stage.assignee})` : (stage.assignee || 'Unassigned')}
                        </Text>
                        <Text size="xs" mt={4}>
                            Created: {stage.createdTime ? new Date(stage.createdTime).toLocaleString() : '-'}

                        </Text>
                        {stage.endTime && (
                            <Text size="xs" mt={4}>
                                Completed: {new Date(stage.endTime).toLocaleString()}
                            </Text>
                        )}

                        {stage.status === 'COMPLETED' && stage.actionTaken && (
                            <Badge
                                color={(() => {
                                    if (!stage.allowedActions) return stage.actionTaken === 'REJECT' ? 'red' : 'green';
                                    try {
                                        const actions = JSON.parse(stage.allowedActions);
                                        const action = Array.isArray(actions) ? actions.find((a: any) =>
                                            (typeof a === 'string' ? a : (a.value || a.actionLabel)) === stage.actionTaken
                                        ) : null;

                                        if (action) {
                                            const style = typeof action === 'string' ? (action === 'REJECT' ? 'red' : 'blue') : (action.buttonStyle || action.style || 'default');
                                            if (style === 'danger' || style === 'red') return 'red';
                                            if (style === 'success' || style === 'green') return 'green';
                                            if (style === 'warning' || style === 'orange') return 'orange';
                                            if (style === 'default' || style === 'gray') return 'gray';
                                            return 'blue';
                                        }
                                    } catch (e) { }
                                    return stage.actionTaken === 'REJECT' ? 'red' : 'green';
                                })()}
                                size="sm"
                                mt={4}
                                leftSection={stage.actionTaken === 'REJECT' ? <IconX size={12} /> : <IconCheck size={12} />}
                            >
                                Action: {stage.actionTaken}
                            </Badge>
                        )}
                        {stage.status === 'ACTIVE' && (
                            <Group mt="xs">
                                <Badge color="blue" size="xs">In Progress</Badge>
                                {stage.taskId && onAction && (
                                    <>
                                        {stage.allowedActions ? (() => {
                                            try {
                                                const parsed = JSON.parse(stage.allowedActions);
                                                // Handle Array of Strings (Legacy) or Array of Objects (New)
                                                const actions = Array.isArray(parsed) ? parsed : [];

                                                return actions.map((action: any, idx: number) => {
                                                    const label = typeof action === 'string' ? action : action.label || action.actionLabel;
                                                    const value = typeof action === 'string' ? action : action.value || action.actionLabel;

                                                    // Map style configuration to Mantine colors
                                                    const actionStyle = typeof action === 'string' ? (action === 'REJECT' ? 'red' : 'blue') : (action.buttonStyle || action.style || 'default');
                                                    let color = 'blue';
                                                    if (actionStyle === 'danger' || actionStyle === 'red') color = 'red';
                                                    else if (actionStyle === 'success' || actionStyle === 'green') color = 'green';
                                                    else if (actionStyle === 'warning' || actionStyle === 'orange') color = 'orange';
                                                    else if (actionStyle === 'default' || actionStyle === 'gray') color = 'gray';

                                                    return (
                                                        <Button
                                                            key={`${idx}-${label}`}
                                                            size="xs"
                                                            variant={actionStyle === 'default' ? 'default' : 'outline'}
                                                            color={actionStyle === 'default' ? undefined : color}

                                                            onClick={() => initiateAction(stage.taskId!, value, stage.allowedActions)}
                                                        >
                                                            {label}
                                                        </Button>
                                                    );
                                                });
                                            } catch (e) {
                                                // Fallback for simple comma-separated string if JSON parse fails
                                                return stage.allowedActions.replace(/[\[\]"]/g, '').split(',').map(action => (
                                                    <Button
                                                        key={action.trim()}
                                                        size="xs"
                                                        variant="outline"
                                                        color={action.trim() === 'REJECT' ? 'red' : 'blue'}

                                                        onClick={() => initiateAction(stage.taskId!, action.trim())}
                                                    >
                                                        {action.trim()}
                                                    </Button>
                                                ));
                                            }
                                        })() : (
                                            <Button size="xs" variant="outline" onClick={() => initiateAction(stage.taskId!, undefined, stage.allowedActions)}>
                                                Complete
                                            </Button>
                                        )}
                                    </>
                                )}
                            </Group>
                        )}

                        {stage.subProcessInstanceId && (
                            <div style={{ marginTop: 8 }}>
                                <Button
                                    variant="subtle"
                                    size="xs"
                                    onClick={() => toggleSubProcess(stage.subProcessInstanceId!)}
                                    leftSection={expandedSubProcesses[stage.subProcessInstanceId!] ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                                >
                                    {expandedSubProcesses[stage.subProcessInstanceId!] ? 'Hide Sub-process' : 'View Sub-process'}
                                </Button>
                                <Collapse in={expandedSubProcesses[stage.subProcessInstanceId!]}>
                                    <Card withBorder padding="sm" mt="xs" bg="gray.0">
                                        <Text size="xs" fw={500} c="dimmed" mb="xs">Sub-workflow: {stage.subProcessInstanceId}</Text>
                                        {expandedSubProcesses[stage.subProcessInstanceId!] && (
                                            <NestedTimeline caseId={stage.subProcessInstanceId!} />
                                        )}
                                    </Card>
                                </Collapse>
                            </div>
                        )}
                    </Timeline.Item>
                ))}
            </Timeline>

            <Modal opened={modalOpen} onClose={() => setModalOpen(false)} title="Confirm Action">
                {pendingAction?.manualReq && (
                    <Card withBorder mb="md" p="xs" bg="blue.0">
                        <Text size="sm" fw={500} mb="xs">Select Next Assignee</Text>
                        {manualLoading ? <Loader size="sm" /> : (
                            <Select
                                data={manualUsers}
                                placeholder="Search User..."
                                searchable
                                value={manualAssignee}
                                onChange={setManualAssignee}
                                description="This action requires you to manually select who handles the next stage."
                            />
                        )}
                    </Card>
                )}

                <Text size="sm" mb="sm">Provide any process variables (JSON) for this action:</Text>
                <JsonInput
                    label="Process Variables"
                    placeholder="{ 'amount': 5000 }"
                    validationError="Invalid JSON"
                    formatOnBlur
                    autosize
                    minRows={4}
                    value={jsonInput}
                    onChange={setJsonInput}
                />
                <Group justify="end" mt="md">
                    <Button variant="default" onClick={() => setModalOpen(false)}>Cancel</Button>
                    <Button onClick={confirmAction}>Confirm</Button>
                </Group>
            </Modal>
        </Card >
    );
}
