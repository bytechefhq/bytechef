import {WorkflowEditorCopilotContext} from '@/pages/platform/workflow-editor/providers/workflowEditorCopilotContext';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

import useWorkflowCodeEditorSheet from '../useWorkflowCodeEditorSheet';

vi.mock('monaco-editor', () => ({
    MarkerSeverity: {Error: 8},
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@/shared/hooks/usePersistJobId', () => ({
    usePersistJobId: () => ({getPersistedJobId: () => null, persistJobId: vi.fn()}),
}));

vi.mock('@/shared/hooks/useWorkflowTestStream', () => ({
    useWorkflowTestStream: () => ({close: vi.fn(), setStreamRequest: vi.fn()}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useValidateWorkflowQuery: () => ({data: undefined, refetch: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

const props = {
    invalidateWorkflowQueries: vi.fn(),
    onSheetOpenClose: vi.fn(),
    workflow: {definition: '{}', id: 'workflow-1'} as Workflow,
};

describe('useWorkflowCodeEditorSheet', () => {
    it('enables the copilot when the workflow editor allows it', () => {
        const {result} = renderHook(() => useWorkflowCodeEditorSheet(props));

        expect(result.current.copilotEnabled).toBe(true);
    });

    it('disables the copilot when the workflow editor runs without copilot', () => {
        const wrapper = ({children}: {children: ReactNode}) =>
            createElement(WorkflowEditorCopilotContext.Provider, {value: false}, children);

        const {result} = renderHook(() => useWorkflowCodeEditorSheet(props), {wrapper});

        expect(result.current.copilotEnabled).toBe(false);
    });
});
