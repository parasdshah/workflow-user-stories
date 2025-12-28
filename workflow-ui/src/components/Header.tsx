import { Group, Button, Title, Container, Paper } from '@mantine/core';
import { useNavigate } from 'react-router-dom';
import { IconList, IconHistory, IconInbox, IconFileAnalytics } from '@tabler/icons-react';
import ServiceHealth from './ServiceHealth';

function Header() {
    const navigate = useNavigate();

    return (
        <Paper withBorder p="md" mb="md" shadow="xs">
            <Container size="xl">
                <ServiceHealth />
                <Group justify="space-between">

                    <Title order={3}>Workflow Engine</Title>
                    <Group>
                        <Button variant="subtle" leftSection={<IconList size={16} />} onClick={() => navigate('/')}>
                            Workflows
                        </Button>
                        <Button variant="subtle" leftSection={<IconHistory size={16} />} onClick={() => navigate('/deployments')}>
                            Deployments
                        </Button>
                        <Button variant="subtle" leftSection={<IconInbox size={16} />} onClick={() => navigate('/inbox')}>
                            Task Inbox
                        </Button>
                        <Button variant="subtle" leftSection={<IconList size={16} />} onClick={() => navigate('/screens')}>
                            Screens
                        </Button>
                        <Button variant="subtle" leftSection={<IconFileAnalytics size={16} />} onClick={() => navigate('/audit')}>
                            Audit Log
                        </Button>
                        <Button variant="subtle" leftSection={<IconList size={16} />} onClick={() => navigate('/rules')}>
                            Rules
                        </Button>
                    </Group>
                </Group>

            </Container>
        </Paper>
    );
}

export default Header;
