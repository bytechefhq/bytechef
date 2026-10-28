import IntegrationWorkflowList from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowList';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationWorkflowCreated: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationWorkflowsQuery: () => ({data: [{id: 'a', label: 'First workflow'}], isLoading: false}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowListItem', () => ({
    default: ({workflow}: {workflow: {label: string}}) => <li>{workflow.label}</li>,
}));

const integration = {componentName: 'asana', id: 7, multipleInstances: false, name: 'Asana'} as Integration;

describe('IntegrationWorkflowList', () => {
    it('should not repeat a Workflows heading under the Workflows tab', () => {
        render(
            <IntegrationWorkflowList
                componentDefinitions={[]}
                integration={integration}
                taskDispatcherDefinitions={[]}
            />
        );

        expect(screen.getByText('First workflow')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Workflows'})).not.toBeInTheDocument();
    });
});
