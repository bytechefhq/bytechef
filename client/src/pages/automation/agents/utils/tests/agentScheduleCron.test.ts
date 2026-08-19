import {
    fromCadenceParameters,
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
        // ISO Monday (1) is Quartz 2; ISO Sunday (7) is Quartz 1.
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
