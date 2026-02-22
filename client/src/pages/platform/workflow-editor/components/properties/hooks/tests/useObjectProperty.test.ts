import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    const mockSaveProperty = vi.fn();

    const storeState = {
        currentComponent: null as {
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
        controlType: 'OBJECT_BUILDER',
        defaultValue: {},
        name: 'testField',
        type: 'OBJECT',
    },
];

describe('useObjectProperty — default value save guard', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.clearAllMocks();

        hoisted.storeState.currentComponent = {
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
            hoisted.storeState.currentComponent = {
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
        hoisted.storeState.currentComponent = {
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

    it('should reset save guard when path changes', async () => {
        const {useObjectProperty} = await import('../useObjectProperty');

        const {rerender} = renderHook(
            ({path, propertyName}: {path: string; propertyName: string}) =>
                useObjectProperty({
                    path,
                    property: {
                        controlType: 'OBJECT_BUILDER',
                        name: propertyName,
                        properties: subPropertyDefinitions,
                        type: 'OBJECT',
                    } as unknown as Parameters<typeof useObjectProperty>[0]['property'],
                }),
            {initialProps: {path: 'response.outputSchema', propertyName: 'outputSchema'}}
        );

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);

        rerender({path: 'response.newSchema', propertyName: 'newSchema'});

        await act(async () => {
            vi.advanceTimersByTime(700);
        });

        expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(2);
        expect(hoisted.mockSaveProperty).toHaveBeenLastCalledWith(
            expect.objectContaining({
                path: 'response.newSchema',
            })
        );
    });
});
