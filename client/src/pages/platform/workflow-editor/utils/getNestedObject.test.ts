import {describe, expect, it} from 'vitest';

import getNestedObject from './getNestedObject';

describe('getNestedObject', () => {
    it('reads a dotted object path', () => {
        expect(getNestedObject({user: {address: {city: 'Zagreb'}}}, 'user.address.city')).toBe('Zagreb');
    });

    it('reads an indexed array property', () => {
        expect(getNestedObject({items: ['first', 'second']}, 'items[1]')).toBe('second');
    });

    it('treats [index] as the first array element', () => {
        expect(getNestedObject({items: [{id: 1}, {id: 2}]}, 'items[index].id')).toBe(1);
    });

    it('reads a bare index from an array', () => {
        expect(getNestedObject([{id: 'a'}, {id: 'b'}], '[1].id')).toBe('b');
    });

    it('reads a property from the first element when the current value is an array', () => {
        expect(getNestedObject({items: [{id: 'a'}, {id: 'b'}]}, 'items.id')).toBe('a');
    });

    it('reads a property from the indexed element when the current value is an array', () => {
        expect(getNestedObject([{id: 'a'}, {id: 'b'}], 'id[1]')).toBe('b');
    });

    it('returns falsy leaf values', () => {
        expect(getNestedObject({count: 0}, 'count')).toBe(0);
        expect(getNestedObject({enabled: false}, 'enabled')).toBe(false);
    });

    it('returns undefined when a segment is missing', () => {
        expect(getNestedObject({user: {}}, 'user.address.city')).toBeUndefined();
    });

    it('returns undefined when indexing a missing array', () => {
        expect(getNestedObject({}, 'items[0]')).toBeUndefined();
    });

    it('returns undefined when indexing past the end of an array of objects', () => {
        expect(getNestedObject([{id: 'a'}], 'id[3]')).toBeUndefined();
    });

    it('returns undefined for an empty segment', () => {
        expect(getNestedObject({a: 1}, '')).toBeUndefined();
    });

    it('returns undefined for a missing root', () => {
        expect(getNestedObject(undefined, 'a')).toBeUndefined();
        expect(getNestedObject(null, 'a')).toBeUndefined();
    });
});
