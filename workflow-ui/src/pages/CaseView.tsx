import { Container, Title, Paper, Group, Text, Grid, Badge, Loader, Button, Code, Stack } from '@mantine/core';
import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { CaseTimeline } from '../components/cases/CaseTimeline';

interface CaseDTO {
    caseId: string;
    workflowCode: string;
    workflowName: string;
    status: string;
    startTime: string;
    endTime?: string;
    startUserId: string;
    processVariables?: any;
}

interface StageDTO {
    stageName: string;
    stageCode: string;
    taskId?: string;
    status: string;
    assignee?: string;
    createdTime?: string;
    endTime?: string;
    allowedActions?: string; // "APPROVE,REJECT"
}

export default function CaseView() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [caseDetails, setCaseDetails] = useState<CaseDTO | null>(null);
    const [stages, setStages] = useState<StageDTO[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id) return;

        const fetchData = async () => {
            try {
                const [caseRes, stagesRes] = await Promise.all([
                    fetch(`/api/runtime/cases/${id}`),
                    fetch(`/api/runtime/cases/${id}/stages`)
                ]);

                if (caseRes.ok && stagesRes.ok) {
                    setCaseDetails(await caseRes.json());
                    setStages(await stagesRes.json());
                } else {
                    console.error("Failed to load case info");
                }
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [id]);

    if (loading) return <Container py="xl"><Loader /></Container>;
    if (!caseDetails) return <Container py="xl"><Text>Case not found</Text></Container>;

    const handleTaskAction = async (taskId: string, outcome?: string, variablesJson?: string) => {
        // if (!confirm(`Are you sure you want to ${outcome || 'Complete'} this task?`)) return; // Removed implicit confirm since we have a modal now

        let variables = {};
        if (variablesJson) {
            try {
                variables = JSON.parse(variablesJson);
            } catch (e) {
                alert("Invalid JSON format");
                return;
            }
        }

        try {
            const res = await fetch(`/api/runtime/cases/${id}/tasks/${taskId}/complete`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: 'completed',
                    outcome: outcome,
                    variables: variables
                })
            });

            if (res.ok) {
                alert('Task completed!');
                window.location.reload();
            } else {
                const txt = await res.text();
                alert('Failed: ' + txt);
            }
        } catch (e) {
            console.error(e);
            alert('Error completing task');
        }
    };

    return (
        <Container size="xl" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>Case: {caseDetails.caseId}</Title>
                <Badge
                    size="xl"
                    color={caseDetails.status === 'ACTIVE' ? 'blue' : 'gray'}
                >
                    {caseDetails.status}
                </Badge>
            </Group>

            <Grid>
                <Grid.Col span={4}>
                    <Paper withBorder p="md" mb="md">
                        <Title order={4} mb="md">Details</Title>
                        <Stack gap="xs">
                            <Group justify="space-between">
                                <Text fw={500}>Workflow:</Text>
                                <Text>{caseDetails.workflowName || caseDetails.workflowCode}</Text>
                            </Group>
                            <Group justify="space-between">
                                <Text fw={500}>Started By:</Text>
                                <Text>{caseDetails.startUserId}</Text>
                            </Group>
                            <Group justify="space-between">
                                <Text fw={500}>Start Time:</Text>
                                <Text>{new Date(caseDetails.startTime).toLocaleString()}</Text>
                            </Group>
                            {caseDetails.endTime && (
                                <Group justify="space-between">
                                    <Text fw={500}>End Time:</Text>
                                    <Text>{new Date(caseDetails.endTime).toLocaleString()}</Text>
                                </Group>
                            )}
                        </Stack>
                    </Paper>

                    <Paper withBorder p="md" mb="md">
                        <Title order={5} mb="sm">Process Variables</Title>

                        <Code block style={{ whiteSpace: 'pre-wrap', maxHeight: '300px', overflowY: 'auto' }}>
                            {JSON.stringify(caseDetails.processVariables || {}, null, 2)}
                        </Code>

                    </Paper>

                    <Button variant="light" fullWidth onClick={() => navigate('/inbox')}>
                        Back to Inbox
                    </Button>
                </Grid.Col>

                <Grid.Col span={8}>
                    <CaseTimeline stages={stages} onAction={handleTaskAction} />
                </Grid.Col>
            </Grid>
        </Container>
    );
}


