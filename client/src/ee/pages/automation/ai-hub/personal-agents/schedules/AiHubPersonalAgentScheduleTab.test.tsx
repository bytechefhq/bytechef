import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

// The graphql.ts generated file currently contains duplicate identifiers that cause
// parse failures under oxc/vite when imported directly. Mock the module and supply
// the handful of enum values used by this component to avoid the parse error.
vi.mock('@/shared/middleware/graphql', () => ({
    ScheduleFrequencyKind: {
        CustomCron: 'CUSTOM_CRON',
        Daily: 'DAILY',
        EveryXMinutes: 'EVERY_X_MINUTES',
        Hourly: 'HOURLY',
        Monthly: 'MONTHLY',
        Weekly: 'WEEKLY',
    },
    ScheduleLifecycleKind: {
        NumberOfRuns: 'NUMBER_OF_RUNS',
        Recurring: 'RECURRING',
    },
}));

const {ScheduleFrequencyKind, ScheduleLifecycleKind} = await import('@/shared/middleware/graphql');

import AiHubPersonalAgentScheduleTab, {
    AiHubPersonalAgentScheduleTabValueI,
    buildDefaultScheduleValue,
} from './AiHubPersonalAgentScheduleTab';

describe('AiHubPersonalAgentScheduleTab', () => {
    const baseValue: AiHubPersonalAgentScheduleTabValueI = buildDefaultScheduleValue();

    it('renders the Enabled switch defaulting to off', () => {
        render(<AiHubPersonalAgentScheduleTab onChange={vi.fn()} value={baseValue} />);

        // The Switch renders with role="switch". The associated <Label htmlFor="schedule-enabled">
        // has text "Run this agent on a schedule"; getByLabelText resolves via the htmlFor/id link.
        const toggle = screen.getByLabelText('Run this agent on a schedule');

        expect(toggle).toBeInTheDocument();
        expect(toggle).not.toBeChecked();
    });

    it('emits onChange when Enabled is toggled on', () => {
        const onChange = vi.fn();

        render(<AiHubPersonalAgentScheduleTab onChange={onChange} value={baseValue} />);

        fireEvent.click(screen.getByRole('switch'));

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({enabled: true}));
    });

    it('shows the Max runs input only when lifecycle is NumberOfRuns', () => {
        const {rerender} = render(<AiHubPersonalAgentScheduleTab onChange={vi.fn()} value={baseValue} />);

        expect(screen.queryByLabelText('Max runs (optional)')).not.toBeInTheDocument();

        rerender(
            <AiHubPersonalAgentScheduleTab
                onChange={vi.fn()}
                value={{...baseValue, lifecycleKind: ScheduleLifecycleKind.NumberOfRuns}}
            />
        );

        expect(screen.getByLabelText('Max runs (optional)')).toBeInTheDocument();
    });

    it('hides the Remove schedule button in create mode', () => {
        render(<AiHubPersonalAgentScheduleTab onChange={vi.fn()} onRemove={vi.fn()} value={baseValue} />);

        expect(screen.queryByRole('button', {name: /remove schedule/i})).not.toBeInTheDocument();
    });

    it('shows Remove schedule when an existingSchedule is supplied', () => {
        const onRemove = vi.fn();

        render(
            <AiHubPersonalAgentScheduleTab
                existingSchedule={{
                    aiHubPersonalAgentId: '42',
                    cronExpression: null,
                    dayOfMonth: null,
                    dayOfWeek: null,
                    effectiveCronExpression: '0 0 9 * * ?',
                    enabled: true,
                    frequencyKind: ScheduleFrequencyKind.Daily,
                    id: '7',
                    intervalMinutes: null,
                    lastRunAt: null,
                    lifecycleKind: ScheduleLifecycleKind.Recurring,
                    maxRuns: null,
                    minuteOfHour: null,
                    nextRunAt: null,
                    prompt: 'Run morning report',
                    remainingRuns: null,
                    startDate: null,
                    timeOfDay: '09:00',
                    title: 'Daily report',
                    zoneId: 'UTC',
                }}
                onChange={vi.fn()}
                onRemove={onRemove}
                value={{...baseValue, enabled: true, prompt: 'Run morning report', title: 'Daily report'}}
            />
        );

        fireEvent.click(screen.getByRole('button', {name: /remove schedule/i}));
        fireEvent.click(screen.getByRole('button', {name: /confirm remove/i}));

        expect(onRemove).toHaveBeenCalledOnce();
    });
});
