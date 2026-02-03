import { useState, useEffect } from 'react';
import { Container, Title, Paper, Group, Select, Indicator, LoadingOverlay, Alert } from '@mantine/core';
import { Calendar } from '@mantine/dates';
import { notifications } from '@mantine/notifications';

interface OrgHoliday {
  id?: number;
  date: string; // YYYY-MM-DD
  description: string;
  region: string;
}

export default function HolidayCalendar() {
  const [region, setRegion] = useState<string>('GLOBAL');
  const [holidays, setHolidays] = useState<OrgHoliday[]>([]);
  const [loading, setLoading] = useState(false);

  // Fetch holidays on mount/region change
  useEffect(() => {
    fetchHolidays();
  }, [region]);

  const fetchHolidays = async () => {
    setLoading(true);
    try {
      const res = await fetch(`/api/holidays?region=${region}`);
      if (res.ok) {
        const data = await res.json();
        setHolidays(data);
      }
    } catch (error) {
      notifications.show({ title: 'Error', message: 'Failed to fetch holidays', color: 'red' });
    } finally {
      setLoading(false);
    }
  };

  const getLocalDateString = (date: Date) => {
    // Returns YYYY-MM-DD in local time
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const isHoliday = (date: Date) => {
    const dateStr = getLocalDateString(date);
    return holidays.find(h => h.date === dateStr);
  };

  const toggleHoliday = async (date: Date) => {
    const dateStr = getLocalDateString(date);
    const existing = isHoliday(date);

    try {
      if (existing) {
        // Delete
        if (existing.id) {
          await fetch(`/api/holidays/${existing.id}`, { method: 'DELETE' });
          setHolidays(prev => prev.filter(h => h.id !== existing.id));
          notifications.show({ title: 'Removed', message: `Holiday removed for ${dateStr}`, color: 'blue' });
        }
      } else {
        // Create
        const newHoliday = { date: dateStr, region, description: 'Holiday' };
        const res = await fetch('/api/holidays', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newHoliday),
        });
        if (res.ok) {
          const saved = await res.json();
          setHolidays(prev => [...prev, saved]);
          notifications.show({ title: 'Added', message: `Holiday set for ${dateStr}`, color: 'green' });
        }
      }
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Operation failed', color: 'red' });
    }
  };

  return (
    <Container size="md" py="xl">
      <Paper p="xl" withBorder>
        <Group justify="space-between" mb="md">
          <Title order={2}>Organization Holidays</Title>
          <Select
            label="Region"
            data={['GLOBAL', 'US', 'IN', 'APAC', 'EMEA']}
            value={region}
            onChange={(v) => setRegion(v || 'GLOBAL')}
          />
        </Group>

        <Alert title="Instructions" color="blue" mb="lg">
          Click on any date in the calendar below to toggle it as a Holiday for the selected Region.
        </Alert>

        <Group justify="center" style={{ position: 'relative' }}>
          <LoadingOverlay visible={loading} />
          <Calendar
            size="xl"
            renderDay={(date: any) => {
              // Explicitly handling date as any -> Date conversion if needed
              const d = new Date(date);
              const holiday = isHoliday(d);
              return (
                <Indicator size={8} color="red" offset={-2} disabled={!holiday}>
                  <div>{d.getDate()}</div>
                </Indicator>
              );
            }}
            getDayProps={(date: any) => {
              const d = new Date(date);
              return {
                selected: !!isHoliday(d),
                onClick: () => toggleHoliday(d),
              };
            }}
          />
        </Group>
      </Paper>
    </Container>
  );
}
