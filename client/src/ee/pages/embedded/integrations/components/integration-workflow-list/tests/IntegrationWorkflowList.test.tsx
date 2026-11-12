import IntegrationWorkflowList from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowList';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    createIntegrationWorkflowMutate: vi.fn(),
    workflows: [{id: 'a', label: 'First workflow'}] as {id: string; label: string}[],
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationWorkflowCreated: vi.fn(), captureIntegrationWorkflowImported: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: () => ({mutate: hoisted.createIntegrationWorkflowMutate}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationWorkflowsQuery: () => ({data: hoisted.workflows, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: () => <div data-testid="workflow-dialog" />,
}));

vi.mock('@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowListItem', () => ({
    default: ({workflow}: {workflow: {label: string}}) => <li>{workflow.label}</li>,
}));

const integration = {componentName: 'asana', id: 7, multipleInstances: false, name: 'Asana'} as Integration;

const renderList = () =>
    render(
        <IntegrationWorkflowList componentDefinitions={[]} integration={integration} taskDispatcherDefinitions={[]} />
    );

describe('IntegrationWorkflowList', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.workflows = [{id: 'a', label: 'First workflow'}];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should not repeat a Workflows heading under the Workflows tab', () => {
        renderList();

        expect(screen.getByText('First workflow')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Workflows'})).not.toBeInTheDocument();
    });

    describe('empty state', () => {
        beforeEach(() => {
            hoisted.workflows = [];
        });

        it('should open the workflow dialog from Create Workflow', async () => {
            renderList();

            expect(screen.queryByTestId('workflow-dialog')).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Create Workflow'}));

            expect(screen.getByTestId('workflow-dialog')).toBeInTheDocument();
        });

        it('should offer Import Workflow next to Create Workflow', async () => {
            renderList();

            await userEvent.click(screen.getByRole('button', {name: 'More Workflow Creation Actions'}));

            expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toBeInTheDocument();
        });

        it('should import the picked workflow file into the integration', async () => {
            renderList();

            const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
            const clickSpy = vi.spyOn(fileInput, 'click');

            await userEvent.click(screen.getByRole('button', {name: 'More Workflow Creation Actions'}));
            await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

            expect(clickSpy).toHaveBeenCalled();

            await userEvent.upload(fileInput, new File(['{"label": "Imported"}'], 'workflow.json'));

            await waitFor(
                () =>
                    expect(hoisted.createIntegrationWorkflowMutate).toHaveBeenCalledWith({
                        id: 7,
                        workflow: {definition: '{"label": "Imported"}'},
                    }),
                {interval: 5, timeout: 5000}
            );
        });
    });
});
