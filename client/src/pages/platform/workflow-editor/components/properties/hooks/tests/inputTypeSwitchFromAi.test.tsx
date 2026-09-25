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
    description: 'The URI to call',
    expressionEnabled: true,
    name: 'uri',
    type: 'STRING',
} as PropertyAllType;

const PROPERTY_PATH = 'parameters.uri';

const renderUriProperty = ({fromAi = false, parameterValue}: {fromAi?: boolean; parameterValue?: string} = {}) => {
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {
            metadata: fromAi ? {ui: {fromAi: [PROPERTY_PATH]}} : undefined,
            name: 'httpClient_1',
            parameters: {},
            workflowNodeName: 'httpClient_1',
        },
        workflowNodeDetailsPanelOpen: true,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

    return renderHook(() => useProperty({parameterValue, path: PROPERTY_PATH, property: uriProperty}), {wrapper});
};

describe('Formula mode on a from-AI property', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-from-ai-switch', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        (saveProperty as unknown as Mock).mockReset();
    });

    it('recognises a property the model fills', () => {
        const {result} = renderUriProperty({fromAi: true});

        expect(result.current.isFromAi).toBe(true);
        expect(result.current.fromAiExpression).toContain("fromAi('uri', 'STRING'");
    });

    describe('while the model fills the property', () => {
        it('hides the Formula switch', () => {
            const {result} = renderUriProperty({fromAi: true, parameterValue: "=fromAi('uri')"});

            expect(result.current.showFormulaSwitch).toBe(false);
            expect(result.current.isFormulaMode).toBe(false);
        });

        it('keeps the from-AI expression in the editor outside Formula mode', () => {
            const {result} = renderUriProperty({fromAi: true});

            const {fromAiExpression} = result.current;

            act(() => result.current.handleFromAiClick?.(true));

            expect(result.current.mentionInput).toBe(true);
            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.showFormulaSwitch).toBe(false);
            expect(result.current.propertyParameterValue).toBe(fromAiExpression);
        });
    });

    describe('customizing the from-AI expression', () => {
        it('opens the expression in Formula mode', () => {
            const {result} = renderUriProperty({fromAi: true});

            const {fromAiExpression} = result.current;

            act(() => result.current.handleFromAiClick?.(false));

            expect(result.current.isFromAi).toBe(false);
            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.showFormulaSwitch).toBe(true);
            expect(result.current.propertyParameterValue).toBe(fromAiExpression);
            expect(saveProperty).toHaveBeenCalledWith(
                expect.objectContaining({
                    fromAi: false,
                    path: PROPERTY_PATH,
                    value: fromAiExpression,
                })
            );
        });
    });

    describe('switching Formula on and off', () => {
        it('leaves the field empty for a property the model does not fill', () => {
            const {result} = renderUriProperty();

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.mentionInput).toBe(true);

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.mentionInput).toBe(true);
            expect(result.current.mentionInputValue).toBe('');
            expect(result.current.propertyParameterValue).toBe('');
        });
    });
});
