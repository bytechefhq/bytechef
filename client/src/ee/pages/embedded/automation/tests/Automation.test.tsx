import {TooltipProvider} from '@/components/ui/tooltip';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Automation from '../Automation';

// ---------------------------------------------------------------------------
// Hoisted mocks
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => {
    return {
        getWorkflowQueryMock: vi.fn(),
        navigateMock: vi.fn(),
        publishMutationMock: vi.fn(),
        workflow: {id: 'workflow-1', label: 'My Workflow Template'} as Record<string, unknown>,
        workflowUuid: 'workflow-1',
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<Record<string, unknown>>()),
    LogLevel: {
        Debug: 'DEBUG',
        Error: 'ERROR',
        Info: 'INFO',
        Trace: 'TRACE',
        Warn: 'WARN',
    },
    PlatformType: {
        Automation: 'AUTOMATION',
        Embedded: 'EMBEDDED',
    },
    useAutomationProjectCategoriesQuery: () => ({data: {automationProjectCategories: []}}),
    useAutomationProjectTagsQuery: () => ({data: {automationProjectTags: []}}),
    useAutomationProjectVersionWorkflowChangesQuery: () => ({
        data: {
            automationProjectVersionWorkflowChanges: {
                baseVersion: 2,
                version: 3,
                workflows: [{changeCount: 1, changeType: 'MODIFIED', label: 'My Workflow Template', workflowUuid: 'w'}],
            },
        },
        refetch: vi.fn(),
    }),
    useAutomationProjectVersionsQuery: () => ({data: {automationProjectVersions: []}}),
    useAutomationProjectsQuery: () => ({
        data: {
            automationProjects: [
                {
                    categoryId: null,
                    description: null,
                    id: 'project-1',
                    lastPublishedVersion: 2,
                    name: 'My Project',
                    published: true,
                    tagIds: [],
                    version: 3,
                    workflowTemplates: [
                        {
                            components: [],
                            description: null,
                            label: 'My Workflow Template',
                            lastModifiedDate: '2026-01-01T10:00:00Z',
                            triggers: [],
                            workflowId: 'draft-workflow-1',
                            workflowUuid: 'workflow-1',
                        },
                        {
                            components: [],
                            description: null,
                            label: 'Second Workflow',
                            lastModifiedDate: '2026-01-02T10:00:00Z',
                            triggers: [],
                            workflowId: 'draft-workflow-2',
                            workflowUuid: 'workflow-2',
                        },
                    ],
                },
            ],
        },
        isLoading: false,
    }),
    useCreateAutomationProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAutomationProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAutomationProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDuplicateAutomationProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDuplicateAutomationProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    usePublishAutomationProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutationMock}),
    useUpdateAutomationProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAutomationProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAutomationProjectWorkflowPermissionExpressionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    WorkflowKeys: {
        workflow: (id: string) => ['automations', id],
        workflows: ['automations'],
    },
    useGetWorkflowQuery: hoisted.getWorkflowQueryMock,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            setWorkflow: vi.fn(),
            workflow: hoisted.workflow,
        }),
}));

vi.mock('@/pages/platform/workflow-editor/WorkflowEditorLayout', () => ({
    default: function WorkflowEditorLayoutMock() {
        const {platformType, useGetComponentDefinitionsQuery} = useWorkflowEditor();

        return (
            <div
                data-component-definitions-source={useGetComponentDefinitionsQuery!({}).data?.[0]?.name}
                data-platform-type={platformType}
                data-testid="workflow-editor-layout"
            />
        );
    },
}));

vi.mock('@/shared/components/LoadingIndicator', () => ({
    default: () => <div data-testid="loading-indicator" />,
}));

vi.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: ReactNode; to: string}) => <a href={to}>{children}</a>,
    useBlocker: () => undefined,
    useNavigate: () => hoisted.navigateMock,
    useParams: () => ({workflowUuid: hoisted.workflowUuid}),
    useSearchParams: () => [new URLSearchParams('')],
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useCreateConnectionMutation: () => ({}),
}));

vi.mock('@/shared/mutations/platform/workflowNodeParameters.mutations', () => ({
    useDeleteClusterElementParameterMutation: () => ({}),
    useDeleteWorkflowNodeParameterMutation: () => ({}),
    useUpdateClusterElementParameterMutation: () => ({}),
    useUpdateWorkflowNodeParameterMutation: () => ({}),
}));

vi.mock('@/shared/mutations/platform/workflows.mutations', () => ({
    default: () => ({}),
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useRun', () => ({
    useRun: () => ({runDisabled: false, runDisabledReasons: []}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useUpdateWorkflowMutation: () => ({}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: (id: number) => ['connectionTags', id]},
    useGetConnectionTagsQuery: () => ({data: []}),
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{name: 'automation'}]}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{name: 'embedded'}]}),
}));

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const renderAutomation = () =>
    render(
        <TooltipProvider>
            <Automation />
        </TooltipProvider>
    );

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

beforeEach(() => {
    hoisted.getWorkflowQueryMock.mockReset();
    hoisted.getWorkflowQueryMock.mockImplementation(() => ({data: hoisted.workflow, isLoading: false}));
    hoisted.navigateMock.mockReset();
    hoisted.publishMutationMock.mockReset();
    hoisted.workflow = {id: 'workflow-1', label: 'My Workflow Template'};
    hoisted.workflowUuid = 'workflow-1';

    useWorkflowEditorStore.setState({
        workflowIsRunning: false,
        workflowTestExecution: undefined,
        workflowTestNodeStates: {},
    });
});

describe('Automation', () => {
    it('renders without crashing and shows the workflow label in the header', () => {
        renderAutomation();

        expect(screen.getAllByText('My Workflow Template').length).toBeGreaterThan(0);
        expect(screen.getByTestId('workflow-editor-layout')).toBeInTheDocument();
    });

    it("loads the draft workflow through the template's workflow id, which publishing replaces", () => {
        renderAutomation();

        expect(hoisted.getWorkflowQueryMock).toHaveBeenCalledWith('draft-workflow-1', true);
        expect(hoisted.getWorkflowQueryMock).not.toHaveBeenCalledWith('workflow-1', true);
    });

    it('lists components through the embedded filter so the palette offers embedded triggers', () => {
        renderAutomation();

        expect(screen.getByTestId('workflow-editor-layout')).toHaveAttribute(
            'data-component-definitions-source',
            'embedded'
        );
    });

    it('searches components as the embedded platform so the search offers the same triggers as the palette', () => {
        renderAutomation();

        expect(screen.getByTestId('workflow-editor-layout')).toHaveAttribute('data-platform-type', 'EMBEDDED');
    });

    it('renders the left sidebar with the project select', () => {
        renderAutomation();

        expect(screen.getByRole('combobox', {name: 'Select project'})).toBeInTheDocument();
    });

    it('renders the left sidebar with the project workflows', () => {
        renderAutomation();

        expect(screen.getByText('My Project')).toBeInTheDocument();
        expect(screen.getByText('Second Workflow')).toBeInTheDocument();
    });

    it('navigates to another workflow when its sidebar item is clicked', () => {
        renderAutomation();

        fireEvent.click(screen.getByText('Second Workflow'));

        expect(hoisted.navigateMock).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor');
    });

    it('renders a Publish action that publishes the project', async () => {
        const user = userEvent.setup();

        renderAutomation();

        await user.click(screen.getByRole('button', {name: 'Publish'}));
        await user.click(screen.getByRole('button', {name: 'Publish button'}));

        expect(hoisted.publishMutationMock).toHaveBeenCalledWith({id: 'project-1'}, expect.anything());
    });

    it('shows the draft version badge for a published project', () => {
        renderAutomation();

        expect(screen.getByText('V3')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    it('clears the test output and the node states of the previous workflow once another workflow opens', () => {
        const {rerender} = renderAutomation();

        useWorkflowEditorStore.setState({
            workflowTestExecution: {job: {id: 'job-1'}} as WorkflowTestExecution,
            workflowTestNodeStates: {aiAgent_1: {status: 'COMPLETED'}},
        });

        hoisted.workflowUuid = 'workflow-2';

        rerender(
            <TooltipProvider>
                <Automation />
            </TooltipProvider>
        );

        expect(useWorkflowEditorStore.getState().workflowTestExecution).toBeUndefined();
        expect(useWorkflowEditorStore.getState().workflowTestNodeStates).toEqual({});
    });
});
