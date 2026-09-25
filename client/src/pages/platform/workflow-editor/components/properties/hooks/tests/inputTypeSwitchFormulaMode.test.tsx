vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

import {useProperty} from '../useProperty';

const wrapper = ({children}: {children: ReactNode}) => (
    <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>{children}</WorkflowEditorProvider>
);

const uriProperty = {
    controlType: 'TEXT',
    expressionEnabled: true,
    name: 'uri',
    type: 'STRING',
} as PropertyAllType;

const renderUriProperty = (parameterValue?: string) =>
    renderHook(() => useProperty({parameterValue, path: 'parameters.uri', property: uriProperty}), {wrapper});

describe('handleInputTypeSwitchButtonClick formula mode', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-switch-test', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'httpClient_1', parameters: {}, workflowNodeName: 'httpClient_1'},
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    // Property definitions carry an explicit null defaultValue when none is set, and a controlled input handed
    // null is uncontrolled as far as React is concerned.
    it('resolves a null property default to an empty string', () => {
        const {result} = renderHook(
            () =>
                useProperty({
                    path: 'parameters.uri',
                    property: {...uriProperty, defaultValue: null} as unknown as PropertyAllType,
                }),
            {wrapper}
        );

        expect(result.current.defaultValue).toBe('');
    });

    it('keeps a falsy property default that is not null', () => {
        const {result} = renderHook(
            () =>
                useProperty({
                    path: 'parameters.timeout',
                    property: {
                        controlType: 'INTEGER',
                        defaultValue: 0,
                        name: 'timeout',
                        type: 'INTEGER',
                    } as unknown as PropertyAllType,
                }),
            {wrapper}
        );

        expect(result.current.defaultValue).toBe(0);
    });

    it('enters formula mode for a saved expression value', () => {
        const {result} = renderUriProperty("=concat('a', 'b')");

        expect(result.current.isFormulaMode).toBe(true);
        expect(result.current.mentionInput).toBe(true);
    });

    // The switch button is the only way out of the mentions editor in the uncontrolled path. Leaving formula
    // mode latched on kept the f(x) icon on the plain input and dropped the field straight back into formula
    // mode the next time it was switched over.
    it('leaves formula mode when switching back to a constant value', () => {
        const {result} = renderUriProperty("=concat('a', 'b')");

        expect(result.current.isFormulaMode).toBe(true);

        act(() => result.current.handleInputTypeSwitchButtonClick());

        expect(result.current.isFormulaMode).toBe(false);
        expect(result.current.mentionInput).toBe(false);
    });

    it('does not carry formula mode back over when the editor is switched on again', () => {
        const {result} = renderUriProperty("=concat('a', 'b')");

        act(() => result.current.handleInputTypeSwitchButtonClick());

        expect(result.current.mentionInput).toBe(false);

        act(() => result.current.handleInputTypeSwitchButtonClick());

        expect(result.current.mentionInput).toBe(true);
        expect(result.current.isFormulaMode).toBe(false);
    });

    // Switching a field that holds a constant over to the editor clears the saved constant. The clear's success
    // callback used to re-dispatch the mode captured before the switch, which dropped the field straight back
    // to the constant input and unmounted the editor the user had just been focused into.
    it('stays in the editor once clearing the constant value succeeds', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                name: 'dataStorage_1',
                parameters: {scope: 'CURRENT_EXECUTION'},
                workflowNodeName: 'dataStorage_1',
            },
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

        (saveProperty as unknown as Mock).mockReset();

        const {result} = renderHook(
            () =>
                useProperty({
                    parameterValue: 'CURRENT_EXECUTION',
                    property: {
                        controlType: 'SELECT',
                        expressionEnabled: true,
                        name: 'scope',
                        options: [{label: 'Current Execution', value: 'CURRENT_EXECUTION'}],
                        type: 'STRING',
                    } as unknown as PropertyAllType,
                }),
            {wrapper}
        );

        expect(result.current.mentionInput).toBe(false);

        act(() => result.current.handleInputTypeSwitchButtonClick());

        expect(result.current.mentionInput).toBe(true);
        expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({value: null}));

        const {successCallback} = (saveProperty as unknown as Mock).mock.calls[0][0];

        act(() => successCallback());

        expect(result.current.mentionInput).toBe(true);
    });
});
