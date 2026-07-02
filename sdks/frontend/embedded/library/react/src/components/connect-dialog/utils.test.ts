import {describe, expect, it} from 'vitest';

import {optionsCacheKey} from './utils';

describe('optionsCacheKey', () => {
    it('keys by component reference, property, and dependency values', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', {workspace: 'W1'})).toBe(
            'slack:1:channel:channel:{"workspace":"W1"}'
        );
    });

    it('treats undefined dependency values as empty', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', undefined as never)).toBe(
            'slack:1:channel:channel:{}'
        );
    });
});
