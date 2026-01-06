import { Handle, Position, type NodeProps } from 'reactflow';
import { Paper, Text, Group, ThemeIcon, RingProgress, Center, Stack } from '@mantine/core';
import { IconUser, IconSettings, IconArrowRight, IconGitBranch, IconCheck, IconClock } from '@tabler/icons-react';

// Start Event Node (Circle)
export function BPMNStartNode({ data }: NodeProps) {
    return (
        <Stack align="center" gap={5}>
            <RingProgress
                size={40}
                thickness={4}
                roundCaps
                sections={[{ value: 100, color: 'green' }]}
                label={
                    <Center>
                        <ThemeIcon color="green" variant="transparent" size="sm">
                            <IconArrowRight size={14} />
                        </ThemeIcon>
                    </Center>
                }
            />
            <Text size="xs" fw={500}>{data.label}</Text>
            <Handle type="source" position={Position.Right} style={{ opacity: 0 }} />
        </Stack>
    );
}

// End Event Node (Bold Circle)
export function BPMNEndNode({ data }: NodeProps) {
    return (
        <Stack align="center" gap={5}>
            <RingProgress
                size={40}
                thickness={6}
                roundCaps
                sections={[{ value: 100, color: 'red' }]}
                label={
                    <Center>
                        <ThemeIcon color="red" variant="transparent" size="sm">
                            <IconCheck size={14} />
                        </ThemeIcon>
                    </Center>
                }
            />
            <Text size="xs" fw={500}>{data.label}</Text>
            <Handle type="target" position={Position.Left} style={{ opacity: 0 }} />
        </Stack>
    );
}

// User Task Node (Rounded Rect with User Icon)
export function BPMNUserTaskNode({ data }: NodeProps) {
    return (
        <>
            <Handle type="target" position={Position.Left} />
            <Paper withBorder p="xs" radius="md" shadow="xs" w={150} style={{ borderColor: '#228be6', borderWidth: 2 }}>
                <Group wrap="nowrap" gap="xs">
                    <ThemeIcon color="blue" variant="light" size="md">
                        <IconUser size={16} />
                    </ThemeIcon>
                    <div style={{ overflow: 'hidden' }}>
                        <Text size="sm" fw={600} truncate>{data.label}</Text>
                        <Text size="xs" c="dimmed" truncate>{data.subLabel || 'User Task'}</Text>
                    </div>
                </Group>
                {/* Boundary Event Attachment Point (Bottom) */}
                {data.hasBoundary && (
                    <div style={{ position: 'absolute', bottom: -10, right: 10 }}>
                        <ThemeIcon color="orange" radius="xl" size="sm" variant="filled" style={{ border: '2px solid white' }}>
                            <IconClock size={12} />
                        </ThemeIcon>
                    </div>
                )}
            </Paper>

            <Handle type="source" position={Position.Right} id="right" />
            <Handle type="source" position={Position.Top} id="top" style={{ top: 10, background: '#555' }} />
            <Handle type="source" position={Position.Bottom} id="bottom" style={{ bottom: 10, background: '#555' }} />
        </>
    );
}

// Service Task Node (Rounded Rect with Gear Icon)
export function BPMNServiceTaskNode({ data }: NodeProps) {
    return (
        <>
            <Handle type="target" position={Position.Left} />
            <Paper withBorder p="xs" radius="md" shadow="xs" w={150} style={{ borderColor: '#fab005', borderWidth: 2 }}>
                <Group wrap="nowrap" gap="xs">
                    <ThemeIcon color="yellow" variant="light" size="md">
                        <IconSettings size={16} />
                    </ThemeIcon>
                    <div style={{ overflow: 'hidden' }}>
                        <Text size="sm" fw={600} truncate>{data.label}</Text>
                        <Text size="xs" c="dimmed" truncate>{data.subLabel || 'Service Task'}</Text>
                    </div>
                </Group>
            </Paper>

            <Handle type="source" position={Position.Right} id="right" />
            <Handle type="source" position={Position.Top} id="top" style={{ top: 10, background: '#555' }} />
            <Handle type="source" position={Position.Bottom} id="bottom" style={{ bottom: 10, background: '#555' }} />
        </>
    );
}

// Call Activity (Nested Workflow)
export function BPMNCallActivityNode({ data }: NodeProps) {
    return (
        <>
            <Handle type="target" position={Position.Left} />
            <Paper withBorder p="xs" radius="md" shadow="xs" w={150} style={{ borderColor: '#7950f2', borderWidth: 4 }}>
                <Group wrap="nowrap" gap="xs">
                    <ThemeIcon color="violet" variant="light" size="md">
                        <IconGitBranch size={16} />
                    </ThemeIcon>
                    <div style={{ overflow: 'hidden' }}>
                        <Text size="sm" fw={600} truncate>{data.label}</Text>
                        <Text size="xs" c="dimmed" truncate>{data.subLabel || 'Call Activity'}</Text>
                    </div>
                </Group>
            </Paper>

            <Handle type="source" position={Position.Right} id="right" />
            <Handle type="source" position={Position.Top} id="top" style={{ top: 10, background: '#555' }} />
            <Handle type="source" position={Position.Bottom} id="bottom" style={{ bottom: 10, background: '#555' }} />
        </>
    );
}

// Gateway Node (Diamond shape simulation)
export function BPMNGatewayNode({ }: NodeProps) {
    return (
        <>
            <Handle type="target" position={Position.Left} />
            <div style={{
                width: 40,
                height: 40,
                backgroundColor: '#fff',
                border: '2px solid #868e96',
                transform: 'rotate(45deg)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: 4
            }}>
                <div style={{ transform: 'rotate(-45deg)' }}>
                    <Text size="xs" fw={700}>X</Text>
                </div>
            </div>
            {/* We might need multiple source handles based on logic, but for simple visualization, Right is OK */}
            <Handle type="source" position={Position.Right} id="right" />
            <Handle type="source" position={Position.Top} id="top" />
            <Handle type="source" position={Position.Bottom} id="bottom" />
        </>
    );
}

// Group Node (Container for Nested Workflow)
export function BPMNGroupNode({ data }: any) {
    return (
        <div style={{
            height: '100%', width: '100%',
            border: '2px dashed #adb5bd',
            backgroundColor: 'rgba(240, 240, 240, 0.5)',
            borderRadius: 8,
            position: 'relative'
        }}>
            <div style={{
                position: 'absolute',
                top: -24,
                left: 0,
                background: '#e9ecef',
                padding: '2px 8px',
                borderRadius: 4,
                fontSize: 12,
                fontWeight: 600,
                color: '#495057'
            }}>
                {data.label}
            </div>
        </div>
    );
}
