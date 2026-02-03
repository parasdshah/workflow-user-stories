import { useState, useEffect } from 'react';
import { Container, Title, Table, Button, Badge, Group, TextInput, Paper } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom'; // Using react-router-dom for navigation

interface CaseDTO {
    caseId: string;
    stageName: string; // From mapped StageDTO or similar
    // Actually standard CaseDTO might not have Stage Name directly if it's the Case object.
    // The requirement implies finding tasks.
    // Let's use a Task-Centric view. "Admin Active Cases" implies seeing WHERE cases are.
    // Ideally we fetch "All Active Tasks" for Admin.
    // CaseService doesn't have "getAllActiveTasks" exposed directly cleanly to frontend except via GroupWorkload fallback (which we killed)
    // or /workload (User specific).
    // Better to fetch "All Active Cases" then for each case show active tasks?
    // OR create a new endpoint? 
    // Existing: GET /api/runtime/cases/active -> Returns CaseDTOs.
    // CaseDTO has status, startTime, etc. typically.
    // To get "Active Task" and "Assignee", we might need enrichment.
    // Let's proceed with `CaseDTO` and maybe fetch stages on expand? 
    // OR simper: Just show the Case list and a "Manage" button that goes to Case View, 
    // AND add the Reassign Button TO THE CASE VIEW or this list if we have data.

    // DECISION: To fulfill "Admin can reassign", listing TASKS is better than CASES.
    // But let's stick to the "Active Workflows Dashboard" requirement.
    // Let's list Cases. Admin clicks "Reassign" -> Selects Task from a dropdown in Modal?
    // COMPLEXITY REDUCTION:
    // Let's assume we list Cases. But Reassignment is per Task.
    // Let's add "Reassign" button to `CaseView.tsx` (the detailed view) AS WELL.
    // BUT user asked for "Create a page of all open / active workflows and allow reassignment."

    // Let's make this page fetch "All Active Cases"
    workflowName: string;
    startTime: string;
    status: string;
}

export default function AdminCaseList() {
    const [cases, setCases] = useState<CaseDTO[]>([]);
    const [loading, setLoading] = useState(false);
    const [search, setSearch] = useState('');
    const navigate = useNavigate();

    // Reassign Modal State
    // Since this is a list of *Cases*, we can't easily trigger Task Reassignment directly without knowing the Task ID.
    // Strategy:
    // 1. Display list of Cases.
    // 2. "Manage" button takes Admin to Case Details.
    // 3. We implement Reassign Button *inside* Case Details (already exists as View Case logic usually).
    // WAIT. Requirement: "Create a page... and allow reassignment."
    // If I show only cases, I force an extra click.
    // If I fetch tasks, it's better.
    // Let's try to fetch "Detailed Active Cases" if possible. 
    // Or just fetch Cases and rely on CaseView. 
    // OR (Best): This page lists Cases. User clicks "Manage". 
    // In CaseView, next to "Active Stages", we add "Reassign" button (Admin only).

    // BUT checking the requirement again: "1. Case already assigned... admin user... can select new user. 2. Case assigned to Group Queue... Admin will assign... Create a page... allow reassignment."
    // It's acceptable to have a list of cases, click one, and see its tasks to reassign.
    // Actually, making `CaseView` simpler for Admin actions is high leverage.

    // Let's build the List Page first.

    useEffect(() => {
        fetchCases();
    }, []);

    const fetchCases = async () => {
        setLoading(true);
        try {
            const res = await fetch('/api/runtime/cases/active');
            if (res.ok) {
                const data = await res.json();
                setCases(data);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const filteredCases = cases.filter(c =>
        c.caseId.toLowerCase().includes(search.toLowerCase()) ||
        (c.workflowName && c.workflowName.toLowerCase().includes(search.toLowerCase()))
    );

    return (
        <Container size="xl" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>Active Workflows (Admin)</Title>
                <TextInput
                    placeholder="Search by Case ID or Name"
                    leftSection={<IconSearch size={16} />}
                    value={search}
                    onChange={(e) => setSearch(e.currentTarget.value)}
                />
            </Group>

            <Paper withBorder radius="md">
                <Table striped highlightOnHover verticalSpacing="sm">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Case ID</Table.Th>
                            <Table.Th>Workflow</Table.Th>
                            <Table.Th>Started At</Table.Th>
                            <Table.Th>Status</Table.Th>
                            <Table.Th>Action</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {filteredCases.map(c => (
                            <Table.Tr key={c.caseId}>
                                <Table.Td fw={500}>{c.caseId}</Table.Td>
                                <Table.Td>{c.workflowName}</Table.Td>
                                <Table.Td>{new Date(c.startTime).toLocaleString()}</Table.Td>
                                <Table.Td>
                                    <Badge color="green">{c.status}</Badge>
                                </Table.Td>
                                <Table.Td>
                                    <Button size="xs" variant="light" onClick={() => navigate(`/cases/${c.caseId}`)}>
                                        Manage
                                    </Button>
                                </Table.Td>
                            </Table.Tr>
                        ))}
                        {filteredCases.length === 0 && !loading && (
                            <Table.Tr>
                                <Table.Td colSpan={5} ta="center">No active cases found</Table.Td>
                            </Table.Tr>
                        )}
                    </Table.Tbody>
                </Table>
            </Paper>
        </Container>
    );
}
