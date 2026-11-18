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
    configurationDialogProps: vi.fn(),
    configurationsSheetProps: vi.fn(),
    createWorkflow: vi.fn(),
    deleteProjectMutation: {isPending: false, mutate: vi.fn()},
    deleteWorkflowMutate: vi.fn(),
    handleWorkflowValueChange: vi.fn(),
    hasUnpublishedChanges: {current: true},
    isMutating: vi.fn(() => 0),
    navigate: vi.fn(),
    project: {
        current: undefined as Record<string, unknown> | undefined,
    },
    restoreProjectVersion: vi.fn(),
    updatePermissionExpressionMutate: vi.fn(),
    updateProjectMutate: vi.fn(),
    useCreateAutomationProjectWorkflow: vi.fn(),
    useRestoreAutomationProjectVersion: vi.fn(),
    versionHistorySheetProps: vi.fn(),
    workflowFileInputClick: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useIsMutating: () => hoisted.isMutating(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/hooks/useAutomationEditorHeader', () => ({
    useAutomationEditorHeader: () => ({
        handlePublishProjectSubmit: vi.fn(),
        handleRunClick: vi.fn(),
        handleShowOutputClick: vi.fn(),
        handleStopClick: vi.fn(),
        handleWorkflowValueChange: hoisted.handleWorkflowValueChange,
        hasUnpublishedChanges: hoisted.hasUnpublishedChanges.current,
        project: hoisted.project.current,
        publishProjectMutationIsPending: false,
    }),
}));

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/hooks/useRestoreAutomationProjectVersion', () => ({
    useRestoreAutomationProjectVersion: (props: unknown) => {
        hoisted.useRestoreAutomationProjectVersion(props);

        return {handleRestoreProjectVersion: hoisted.restoreProjectVersion, restoreProjectVersionPending: true};
    },
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
    useDeleteAutomationProjectMutation: () => hoisted.deleteProjectMutation,
    useDeleteAutomationProjectWorkflowMutation: () => ({mutate: hoisted.deleteWorkflowMutate}),
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
        default: (props: Record<string, unknown>) => {
            hoisted.versionHistorySheetProps(props);

            return null;
        },
    })
);

vi.mock(
    '@/ee/pages/embedded/automations/components/automation-project-configuration-dialog/AutomationProjectConfigurationDialog',
    () => ({
        default: (props: Record<string, unknown>) => {
            hoisted.configurationDialogProps(props);

            return (
                <div role="dialog">
                    Configuration Dialog
                    <button onClick={() => (props.onSuccess as ((id: string) => void) | undefined)?.('7')}>
                        Save Configuration
                    </button>
                </div>
            );
        },
    })
);

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/components/AutomationConfigurationsSheet', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.configurationsSheetProps(props);

        return <div role="complementary">Configurations Sheet</div>;
    },
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowActionsButton', () => ({
    default: () => <button>Test</button>,
}));

vi.mock('@/shared/components/PublishPopover', () => ({
    default: ({disabled}: {disabled?: boolean}) => <button disabled={disabled}>Publish</button>,
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

    hoisted.deleteProjectMutation = {isPending: false, mutate: vi.fn()};
    hoisted.hasUnpublishedChanges.current = true;
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

it('orders the header actions Test, Publish, Configure, Automation Configurations, Output, Settings', () => {
    renderAutomationEditorHeader();

    const actionNames = screen
        .getAllByRole('button')
        .map((button) => button.getAttribute('aria-label') || button.textContent)
        .filter((name) =>
            ['Automation Configurations', 'Configure', 'Output', 'Publish', 'Settings', 'Test'].includes(name ?? '')
        );

    expect(actionNames).toEqual(['Test', 'Publish', 'Configure', 'Automation Configurations', 'Output', 'Settings']);
});

it('groups Publish, Configure and the configurations list', () => {
    renderAutomationEditorHeader();

    const buttonGroup = screen.getByRole('group');

    expect(within(buttonGroup).getByRole('button', {name: 'Publish'})).toBeInTheDocument();
    expect(within(buttonGroup).getByRole('button', {name: 'Configure'})).toBeInTheDocument();
    expect(within(buttonGroup).getByRole('button', {name: 'Automation Configurations'})).toBeInTheDocument();
});

it('enables Publish while the project has unpublished changes', () => {
    renderAutomationEditorHeader();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeEnabled();
});

it('disables Publish when the project has no unpublished changes', () => {
    hoisted.hasUnpublishedChanges.current = false;

    renderAutomationEditorHeader();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
});

it('disables Configure and explains why while the project is unpublished', async () => {
    renderAutomationEditorHeader();

    const configureButton = screen.getByRole('button', {name: 'Configure'});

    expect(configureButton).toBeDisabled();

    await userEvent.hover(configureButton.parentElement!);

    expect(await screen.findByRole('tooltip')).toHaveTextContent('Publish the project before configuring it.');
});

it('opens the configuration dialog with tabs for this project from Configure', async () => {
    hoisted.project.current = {...mockProject, lastPublishedVersion: 1, published: true};

    renderAutomationEditorHeader();

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Configure'}));

    expect(screen.getByRole('dialog')).toHaveTextContent('Configuration Dialog');
    expect(hoisted.configurationDialogProps).toHaveBeenCalledWith(
        expect.objectContaining({project: hoisted.project.current, showTabs: true})
    );
});

it('opens the automation configurations sheet once Configure creates a configuration', async () => {
    hoisted.project.current = {...mockProject, lastPublishedVersion: 1, published: true};

    renderAutomationEditorHeader();

    await userEvent.click(screen.getByRole('button', {name: 'Configure'}));
    await userEvent.click(screen.getByRole('button', {name: 'Save Configuration'}));

    expect(screen.getByRole('complementary')).toHaveTextContent('Configurations Sheet');
});

it('opens the automation configurations sheet from the list button', async () => {
    renderAutomationEditorHeader();

    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Automation Configurations'}));

    expect(screen.getByRole('complementary')).toHaveTextContent('Configurations Sheet');
    expect(hoisted.configurationsSheetProps).toHaveBeenCalledWith(
        expect.objectContaining({project: mockProject, sheetOpen: true})
    );
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

it('shows the labelled save indicator after the workflow select while a mutation is saving', () => {
    hoisted.isMutating.mockReturnValue(1);

    renderAutomationEditorHeader();

    const loadingIndicator = screen.getByLabelText('Loading indicator');

    expect(loadingIndicator).toHaveTextContent('Saving…');
    expect(
        loadingIndicator.compareDocumentPosition(screen.getByRole('button', {name: 'Settings'})) &
            Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy();
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

it('toggles the projects sidebar from the See projects button', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByRole('button', {name: 'See projects'}));

    expect(useAutomationEditorSidebarStore.getState().leftSidebarOpen).toBe(true);

    await userEvent.click(screen.getByRole('button', {name: 'See projects'}));

    expect(useAutomationEditorSidebarStore.getState().leftSidebarOpen).toBe(false);
});

it('creates workflows in the current project', () => {
    renderAutomationEditorHeader();

    expect(hoisted.useCreateAutomationProjectWorkflow).toHaveBeenCalledWith(
        expect.objectContaining({projectId: 'project-1'})
    );
});

it('shows the project Duplicate action for a visual project', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));

    expect(screen.getByRole('menuitem', {name: 'Duplicate'})).toBeInTheDocument();
});

it('hides the project Duplicate action for a code project', async () => {
    hoisted.project.current = {...mockProject, codeWorkflowProject: true};

    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));

    expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', {name: 'Duplicate'})).not.toBeInTheDocument();
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

it('opens the next remaining workflow of the project after deleting the current workflow', async () => {
    hoisted.deleteWorkflowMutate.mockImplementation((_variables, options) => options.onSuccess());

    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
    await userEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', {name: 'Delete'}));

    expect(hoisted.deleteWorkflowMutate).toHaveBeenCalledWith({workflowUuid: 'workflow-1'}, expect.anything());
    expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor');
});

it('returns to the automations list after deleting the last workflow of the project', async () => {
    hoisted.deleteWorkflowMutate.mockImplementation((_variables, options) => options.onSuccess());
    hoisted.project.current = {...mockProject, workflowTemplates: mockProject.workflowTemplates.slice(0, 1)};

    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
    await userEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', {name: 'Delete'}));

    expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations');
});

it('keeps the delete project dialog open with a pending state until the deletion settles', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
    await userEvent.click(await screen.findByRole('button', {name: 'Confirm Project Deletion'}));

    expect(hoisted.deleteProjectMutation.mutate).toHaveBeenCalledWith({id: 'project-1'}, expect.anything());
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
});

it('disables the delete project confirmation while the deletion is pending', async () => {
    hoisted.deleteProjectMutation = {isPending: true, mutate: vi.fn()};

    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

    expect(await screen.findByRole('button', {name: 'Confirm Project Deletion'})).toBeDisabled();
});

it('closes the delete project dialog once the deletion settles', async () => {
    renderAutomationEditorHeader();

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
    await userEvent.click(await screen.findByRole('button', {name: 'Confirm Project Deletion'}));

    const [, mutationOptions] = hoisted.deleteProjectMutation.mutate.mock.calls[0];

    act(() => {
        mutationOptions.onSettled();
        mutationOptions.onSuccess();
    });

    await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations');
});

it('opens the project version history sheet with the restore handler for the current workflow', async () => {
    renderAutomationEditorHeader();

    expect(hoisted.useRestoreAutomationProjectVersion).toHaveBeenCalledWith({
        currentWorkflowUuid: 'workflow-1',
        projectId: 'project-1',
    });
    expect(hoisted.versionHistorySheetProps).toHaveBeenLastCalledWith(
        expect.objectContaining({
            onRestoreProjectVersion: hoisted.restoreProjectVersion,
            open: false,
            projectId: 'project-1',
            restoreProjectVersionPending: true,
        })
    );

    await userEvent.click(screen.getByLabelText('Settings'));
    await userEvent.click(screen.getByLabelText('Project tab'));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Project History'}));

    expect(hoisted.versionHistorySheetProps).toHaveBeenLastCalledWith(expect.objectContaining({open: true}));
});
