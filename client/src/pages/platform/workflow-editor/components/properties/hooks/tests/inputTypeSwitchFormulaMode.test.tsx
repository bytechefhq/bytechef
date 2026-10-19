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

describe('handleFormulaSwitch formula mode', () => {
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

    it('turns formula mode on for a text value and stays in the editor', () => {
        (saveProperty as unknown as Mock).mockReset();

        const {result} = renderUriProperty('hello');

        expect(result.current.isFormulaMode).toBe(false);
        expect(result.current.mentionInput).toBe(true);

        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(true);
        expect(result.current.mentionInput).toBe(true);
        expect(result.current.propertyParameterValue).toBe("='hello'");
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: "='hello'"}));
    });

    it('turns a quoted formula literal back into text and stays in the editor', () => {
        (saveProperty as unknown as Mock).mockReset();

        const {result} = renderUriProperty("='hello'");

        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(false);
        expect(result.current.mentionInput).toBe(true);
        expect(result.current.propertyParameterValue).toBe('hello');
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 'hello'}));
    });

    it('leaves formula mode and clears a formula that is not a literal', () => {
        (saveProperty as unknown as Mock).mockReset();

        const {result} = renderUriProperty("=concat('a', 'b')");

        expect(result.current.isFormulaMode).toBe(true);

        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(false);
        expect(result.current.mentionInput).toBe(true);
        expect(result.current.mentionInputValue).toBe('');
        expect(result.current.propertyParameterValue).toBe('');
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: null}));
    });
});
