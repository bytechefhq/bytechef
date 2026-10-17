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
        ["input['", {callSegments: [false], quote: "'", root: 'input', segments: ['']}],
        ['input["cus', {callSegments: [false], quote: '"', root: 'input', segments: ['cus']}],
        [
            "context['component'][\"slack\"]['se",
            {callSegments: [false, false, false], quote: "'", root: 'context', segments: ['component', 'slack', 'se']},
        ],
        ['context.input()["', {callSegments: [true, false], quote: '"', root: 'context', segments: ['input', '']}],
        ["input['customer'].", {callSegments: [false, false], root: 'input', segments: ['customer', '']}],
    ])('parses %s', (text, expected) => {
        expect(parseMemberPath(text)).toEqual(expected);
    });

    it('returns undefined for a non-identifier tail', () => {
        expect(parseMemberPath('x = 1 +')).toBeUndefined();
        expect(parseMemberPath('')).toBeUndefined();
        expect(parseMemberPath("x = ['")).toBeUndefined();
        expect(parseMemberPath("['a")).toBeUndefined();
    });
});
