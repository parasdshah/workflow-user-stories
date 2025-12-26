import { Group, Badge, Text, Paper, Loader } from '@mantine/core';
import { IconCheck, IconX } from '@tabler/icons-react';
import { useState, useEffect } from 'react';

interface ServiceStatus {
    name: string;
    url: string;
    status: 'up' | 'down' | 'checking';
}

function ServiceHealth() {
    const [services, setServices] = useState<ServiceStatus[]>([
        { name: 'Registry', url: 'http://localhost:8761/actuator/health', status: 'checking' },
        { name: 'Gateway', url: 'http://localhost:8080/actuator/health', status: 'checking' },
        { name: 'Workflow', url: 'http://localhost:8081/actuator/health', status: 'checking' },
    ]);

    useEffect(() => {
        const checkHealth = async () => {
            const updated = await Promise.all(services.map(async (s) => {
                try {
                    const res = await fetch(s.url);
                    if (res.ok) {
                        return { ...s, status: 'up' as const };
                    }
                    return { ...s, status: 'down' as const };
                } catch (e) {
                    return { ...s, status: 'down' as const };
                }
            }));
            setServices(updated);
        };

        checkHealth();
        const interval = setInterval(checkHealth, 10000); // Check every 10s
        return () => clearInterval(interval);
    }, []);

    const getIcon = (status: string) => {
        if (status === 'checking') return <Loader size={12} />;
        if (status === 'up') return <IconCheck size={14} color="green" />;
        return <IconX size={14} color="red" />;
    };

    const getColor = (status: string) => {
        if (status === 'up') return 'green';
        if (status === 'down') return 'red';
        return 'gray';
    };

    return (
        <Paper p="xs" withBorder mb="md">
            <Group>
                <Text size="sm" fw={500}>System Status:</Text>
                {services.map((s) => (
                    <Badge key={s.name} leftSection={getIcon(s.status)} color={getColor(s.status)} variant="light">
                        {s.name}
                    </Badge>
                ))}
            </Group>
        </Paper>
    );
}

export default ServiceHealth;
