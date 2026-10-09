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
import {type Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useProperty} from '../useProperty';

const wrapper = ({children}: {children: ReactNode}) => (
    <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>{children}</WorkflowEditorProvider>
);

const maxTokensProperty = {
    controlType: 'INTEGER',
    defaultValue: 16000,
    maxValue: 128000,
    minValue: 1,
    name: 'maxTokens',
    required: true,
    type: 'INTEGER',
} as unknown as PropertyAllType;

const setCurrentNodeParameters = (parameters: {[key: string]: unknown}) => {
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {
            clusterElementType: 'model',
            name: 'anthropic_model_1',
            parameters,
            workflowNodeName: 'anthropic_model_1',
        },
        workflowNodeDetailsPanelOpen: true,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
};

const renderMaxTokensProperty = (property: PropertyAllType = maxTokensProperty) =>
    renderHook(() => useProperty({property}), {wrapper});

describe('useProperty required property default value', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-required-default', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('shows and saves the default when a required property is missing from stored parameters', () => {
        setCurrentNodeParameters({model: 'claude-sonnet-5-5'});

        const {result} = renderMaxTokensProperty();

        expect(result.current.inputValue).toBe(16000);

        act(() => {
            vi.advanceTimersByTime(250);
        });

        expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({path: 'maxTokens', value: 16000}));
    });

    it('shows and saves the default when a required property was cleared to null', () => {
        setCurrentNodeParameters({maxTokens: null, model: 'claude-sonnet-5-5'});

        const {result} = renderMaxTokensProperty();

        expect(result.current.inputValue).toBe(16000);

        act(() => {
            vi.advanceTimersByTime(250);
        });

        expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({path: 'maxTokens', value: 16000}));
    });

    // PropertyDynamicProperties hands a dynamic property its definition default as parameterValue even when the
    // workflow has nothing stored at its path.
    it('saves the default of a required dynamic property that is missing from stored parameters', () => {
        setCurrentNodeParameters({model: 'claude-sonnet-5-5', settings: {}});

        const {result} = renderHook(
            () =>
                useProperty({
                    dynamicPropertySource: 'settings',
                    objectName: 'settings',
                    parameterValue: 16000 as never,
                    path: 'settings',
                    property: maxTokensProperty,
                }),
            {wrapper}
        );

        expect(result.current.inputValue).toBe(16000);

        act(() => {
            vi.advanceTimersByTime(250);
        });

        expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({path: 'settings.maxTokens', value: 16000}));
    });

    it('keeps an explicit value of a required dynamic property', () => {
        setCurrentNodeParameters({model: 'claude-sonnet-5-5', settings: {maxTokens: 4096}});

        renderHook(
            () =>
                useProperty({
                    dynamicPropertySource: 'settings',
                    objectName: 'settings',
                    parameterValue: 4096 as never,
                    path: 'settings',
                    property: maxTokensProperty,
                }),
            {wrapper}
        );

        act(() => {
            vi.advanceTimersByTime(250);
        });

        expect(saveProperty).not.toHaveBeenCalled();
    });

    it('keeps a stored value of a required property', () => {
        setCurrentNodeParameters({maxTokens: 4096, model: 'claude-sonnet-5-5'});

        const {result} = renderMaxTokensProperty();

        expect(result.current.inputValue).toBe(4096);

        act(() => {
            vi.advanceTimersByTime(250);
        });

        expect(saveProperty).not.toHaveBeenCalled();
    });

    it('leaves an optional property with a default empty when it is missing', () => {
        setCurrentNodeParameters({model: 'claude-sonnet-5-5'});

        const {result} = renderMaxTokensProperty({...maxTokensProperty, required: false} as PropertyAllType);

        expect(result.current.inputValue).toBe('');

        act(() => {
            vi.advanceTimersByTime(250);
        });

        expect(saveProperty).not.toHaveBeenCalled();
    });
});
