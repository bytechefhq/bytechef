import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationWorkflowListItem from '../AutomationWorkflowProjectConfigurationWorkflowListItem';

const mocks = vi.hoisted(() => ({
    enableWorkflowMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    workflowTriggerAndComponentsRowProps: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutate: mocks.enableWorkflowMutate,
    }),
}));

vi.mock('../AutomationWorkflowProjectConfigurationWorkflowDialog', () => ({
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

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type WorkflowType = AutomationWorkflowProjectConfigurationType['workflows'][number];
type WorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

const workflow: WorkflowType = {
    enabled: true,
    label: 'Mailer',
    lastExecutionDate: null,
    providedConnections: null,
    providedInputs: null,
    workflowId: 'pinned-wf-1',
    workflowUuid: 'wf-1',
};

const configuration: AutomationWorkflowProjectConfigurationType = {
    automationHubVisible: true,
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
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

const project: AutomationWorkflowProjectType = {
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
    version: 3,
    workflowTemplates: [workflowTemplate],
};

const renderItem = (
    overrides: {workflow?: Partial<WorkflowType>; workflowTemplate?: WorkflowTemplateType | null} = {}
) =>
    render(
        <TooltipProvider>
            <ul>
                <AutomationWorkflowProjectConfigurationWorkflowListItem
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

describe('AutomationWorkflowProjectConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        windowResizeObserver();

        useAutomationWorkflowExecutionsSheetStore.setState({
            automationWorkflowExecutionsSheetOpen: false,
            environmentId: undefined,
            projectName: undefined,
            workflow: undefined,
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
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

        const state = useAutomationWorkflowExecutionsSheetStore.getState();

        expect(state.automationWorkflowExecutionsSheetOpen).toBe(true);
        expect(state.environmentId).toBe(2);
        expect(state.workflow).toEqual({label: 'Mailer', workflowId: 'pinned-wf-1', workflowUuid: 'wf-1'});
    });

    it('opens the executions from the actions menu', async () => {
        const user = userEvent.setup();

        renderItem();

        screen.getByRole('button', {name: 'Workflow Actions'}).focus();

        await user.keyboard('{ArrowDown}');
        await user.click(await screen.findByRole('menuitem', {name: 'View Executions'}));

        const state = useAutomationWorkflowExecutionsSheetStore.getState();

        expect(state.automationWorkflowExecutionsSheetOpen).toBe(true);
        expect(state.projectName).toBe('Mail Project');
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

    it('mutes the label of a disabled workflow', () => {
        renderItem({workflow: {enabled: false}});

        expect(screen.getByText('Mailer').parentElement).toHaveClass('text-muted-foreground');
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

    it('says when the workflow has not executed yet', () => {
        renderItem();

        expect(screen.getByText('No executions')).toBeInTheDocument();
    });
});
