import {describe, expect, it} from 'vitest';

import {formatCost, formatLatency, formatScore, formatTimestamp, statusBadgeClass} from '../format';

describe('formatCost', () => {
    it('renders the placeholder for null and undefined', () => {
        expect(formatCost(null)).toBe('—');
        expect(formatCost(undefined)).toBe('—');
    });

    it('renders four decimal places of precision', () => {
        // The grid columns are narrow; truncating to 4 decimals keeps the right-aligned cost column from
        // wrapping while preserving sub-cent visibility for cheap models. A regression that switched to
        // toString() would render arbitrary-precision floats and break the layout.
        expect(formatCost(0.001234)).toBe('$0.0012');
        expect(formatCost(1.5)).toBe('$1.5000');
    });

    it('handles zero and negative values', () => {
        expect(formatCost(0)).toBe('$0.0000');
        expect(formatCost(-0.5)).toBe('$-0.5000');
    });
});

describe('formatLatency', () => {
    it('renders the placeholder for null and undefined', () => {
        expect(formatLatency(null)).toBe('—');
        expect(formatLatency(undefined)).toBe('—');
    });

    it('uses ms for sub-second values', () => {
        expect(formatLatency(0)).toBe('0 ms');
        expect(formatLatency(999)).toBe('999 ms');
    });

    it('switches to seconds at the 1000ms boundary', () => {
        // The 1000ms boundary is exact: 999 stays in ms, 1000 jumps to seconds. Operators reading the
        // grid expect a continuous transition, so the cutoff is documented behavior — a regression to
        // `<= 1000` would render "1.00 s" for 999ms which is misleading.
        expect(formatLatency(1000)).toBe('1.00 s');
        expect(formatLatency(1234)).toBe('1.23 s');
        expect(formatLatency(60_000)).toBe('60.00 s');
    });
});

describe('formatScore', () => {
    it('renders numeric values via toString', () => {
        expect(formatScore({dataType: 'NUMERIC', value: 0.95})).toBe('0.95');
        expect(formatScore({dataType: 'NUMERIC', value: 1})).toBe('1');
    });

    it('renders boolean values via toString', () => {
        // BOOLEAN scores are persisted as 0 / 1 in the value column — formatScore reads the same numeric
        // column for both NUMERIC and BOOLEAN dataTypes. A regression that branched on dataType to render
        // "true" / "false" would not match the way the AggregateScoreDeltas table renders averages.
        expect(formatScore({dataType: 'BOOLEAN', value: 1})).toBe('1');
        expect(formatScore({dataType: 'BOOLEAN', value: 0})).toBe('0');
    });

    it('falls back to stringValue for non-numeric dataTypes', () => {
        expect(formatScore({dataType: 'CATEGORICAL', stringValue: 'positive', value: null})).toBe('positive');
    });

    it('renders the placeholder when the relevant column is null', () => {
        expect(formatScore({dataType: 'NUMERIC', value: null})).toBe('—');
        expect(formatScore({dataType: 'CATEGORICAL', stringValue: null, value: null})).toBe('—');
    });
});

describe('formatTimestamp', () => {
    it('renders the placeholder for null and undefined', () => {
        expect(formatTimestamp(null)).toBe('—');
        expect(formatTimestamp(undefined)).toBe('—');
    });

    it('renders a non-empty string for a valid epoch timestamp', () => {
        // Locale-dependent — assert non-empty + contains the year so the test is portable across CI
        // locales (the toLocaleString() output varies by system).
        const result = formatTimestamp(Date.UTC(2026, 3, 28));

        expect(result).not.toBe('—');
        expect(result).toMatch(/2026|26/);
    });
});

describe('statusBadgeClass', () => {
    it('returns distinct classes per known status', () => {
        const completed = statusBadgeClass('COMPLETED');
        const failed = statusBadgeClass('FAILED');
        const pending = statusBadgeClass('PENDING');
        const running = statusBadgeClass('RUNNING');

        expect(completed).not.toBe(failed);
        expect(failed).not.toBe(pending);
        expect(pending).not.toBe(running);
    });

    it('returns the secondary fallback for unknown statuses', () => {
        // A future backend that adds a new AiExperimentRunStatus (e.g. CANCELLED) before the UI does
        // would otherwise render with a bare empty class — the secondary fallback ensures the badge is
        // still visible until the UI catches up.
        expect(statusBadgeClass('CANCELLED')).toContain('secondary');
        expect(statusBadgeClass('')).toContain('secondary');
    });
});
