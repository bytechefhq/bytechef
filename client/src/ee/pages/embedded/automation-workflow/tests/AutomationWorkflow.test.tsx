import {TooltipProvider} from '@/components/ui/tooltip';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflow from '../AutomationWorkflow';

// ---------------------------------------------------------------------------
// Hoisted mocks
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => {
    return {
        getWorkflowQueryMock: vi.fn(),
        navigateMock: vi.fn(),
        publishMutationMock: vi.fn(),
        workflow: {id: 'workflow-1', label: 'My Workflow Template'} as Record<string, unknown>,
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
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
    useAutomationWorkflowProjectCategoriesQuery: () => ({data: {automationWorkflowProjectCategories: []}}),
    useAutomationWorkflowProjectTagsQuery: () => ({data: {automationWorkflowProjectTags: []}}),
    useAutomationWorkflowProjectVersionsQuery: () => ({data: {automationWorkflowProjectVersions: []}}),
    useAutomationWorkflowProjectsQuery: () => ({
        data: {
            automationWorkflowProjects: [
                {
                    categoryId: null,
                    description: null,
                    id: 'project-1',
                    lastPublishedVersion: 2,
                    name: 'My Project',
                    published: true,
                    tagIds: [],
                    version: 1,
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
    useCreateAutomationWorkflowProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAutomationWorkflowProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDuplicateAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDuplicateAutomationWorkflowProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    usePublishAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutationMock}),
    useUpdateAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAutomationWorkflowProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    WorkflowKeys: {
        workflow: (id: string) => ['automationWorkflows', id],
        workflows: ['automationWorkflows'],
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
    useBlocker: () => undefined,
    useNavigate: () => hoisted.navigateMock,
    useParams: () => ({workflowUuid: 'workflow-1'}),
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
    useRun: () => ({runDisabled: false}),
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

const renderAutomationWorkflow = () =>
    render(
        <TooltipProvider>
            <AutomationWorkflow />
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
});

describe('AutomationWorkflow', () => {
    it('renders without crashing and shows the workflow label in the header', () => {
        renderAutomationWorkflow();

        expect(screen.getAllByText('My Workflow Template').length).toBeGreaterThan(0);
        expect(screen.getByTestId('workflow-editor-layout')).toBeInTheDocument();
    });

    it("loads the draft workflow through the template's workflow id, which publishing replaces", () => {
        renderAutomationWorkflow();

        expect(hoisted.getWorkflowQueryMock).toHaveBeenCalledWith('draft-workflow-1', true);
        expect(hoisted.getWorkflowQueryMock).not.toHaveBeenCalledWith('workflow-1', true);
    });

    it('lists components through the embedded filter so the palette offers embedded triggers', () => {
        renderAutomationWorkflow();

        expect(screen.getByTestId('workflow-editor-layout')).toHaveAttribute(
            'data-component-definitions-source',
            'embedded'
        );
    });

    it('searches components as the embedded platform so the search offers the same triggers as the palette', () => {
        renderAutomationWorkflow();

        expect(screen.getByTestId('workflow-editor-layout')).toHaveAttribute('data-platform-type', 'EMBEDDED');
    });

    it('renders the left sidebar with the project select', () => {
        renderAutomationWorkflow();

        expect(screen.getByRole('combobox', {name: 'Select project'})).toBeInTheDocument();
    });

    it('renders the left sidebar with the project workflows', () => {
        renderAutomationWorkflow();

        expect(screen.getByText('My Project')).toBeInTheDocument();
        expect(screen.getByText('Second Workflow')).toBeInTheDocument();
    });

    it('navigates to another workflow when its sidebar item is clicked', () => {
        renderAutomationWorkflow();

        fireEvent.click(screen.getByText('Second Workflow'));

        expect(hoisted.navigateMock).toHaveBeenCalledWith('/embedded/automation-workflows/workflow-2/editor');
    });

    it('renders a Publish action that publishes the project', async () => {
        const user = userEvent.setup();

        renderAutomationWorkflow();

        await user.click(screen.getByRole('button', {name: 'Publish'}));
        await user.click(screen.getByRole('button', {name: 'Publish button'}));

        expect(hoisted.publishMutationMock).toHaveBeenCalledWith({id: 'project-1'}, expect.anything());
    });

    it('shows the published version badge for the project', () => {
        renderAutomationWorkflow();

        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    });
});
