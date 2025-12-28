
import { Container, Title, Paper, Group, Button, Table, ActionIcon, Modal, TextInput, FileInput, Text, Stack } from '@mantine/core';
import { IconUpload, IconTrash, IconFileSpreadsheet } from '@tabler/icons-react';
import { useDisclosure } from '@mantine/hooks';
import { useForm } from '@mantine/form';
import { useState, useEffect } from 'react';

interface DecisionTable {
    id: string;
    key: string;
    name: string;
    version: number;
    deploymentId: string;
    resourceName: string;
}

export default function RuleManagement() {
    const [rules, setRules] = useState<DecisionTable[]>([]);
    const [opened, { open, close }] = useDisclosure(false);

    const form = useForm({
        initialValues: {
            key: '',
            name: '',
            file: null as File | null,
        },
        validate: {
            key: (value) => !value ? 'Key is required' : /^[a-zA-Z0-9_]+$/.test(value) ? null : 'Key must be alphanumeric',
            name: (value) => !value ? 'Name is required' : null,
            file: (value) => !value ? 'CSV File is required' : null,
        }
    });

    const fetchRules = () => {
        fetch('/api/rules')
            .then(res => res.json())
            .then(data => setRules(data))
            .catch(err => console.error("Failed to fetch rules", err));
    };

    useEffect(() => {
        fetchRules();
    }, []);

    const handleDelete = async (deploymentId: string) => {
        if (!confirm('Are you sure you want to delete this rule deployment?')) return;

        try {
            const res = await fetch(`/api/rules/${deploymentId}`, { method: 'DELETE' });
            if (res.ok) {
                fetchRules();
            } else {
                alert("Failed to delete rule");
            }
        } catch (error) {
            console.error(error);
            alert("Error deleting rule");
        }
    };

    const handleUpload = async (values: typeof form.values) => {
        if (!values.file) return;

        const formData = new FormData();
        formData.append('file', values.file);
        formData.append('key', values.key);
        formData.append('name', values.name);

        try {
            const res = await fetch('/api/rules/upload', {
                method: 'POST',
                body: formData
            });

            if (res.ok) {
                alert("Rule uploaded successfully!");
                close();
                form.reset();
                fetchRules();
            } else {
                const txt = await res.text();
                alert(`Upload failed: ${txt}`);
            }
        } catch (error: any) {
            console.error(error);
            alert(`Error uploading rule: ${error.message}`);
        }
    };

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>Business Rules (DMN)</Title>
                <Button leftSection={<IconUpload size={16} />} onClick={open}>Upload Rule</Button>
            </Group>

            <Paper withBorder p="md">
                {rules.length === 0 ? (
                    <Text c="dimmed" ta="center" py="xl">No rules deployed yet. Upload a CSV file to get started.</Text>
                ) : (
                    <Table>
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>Name</Table.Th>
                                <Table.Th>Key</Table.Th>
                                <Table.Th>Version</Table.Th>
                                <Table.Th>Resource</Table.Th>
                                <Table.Th>Deployment ID</Table.Th>
                                <Table.Th>Action</Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>
                            {rules.map(rule => (
                                <Table.Tr key={rule.id}>
                                    <Table.Td>{rule.name}</Table.Td>
                                    <Table.Td>{rule.key}</Table.Td>
                                    <Table.Td>{rule.version}</Table.Td>
                                    <Table.Td>{rule.resourceName}</Table.Td>
                                    <Table.Td>{rule.deploymentId}</Table.Td>
                                    <Table.Td>
                                        <ActionIcon color="red" variant="subtle" onClick={() => handleDelete(rule.deploymentId)}>
                                            <IconTrash size={16} />
                                        </ActionIcon>
                                    </Table.Td>
                                </Table.Tr>
                            ))}
                        </Table.Tbody>
                    </Table>
                )}
            </Paper>

            <Modal opened={opened} onClose={close} title="Upload Business Rule (CSV)">
                <form onSubmit={form.onSubmit(handleUpload)}>
                    <Stack>
                        <TextInput
                            label="Rule Key"
                            description="Unique identifier for the decision table (e.g. RISK_RULE)"
                            placeholder="RISK_RULE"
                            {...form.getInputProps('key')}
                        />
                        <TextInput
                            label="Rule Name"
                            placeholder="Risk Assessment Rule"
                            {...form.getInputProps('name')}
                        />
                        <FileInput
                            label="CSV File"
                            accept=".csv"
                            leftSection={<IconFileSpreadsheet size={16} />}
                            placeholder="Select CSV file"
                            {...form.getInputProps('file')}
                        />

                        <Button type="submit" mt="md">Upload & Deploy</Button>
                    </Stack>
                </form>
            </Modal>
        </Container>
    );
}
