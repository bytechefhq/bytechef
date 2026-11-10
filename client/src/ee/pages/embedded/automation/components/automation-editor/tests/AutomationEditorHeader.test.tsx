import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationEditorHeader from '@/ee/pages/embedded/automation/components/automation-editor/AutomationEditorHeader';
import {useAutomationEditorSidebarStore} from '@/ee/pages/embedded/automation/components/automation-editor/stores/useAutomationEditorSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {onlineManager} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    createWorkflow: vi.fn(),
    handleWorkflowValueChange: vi.fn(),
    isMutating: vi.fn(() => 0),
    project: {
        current: undefined as Record<string, unknown> | undefined,
    },
    updatePermissionExpressionMutate: vi.fn(),
    updateProjectMutate: vi.fn(),
    useCreateAutomationProjectWorkflow: vi.fn(),
    workflowFileInputClick: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useIsMutating: () => hoisted.isMutating(),
}));

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/hooks/useAutomationEditorHeader', () => ({
    useAutomationEditorHeader: () => ({
        handlePublishProjectSubmit: vi.fn(),
        handleRunClick: vi.fn(),
        handleShowOutputClick: vi.fn(),
        handleStopClick: vi.fn(),
        handleWorkflowValueChange: hoisted.handleWorkflowValueChange,
        project: hoisted.project.current,
        publishProjectMutationIsPending: false,
    }),
}));

vi.mock('@/ee/pages/embedded/automation/hooks/useCreateAutomationProjectWorkflow', () => ({
    useCreateAutomationProjectWorkflow: (props: unknown) => {
        hoisted.useCreateAutomationProjectWorkflow(props);

        return {
            createWorkflow: hoisted.createWorkflow,
            handleWorkflowFileChange: vi.fn(),
            workflowFileInputRef: {
                get current() {
                    return {click: hoisted.workflowFileInputClick};
                },
                set current(_value) {},
            },
        };
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationProjectCategoriesQuery: () => ({data: {automationProjectCategories: []}}),
    useAutomationProjectTagsQuery: () => ({data: {automationProjectTags: []}}),
    useAutomationProjectsQuery: () => ({data: undefined}),
    useDeleteAutomationProjectMutation: () => ({mutate: vi.fn()}),
    useDeleteAutomationProjectWorkflowMutation: () => ({mutate: vi.fn()}),
    useDuplicateAutomationProjectMutation: () => ({mutate: vi.fn()}),
    useDuplicateAutomationProjectWorkflowMutation: () => ({mutate: vi.fn()}),
    useUpdateAutomationProjectMutation: () => ({mutate: hoisted.updateProjectMutate}),
    useUpdateAutomationProjectWorkflowPermissionExpressionMutation: () => ({
        mutate: hoisted.updatePermissionExpressionMutate,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationProjectVersionHistorySheet',
    () => ({
        default: () => null,
    })
);

vi.mock('@/pages/platform/workflow-editor/components/WorkflowActionsButton', () => ({
    default: () => <button>Test</button>,
}));

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/components/PublishPopover', () => ({
    default: () => <button>Publish</button>,
}));

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/components/OutputButton', () => ({
    default: () => <button>Output</button>,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({
    default: () => false,
}));

const mockProject = {
    id: 'project-1',
    lastPublishedVersion: null,
    name: 'Mailing',
    permissionExpression: 'plan == basic',
    published: false,
    version: 1,
    workflowTemplates: [
        {label: 'Mailer', permissionExpression: 'plan == pro', workflowId: 'draft-1', workflowUuid: 'workflow-1'},
        {label: 'Reporter', permissionExpression: null, workflowId: 'draft-2', workflowUuid: 'workflow-2'},
    ],
};

const updateWorkflowMutate = vi.fn();

const renderAutomationEditorHeader = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationEditorHeader
                    bottomResizablePanelRef={{current: null}}
                    currentWorkflowId="workflow-1"
                    projectId="project-1"
                    runDisabled={false}
                    updateWorkflowMutation={{mutate: updateWorkflowMutate} as unknown as UpdateWorkflowMutationType}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.project.current = mockProject;
    hoisted.isMutating.mockReturnValue(0);

    useAutomationEditorSidebarStore.setState({leftSidebarOpen: false});
    useWorkflowDataStore.setState({
        workflow: {
            definition: '{"label":"Mailer"}',
            description: '',
            id: 'draft-1',
            label: 'Mailer',
            nodeNames: [],
            version: 1,
        },
    });

    onlineManager.setOnline(true);
});

afterEach(() => {
    onlineManager.setOnline(true);
});

it('orders the header actions Test, Publish, Output, Settings', () => {
    renderAutomationEditorHeader();

    const actionNames = screen
        .getAllByRole('button')
        .map((button) => button.getAttribute('aria-label') || button.textContent)
        .filter((name) => ['Output', 'Publish', 'Settings', 'Test'].includes(name ?? ''));

    expect(actionNames).toEqual(['Test', 'Publish', 'Output', 'Settings']);
});

it('shows the current workflow in the breadcrumb switcher', () => {
    renderAutomationEditorHeader();

    expect(screen.getByRole('heading', {name: 'Mailing'})).toBeInTheDocument();
    expect(screen.getByLabelText('Select workflow')).toHaveTextContent('Mailer');
});

it('switches workflows by uuid from the breadcrumb switcher', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Select workflow'));
    await userEvent.click(within(screen.getByRole('menu')).getByText('Reporter'));

    expect(hoisted.handleWorkflowValueChange).toHaveBeenCalledWith('workflow-2');
});

it('hides the save indicator while nothing is being saved and the app is online', () => {
    renderAutomationEditorHeader();

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
});

it('shows the save indicator while a mutation is saving', () => {
    hoisted.isMutating.mockReturnValue(1);

    renderAutomationEditorHeader();

    expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();
});

it('shows the save indicator as soon as the app goes offline', () => {
    renderAutomationEditorHeader();

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();

    act(() => {
        onlineManager.setOnline(false);
    });

    expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();

    act(() => {
        onlineManager.setOnline(true);
    });

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
});

it('shows a skeleton until the project loads', () => {
    hoisted.project.current = undefined;

    renderAutomationEditorHeader();

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('toggles the workflows sidebar from the sidebar button', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getAllByRole('button')[0]);

    expect(useAutomationEditorSidebarStore.getState().leftSidebarOpen).toBe(true);

    await userEvent.click(screen.getAllByRole('button')[0]);

    expect(useAutomationEditorSidebarStore.getState().leftSidebarOpen).toBe(false);
});

it('creates workflows in the current project', () => {
    renderAutomationEditorHeader();

    expect(hoisted.useCreateAutomationProjectWorkflow).toHaveBeenCalledWith(
        expect.objectContaining({projectId: 'project-1'})
    );
});

it('creates a workflow from the New Workflow dialog opened from the settings menu', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

    const workflowDialog = await screen.findByRole('dialog', {name: 'Create Workflow'});

    await userEvent.type(within(workflowDialog).getByLabelText('Label'), 'Notifier');
    await userEvent.click(within(workflowDialog).getByRole('button', {name: 'Save'}));

    await waitFor(() => {
        expect(hoisted.createWorkflow).toHaveBeenCalledWith(expect.objectContaining({label: 'Notifier'}));
    });

    await waitFor(() => {
        expect(screen.queryByRole('dialog', {name: 'Create Workflow'})).not.toBeInTheDocument();
    });
});

it('opens the workflow file picker from the Import Workflow settings menu entry', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

    expect(hoisted.workflowFileInputClick).toHaveBeenCalledTimes(1);
});

it('saves the permission expression from the Edit Project dialog', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

    const projectDialog = await screen.findByRole('dialog', {name: 'Edit Project'});
    const permissionExpressionInput = within(projectDialog).getByLabelText('Permission Expression');

    expect(permissionExpressionInput).toHaveValue('plan == basic');

    await userEvent.clear(permissionExpressionInput);
    await userEvent.type(permissionExpressionInput, 'plan == enterprise');
    await userEvent.click(within(projectDialog).getByRole('button', {name: 'Save'}));

    await waitFor(() => {
        expect(hoisted.updateProjectMutate).toHaveBeenCalledWith(
            expect.objectContaining({
                id: 'project-1',
                name: 'Mailing',
                permissionExpression: 'plan == enterprise',
            }),
            expect.anything()
        );
    });
});

it("prefills and saves the current workflow's permission expression from the Edit Workflow dialog", async () => {
    updateWorkflowMutate.mockImplementation((_variables, options) => options.onSuccess());

    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

    const workflowDialog = await screen.findByRole('dialog', {name: 'Edit Workflow'});
    const permissionExpressionInput = within(workflowDialog).getByLabelText('Permission Expression');

    expect(permissionExpressionInput).toHaveValue('plan == pro');

    await userEvent.clear(permissionExpressionInput);
    await userEvent.type(permissionExpressionInput, 'plan == enterprise');
    await userEvent.click(within(workflowDialog).getByRole('button', {name: 'Save'}));

    await waitFor(() => {
        expect(hoisted.updatePermissionExpressionMutate).toHaveBeenCalledWith(
            {permissionExpression: 'plan == enterprise', workflowUuid: 'workflow-1'},
            expect.anything()
        );
    });

    expect(updateWorkflowMutate).toHaveBeenCalledWith(
        expect.objectContaining({id: 'draft-1', workflow: expect.objectContaining({version: 1})}),
        expect.anything()
    );
});
