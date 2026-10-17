import {HttpMethod} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {getHttpMethodBadgeColor, getHttpMethodPillColor} from '../httpMethod-utils';

describe('getHttpMethodBadgeColor', () => {
    it.each([
        [HttpMethod.Get, 'text-content-brand-primary'],
        [HttpMethod.Post, 'text-content-success-primary'],
        [HttpMethod.Put, 'text-content-warning-primary'],
        [HttpMethod.Patch, 'text-orange-700 dark:text-orange-400'],
        [HttpMethod.Delete, 'text-content-destructive-primary'],
    ])('colours %s', (method, expectedClasses) => {
        expect(getHttpMethodBadgeColor(method)).toBe(expectedClasses);
    });

    it('falls back to a neutral colour without a method', () => {
        expect(getHttpMethodBadgeColor(null)).toBe('text-content-neutral-secondary');
        expect(getHttpMethodBadgeColor()).toBe('text-content-neutral-secondary');
    });
});

describe('getHttpMethodPillColor', () => {
    it.each([
        ['GET', 'bg-surface-success-secondary text-content-success-primary'],
        ['POST', 'bg-surface-brand-secondary text-content-brand-primary'],
        ['PUT', 'bg-yellow-100 text-yellow-800 dark:bg-yellow-950 dark:text-yellow-300'],
        ['PATCH', 'bg-surface-warning-secondary text-content-warning-primary'],
        ['DELETE', 'bg-surface-destructive-secondary text-content-destructive'],
    ])('colours %s', (method, expectedClasses) => {
        expect(getHttpMethodPillColor(method)).toBe(expectedClasses);
    });

    it('matches the method case-insensitively', () => {
        expect(getHttpMethodPillColor('get')).toBe(getHttpMethodPillColor('GET'));
    });

    it('falls back to a neutral colour for an unknown or missing method', () => {
        const neutralClasses = 'bg-surface-neutral-secondary text-content-neutral-primary';

        expect(getHttpMethodPillColor('OPTIONS')).toBe(neutralClasses);
        expect(getHttpMethodPillColor(null)).toBe(neutralClasses);
    });
});
