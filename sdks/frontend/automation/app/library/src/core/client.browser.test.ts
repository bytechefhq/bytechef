// @vitest-environment jsdom
import {describe, expect, it} from 'vitest';

import {createBytechefClient} from './client';

describe('createBytechefClient in a browser-like environment', () => {
    it('throws before any request is possible', () => {
        expect(() => createBytechefClient({appKey: 'k', url: 'https://x'})).toThrowError(/server/i);
    });
});
