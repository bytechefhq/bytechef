import {describe, expect, it} from 'vitest';

import {parseMemberPath} from './memberPath';

describe('parseMemberPath', () => {
    it.each([
        ['input.', {callSegments: [false], root: 'input', segments: ['']}],
        ['input.a.', {callSegments: [false, false], root: 'input', segments: ['a', '']}],
        ['context.input().', {callSegments: [true, false], root: 'context', segments: ['input', '']}],
        ["connection['googleMaps'].", {callSegments: [false, false], root: 'connection', segments: ['googleMaps', '']}],
        [
            'connection["googleMaps"].api',
            {callSegments: [false, false], root: 'connection', segments: ['googleMaps', 'api']},
        ],
        ["connection['a']['b'].", {callSegments: [false, false, false], root: 'connection', segments: ['a', 'b', '']}],
        [
            "context['x'].y['z'].",
            {callSegments: [false, false, false, false], root: 'context', segments: ['x', 'y', 'z', '']},
        ],
        ['foo(bar.', {callSegments: [false], root: 'bar', segments: ['']}],
        ['  con', {callSegments: [], root: 'con', segments: []}],
    ])('parses %s', (text, expected) => {
        expect(parseMemberPath(text)).toEqual(expected);
    });

    it('returns undefined for a non-identifier tail', () => {
        expect(parseMemberPath('x = 1 +')).toBeUndefined();
        expect(parseMemberPath('')).toBeUndefined();
    });
});
