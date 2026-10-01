import AgentScheduleDialog from '@/pages/automation/agents/components/detail/AgentScheduleDialog';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

describe('AgentScheduleDialog', () => {
    it('submits a generated cron expression alongside the cadence fields', () => {
        const onSubmit = vi.fn();

        render(<AgentScheduleDialog onClose={vi.fn()} onSubmit={onSubmit} open={true} />);

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Daily digest'}});
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:30'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({expression: '30 9 * * ?', frequencyKind: 'DAILY', timeOfDay: '09:30'})
        );
    });

    it('blocks submit and shows the cadence error when a required field is missing', () => {
        const onSubmit = vi.fn();

        render(<AgentScheduleDialog onClose={vi.fn()} onSubmit={onSubmit} open={true} />);

        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText('A time of day is required.')).toBeInTheDocument();
    });

    it('opens a schedule with no stored frequencyKind as a custom cron', () => {
        render(
            <AgentScheduleDialog
                onClose={vi.fn()}
                onSubmit={vi.fn()}
                open={true}
                schedule={{expression: '0 9 * * ?', name: 'Legacy', prompt: 'Run', timezone: 'UTC'}}
            />
        );

        expect(screen.getByLabelText('Cron expression')).toHaveValue('0 9 * * ?');
    });
});
