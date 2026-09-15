import WorkflowEditorReadOnlyProvider from '@/pages/platform/workflow-editor/providers/WorkflowEditorReadOnlyProvider';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useProperty} from '../useProperty';

const INPUT_SAVE_DEBOUNCE = 600;

const NULL_PROPERTY_SAVE_DELAY = 200;

const uriProperty = {
    controlType: 'URL',
    name: 'uri',
    type: 'STRING',
} as unknown as PropertyAllType;

const hoisted = vi.hoisted(() => ({
    deleteMutateAsync: vi.fn(),
    updateMutateAsync: vi.fn(),
}));

const createWrapper =
    (readOnly: boolean) =>
    ({children}: {children: ReactNode}) => (
        <WorkflowEditorReadOnlyProvider readOnly={readOnly}>
            <WorkflowEditorProvider
                value={
                    {
                        ...workflowEditorProviderTestValue,
                        deleteWorkflowNodeParameterMutation: {
                            isPending: false,
                            mutateAsync: hoisted.deleteMutateAsync,
                        },
                        updateWorkflowNodeParameterMutation: {
                            isPending: false,
                            mutateAsync: hoisted.updateMutateAsync,
                        },
                    } as never
                }
            >
                {children}
            </WorkflowEditorProvider>
        </WorkflowEditorReadOnlyProvider>
    );

const nullProperty = {
    controlType: 'NULL',
    name: 'nothing',
    type: 'NULL',
} as unknown as PropertyAllType;

const waitPastNullPropertySave = () =>
    act(async () => {
        await new Promise((resolve) => setTimeout(resolve, NULL_PROPERTY_SAVE_DELAY + 100));
    });

const renderNullProperty = (readOnly: boolean) =>
    renderHook(() => useProperty({path: 'nothing', property: nullProperty}), {wrapper: createWrapper(readOnly)});

describe('property writes in read-only mode', () => {
    beforeEach(() => {
        hoisted.deleteMutateAsync.mockReset().mockResolvedValue({parameters: {}});
        hoisted.updateMutateAsync.mockReset().mockResolvedValue({parameters: {}});

        useWorkflowDataStore.setState({
            workflow: {definition: '{"tasks":[]}', id: 'workflow-read-only-test', nodeNames: [], tasks: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'httpClient_1', parameters: {}, workflowNodeName: 'httpClient_1'},
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('saves a cleared input when the editor is editable', async () => {
        const {result} = renderHook(() => useProperty({path: 'uri', property: uriProperty}), {
            wrapper: createWrapper(false),
        });

        await act(async () => {
            result.current.handleInputClear();

            await new Promise((resolve) => setTimeout(resolve, INPUT_SAVE_DEBOUNCE + 100));
        });

        expect(hoisted.updateMutateAsync).toHaveBeenCalledTimes(1);
    });

    it('never saves a cleared input in read-only mode', async () => {
        const {result} = renderHook(() => useProperty({path: 'uri', property: uriProperty}), {
            wrapper: createWrapper(true),
        });

        await act(async () => {
            result.current.handleInputClear();

            await new Promise((resolve) => setTimeout(resolve, INPUT_SAVE_DEBOUNCE + 100));
        });

        expect(hoisted.updateMutateAsync).not.toHaveBeenCalled();
    });

    it('never saves an unset NULL property after mount in read-only mode', async () => {
        renderNullProperty(true);

        await waitPastNullPropertySave();

        expect(hoisted.updateMutateAsync).not.toHaveBeenCalled();
        expect(hoisted.deleteMutateAsync).not.toHaveBeenCalled();
    });

    it('never deletes a custom property in read-only mode', async () => {
        const {result} = renderNullProperty(true);

        await act(async () => {
            result.current.handleDeleteCustomPropertyClick('nothing');

            await new Promise((resolve) => setTimeout(resolve, 0));
        });

        expect(hoisted.deleteMutateAsync).not.toHaveBeenCalled();
    });

    it('deletes a custom property when the editor is editable', async () => {
        const {result} = renderNullProperty(false);

        await act(async () => {
            result.current.handleDeleteCustomPropertyClick('nothing');

            await new Promise((resolve) => setTimeout(resolve, 0));
        });

        expect(hoisted.deleteMutateAsync).toHaveBeenCalledTimes(1);
    });
});
