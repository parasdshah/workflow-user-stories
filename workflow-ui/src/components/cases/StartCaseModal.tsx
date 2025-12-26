import { Modal, Button, JsonInput, Group, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

interface StartCaseModalProps {
    opened: boolean;
    onClose: () => void;
    workflowCode: string;
    workflowName: string;
}

export function StartCaseModal({ opened, onClose, workflowCode, workflowName }: StartCaseModalProps) {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);

    const form = useForm({
        initialValues: {
            variables: '{}',
        },
        validate: {
            variables: (value) => {
                try {
                    const parsed = JSON.parse(value);
                    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
                        return 'Must be a JSON object {}';
                    }
                    return null;
                } catch (e) {
                    return 'Invalid JSON';
                }
            },
        },
    });

    // Reset form when modal opens
    useEffect(() => {
        if (opened) {
            form.reset();
        }
    }, [opened]);

    const handleSubmit = async (values: typeof form.values) => {
        setLoading(true);
        try {
            if (!workflowCode) {
                alert("Workflow Code is missing");
                setLoading(false);
                return;
            }

            const payload = {
                workflowCode: workflowCode,
                variables: JSON.parse(values.variables) || {}, // Fallback to empty object
                userId: 'current-user',
            };

            const response = await fetch('/api/runtime/cases', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                const caseId = await response.text();
                onClose();
                navigate(`/cases/${caseId}`);
            } else {
                console.error("Start case failed:", response.status, response.statusText);
                alert(`Failed to start case: ${response.status} ${response.statusText}`);
            }
        } catch (error) {
            console.error(error);
            alert('Error starting case');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Modal opened={opened} onClose={onClose} title={`Start Case: ${workflowName}`}>
            <form onSubmit={form.onSubmit(handleSubmit)}>
                <Stack>
                    <TextInput
                        label="Workflow Code"
                        value={workflowCode}
                        disabled
                    />

                    <JsonInput
                        label="Initial Variables (JSON)"
                        placeholder='{ "amount": 1000 }'
                        validationError="Invalid JSON"
                        formatOnBlur
                        autosize
                        minRows={4}
                        {...form.getInputProps('variables')}
                    />

                    <Group justify="flex-end" mt="md">
                        <Button variant="default" onClick={onClose}>Cancel</Button>
                        <Button type="submit" loading={loading}>Start</Button>
                    </Group>
                </Stack>
            </form>
        </Modal>
    );
}
