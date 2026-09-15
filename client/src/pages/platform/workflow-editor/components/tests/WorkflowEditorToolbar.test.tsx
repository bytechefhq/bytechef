import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowEditorToolbar from '@/pages/platform/workflow-editor/components/WorkflowEditorToolbar';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {ReactFlowProvider} from '@xyflow/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowUndoRedo', () => ({
    default: () => ({canRedo: true, canUndo: true, handleRedo: vi.fn(), handleUndo: vi.fn()}),
}));

const EDITABLE_TOOLBAR_BUTTON_COUNT = 9;

const READ_ONLY_TOOLBAR_BUTTON_COUNT = 5;

const renderToolbar = ({contextReadOnly = false, readOnly = false}: {contextReadOnly?: boolean; readOnly?: boolean}) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={contextReadOnly}>
            <ReactFlowProvider>
                <TooltipProvider>
                    <WorkflowEditorToolbar enableUndoRedo readOnly={readOnly} />
                </TooltipProvider>
            </ReactFlowProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

afterEach(() => {
    resetAll();
});

describe('WorkflowEditorToolbar', () => {
    it('offers zoom, layout, reset layout, node lock, undo and redo when editable', () => {
        renderToolbar({});

        expect(screen.getAllByRole('button')).toHaveLength(EDITABLE_TOOLBAR_BUTTON_COUNT);
    });

    it('keeps hiding reset layout, node lock, undo and redo for the existing read-only workflow sheets', () => {
        renderToolbar({readOnly: true});

        expect(screen.getAllByRole('button')).toHaveLength(READ_ONLY_TOOLBAR_BUTTON_COUNT);
    });

    it('is unaffected by the viewer context alone when the editor passes no read-only flag', () => {
        renderToolbar({contextReadOnly: true});

        expect(screen.getAllByRole('button')).toHaveLength(EDITABLE_TOOLBAR_BUTTON_COUNT);
    });
});
