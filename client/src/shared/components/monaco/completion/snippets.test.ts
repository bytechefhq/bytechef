import {describe, expect, it} from 'vitest';

import {buildActionCallSnippet, buildFunctionSnippet} from './snippets';

describe('snippets', () => {
    it.each([
        [['gm'], false, "getAddress({$1}, 'gm')"],
        [['a', 'b'], false, "getAddress({$1}, '$2')"],
        [[], false, 'getAddress({$1})'],
        [['gm'], true, "getAddress({$1}, 'gm', {$3})"],
        [[], true, 'getAddress({$1}, null, {$2})'],
    ])('builds %s / cluster=%s', (connections, cluster, expected) => {
        expect(buildActionCallSnippet('getAddress', 'javascript', connections, cluster)).toBe(expected);
    });

    it.each([
        ['python', 'chat({$1}, None, {$2})'],
        ['ruby', 'chat({$1}, nil, {$2})'],
    ] as const)('passes a %s null connection before cluster elements', (language, expected) => {
        expect(buildActionCallSnippet('chat', language, [], true)).toBe(expected);
    });

    it('builds a function snippet', () => {
        expect(buildFunctionSnippet('log')).toBe('log($1)');
    });
});
