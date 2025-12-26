import { Container, Title, Code, Paper, Button, Group, Loader, Text } from '@mantine/core';
import { useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';

function DiagramPreview() {
    const { code } = useParams();
    const [xml, setXml] = useState('');
    const [loading, setLoading] = useState(true);
    const [deploying, setDeploying] = useState(false);
    const [log, setLog] = useState<string>('');

    useEffect(() => {
        if (!code) return;
        setLoading(true);
        fetch(`/api/deployments/preview/${code}`)
            .then(res => res.text()) // Controller returns String
            .then(data => setXml(data))
            .catch(err => console.error(err))
            .finally(() => setLoading(false));
    }, [code]);

    const handleDeploy = async () => {
        if (!code) return;
        if (!confirm('Deploy this workflow?')) return;

        setDeploying(true);
        setLog('Deploying...');

        try {
            const res = await fetch(`/api/deployments/${code}`, { method: 'POST' });
            if (res.ok) {
                const id = await res.text(); // or just ID
                setLog(`SUCCESS: Deployment successful! Deployment ID: ${id}`);
            } else {
                const txt = await res.text();
                setLog(`ERROR: Deployment failed.\n\nDetails:\n${txt}`);
            }
        } catch (e: any) {
            console.error(e);
            setLog(`EXCEPTION: ${e.message}`);
        } finally {
            setDeploying(false);
        }
    };

    if (loading) return <Container py="xl"><Loader /></Container>;

    return (
        <Container size="xl" py="xl" style={{ height: '90vh', display: 'flex', flexDirection: 'column' }}>
            <Group justify="space-between" mb="md">
                <Title order={3}>Preview: {code}</Title>
                <Button loading={deploying} onClick={handleDeploy}>Deploy Now</Button>
            </Group>

            {log && (
                <Paper withBorder p="sm" mb="md" bg="gray.1">
                    <Text fw={500} size="sm" mb="xs">Deployment Log:</Text>
                    <Code block style={{ whiteSpace: 'pre-wrap', maxHeight: '200px', overflowY: 'auto' }}>
                        {log}
                    </Code>
                </Paper>
            )}

            <Paper withBorder p={0} style={{ flex: 1, overflow: 'hidden' }}>
                <Code block style={{ height: '100%', overflow: 'auto' }}>
                    {xml}
                </Code>
            </Paper>
        </Container>
    );
}

export default DiagramPreview;
