import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogToolbar from '../PropertyCodeEditorDialogToolbar';

const hoisted = vi.hoisted(() => ({
    scriptIsRunning: false,
}));

vi.mock('../hooks', () => ({
    usePropertyCodeEditorDialogToolbar: () => ({
        copilotEnabled: true,
        dirty: true,
        handleCopilotClick: vi.fn(),
        handleRunClick: vi.fn(),
        handleSaveClick: vi.fn(),
        handleStopClick: vi.fn(),
        saving: false,
        scriptIsRunning: hoisted.scriptIsRunning,
    }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@/components/ui/dialog', () => ({
    DialogClose: ({children}: {children: React.ReactNode}) => <div data-testid="dialog-close">{children}</div>,
}));

const renderToolbar = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <TooltipProvider>
                <PropertyCodeEditorDialogToolbar
                    language="javascript"
                    onChange={vi.fn()}
                    workflowId="workflow-1"
                    workflowNodeName="script_1"
                />
            </TooltipProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('PropertyCodeEditorDialogToolbar in read-only mode', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.scriptIsRunning = false;
    });

    afterEach(() => {
        resetAll();
    });

    it('offers Save and Run when the editor is editable', () => {
        renderToolbar(false);

        expect(screen.getByRole('button', {name: 'Save script'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Run script'})).toBeInTheDocument();
    });

    it('hides Save, Run and the Copilot entry in read-only mode', () => {
        renderToolbar(true);

        expect(screen.getByText('Edit Script')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Save script'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Run script'})).not.toBeInTheDocument();
        expect(screen.getAllByRole('button')).toHaveLength(2);
    });
});
