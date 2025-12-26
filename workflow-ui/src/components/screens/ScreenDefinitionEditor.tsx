import { Container, Title, Button, TextInput, Group, Stack, Paper, JsonInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useNavigate, useParams } from 'react-router-dom';
import { useEffect } from 'react';

export function ScreenDefinitionEditor() {
    const { code } = useParams();
    const navigate = useNavigate();
    const isNew = code === 'new';

    const form = useForm({
        initialValues: {
            screenCode: '',
            description: '',
            layoutJson: '{}',
        },
        validate: {
            screenCode: (value) => (value.length < 3 ? 'Code must be at least 3 chars' : null),
            layoutJson: (value) => {
                try {
                    JSON.parse(value);
                    return null;
                } catch (e) {
                    return 'Invalid JSON';
                }
            }
        }
    });

    useEffect(() => {
        if (!isNew && code) {
            fetch(`/api/screens/${code}`)
                .then(res => {
                    if (!res.ok) throw new Error('Network response was not ok');
                    return res.json();
                })
                .then(data => {
                    form.setValues({
                        screenCode: data.screenCode,
                        description: data.description || '',
                        layoutJson: data.layoutJson || '{}'
                    });
                })
                .catch(error => {
                    console.error("Error fetching screen:", error);
                    alert("Error fetching screen details");
                });
        }
    }, [code, isNew]);

    const handleSave = async (values: typeof form.values) => {
        try {
            const res = await fetch('/api/screens', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values)
            });

            if (res.ok) {
                alert('Saved successfully!');
                navigate('/screens');
            } else {
                alert('Failed to save.');
            }
        } catch (err) {
            console.error(err);
            alert('Error saving screen.');
        }
    };

    return (
        <Container size="md" py="xl">
            <Title order={2} mb="lg">{isNew ? 'Create Screen' : 'Edit Screen'}</Title>

            <Paper withBorder p="xl">
                <form onSubmit={form.onSubmit(handleSave)}>
                    <Stack>
                        <TextInput
                            label="Screen Code"
                            description="Unique identifier for this screen"
                            required
                            disabled={!isNew}
                            {...form.getInputProps('screenCode')}
                        />

                        <TextInput
                            label="Description"
                            {...form.getInputProps('description')}
                        />

                        <JsonInput
                            label="Layout JSON"
                            placeholder="{ ... }"
                            validationError="Invalid JSON"
                            formatOnBlur
                            autosize
                            minRows={10}
                            {...form.getInputProps('layoutJson')}
                        />

                        <Group justify="flex-end" mt="md">
                            <Button variant="default" onClick={() => navigate('/screens')}>Cancel</Button>
                            <Button type="submit">Save</Button>
                        </Group>
                    </Stack>
                </form>
            </Paper>
        </Container>
    );
}
