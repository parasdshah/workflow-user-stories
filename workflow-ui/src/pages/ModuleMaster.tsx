import { Container, Title, Table, Button, Group, Modal, TextInput, ActionIcon } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useForm } from '@mantine/form';
import { IconTrash, IconPlus } from '@tabler/icons-react';
import { useEffect, useState } from 'react';

interface Module {
    id?: number;
    moduleCode: string;
    moduleName: string;
    description?: string;
}

export default function ModuleMaster() {
    const [modules, setModules] = useState<Module[]>([]);
    const [opened, { open, close }] = useDisclosure(false);

    const form = useForm({
        initialValues: {
            moduleCode: '',
            moduleName: '',
            description: ''
        },
        validate: {
            moduleCode: (value) => (value ? null : 'Code is required'),
            moduleName: (value) => (value ? null : 'Name is required'),
        }
    });

    const fetchModules = async () => {
        try {
            const res = await fetch('/api/modules');
            if (res.ok) {
                setModules(await res.json());
            }
        } catch (error) {
            console.error(error);
        }
    };

    useEffect(() => {
        fetchModules();
    }, []);

    const handleSubmit = async (values: typeof form.values) => {
        try {
            const res = await fetch('/api/modules', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values)
            });
            if (res.ok) {
                fetchModules();
                close();
                form.reset();
            } else {
                alert('Failed to save module');
            }
        } catch (error) {
            console.error(error);
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Are you sure?')) return;
        try {
            await fetch(`/api/modules/${id}`, { method: 'DELETE' });
            fetchModules();
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <Container size="lg" py="xl">
            <Group justify="space-between" mb="lg">
                <Title order={2}>Module Master</Title>
                <Button leftSection={<IconPlus size={16} />} onClick={open}>Add Module</Button>
            </Group>

            <Table withTableBorder withColumnBorders striped>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Code</Table.Th>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>Description</Table.Th>
                        <Table.Th>Action</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {modules.map(module => (
                        <Table.Tr key={module.id}>
                            <Table.Td>{module.moduleCode}</Table.Td>
                            <Table.Td>{module.moduleName}</Table.Td>
                            <Table.Td>{module.description}</Table.Td>
                            <Table.Td>
                                <ActionIcon color="red" variant="subtle" onClick={() => module.id && handleDelete(module.id)}>
                                    <IconTrash size={16} />
                                </ActionIcon>
                            </Table.Td>
                        </Table.Tr>
                    ))}
                </Table.Tbody>
            </Table>

            <Modal opened={opened} onClose={close} title="Add New Module">
                <form onSubmit={form.onSubmit(handleSubmit)}>
                    <TextInput label="Module Code" placeholder="e.g. LN" mb="sm" required {...form.getInputProps('moduleCode')} />
                    <TextInput label="Module Name" placeholder="e.g. Loans" mb="sm" required {...form.getInputProps('moduleName')} />
                    <TextInput label="Description" placeholder="Optional description" mb="lg" {...form.getInputProps('description')} />
                    <Button type="submit" fullWidth>Save Module</Button>
                </form>
            </Modal>
        </Container>
    );
}
