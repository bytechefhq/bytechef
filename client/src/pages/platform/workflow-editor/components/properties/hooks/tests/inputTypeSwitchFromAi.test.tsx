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

/**
 * The input type switch restores the from-AI expression instead of clearing the field when a property the model
 * fills is switched back over to the editor. Clearing it left the editor showing the from-AI styling with no
 * content behind it.
 */

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

describe('handleInputTypeSwitchButtonClick from-AI restoration', () => {
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

    describe('switching to a constant value', () => {
        it('clears the field even for a from-AI property', () => {
            const {result} = renderUriProperty({fromAi: true, parameterValue: "=fromAi('uri')"});

            act(() => result.current.handleInputTypeSwitchButtonClick());

            expect(result.current.mentionInput).toBe(false);
            expect(result.current.mentionInputValue).toBe('');
            expect(result.current.propertyParameterValue).toBe('');
        });
    });

    describe('switching back over to the editor', () => {
        it('restores the from-AI expression as the editor content', () => {
            const {result} = renderUriProperty({fromAi: true});

            const {fromAiExpression} = result.current;

            act(() => result.current.handleInputTypeSwitchButtonClick());

            expect(result.current.mentionInput).toBe(false);

            act(() => result.current.handleInputTypeSwitchButtonClick());

            expect(result.current.mentionInput).toBe(true);
            expect(result.current.propertyParameterValue).toBe(fromAiExpression);
            expect(result.current.mentionInputValue).toBe(fromAiExpression.substring(1));
        });

        it('saves the restored expression back as a from-AI parameter', () => {
            const {result} = renderUriProperty({fromAi: true});

            const {fromAiExpression} = result.current;

            act(() => result.current.handleInputTypeSwitchButtonClick());

            (saveProperty as unknown as Mock).mockReset();

            act(() => result.current.handleInputTypeSwitchButtonClick());

            expect(saveProperty).toHaveBeenCalledWith(
                expect.objectContaining({
                    fromAi: true,
                    includeInMetadata: true,
                    path: PROPERTY_PATH,
                    value: fromAiExpression,
                })
            );
        });

        it('leaves the field empty for a property the model does not fill', () => {
            const {result} = renderUriProperty();

            act(() => result.current.handleInputTypeSwitchButtonClick());

            expect(result.current.mentionInput).toBe(false);

            act(() => result.current.handleInputTypeSwitchButtonClick());

            expect(result.current.mentionInput).toBe(true);
            expect(result.current.mentionInputValue).toBe('');
            expect(result.current.propertyParameterValue).toBe('');
        });
    });
});
