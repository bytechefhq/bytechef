import {describe, expect, it} from 'vitest';

import {getLocationLabel, getStatusCodeColor} from '../endpointEditor-utils';

describe('getLocationLabel', () => {
    it.each([
        ['query', 'Query'],
        ['path', 'Path'],
        ['header', 'Header'],
    ] as const)('labels %s', (location, expectedLabel) => {
        expect(getLocationLabel(location)).toBe(expectedLabel);
    });
});

describe('getStatusCodeColor', () => {
    it.each([
        ['200', 'bg-surface-success-secondary text-content-success-primary'],
        ['204', 'bg-surface-success-secondary text-content-success-primary'],
        ['301', 'bg-surface-brand-secondary text-content-brand-primary'],
        ['404', 'bg-surface-warning-secondary text-content-warning-primary'],
        ['500', 'bg-surface-destructive-secondary text-content-destructive'],
        ['503', 'bg-surface-destructive-secondary text-content-destructive'],
    ])('colours status %s', (statusCode, expectedClasses) => {
        expect(getStatusCodeColor(statusCode)).toBe(expectedClasses);
    });

    it('falls back to a neutral colour for an informational or non-numeric code', () => {
        const neutralClasses = 'bg-surface-neutral-secondary text-content-neutral-primary';

        expect(getStatusCodeColor('100')).toBe(neutralClasses);
        expect(getStatusCodeColor('default')).toBe(neutralClasses);
    });
});
