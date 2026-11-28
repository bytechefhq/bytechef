import AgentScheduleFrequencyFields from '@/pages/automation/agents/components/detail/AgentScheduleFrequencyFields';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

describe('AgentScheduleFrequencyFields', () => {
    it('renders the interval input for EVERY_X_MINUTES', () => {
        render(<AgentScheduleFrequencyFields cadence={{frequencyKind: 'EVERY_X_MINUTES'}} onChange={vi.fn()} />);

        expect(screen.getByLabelText('Every')).toBeInTheDocument();
        expect(screen.queryByLabelText('Time')).not.toBeInTheDocument();
    });

    it('renders both a day and a time for WEEKLY', () => {
        render(<AgentScheduleFrequencyFields cadence={{frequencyKind: 'WEEKLY'}} onChange={vi.fn()} />);

        expect(screen.getByLabelText('Day of week')).toBeInTheDocument();
        expect(screen.getByLabelText('Time')).toBeInTheDocument();
    });

    it('renders the cron input for CUSTOM_CRON', () => {
        render(<AgentScheduleFrequencyFields cadence={{frequencyKind: 'CUSTOM_CRON'}} onChange={vi.fn()} />);

        expect(screen.getByLabelText('Cron expression')).toBeInTheDocument();
    });

    it('shows the error message for the field it belongs to', () => {
        render(
            <AgentScheduleFrequencyFields
                cadence={{frequencyKind: 'DAILY'}}
                errors={{timeOfDay: 'A time of day is required.'}}
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('A time of day is required.')).toBeInTheDocument();
    });
});
