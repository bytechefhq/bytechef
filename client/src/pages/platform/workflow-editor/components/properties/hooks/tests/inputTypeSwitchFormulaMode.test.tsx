vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

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
});
