import {describe, expect, it} from 'vitest';

import {getJobDuration, getJobExecutionDate} from '../jobTiming';

describe('jobTiming', () => {
    describe('getJobDuration', () => {
        it('returns the rounded duration in milliseconds', () => {
            expect(
                getJobDuration({
                    endDate: new Date('2026-10-20T10:00:05.400Z'),
                    startDate: new Date('2026-10-20T10:00:00Z'),
                })
            ).toBe('5400ms');
        });

        it('returns nothing while the job has not ended', () => {
            expect(getJobDuration({startDate: new Date('2026-10-20T10:00:00Z')})).toBeUndefined();
            expect(getJobDuration(undefined)).toBeUndefined();
        });
    });

    describe('getJobExecutionDate', () => {
        it('returns the local start date and time', () => {
            const startDate = new Date('2026-10-20T10:00:00Z');

            expect(getJobExecutionDate({startDate})).toBe(
                `${startDate.toLocaleDateString()} ${startDate.toLocaleTimeString()}`
            );
        });

        it('returns nothing when the job has not started', () => {
            expect(getJobExecutionDate({})).toBeUndefined();
        });
    });
});
