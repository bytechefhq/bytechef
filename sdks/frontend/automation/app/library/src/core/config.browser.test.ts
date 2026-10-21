// @vitest-environment jsdom
import {describe, expect, it} from 'vitest';

import {assertServerRuntime} from './config';

describe('assertServerRuntime in a browser-like environment', () => {
    it('throws because window exists', () => {
        expect(typeof window).toBe('object');
        expect(() => assertServerRuntime()).toThrowError(/server/i);
    });
});
