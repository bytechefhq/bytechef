import {describe, expect, it} from 'vitest';

import {assertSupportedNodeVersion, satisfies} from './check-node-version.mjs';

describe('check-node-version', () => {
    describe('satisfies', () => {
        it('rejects Node 20.12.0 — the version that broke the production Docker build', () => {
            expect(satisfies('20.12.0')).toBe(false);
        });

        it('rejects Node 20.18.99 — anything below the 20.19 threshold', () => {
            expect(satisfies('20.18.99')).toBe(false);
        });

        it('accepts Node 20.19.0 — the minimum supported 20.x release', () => {
            expect(satisfies('20.19.0')).toBe(true);
        });

        it('accepts later 20.x patch releases', () => {
            expect(satisfies('20.20.5')).toBe(true);
        });

        it('rejects Node 21.x — non-LTS, not listed by Vite as supported', () => {
            expect(satisfies('21.7.3')).toBe(false);
        });

        it('rejects Node 22.11.0 — below the 22.12 threshold', () => {
            expect(satisfies('22.11.0')).toBe(false);
        });

        it('accepts Node 22.12.0 — the minimum supported 22.x release', () => {
            expect(satisfies('22.12.0')).toBe(true);
        });

        it('accepts current 22.x LTS releases', () => {
            expect(satisfies('22.20.0')).toBe(true);
        });

        it('accepts Node 23+ — newer majors are allowed', () => {
            expect(satisfies('23.0.0')).toBe(true);
            expect(satisfies('24.5.1')).toBe(true);
        });
    });

    describe('assertSupportedNodeVersion', () => {
        it('throws with a clear message for unsupported versions', () => {
            expect(() => assertSupportedNodeVersion('20.12.0')).toThrow(/Node\.js 20\.12\.0 is not supported/);
            expect(() => assertSupportedNodeVersion('20.12.0')).toThrow(/20\.19\+ or 22\.12\+/);
        });

        it('does not throw for supported versions', () => {
            expect(() => assertSupportedNodeVersion('20.19.0')).not.toThrow();
            expect(() => assertSupportedNodeVersion('22.12.0')).not.toThrow();
            expect(() => assertSupportedNodeVersion('24.0.0')).not.toThrow();
        });

        it('defaults to the currently running Node version, which must be supported while running this test', () => {
            expect(() => assertSupportedNodeVersion()).not.toThrow();
        });
    });
});
