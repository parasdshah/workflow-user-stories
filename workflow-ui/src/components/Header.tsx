import { Group, Button, Title, Container, Paper, Menu } from '@mantine/core';
import { useNavigate } from 'react-router-dom';
import { IconList, IconHistory, IconInbox, IconFileAnalytics, IconSettings, IconChartBar, IconDeviceDesktop, IconGavel, IconPackage, IconUsers } from '@tabler/icons-react';
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

                        {/* Tasks Menu */}
                        <Menu shadow="md" width={200}>
                            <Menu.Target>
                                <Button variant="subtle" leftSection={<IconInbox size={16} />}>
                                    Tasks
                                </Button>
                            </Menu.Target>
                            <Menu.Dropdown>
                                <Menu.Item leftSection={<IconInbox size={14} />} onClick={() => navigate('/inbox')}>
                                    Inbox
                                </Menu.Item>
                                <Menu.Item leftSection={<IconChartBar size={14} />} onClick={() => navigate('/workload')}>
                                    Workload Dashboard
                                </Menu.Item>
                            </Menu.Dropdown>
                        </Menu>

                        {/* Configuration Menu */}
                        <Menu shadow="md" width={200}>
                            <Menu.Target>
                                <Button variant="subtle" leftSection={<IconSettings size={16} />}>
                                    Configuration
                                </Button>
                            </Menu.Target>
                            <Menu.Dropdown>
                                <Menu.Item leftSection={<IconDeviceDesktop size={14} />} onClick={() => navigate('/screens')}>
                                    Screens
                                </Menu.Item>
                                <Menu.Item leftSection={<IconFileAnalytics size={14} />} onClick={() => navigate('/audit')}>
                                    Audit Log
                                </Menu.Item>
                                <Menu.Item leftSection={<IconGavel size={14} />} onClick={() => navigate('/rules')}>
                                    Rules
                                </Menu.Item>
                                <Menu.Item leftSection={<IconPackage size={14} />} onClick={() => navigate('/modules')}>
                                    Modules
                                </Menu.Item>
                            </Menu.Dropdown>
                        </Menu>

                        <Button variant="subtle" leftSection={<IconUsers size={16} />} onClick={() => navigate('/admin/hrms')}>
                            HRMS Console
                        </Button>
                    </Group>
                </Group>

            </Container>
        </Paper>
    );
}

export default Header;
