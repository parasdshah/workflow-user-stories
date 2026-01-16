import { Paper, SegmentedControl, Stack, Text, TextInput, Group, Select, Button, ActionIcon, Card, SimpleGrid, Badge } from '@mantine/core';
import { IconTrash } from '@tabler/icons-react';
import { useState, useEffect } from 'react';

// Define the structure of our Assignment Rule
export interface AssignmentRule {
    variable: string; // amount, region, etc
    operator: string; // >, <, ==
    value: string;    // 50000, India
    role: string;     // MANAGER, APPROVER
    matrixRegion?: string; // Optional override
}

interface AssignmentBuilderProps {
    value: string; // JSON string from backend
    onChange: (json: string) => void;
}

export function AssignmentBuilder({ value, onChange }: AssignmentBuilderProps) {
    const [mechanism, setMechanism] = useState<string>('GROUP');
    const [groupName, setGroupName] = useState<string>('');
    const [matrixRules, setMatrixRules] = useState<AssignmentRule[]>([]);

    // Load initial state
    useEffect(() => {
        if (!value) return;
        try {
            const parsed = JSON.parse(value);
            setMechanism(parsed.mechanism || 'GROUP');
            setGroupName(parsed.groupName || '');
            setMatrixRules(parsed.rules || []);
        } catch (e) {
            console.warn("Invalid Assignment JSON", e);
        }
    }, [value]);

    // Save changes
    const [roles, setRoles] = useState<any[]>([]);

    useEffect(() => {
        fetch('/api/hrms/roles')
            .then(res => {
                if (!res.ok) throw new Error('API Error');
                return res.json();
            })
            .then(data => {
                if (Array.isArray(data)) setRoles(data);
                else setRoles([]);
            })
            .catch(err => {
                console.error("Failed to load roles", err);
                setRoles([]);
            });
    }, []);

    // Save changes
    const updateJson = (mech: string, grp: string, rules: AssignmentRule[]) => {
        const payload = {
            mechanism: mech,
            groupName: grp,
            rules: rules
        };
        onChange(JSON.stringify(payload));
    };

    const handleMechanismChange = (val: string) => {
        setMechanism(val);
        updateJson(val, groupName, matrixRules);
    };

    const handleGroupChange = (val: string) => {
        setGroupName(val);
        updateJson(mechanism, val, matrixRules);
    };

    const addRule = () => {
        const newRule = { variable: 'amount', operator: '>', value: '0', role: '' };
        const updated = [...matrixRules, newRule];
        setMatrixRules(updated);
        updateJson(mechanism, groupName, updated);
    };

    const removeRule = (idx: number) => {
        const updated = matrixRules.filter((_, i) => i !== idx);
        setMatrixRules(updated);
        updateJson(mechanism, groupName, updated);
    };

    const updateRule = (idx: number, field: keyof AssignmentRule, val: string) => {
        const updated = [...matrixRules];
        updated[idx] = { ...updated[idx], [field]: val };
        setMatrixRules(updated);
        updateJson(mechanism, groupName, updated);
    };

    const roleOptions = roles.map(r => ({ value: r.roleCode, label: r.roleName }));

    return (
        <Stack>
            <Paper p="sm" withBorder bg="gray.0">
                <Text size="sm" fw={500} mb="xs">Assignment Mechanism</Text>
                <SegmentedControl
                    fullWidth
                    value={mechanism}
                    onChange={handleMechanismChange}
                    data={[
                        { label: 'Group Queue', value: 'GROUP' },
                        { label: 'Round Robin', value: 'ROUND_ROBIN' },
                        { label: 'Matrix Rules', value: 'MATRIX' }
                    ]}
                />
            </Paper>

            {mechanism === 'GROUP' && (
                <Card withBorder p="sm">
                    <Text size="sm" mb={5}>Assign to Group</Text>
                    <Select
                        placeholder="Select Group/Role"
                        data={roleOptions}
                        searchable
                        value={groupName}
                        onChange={(val) => handleGroupChange(val || '')}
                    />
                    <Text size="xs" c="dimmed" mt={5}>Tasks will appear in the shared queue for this group.</Text>
                </Card>
            )}

            {mechanism === 'ROUND_ROBIN' && (
                <Card withBorder p="sm">
                    <Text size="sm" mb={5}>Round Robin Pool</Text>
                    <Select
                        placeholder="Select Pool/Role"
                        data={roleOptions}
                        searchable
                        value={groupName}
                        onChange={(val) => handleGroupChange(val || '')}
                    />
                    <Text size="xs" c="dimmed" mt={5}>Tasks will be automatically assigned to members of this group in turns.</Text>
                </Card>
            )}

            {mechanism === 'MATRIX' && (
                <Card withBorder p="sm">
                    <Group justify="space-between" mb="sm">
                        <Text fw={500} size="sm">Matrix Resolution Rules</Text>
                        <Button size="xs" variant="light" onClick={addRule}>+ Add Rule</Button>
                    </Group>

                    {matrixRules.length === 0 ? (
                        <Text size="xs" c="dimmed" ta="center" py="md">No rules defined. <br />Use this to select Approvers based on Amount, Region, etc.</Text>
                    ) : (
                        <Stack gap="xs">
                            {matrixRules.map((rule, idx) => (
                                <Paper key={idx} p="xs" withBorder>
                                    <Group justify="space-between" mb={5}>
                                        <Badge variant="dot" size="sm">Rule {idx + 1}</Badge>
                                        <ActionIcon color="red" size="sm" variant="subtle" onClick={() => removeRule(idx)}>
                                            <IconTrash size={14} />
                                        </ActionIcon>
                                    </Group>
                                    <SimpleGrid cols={5} spacing="xs">
                                        <Select
                                            placeholder="Var"
                                            data={['amount', 'region', 'product', 'riskScore']}
                                            value={rule.variable}
                                            onChange={(v) => updateRule(idx, 'variable', v || '')}
                                        />
                                        <Select
                                            placeholder="Op"
                                            data={['>', '<', '==', '!=', 'contains']}
                                            value={rule.operator}
                                            onChange={(v) => updateRule(idx, 'operator', v || '')}
                                        />
                                        <TextInput
                                            placeholder="Value"
                                            value={rule.value}
                                            onChange={(e) => updateRule(idx, 'value', e.currentTarget.value)}
                                        />
                                        <Text size="sm" ta="center" mt={5}>→</Text>
                                        <Select
                                            placeholder="Role"
                                            data={roleOptions}
                                            searchable
                                            value={rule.role}
                                            onChange={(v) => updateRule(idx, 'role', v || '')}
                                        />
                                    </SimpleGrid>
                                </Paper>
                            ))}
                        </Stack>
                    )}
                </Card>
            )}
        </Stack>
    );
}
