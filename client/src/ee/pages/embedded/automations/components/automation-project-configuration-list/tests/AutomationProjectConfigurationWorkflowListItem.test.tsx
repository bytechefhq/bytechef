import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import useAutomationExecutionsSheetStore from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
import {AutomationProjectConfigurationsQuery, AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectConfigurationWorkflowListItem from '../AutomationProjectConfigurationWorkflowListItem';

const mocks = vi.hoisted(() => ({
    deniedScopes: new Set<string>(),
    enableWorkflowMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    workflowTriggerAndComponentsRowProps: vi.fn(),
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: (scope: string) => !mocks.deniedScopes.has(scope),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEnableAutomationProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutate: mocks.enableWorkflowMutate,
    }),
}));

vi.mock('../AutomationProjectConfigurationWorkflowDialog', () => ({
    default: ({onClose}: {onClose: () => void}) => (
        <div>
            <span>Provided Values Dialog</span>

            <button onClick={onClose} type="button">
                Close Dialog
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: (props: unknown) => {
        mocks.workflowTriggerAndComponentsRowProps(props);

        return null;
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type WorkflowType = AutomationProjectConfigurationType['workflows'][number];
type WorkflowTemplateType = AutomationProjectType['workflowTemplates'][number];

const workflow: WorkflowType = {
    awaitedInputLabels: [],
    enabled: true,
    label: 'Mailer',
    lastExecutionDate: null,
    providedConnections: null,
    providedInputs: null,
    workflowId: 'pinned-wf-1',
    workflowUuid: 'wf-1',
};

const configuration: AutomationProjectConfigurationType = {
    automationHubVisible: true,
    connectedUserCount: 0,
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    name: 'STAGING',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 2,
    workflows: [workflow],
};

const workflowTemplate: WorkflowTemplateType = {
    components: [
        {icon: null, name: 'gmail', title: 'Gmail'},
        {icon: '<svg/>', name: 'slack', title: null},
    ],
    description: null,
    label: 'Mailer',
    lastModifiedDate: null,
    permissionExpression: null,
    triggers: [{icon: '<svg/>', name: 'gmail', title: null}],
    workflowId: 'draft-wf-1',
    workflowUuid: 'wf-1',
};

const project: AutomationProjectType = {
    categoryId: null,
    codeWorkflowDraftPublishable: null,
    codeWorkflowLanguage: null,
    codeWorkflowProject: false,
    configurationCount: 1,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: 2,
    name: 'Mail Project',
    permissionExpression: null,
    published: true,
    tagIds: [],
    unpublishedChanges: false,
    version: 3,
    workflowTemplates: [workflowTemplate],
};

const renderItem = (
    overrides: {workflow?: Partial<WorkflowType>; workflowTemplate?: WorkflowTemplateType | null} = {}
) =>
    render(
        <TooltipProvider>
            <ul>
                <AutomationProjectConfigurationWorkflowListItem
                    configuration={configuration}
                    project={project}
                    workflow={{...workflow, ...overrides.workflow}}
                    workflowTemplate={
                        overrides.workflowTemplate === null
                            ? undefined
                            : (overrides.workflowTemplate ?? workflowTemplate)
                    }
                />
            </ul>
        </TooltipProvider>
    );

describe('AutomationProjectConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        mocks.deniedScopes.clear();

        windowResizeObserver();

        useAutomationExecutionsSheetStore.setState({
            automationExecutionsSheetOpen: false,
            environmentId: undefined,
            projectName: undefined,
            workflow: undefined,
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows the workflow description in a tooltip on the workflow name', async () => {
        renderItem({workflowTemplate: {...workflowTemplate, description: 'Sends the weekly digest'}});

        await userEvent.hover(screen.getByText('Mailer'));

        await waitFor(() =>
            expect(document.querySelector('[data-slot="tooltip-content"]')).toHaveTextContent('Sends the weekly digest')
        );
    });

    it('does not show a tooltip on the workflow name when the workflow has no description', () => {
        renderItem();

        expect(screen.getByText('Mailer')).not.toHaveAttribute('data-slot', 'tooltip-trigger');
    });

    it('renders the workflow row as a native button', () => {
        renderItem();

        const workflowButton = screen.getByRole('button', {name: 'Mailer'});

        expect(workflowButton.tagName).toBe('BUTTON');
        expect(workflowButton).toHaveAttribute('type', 'button');
    });

    it('opens the executions from the keyboard', async () => {
        const user = userEvent.setup();

        renderItem();

        screen.getByRole('button', {name: 'Mailer'}).focus();

        await user.keyboard('{Enter}');

        const state = useAutomationExecutionsSheetStore.getState();

        expect(state.automationExecutionsSheetOpen).toBe(true);
        expect(state.environmentId).toBe(2);
        expect(state.workflow).toEqual({label: 'Mailer', workflowId: 'pinned-wf-1', workflowUuid: 'wf-1'});
    });

    it('does not offer the executions in the actions menu', async () => {
        const user = userEvent.setup();

        renderItem();

        screen.getByRole('button', {name: 'Workflow Actions'}).focus();

        await user.keyboard('{ArrowDown}');

        expect(await screen.findByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'View Executions'})).not.toBeInTheDocument();
    });

    it('opens and closes the provided values dialog from the actions menu', async () => {
        const user = userEvent.setup();

        renderItem();

        expect(screen.queryByText('Provided Values Dialog')).not.toBeInTheDocument();

        screen.getByRole('button', {name: 'Workflow Actions'}).focus();

        await user.keyboard('{ArrowDown}');
        await user.click(await screen.findByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByText('Provided Values Dialog')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Close Dialog'}));

        expect(screen.queryByText('Provided Values Dialog')).not.toBeInTheDocument();
    });

    it("passes the template's deduplicated components and trigger labels to the components row", () => {
        renderItem();

        expect(mocks.workflowTriggerAndComponentsRowProps).toHaveBeenCalledWith(
            expect.objectContaining({
                filteredComponentNames: ['gmail', 'slack'],
                workflow: {
                    triggers: [{label: 'gmail'}],
                    workflowTriggerComponentNames: ['gmail'],
                },
                workflowComponentDefinitions: {
                    gmail: {icon: undefined, name: 'gmail', title: 'Gmail'},
                    slack: {icon: '<svg/>', name: 'slack', title: undefined},
                },
            })
        );
    });

    it('labels the trigger with its title when it has one', () => {
        renderItem({
            workflowTemplate: {...workflowTemplate, triggers: [{icon: null, name: 'gmail', title: 'New Email'}]},
        });

        expect(mocks.workflowTriggerAndComponentsRowProps).toHaveBeenCalledWith(
            expect.objectContaining({
                workflow: {triggers: [{label: 'New Email'}], workflowTriggerComponentNames: ['gmail']},
            })
        );
    });

    it('omits the components row when the latest draft no longer has the workflow', () => {
        renderItem({workflowTemplate: null});

        expect(mocks.workflowTriggerAndComponentsRowProps).not.toHaveBeenCalled();
    });

    it('falls back to the workflow uuid when the workflow has no label', () => {
        renderItem({workflow: {label: null}});

        expect(screen.getByRole('button', {name: 'wf-1'})).toBeInTheDocument();
    });

    it('does not offer opening the workflow in its project by default', () => {
        renderItem();

        expect(screen.queryByRole('button', {name: 'Open in project'})).not.toBeInTheDocument();
    });

    it('mutes the label of a disabled workflow', () => {
        renderItem({workflow: {enabled: false}});

        expect(screen.getByRole('button', {name: 'Mailer'}).parentElement).toHaveClass('text-muted-foreground');
    });

    it('shows when the workflow last executed', () => {
        const lastExecutionDate = new Date('2026-10-01T10:15:00Z');

        renderItem({workflow: {lastExecutionDate: lastExecutionDate.toISOString()}});

        expect(
            screen.getByText(
                `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
            )
        ).toBeInTheDocument();
        expect(screen.queryByText('No executions')).not.toBeInTheDocument();
    });

    it('marks an enabled workflow that awaits a required internal-only input', async () => {
        renderItem({workflow: {awaitedInputLabels: ['Contact Page URL']}});

        await userEvent.hover(screen.getByText('Awaits configuration'));

        expect(
            await screen.findByRole('tooltip', {
                name: 'Does not run until Contact Page URL is set. Edit the workflow to set it.',
            })
        ).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable Mailer'})).toBeChecked();
    });

    it('names every required internal-only input an enabled workflow awaits', async () => {
        renderItem({workflow: {awaitedInputLabels: ['Contact Page URL', 'Region', 'API Key']}});

        await userEvent.hover(screen.getByText('Awaits configuration'));

        expect(
            await screen.findByRole('tooltip', {
                name: 'Does not run until Contact Page URL, Region, and API Key are set. Edit the workflow to set them.',
            })
        ).toBeInTheDocument();
    });

    it('does not mark a workflow that awaits nothing or is switched off', () => {
        const {unmount} = renderItem();

        expect(screen.queryByText('Awaits configuration')).not.toBeInTheDocument();

        unmount();

        renderItem({workflow: {awaitedInputLabels: ['Contact Page URL'], enabled: false}});

        expect(screen.queryByText('Awaits configuration')).not.toBeInTheDocument();
    });

    it('says when the workflow has not executed yet', () => {
        renderItem();

        expect(screen.getByText('No executions')).toBeInTheDocument();
    });

    it('disables the toggle and hides the workflow actions without the edit scope', () => {
        mocks.deniedScopes = new Set(['INTEGRATION_INSTANCE_CONFIGURATION_EDIT']);

        renderItem();

        expect(screen.getByRole('switch')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Workflow Actions'})).not.toBeInTheDocument();
    });
});
