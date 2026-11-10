import {describe, expect, test, vi} from 'vitest';

import {getPropertySaveOverride, registerPropertySaveOverride} from './propertySaveOverrides';

describe('propertySaveOverrides', () => {
    test('returns the override registered for a node and path', () => {
        const save = vi.fn();

        const unregister = registerPropertySaveOverride({
            path: 'transitions[0].condition',
            save,
            workflowNodeName: 'graph_1',
        });

        expect(getPropertySaveOverride('graph_1', 'transitions[0].condition')).toBe(save);
        expect(getPropertySaveOverride('graph_1', 'transitions[1].condition')).toBeUndefined();
        expect(getPropertySaveOverride('graph_2', 'transitions[0].condition')).toBeUndefined();
        expect(getPropertySaveOverride(undefined, 'transitions[0].condition')).toBeUndefined();

        unregister();

        expect(getPropertySaveOverride('graph_1', 'transitions[0].condition')).toBeUndefined();
    });

    test('a stale unregister leaves a newer registration for the same path in place', () => {
        const firstSave = vi.fn();
        const secondSave = vi.fn();

        const unregisterFirst = registerPropertySaveOverride({
            path: 'transitions[0].to',
            save: firstSave,
            workflowNodeName: 'graph_1',
        });
        const unregisterSecond = registerPropertySaveOverride({
            path: 'transitions[0].to',
            save: secondSave,
            workflowNodeName: 'graph_1',
        });

        unregisterFirst();

        expect(getPropertySaveOverride('graph_1', 'transitions[0].to')).toBe(secondSave);

        unregisterSecond();

        expect(getPropertySaveOverride('graph_1', 'transitions[0].to')).toBeUndefined();
    });
});
