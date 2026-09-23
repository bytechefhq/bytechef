import useCopilotBuildModeReadOnlySync from '@/pages/platform/workflow-editor/hooks/useCopilotBuildModeReadOnlySync';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it} from 'vitest';

const createWrapper =
    (readOnly: boolean) =>
    ({children}: {children: ReactNode}) => (
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>{children}</WorkflowEditorReadOnlyContext.Provider>
    );

describe('useCopilotBuildModeReadOnlySync', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({buildModeDisabled: false});
        useCopilotStore.setState({context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR}});
    });

    it('leaves Build mode available when the editor is editable', () => {
        renderHook(() => useCopilotBuildModeReadOnlySync(), {wrapper: createWrapper(false)});

        expect(useCopilotPanelStore.getState().buildModeDisabled).toBe(false);
        expect(useCopilotStore.getState().context.mode).toBe(MODE.BUILD);
    });

    it('disables Build mode and falls back to Ask in read-only mode', () => {
        renderHook(() => useCopilotBuildModeReadOnlySync(), {wrapper: createWrapper(true)});

        expect(useCopilotPanelStore.getState().buildModeDisabled).toBe(true);
        expect(useCopilotStore.getState().context.mode).toBe(MODE.ASK);
    });

    it('makes Build mode available again once the read-only editor unmounts', () => {
        const {unmount} = renderHook(() => useCopilotBuildModeReadOnlySync(), {wrapper: createWrapper(true)});

        unmount();

        expect(useCopilotPanelStore.getState().buildModeDisabled).toBe(false);
    });
});
