import { Container, Title, Table, Badge, Text } from '@mantine/core';
import { useState, useEffect } from 'react';

interface DeploymentDto {
    id: string;
    name: string;
    deploymentTime: string;
}

function DeploymentHistory() {
    const [deployments, setDeployments] = useState<DeploymentDto[]>([]);

    useEffect(() => {
        fetch('/api/deployments')
            .then(res => res.json())
            .then(data => setDeployments(data))
            .catch(err => console.error("Error fetching deployments:", err));
    }, []);

    return (
        <Container size="lg" py="xl">
            <Title order={2} mb="lg">Deployment History</Title>
            <Table>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>ID</Table.Th>
                        <Table.Th>Name (Workflow Code)</Table.Th>
                        <Table.Th>Date</Table.Th>
                        <Table.Th>Status</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {deployments.map((d) => (
                        <Table.Tr key={d.id}>
                            <Table.Td>{d.id}</Table.Td>
                            <Table.Td>{d.name}</Table.Td>
                            <Table.Td>{new Date(d.deploymentTime).toLocaleString()}</Table.Td>
                            <Table.Td><Badge color="green">Deployed</Badge></Table.Td>
                        </Table.Tr>
                    ))}
                    {deployments.length === 0 && (
                        <Table.Tr>
                            <Table.Td colSpan={4} align="center"><Text c="dimmed">No deployments found.</Text></Table.Td>
                        </Table.Tr>
                    )}
                </Table.Tbody>
            </Table>
        </Container>
    );
}

export default DeploymentHistory;
