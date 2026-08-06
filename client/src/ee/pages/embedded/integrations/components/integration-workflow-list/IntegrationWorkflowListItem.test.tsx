import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationWorkflowListItem from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowListItem';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

vi.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: React.ReactNode; to: string}) => <a href={to}>{children}</a>,
    useSearchParams: () => [new URLSearchParams(''), vi.fn()],
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useDeleteWorkflowMutation: () => ({mutate: vi.fn()}),
    useUpdateWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
    WorkflowKeys: {workflow: (id: string) => ['workflow', id]},
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    IntegrationWorkflowKeys: {integrationWorkflows: (id: number) => ['integrationWorkflows', id]},
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integrations: ['integrations']},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useIntegrationWorkflowsByIntegrationIdQuery: () => ({data: null}),
    useUpdateIntegrationWorkflowPermissionExpressionMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    WorkflowTestConfigurationKeys: {workflowTestConfiguration: (id: string) => ['workflowTestConfiguration', id]},
}));

vi.mock('@/shared/components/WorkflowComponentsList', () => ({
    default: ({filteredComponentNames}: {filteredComponentNames: string[]}) => (
        <div data-testid="workflow-components-list">{filteredComponentNames.join(',')}</div>
    ),
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: () => <div data-testid="workflow-dialog" />,
}));

vi.mock('@/shared/components/DeleteWorkflowAlertDialog', () => ({
    default: () => <div data-testid="delete-workflow-dialog" />,
}));

const integration = {codeWorkflow: false, id: 1, name: 'Mailchimp'} as Integration;

const componentDefinitions = {
    gmail: {icon: '<svg />', name: 'gmail', title: 'Gmail', version: 1},
    manual: {icon: '<svg />', name: 'manual', title: 'Manual', version: 1},
    slack: {icon: '<svg />', name: 'slack', title: 'Slack', version: 1},
};

const renderItem = (workflow: Partial<Workflow>) =>
    render(
        <TooltipProvider>
            <IntegrationWorkflowListItem
                filteredComponentNames={workflow.workflowTaskComponentNames ?? []}
                integration={integration}
                workflow={
                    {
                        integrationWorkflowId: 10,
                        label: 'My Workflow',
                        ...workflow,
                    } as Workflow
                }
                workflowComponentDefinitions={componentDefinitions}
                workflowTaskDispatcherDefinitions={{}}
            />
        </TooltipProvider>
    );

describe('IntegrationWorkflowListItem', () => {
    it('shows the trigger as its own chip, matching the projects list', () => {
        renderItem({
            triggers: [{label: 'manual', name: 'trigger_1', type: 'manual/v1/manual'}],
            workflowTaskComponentNames: ['manual', 'slack', 'gmail'],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getByText('manual')).toBeInTheDocument();
    });

    it('hands the shared components list only the task components, not the trigger', () => {
        renderItem({
            triggers: [{label: 'manual', name: 'trigger_1', type: 'manual/v1/manual'}],
            workflowTaskComponentNames: ['manual', 'slack', 'gmail'],
            workflowTriggerComponentNames: ['manual'],
        });

        // Exact text, not toHaveTextContent: that matches substrings, so an unsliced "manual,slack,gmail" would
        // pass while the trigger got drawn twice — once as its chip, once as a task icon.
        expect(screen.getByTestId('workflow-components-list').textContent).toBe('slack,gmail');
    });

    it('renders no trigger chip for a workflow that declares none', () => {
        renderItem({workflowTaskComponentNames: ['slack']});

        expect(screen.getByTestId('workflow-components-list').textContent).toBe('slack');
        expect(screen.queryByText('Unknown Trigger')).not.toBeInTheDocument();
    });

    it('falls back to the trigger component title when the trigger carries no label', () => {
        renderItem({
            triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
            workflowTaskComponentNames: ['manual', 'slack'],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getByText('Manual')).toBeInTheDocument();
    });
});
