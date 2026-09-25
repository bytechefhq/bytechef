import {describe, expect, it} from 'vitest';

import {isEmptyPillContainerValue} from './pillContainerValue';

describe('isEmptyPillContainerValue', () => {
    it.each([undefined, null, '', []])('treats %j as an empty array', (value) => {
        expect(isEmptyPillContainerValue({controlType: 'ARRAY_BUILDER', value})).toBe(true);
    });

    it('treats an array with an item as not empty', () => {
        expect(isEmptyPillContainerValue({controlType: 'ARRAY_BUILDER', value: [null]})).toBe(false);
    });

    it('treats an object with only blank defined sub-properties as empty', () => {
        expect(
            isEmptyPillContainerValue({
                controlType: 'OBJECT_BUILDER',
                definedPropertyNames: ['name', 'tags', 'nested'],
                value: {name: '', nested: {inner: null}, tags: []},
            })
        ).toBe(true);
    });

    it('treats an object with a defined sub-property value as not empty', () => {
        expect(
            isEmptyPillContainerValue({
                controlType: 'OBJECT_BUILDER',
                definedPropertyNames: ['count'],
                value: {count: 0},
            })
        ).toBe(false);
    });

    it('treats an object with a custom entry as not empty, even without a value', () => {
        expect(isEmptyPillContainerValue({controlType: 'OBJECT_BUILDER', value: {header: null}})).toBe(false);
    });

    it('treats a file entry object like any other object', () => {
        expect(isEmptyPillContainerValue({controlType: 'FILE_ENTRY', value: {}})).toBe(true);
    });

    it.each([undefined, null, '', '{}', {}])('treats %j as no schema', (value) => {
        expect(isEmptyPillContainerValue({controlType: 'JSON_SCHEMA_BUILDER', value})).toBe(true);
    });

    it('treats a schema as not empty', () => {
        expect(isEmptyPillContainerValue({controlType: 'JSON_SCHEMA_BUILDER', value: '{"type":"object"}'})).toBe(false);
    });
});
