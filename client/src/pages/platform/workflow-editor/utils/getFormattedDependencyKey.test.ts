import {describe, expect, it} from 'vitest';

import getFormattedDependencyKey from './getFormattedDependencyKey';

describe('getFormattedDependencyKey', () => {
    it('should return empty string for empty array', () => {
        expect(getFormattedDependencyKey([])).toBe('');
    });

    it('should return empty string when called with no argument', () => {
        expect(getFormattedDependencyKey()).toBe('');
    });

    it('should return empty string if any value is undefined', () => {
        expect(getFormattedDependencyKey(['a', undefined, 'b'])).toBe('');
    });

    it('should return empty string if only value is undefined', () => {
        expect(getFormattedDependencyKey([undefined])).toBe('');
    });

    it('should convert primitive string values with String()', () => {
        expect(getFormattedDependencyKey(['hello'])).toBe('hello');
    });

    it('should convert primitive number values with String()', () => {
        expect(getFormattedDependencyKey([42])).toBe('42');
    });

    it('should convert boolean values with String()', () => {
        expect(getFormattedDependencyKey([true, false])).toBe('true,false');
    });

    it('should JSON.stringify object values', () => {
        expect(getFormattedDependencyKey([{key: 'value'}])).toBe('{"key":"value"}');
    });

    it('should JSON.stringify array values', () => {
        expect(getFormattedDependencyKey([[1, 2, 3]])).toBe('[1,2,3]');
    });

    it('should join multiple values with commas', () => {
        expect(getFormattedDependencyKey(['a', 'b', 'c'])).toBe('a,b,c');
    });

    it('should handle mixed primitives and objects', () => {
        expect(getFormattedDependencyKey(['hello', {id: 1}, 42])).toBe('hello,{"id":1},42');
    });

    it('should produce identical keys for structurally equal objects (deep equality)', () => {
        const keyA = getFormattedDependencyKey([{a: 1, b: 2}]);
        const keyB = getFormattedDependencyKey([{a: 1, b: 2}]);

        expect(keyA).toBe(keyB);
    });

    it('should produce different keys for structurally different objects', () => {
        const keyA = getFormattedDependencyKey([{a: 1}]);
        const keyB = getFormattedDependencyKey([{a: 2}]);

        expect(keyA).not.toBe(keyB);
    });

    it('should handle null values (passes through as "null")', () => {
        expect(getFormattedDependencyKey([null])).toBe('null');
    });

    it('should handle zero (falsy but defined)', () => {
        expect(getFormattedDependencyKey([0])).toBe('0');
    });

    it('should handle false (falsy but defined)', () => {
        expect(getFormattedDependencyKey([false])).toBe('false');
    });

    it('should handle empty string (falsy but defined)', () => {
        expect(getFormattedDependencyKey([''])).toBe('');
    });

    it('should handle empty object', () => {
        expect(getFormattedDependencyKey([{}])).toBe('{}');
    });

    it('should produce same key for two separate empty objects (reference inequality does not matter)', () => {
        const objectA = {};
        const objectB = {};

        expect(objectA !== objectB).toBe(true);
        expect(getFormattedDependencyKey([objectA])).toBe(getFormattedDependencyKey([objectB]));
    });
});
