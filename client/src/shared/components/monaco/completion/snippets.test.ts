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

    it.each(['javascript', 'python', 'ruby'] as const)(
        'escapes a %s connection name for the single-quoted string and the snippet syntax',
        (language) => {
            expect(buildActionCallSnippet('send', language, ["o'brien"], false)).toBe("send({$1}, 'o\\\\'brien')");
            expect(buildActionCallSnippet('send', language, ['cost$1}'], false)).toBe("send({$1}, 'cost\\$1\\}')");
            expect(buildActionCallSnippet('send', language, ['back\\slash'], false)).toBe(
                "send({$1}, 'back\\\\\\\\slash')"
            );
        }
    );

    it('builds a function snippet', () => {
        expect(buildFunctionSnippet('log')).toBe('log($1)');
    });
});
