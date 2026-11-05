import {TooltipProvider} from '@/components/ui/tooltip';
import {
    getCompletionContext,
    unregisterCompletionContext,
} from '@/shared/components/monaco/completion/completionContextRegistry';
import {Workflow, WorkflowFormat} from '@/shared/middleware/platform/configuration';
import {fireEvent, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowCodeEditorSheet from '../WorkflowCodeEditorSheet';

const hoisted = vi.hoisted(() => ({
    editorFocus: vi.fn(),
    editorOptions: {current: undefined as Record<string, unknown> | undefined},
    sheetState: {
        copilotEnabled: false,
        copilotPanelOpen: false,
        definition: '{}',
        dirty: false,
        errors: [] as string[],
        errorsAccordionOpen: false,
        handleCopilotClick: vi.fn(),
        handleCopilotClose: vi.fn(),
        handleDefinitionChange: vi.fn(),
        handleOpenChange: vi.fn(),
        handleRunClick: vi.fn(),
        handleSaveClick: vi.fn(),
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
        warnings: [] as string[],
        warningsAccordionOpen: false,
        workflowIsRunning: false,
        workflowTestExecution: undefined,
    },
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet', () => ({
    default: () => hoisted.sheetState,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: () => false,
}));

vi.mock('@/pages/platform/workflow-editor/components/RunDisabledReasons', () => ({
    default: () => <div />,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput', () => ({
    default: () => <div />,
}));

vi.mock(
    '@/pages/platform/workflow-editor/components/workflow-test-configuration/WorkflowTestConfigurationDialog',
    () => ({default: () => <div />})
);

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({
    default: () => <div />,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/components/monaco/completion/useComponentCatalog', () => ({
    useComponentCatalog: () => ({components: [], getConnectionDefinition: vi.fn(), getDefinition: vi.fn()}),
}));

vi.mock('@/shared/components/monaco/completion/useWorkflowNodeOutputsForCompletion', () => ({
    useWorkflowNodeOutputsForCompletion: () => () => Promise.resolve([]),
}));

vi.mock(
    '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/useEvaluatorFunctionDefinitions',
    () => ({useEvaluatorFunctionDefinitions: () => []})
);

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        onMount,
        options,
    }: {
        onMount: (editor: {focus: () => void; getModel: () => unknown}) => void;
        options?: Record<string, unknown>;
    }) => {
        hoisted.editorOptions.current = options;

        return (
            <button
                data-testid="mount-btn"
                onClick={() =>
                    onMount({
                        focus: hoisted.editorFocus,
                        getModel: () => ({uri: {toString: () => 'inmemory://test'}}),
                    })
                }
                type="button"
            >
                Mount
            </button>
        );
    },
}));

const renderSheet = (workflow: Partial<Workflow>) =>
    render(
        <TooltipProvider>
            <WorkflowCodeEditorSheet
                invalidateWorkflowQueries={vi.fn()}
                onSheetOpenClose={vi.fn()}
                runDisabled={false}
                sheetOpen
                testConfigurationDisabled={false}
                workflow={{definition: '{}', id: 'workflow-1', ...workflow} as Workflow}
            />
        </TooltipProvider>
    );

describe('WorkflowCodeEditorSheet', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        unregisterCompletionContext('inmemory://test');
        resetAll();
        vi.clearAllMocks();
    });

    it('registers a workflow definition completion context in the workflow format', async () => {
        renderSheet({format: WorkflowFormat.Yaml});

        fireEvent.click(await screen.findByTestId('mount-btn'));

        expect(getCompletionContext('inmemory://test')).toMatchObject({format: 'yaml', kind: 'workflowDefinition'});
    });

    it('defaults the completion format to json', async () => {
        renderSheet({});

        fireEvent.click(await screen.findByTestId('mount-btn'));

        expect(getCompletionContext('inmemory://test')).toMatchObject({format: 'json', kind: 'workflowDefinition'});
    });

    it('lets YAML suggest while typing a plain value and drops the word list', async () => {
        renderSheet({format: WorkflowFormat.Yaml});

        await screen.findByTestId('mount-btn');

        expect(hoisted.editorOptions.current).toMatchObject({
            quickSuggestions: {comments: 'off', other: 'on', strings: 'on'},
            wordBasedSuggestions: 'off',
        });
    });

    it('keeps the default suggestion options for JSON', async () => {
        renderSheet({});

        await screen.findByTestId('mount-btn');

        expect(hoisted.editorOptions.current?.quickSuggestions).toBeUndefined();
        expect(hoisted.editorOptions.current?.wordBasedSuggestions).toBeUndefined();
    });

    it('focuses the editor on mount', async () => {
        renderSheet({});

        fireEvent.click(await screen.findByTestId('mount-btn'));

        expect(hoisted.editorFocus).toHaveBeenCalled();
    });
});
