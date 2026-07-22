import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    const mockSaveProperty = vi.fn();

    type StoreShapeType = {
        currentNode: {
            componentName?: string;
            metadata?: {ui?: {dynamicPropertyTypes?: Record<string, string>}};
            parameters?: Record<string, unknown>;
            workflowNodeName?: string;
        } | null;
    };

    const storeState: StoreShapeType = {
        currentNode: null,
    };

    return {
        mockSaveProperty,
        storeState,
    };
});

vi.mock('../../../../utils/saveProperty', () => ({
    default: hoisted.mockSaveProperty,
}));

vi.mock('../../../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: Object.assign((selector: (state: typeof hoisted.storeState) => unknown) => selector(hoisted.storeState), {
        getState: () => hoisted.storeState,
    }),
}));

vi.mock('../../../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: {workflow: {id: string}}) => unknown) => selector({workflow: {id: 'workflow-1'}}),
}));

vi.mock('../../../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        updateClusterElementParameterMutation: undefined,
        updateWorkflowNodeParameterMutation: {mutateAsync: vi.fn()},
    }),
}));

describe('useArrayProperty — hydration from currentNode.parameters', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        hoisted.storeState.currentNode = null;
    });

    it('hydrates items on REMOUNT with items:null (exact var_v1 shape)', async () => {
        hoisted.storeState.currentNode = {
            componentName: 'var',
            metadata: {
                ui: {
                    dynamicPropertyTypes: {
                        'value[0]': 'STRING',
                        'value[1]': 'STRING',
                        'value[2]': 'STRING',
                    },
                },
            },
            parameters: {
                type: 'ARRAY',
                value: ['www', 'wqewqe', 'rrrr'],
            },
            workflowNodeName: 'var_1',
        };

        const {useArrayProperty} = await import('../useArrayProperty');

        // First mount (user originally added 3 items).
        const first = renderHook(() =>
            useArrayProperty({
                onDeleteClick: vi.fn(),
                path: 'value',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                property: {controlType: 'ARRAY_BUILDER', items: null, name: 'value', type: 'ARRAY'} as any,
            })
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(first.result.current.arrayItems).toHaveLength(3);

        // Simulate "switch to Description tab" — unmount Properties.
        first.unmount();

        // Simulate "switch back to Properties" — fresh mount.
        // currentNode in the store is unchanged from before (Zustand store persists).
        const second = renderHook(() =>
            useArrayProperty({
                onDeleteClick: vi.fn(),
                path: 'value',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                property: {controlType: 'ARRAY_BUILDER', items: null, name: 'value', type: 'ARRAY'} as any,
            })
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(second.result.current.arrayItems).toHaveLength(3);
    });

    it('hydrates items when parameterValue contains a null slot (reproduces add-then-tab-switch)', async () => {
        // Server's setParameter may leave a null in the array when the user clicks
        // "Add" for a new item but hasn't typed a value yet. On tab return we hit
        // hydration with a null value. This checks the third branch handles it.
        hoisted.storeState.currentNode = {
            componentName: 'var',
            metadata: {
                ui: {
                    dynamicPropertyTypes: {
                        'value[0]': 'STRING',
                        'value[1]': 'STRING',
                    },
                },
            },
            parameters: {
                type: 'ARRAY',
                value: ['www', null],
            },
            workflowNodeName: 'var_1',
        };

        const {useArrayProperty} = await import('../useArrayProperty');

        const {result} = renderHook(() =>
            useArrayProperty({
                onDeleteClick: vi.fn(),
                path: 'value',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                property: {controlType: 'ARRAY_BUILDER', items: null, name: 'value', type: 'ARRAY'} as any,
            })
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(result.current.arrayItems).toHaveLength(2);
    });

    it('hydrates three STRING items from parameters.value on mount (var component scenario)', async () => {
        // This mirrors the workflow JSON from the bug report:
        //   parameters: {type: 'ARRAY', value: ['www', 'wqewqe', 'rrrr']}
        //   metadata.ui.dynamicPropertyTypes: {value[0]: 'STRING', value[1]: 'STRING', value[2]: 'STRING'}
        hoisted.storeState.currentNode = {
            componentName: 'var',
            metadata: {
                ui: {
                    dynamicPropertyTypes: {
                        'value[0]': 'STRING',
                        'value[1]': 'STRING',
                        'value[2]': 'STRING',
                    },
                },
            },
            parameters: {
                type: 'ARRAY',
                value: ['www', 'wqewqe', 'rrrr'],
            },
            workflowNodeName: 'var_1',
        };

        const {useArrayProperty} = await import('../useArrayProperty');

        const {result} = renderHook(() =>
            useArrayProperty({
                onDeleteClick: vi.fn(),
                path: 'value',
                property: {
                    controlType: 'ARRAY_BUILDER',
                    name: 'value',
                    type: 'ARRAY',
                    // Var component's array(VALUE) has no items() specified.
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any,
            })
        );

        // Allow the mount-time hydration effect to run.
        await act(async () => {
            await Promise.resolve();
        });

        expect(result.current.arrayItems).toHaveLength(3);

        const items = result.current.arrayItems as Array<{defaultValue: unknown; type: string}>;

        expect(items[0].defaultValue).toBe('www');
        expect(items[0].type).toBe('STRING');
        expect(items[1].defaultValue).toBe('wqewqe');
        expect(items[2].defaultValue).toBe('rrrr');
    });

    it('preserves expressionEnabled:false from the item definition on hydration (skills scenario)', async () => {
        // Regression for the skillsTool "Dynamic" toggle: the item definition
        // (integer(SKILL_ID).expressionEnabled(false)) must survive hydration so
        // Property.tsx hides the dynamic/expression toggle. The scalar hydration
        // branch previously hardcoded expressionEnabled: true, clobbering it.
        hoisted.storeState.currentNode = {
            componentName: 'aiAgentUtils',
            metadata: {
                ui: {
                    dynamicPropertyTypes: {
                        'skills[0]': 'INTEGER',
                        'skills[1]': 'INTEGER',
                    },
                },
            },
            parameters: {
                skills: [1, 2],
            },
            workflowNodeName: 'aiAgentUtils_1',
        };

        const {useArrayProperty} = await import('../useArrayProperty');

        const {result} = renderHook(() =>
            useArrayProperty({
                onDeleteClick: vi.fn(),
                path: 'skills',
                property: {
                    controlType: 'ARRAY_BUILDER',
                    items: [
                        {
                            controlType: 'SELECT',
                            expressionEnabled: false,
                            label: 'Skill',
                            name: 'skillId',
                            type: 'INTEGER',
                        },
                    ],
                    name: 'skills',
                    type: 'ARRAY',
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any,
            })
        );

        await act(async () => {
            await Promise.resolve();
        });

        const items = result.current.arrayItems as Array<{expressionEnabled: boolean}>;

        expect(items).toHaveLength(2);
        expect(items[0].expressionEnabled).toBe(false);
        expect(items[1].expressionEnabled).toBe(false);
    });

    it('re-syncs an item defaultValue from parameters after a save changes it', async () => {
        // Regression for the skills reset: mount hydration runs once, so after a save updates
        // currentNode.parameters the cached defaultValue must re-sync, otherwise the stale
        // value overrides the user's selection and the row appears to revert.
        hoisted.storeState.currentNode = {
            componentName: 'aiAgentUtils',
            metadata: {
                ui: {
                    dynamicPropertyTypes: {
                        'skills[0]': 'INTEGER',
                        'skills[1]': 'INTEGER',
                    },
                },
            },
            parameters: {
                skills: [1057, 1060],
            },
            workflowNodeName: 'aiAgentUtils_1',
        };

        const {useArrayProperty} = await import('../useArrayProperty');

        const {rerender, result} = renderHook(() =>
            useArrayProperty({
                onDeleteClick: vi.fn(),
                path: 'skills',
                property: {
                    controlType: 'ARRAY_BUILDER',
                    items: [{controlType: 'SELECT', name: 'skillId', type: 'INTEGER'}],
                    name: 'skills',
                    type: 'ARRAY',
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any,
            })
        );

        await act(async () => {
            await Promise.resolve();
        });

        let items = result.current.arrayItems as Array<{defaultValue: unknown}>;

        expect(items[0].defaultValue).toBe(1057);

        hoisted.storeState.currentNode = {
            ...hoisted.storeState.currentNode,
            parameters: {skills: [1099, 1060]},
        };

        rerender();

        await act(async () => {
            await Promise.resolve();
        });

        items = result.current.arrayItems as Array<{defaultValue: unknown}>;

        expect(items[0].defaultValue).toBe(1099);
        expect(items[1].defaultValue).toBe(1060);
    });
});
