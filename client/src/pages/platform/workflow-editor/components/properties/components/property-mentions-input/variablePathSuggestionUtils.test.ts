import {describe, expect, it} from 'vitest';

import {findVariablePathSuggestionMatch} from './variablePathSuggestionUtils';

function match(text: string) {
    const $position = {nodeBefore: {isText: true, text}, pos: 100} as never;

    return findVariablePathSuggestionMatch({$position});
}

describe('findVariablePathSuggestionMatch', () => {
    it('matches the partial path inside the first argument of getOrDefault', () => {
        expect(match("=getOrDefault('att")).toEqual({query: 'att', range: {from: 97, to: 100}, text: 'att'});
    });

    it('matches inside the first argument of exists, with double quotes and a space', () => {
        expect(match('=exists( "judge.ans')).toEqual({
            query: 'judge.ans',
            range: {from: 91, to: 100},
            text: 'judge.ans',
        });
    });

    it('matches an empty path right after the opening quote, so every path is offered', () => {
        expect(match("=exists('")).toEqual({query: '', range: {from: 100, to: 100}, text: ''});
    });

    it('matches inside a call nested in a larger formula', () => {
        expect(match("=${question} + getOrDefault('feed")?.query).toBe('feed');
    });

    it('matches a path with an array index', () => {
        expect(match("=exists('items[0].na")?.query).toBe('items[0].na');
    });

    it('does not match once the string is closed', () => {
        expect(match("=getOrDefault('attempts'")).toBeNull();
        expect(match("=getOrDefault('attempts', ")).toBeNull();
    });

    it('does not match the second argument', () => {
        expect(match("=getOrDefault('attempts', 'fall")).toBeNull();
    });

    it('does not match other functions or member calls', () => {
        expect(match("=concat('ab")).toBeNull();
        expect(match("=list.exists('ab")).toBeNull();
        expect(match("=notexists('ab")).toBeNull();
    });

    it('returns null when nodeBefore is not a text node', () => {
        const $position = {nodeBefore: {isText: false, text: undefined}, pos: 4} as never;

        expect(findVariablePathSuggestionMatch({$position})).toBeNull();
    });
});
