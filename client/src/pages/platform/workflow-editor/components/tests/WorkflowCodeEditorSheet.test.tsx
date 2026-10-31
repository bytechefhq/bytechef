import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    handleSaveClick: vi.fn(),
    workflowIsRunning: false,
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet', () => ({
    default: () => ({
        copilotEnabled: false,
        copilotPanelOpen: false,
        definition: '{}',
        dirty: true,
        errors: [],
        errorsAccordionOpen: false,
        handleCopilotClick: vi.fn(),
        handleCopilotClose: vi.fn(),
        handleDefinitionChange: vi.fn(),
        handleOpenChange: vi.fn(),
        handleRunClick: vi.fn(),
        handleSaveClick: hoisted.handleSaveClick,
        handleStopClick: vi.fn(),
        handleUnsavedChangesAlertDialogClose: vi.fn(),
        handleUnsavedChangesAlertDialogOpen: vi.fn(),
        handleValidate: vi.fn(),
        handleWorkflowTestConfigurationDialog: vi.fn(),
        hasErrors: false,
        projectName: 'Project',
        setErrorsAccordionOpen: vi.fn(),
        setWarningsAccordionOpen: vi.fn(),
        showWorkflowTestConfigurationDialog: false,
        unsavedChangesAlertDialogOpen: false,
        warnings: [],
        warningsAccordionOpen: false,
        workflowIsRunning: hoisted.workflowIsRunning,
        workflowTestExecution: undefined,
    }),
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({options}: {options?: {readOnly?: boolean}}) => (
        <span data-testid="workflow-code-editor-read-only">{String(options?.readOnly)}</span>
    ),
}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => null}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput', () => ({default: () => null}));

vi.mock(
    '@/pages/platform/workflow-editor/components/workflow-test-configuration/WorkflowTestConfigurationDialog',
    () => ({
        default: () => null,
    })
);

vi.mock('@/components/UnsavedChangesAlertDialog', () => ({default: () => null}));

const renderSheet = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <TooltipProvider>
                <WorkflowCodeEditorSheet
                    invalidateWorkflowQueries={vi.fn()}
                    onSheetOpenClose={vi.fn()}
                    runDisabled={false}
                    sheetOpen={true}
                    testConfigurationDisabled={false}
                    workflow={{definition: '{}', format: 'JSON', id: 'workflow-1', label: 'Workflow One'}}
                />
            </TooltipProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('WorkflowCodeEditorSheet', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.workflowIsRunning = false;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('offers Save, Test and an editable definition when the editor is editable', async () => {
        renderSheet(false);

        expect(await screen.findByTestId('workflow-code-editor-read-only')).toHaveTextContent('false');
        expect(screen.getByRole('button', {name: 'Save current workflow'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test Configuration'})).toBeEnabled();
    });

    it('shows a read-only definition without Save or Test in read-only mode', async () => {
        renderSheet(true);

        expect(await screen.findByTestId('workflow-code-editor-read-only')).toHaveTextContent('true');
        expect(screen.queryByRole('button', {name: 'Save current workflow'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Test'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test Configuration'})).toBeDisabled();
    });

    it('still offers Stop for a running test in read-only mode', async () => {
        hoisted.workflowIsRunning = true;

        renderSheet(true);

        expect(await screen.findByRole('button', {name: 'Stop'})).toBeInTheDocument();
    });
});
