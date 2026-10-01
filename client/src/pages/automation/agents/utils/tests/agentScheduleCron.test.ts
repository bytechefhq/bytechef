import {
    AgentScheduleCadenceI,
    SCHEDULE_FREQUENCY_KINDS,
    describeCadence,
    describeCronExpression,
    fromCadenceParameters,
    fromCronExpression,
    toCadenceParameters,
    toCronExpression,
    validateAgentScheduleCadence,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import {describe, expect, it} from 'vitest';

describe('toCronExpression', () => {
    it('generates a five-field expression for EVERY_X_MINUTES', () => {
        expect(toCronExpression({frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 5})).toBe('0/5 * * * ?');
    });

    it('generates a five-field expression for HOURLY', () => {
        expect(toCronExpression({frequencyKind: 'HOURLY', minuteOfHour: 15})).toBe('15 * * * ?');
    });

    it('generates a five-field expression for DAILY', () => {
        expect(toCronExpression({frequencyKind: 'DAILY', timeOfDay: '09:30'})).toBe('30 9 * * ?');
    });

    it('converts an ISO weekday to a Quartz weekday for WEEKLY', () => {
        expect(toCronExpression({dayOfWeek: 1, frequencyKind: 'WEEKLY', timeOfDay: '08:00'})).toBe('0 8 ? * 2');
        expect(toCronExpression({dayOfWeek: 7, frequencyKind: 'WEEKLY', timeOfDay: '08:00'})).toBe('0 8 ? * 1');
    });

    it('generates a five-field expression for MONTHLY', () => {
        expect(toCronExpression({dayOfMonth: 3, frequencyKind: 'MONTHLY', timeOfDay: '23:05'})).toBe('5 23 3 * ?');
    });

    it('passes a custom cron expression through, trimmed', () => {
        expect(toCronExpression({cronExpression: '  0 9 * * 1  ', frequencyKind: 'CUSTOM_CRON'})).toBe('0 9 * * 1');
    });
});

describe('validateAgentScheduleCadence', () => {
    it('requires the cadence field each kind owns', () => {
        expect(validateAgentScheduleCadence({frequencyKind: 'EVERY_X_MINUTES'}).intervalMinutes).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'HOURLY'}).minuteOfHour).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'DAILY'}).timeOfDay).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'WEEKLY'}).dayOfWeek).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'MONTHLY'}).dayOfMonth).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'CUSTOM_CRON'}).cronExpression).toBeDefined();
    });

    it('rejects an out-of-range interval', () => {
        expect(
            validateAgentScheduleCadence({frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 60}).intervalMinutes
        ).toBeDefined();
    });

    it('accepts a complete cadence', () => {
        expect(validateAgentScheduleCadence({dayOfWeek: 3, frequencyKind: 'WEEKLY', timeOfDay: '07:00'})).toEqual({});
    });
});

describe('cadence parameter round-trip', () => {
    it('restores the picker state it stored', () => {
        const cadence = {dayOfWeek: 3, frequencyKind: 'WEEKLY', timeOfDay: '07:00'} as const;

        expect(fromCadenceParameters(toCadenceParameters(cadence))).toEqual({
            cronExpression: null,
            dayOfMonth: null,
            dayOfWeek: 3,
            frequencyKind: 'WEEKLY',
            intervalMinutes: null,
            minuteOfHour: null,
            timeOfDay: '07:00',
        });
    });

    it('falls back to CUSTOM_CRON when no frequencyKind was stored', () => {
        expect(fromCadenceParameters({expression: '0 9 * * ?'})).toEqual({
            cronExpression: '0 9 * * ?',
            dayOfMonth: null,
            dayOfWeek: null,
            frequencyKind: 'CUSTOM_CRON',
            intervalMinutes: null,
            minuteOfHour: null,
            timeOfDay: null,
        });
    });
});

describe('describeCadence', () => {
    it('names each picker cadence', () => {
        expect(describeCadence({frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 15})).toBe('Every 15 minutes');
        expect(describeCadence({frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 1})).toBe('Every minute');
        expect(describeCadence({frequencyKind: 'HOURLY', minuteOfHour: 5})).toBe('Hourly at :05');
        expect(describeCadence({frequencyKind: 'DAILY', timeOfDay: '09:00'})).toBe('Daily at 09:00');
        expect(describeCadence({dayOfWeek: 1, frequencyKind: 'WEEKLY', timeOfDay: '09:00'})).toBe(
            'Weekly on Monday at 09:00'
        );
        expect(describeCadence({dayOfMonth: 3, frequencyKind: 'MONTHLY', timeOfDay: '09:00'})).toBe(
            'Monthly on day 3 at 09:00'
        );
    });

    it('reads back what the picker stored', () => {
        const parameters = toCadenceParameters({dayOfWeek: 7, frequencyKind: 'WEEKLY', timeOfDay: '18:30'});

        expect(describeCadence(fromCadenceParameters(parameters))).toBe('Weekly on Sunday at 18:30');
    });

    it('falls back to the expression of a hand-written row', () => {
        expect(describeCadence(fromCadenceParameters({expression: '0 9 * * ?'}))).toBe('0 9 * * ?');
    });

    it('says nothing for a cadence missing its own fields', () => {
        expect(describeCadence({frequencyKind: 'DAILY'})).toBe('');
    });
});

describe('fromCronExpression', () => {
    const pickerCadences: AgentScheduleCadenceI[] = [
        {frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 5},
        {frequencyKind: 'HOURLY', minuteOfHour: 30},
        {frequencyKind: 'DAILY', timeOfDay: '07:57'},
        {dayOfWeek: 1, frequencyKind: 'WEEKLY', timeOfDay: '09:00'},
        {dayOfWeek: 7, frequencyKind: 'WEEKLY', timeOfDay: '23:05'},
        {dayOfMonth: 14, frequencyKind: 'MONTHLY', timeOfDay: '06:15'},
    ];

    it.each(pickerCadences)('round-trips $frequencyKind through its cron expression', (cadence) => {
        expect(fromCronExpression(toCronExpression(cadence))).toEqual(cadence);
    });

    it('covers every frequency kind the picker offers, so a new one cannot be forgotten here', () => {
        const roundTripped = new Set(pickerCadences.map((cadence) => cadence.frequencyKind));

        expect(
            [...SCHEDULE_FREQUENCY_KINDS]
                .filter((kind) => kind !== 'CUSTOM_CRON')
                .every((kind) => roundTripped.has(kind))
        ).toBe(true);
    });

    it.each(['0 9 * * *', '0 0 9 * * ?', 'not a cron', '', '0 9 * 3 ?'])(
        'treats %s as a custom expression',
        (expression) => {
            expect(fromCronExpression(expression)).toEqual({cronExpression: expression, frequencyKind: 'CUSTOM_CRON'});
        }
    );
});

describe('describeCronExpression', () => {
    it('reads a picker-written expression in words', () => {
        expect(describeCronExpression('57 7 * * ?')).toBe('Daily at 07:57');
        expect(describeCronExpression('0/15 * * * ?')).toBe('Every 15 minutes');
        expect(describeCronExpression('30 * * * ?')).toBe('Hourly at :30');
        expect(describeCronExpression('0 9 ? * 2')).toBe('Weekly on Monday at 09:00');
        expect(describeCronExpression('15 6 14 * ?')).toBe('Monthly on day 14 at 06:15');
    });

    it('falls back to the expression itself when it reads as nothing else', () => {
        expect(describeCronExpression('0 9 * * *')).toBe('0 9 * * *');
    });
});
