import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    const mockSaveProperty = vi.fn();

    const storeState = {
        currentNode: null as {
            metadata?: {ui?: {dynamicPropertyTypes?: Record<string, string>}};
            parameters?: Record<string, unknown>;
        } | null,
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

const subPropertyDefinitions = [
    {
        controlType: 'TEXT',
        defaultValue: 'defaultText',
        name: 'testField',
        type: 'STRING',
    },
];

const renderObjectProperty = async (properties: Array<Record<string, unknown>>) => {
    const {useObjectProperty} = await import('../useObjectProperty');

    return renderHook(() =>
        useObjectProperty({
            path: 'response.outputSchema',
            property: {
                controlType: 'OBJECT_BUILDER',
                name: 'outputSchema',
                properties,
                type: 'OBJECT',
            } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
        })
    );
};

describe('useObjectProperty — default value save guard', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.clearAllMocks();

        hoisted.storeState.currentNode = {
            parameters: {response: {}},
        };
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should save default values when object does not exist in parameters', async () => {
        const {useObjectProperty} = await import('../useObjectProperty');

        renderHook(() =>
            useObjectProperty({
                path: 'response.outputSchema',
                property: {
                    controlType: 'OBJECT_BUILDER',
                    name: 'outputSchema',
                    properties: subPropertyDefinitions,
                    type: 'OBJECT',
                } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
            })
        );

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
        expect(hoisted.mockSaveProperty).toHaveBeenCalledWith(
            expect.objectContaining({
                path: 'response.outputSchema',
                type: 'OBJECT',
                workflowId: 'workflow-1',
            })
        );
    });

    it('should not save when no sub-property has a default value', async () => {
        await renderObjectProperty([
            {controlType: 'TEXT', name: 'firstField', type: 'STRING'},
            {controlType: 'INTEGER', name: 'secondField', type: 'INTEGER'},
        ]);

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
    });

    it('should not save when the defaults hold only empty objects and arrays', async () => {
        await renderObjectProperty([
            {
                controlType: 'OBJECT_BUILDER',
                name: 'nestedObject',
                properties: [{controlType: 'TEXT', name: 'innerField', type: 'STRING'}],
                type: 'OBJECT',
            },
            {controlType: 'OBJECT_BUILDER', defaultValue: {}, name: 'emptyObject', type: 'OBJECT'},
            {controlType: 'ARRAY_BUILDER', defaultValue: [], name: 'emptyArray', type: 'ARRAY'},
        ]);

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
    });

    it('should save when a nested sub-property has a default value', async () => {
        await renderObjectProperty([
            {controlType: 'TEXT', name: 'firstField', type: 'STRING'},
            {
                controlType: 'OBJECT_BUILDER',
                name: 'nestedObject',
                properties: [{controlType: 'INTEGER', defaultValue: 5, name: 'innerField', type: 'INTEGER'}],
                type: 'OBJECT',
            },
        ]);

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
        expect(hoisted.mockSaveProperty).toHaveBeenCalledWith(
            expect.objectContaining({
                value: {firstField: undefined, nestedObject: {innerField: 5}},
            })
        );
    });

    it('should not re-save after parameters update triggers subProperties rebuild', async () => {
        const {useObjectProperty} = await import('../useObjectProperty');

        const {rerender} = renderHook(
            ({properties}) =>
                useObjectProperty({
                    path: 'response.outputSchema',
                    property: {
                        controlType: 'OBJECT_BUILDER',
                        name: 'outputSchema',
                        properties,
                        type: 'OBJECT',
                    } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
                }),
            {initialProps: {properties: subPropertyDefinitions}}
        );

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);

        act(() => {
            hoisted.storeState.currentNode = {
                parameters: {response: {outputSchema: {testField: {}}}},
            };
        });

        rerender({properties: subPropertyDefinitions});

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
    });

    it('should not save when object already exists in parameters', async () => {
        hoisted.storeState.currentNode = {
            parameters: {response: {outputSchema: {testField: {}}}},
        };

        const {useObjectProperty} = await import('../useObjectProperty');

        renderHook(() =>
            useObjectProperty({
                path: 'response.outputSchema',
                property: {
                    controlType: 'OBJECT_BUILDER',
                    name: 'outputSchema',
                    properties: subPropertyDefinitions,
                    type: 'OBJECT',
                } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
            })
        );

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
    });

    it('should not add a duplicate subproperty when the typed name matches an existing one', async () => {
        hoisted.storeState.currentNode = {
            parameters: {response: {outputSchema: {testField: {}}}},
        };

        const {useObjectProperty} = await import('../useObjectProperty');

        const {result} = renderHook(() =>
            useObjectProperty({
                path: 'response.outputSchema',
                property: {
                    controlType: 'OBJECT_BUILDER',
                    name: 'outputSchema',
                    properties: subPropertyDefinitions,
                    type: 'OBJECT',
                } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
            })
        );

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(result.current.subProperties).toHaveLength(1);
        expect(result.current.existingSubPropertyNames).toContain('testField');

        act(() => {
            result.current.handleAddItemClick({name: 'testField', type: 'STRING'});
        });

        expect(result.current.subProperties).toHaveLength(1);
        expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
    });

    it('should reset save guard when path changes', async () => {
        const {useObjectProperty} = await import('../useObjectProperty');

        const {rerender} = renderHook(
            ({
                path,
                properties,
                propertyName,
            }: {
                path: string;
                properties: Array<Record<string, unknown>>;
                propertyName: string;
            }) =>
                useObjectProperty({
                    path,
                    property: {
                        controlType: 'OBJECT_BUILDER',
                        name: propertyName,
                        properties,
                        type: 'OBJECT',
                    } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
                }),
            {
                initialProps: {
                    path: 'response.outputSchema',
                    properties: subPropertyDefinitions,
                    propertyName: 'outputSchema',
                },
            }
        );

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);

        rerender({path: 'response.newSchema', properties: [...subPropertyDefinitions], propertyName: 'newSchema'});

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(2);
        expect(hoisted.mockSaveProperty).toHaveBeenLastCalledWith(
            expect.objectContaining({
                path: 'response.newSchema',
                value: {testField: 'defaultText'},
            })
        );
    });
});
