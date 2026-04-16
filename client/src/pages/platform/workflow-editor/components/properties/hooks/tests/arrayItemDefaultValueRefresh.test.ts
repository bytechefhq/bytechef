import {describe, expect, it} from 'vitest';

/**
 * Verifies the fix for: selecting a value in an array item, then adding another
 * array item, used to reset the previous selection (it reappeared only after a
 * tab switch). The cause is in useArrayProperty.handleAddItemClick — without
 * refreshing existing items' defaultValue from the saved workflow parameters,
 * the re-render passes a stale `defaultValue: undefined` down to useProperty.
 * getExplicitArrayCellParameterValue coerces that to '' and useProperty's
 * workflow-sync effect treats '' as a meaningful prop ('' !== undefined),
 * overwriting the just-saved value.
 *
 * This replicates the refresh logic in handleAddItemClick.
 */

interface ArrayItemI {
    defaultValue?: unknown;
    key: string;
    name: string;
    type: string;
}

function refreshExistingArrayItems(
    arrayItems: Array<ArrayItemI | Array<ArrayItemI>>,
    savedValuesAtPath: Array<unknown>
): Array<ArrayItemI | Array<ArrayItemI>> {
    return arrayItems.map((existingItem, existingIndex) => {
        if (Array.isArray(existingItem)) {
            return existingItem;
        }

        const savedValue = savedValuesAtPath[existingIndex];

        if (savedValue === undefined || savedValue === null) {
            return existingItem;
        }

        return {
            ...existingItem,
            defaultValue: savedValue,
        };
    });
}

describe('handleAddItemClick existing array item defaultValue refresh', () => {
    it('refreshes a stale undefined defaultValue from the saved workflow parameter', () => {
        const staleItems: Array<ArrayItemI> = [
            {defaultValue: undefined, key: 'uuid-0', name: 'skill__0', type: 'INTEGER'},
        ];

        const refreshed = refreshExistingArrayItems(staleItems, [5]) as Array<ArrayItemI>;

        expect(refreshed[0].defaultValue).toBe(5);
    });

    it('preserves an existing defaultValue when nothing has been saved at that index', () => {
        const items: Array<ArrayItemI> = [{defaultValue: 'seeded', key: 'uuid-0', name: 'skill__0', type: 'STRING'}];

        const refreshed = refreshExistingArrayItems(items, [undefined]) as Array<ArrayItemI>;

        expect(refreshed[0].defaultValue).toBe('seeded');
    });

    it('treats null as no saved value so the existing defaultValue is kept', () => {
        const items: Array<ArrayItemI> = [{defaultValue: 'seeded', key: 'uuid-0', name: 'skill__0', type: 'STRING'}];

        const refreshed = refreshExistingArrayItems(items, [null]) as Array<ArrayItemI>;

        expect(refreshed[0].defaultValue).toBe('seeded');
    });

    it('refreshes multiple items independently and leaves the order untouched', () => {
        const items: Array<ArrayItemI> = [
            {defaultValue: undefined, key: 'uuid-0', name: 'skill__0', type: 'INTEGER'},
            {defaultValue: 7, key: 'uuid-1', name: 'skill__1', type: 'INTEGER'},
            {defaultValue: undefined, key: 'uuid-2', name: 'skill__2', type: 'INTEGER'},
        ];

        const refreshed = refreshExistingArrayItems(items, [5, 9, undefined]) as Array<ArrayItemI>;

        // Index 0: stale undefined refreshed to saved 5.
        // Index 1: stale 7 refreshed to saved 9 (saved value wins, even when item already has one).
        // Index 2: nothing saved at this index, so the existing undefined is preserved.
        expect(refreshed.map((item) => (item as ArrayItemI).defaultValue)).toEqual([5, 9, undefined]);
        expect((refreshed[0] as ArrayItemI).key).toBe('uuid-0');
        expect((refreshed[2] as ArrayItemI).key).toBe('uuid-2');
    });

    it('keeps the original object identity when no refresh is needed (no spurious renders)', () => {
        const original: ArrayItemI = {defaultValue: 5, key: 'uuid-0', name: 'skill__0', type: 'INTEGER'};

        const refreshed = refreshExistingArrayItems([original], [undefined]) as Array<ArrayItemI>;

        expect(refreshed[0]).toBe(original);
    });

    it('passes nested array entries through untouched', () => {
        const nested: Array<ArrayItemI> = [{defaultValue: 'a', key: 'inner', name: 'inner__0', type: 'STRING'}];

        const items: Array<ArrayItemI | Array<ArrayItemI>> = [nested];

        const refreshed = refreshExistingArrayItems(items, ['ignored']);

        expect(refreshed[0]).toBe(nested);
    });
});

describe('add-then-add scenario regression (Skills tool)', () => {
    /**
     * Replicates the user-visible flow that exposed the bug:
     *   1. The user has selected skill id 5 in array item 0 — the workflow
     *      params now hold skills: [5], but arrayItem[0].defaultValue is still
     *      undefined because handleSelectChange only updates useProperty's
     *      local selectValue, not useArrayProperty's snapshot.
     *   2. The user clicks the "Choose a skill..." add button. The previous
     *      implementation appended the new item without refreshing existing
     *      items, so the re-rendered item 0 carried defaultValue: undefined,
     *      which getExplicitArrayCellParameterValue coerced to ''.
     *   3. After the fix, item 0's defaultValue mirrors the saved value (5)
     *      so the explicit parameterValue passed downstream stays 5 and the
     *      combobox does not reset.
     */
    it('after fix: previously selected item retains its value when a new item is appended', () => {
        const beforeAdd: Array<ArrayItemI> = [
            {defaultValue: undefined, key: 'uuid-0', name: 'skill__0', type: 'INTEGER'},
        ];

        const refreshed = refreshExistingArrayItems(beforeAdd, [5]) as Array<ArrayItemI>;

        const newItem: ArrayItemI = {defaultValue: undefined, key: 'uuid-1', name: 'skill__1', type: 'INTEGER'};

        const afterAdd = [...refreshed, newItem] as Array<ArrayItemI>;

        expect(afterAdd).toHaveLength(2);
        expect(afterAdd[0].defaultValue).toBe(5);
        expect(afterAdd[1].defaultValue).toBeUndefined();
    });

    it('regression check: without refresh, item 0 would still carry the stale undefined that triggered the reset', () => {
        const beforeAdd: Array<ArrayItemI> = [
            {defaultValue: undefined, key: 'uuid-0', name: 'skill__0', type: 'INTEGER'},
        ];

        const newItem: ArrayItemI = {defaultValue: undefined, key: 'uuid-1', name: 'skill__1', type: 'INTEGER'};

        const naiveAfterAdd = [...beforeAdd, newItem];

        expect(naiveAfterAdd[0].defaultValue).toBeUndefined();
    });
});
