import { Container, Title, Button, Table, Group, ActionIcon, Paper } from '@mantine/core';
import { IconEdit, IconPlus } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';

interface ScreenDefinition {
    screenCode: string;
    description: string;
    updatedAt: string;
}

export function ScreenDefinitionList() {
    const navigate = useNavigate();
    const [screens, setScreens] = useState<ScreenDefinition[]>([]);

    useEffect(() => {
        fetch('/api/screens')
            .then(res => {
                if (!res.ok) throw new Error("Failed to fetch");
                return res.json();
            })
            .then(data => {
                if (Array.isArray(data)) {
                    setScreens(data);
                } else {
                    console.error("Expected array but got:", data);
                    setScreens([]);
                }
            })
            .catch(err => {
                console.error("Error fetching screens:", err);
                setScreens([]);
            });
    }, []);

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>Screen Definitions</Title>
                <Button leftSection={<IconPlus size={16} />} onClick={() => navigate('/screens/new')}>
                    Create Screen
                </Button>
            </Group>

            <Paper withBorder p="md">
                <Table>
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Screen Code</Table.Th>
                            <Table.Th>Description</Table.Th>
                            <Table.Th>Last Updated</Table.Th>
                            <Table.Th>Actions</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {screens.map(screen => (
                            <Table.Tr key={screen.screenCode}>
                                <Table.Td>{screen.screenCode}</Table.Td>
                                <Table.Td>{screen.description}</Table.Td>
                                <Table.Td>{new Date(screen.updatedAt).toLocaleString()}</Table.Td>
                                <Table.Td>
                                    <ActionIcon variant="subtle" onClick={() => navigate(`/screens/${screen.screenCode}`)}>
                                        <IconEdit size={16} />
                                    </ActionIcon>
                                </Table.Td>
                            </Table.Tr>
                        ))}
                    </Table.Tbody>
                </Table>
            </Paper>
        </Container>
    );
}
