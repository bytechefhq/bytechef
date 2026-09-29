import {WorkflowEditorCopilotContext} from '@/pages/platform/workflow-editor/providers/workflowEditorCopilotContext';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

import useClusterElementsCanvasDialog from '../useClusterElementsCanvasDialog';

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

const props = {onOpenChange: vi.fn()};

describe('useClusterElementsCanvasDialog', () => {
    it('enables the copilot when the workflow editor allows it', () => {
        const {result} = renderHook(() => useClusterElementsCanvasDialog(props));

        expect(result.current.copilotEnabled).toBe(true);
    });

    it('disables the copilot when the workflow editor runs without copilot', () => {
        const wrapper = ({children}: {children: ReactNode}) =>
            createElement(WorkflowEditorCopilotContext.Provider, {value: false}, children);

        const {result} = renderHook(() => useClusterElementsCanvasDialog(props), {wrapper});

        expect(result.current.copilotEnabled).toBe(false);
    });
});
