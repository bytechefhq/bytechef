import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/WorkflowExecutions';
import {mockScrollIntoView, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useWorkflowExecutionsMock} = vi.hoisted(() => ({
    useWorkflowExecutionsMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions', () => ({
    useWorkflowExecutions: useWorkflowExecutionsMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: () => ({data: undefined}),
    useGetIntegrationInstanceConfigurationsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/hooks/useOnEnvironmentChange', () => ({
    useOnEnvironmentChange: vi.fn(),
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => null,
}));

const connectedUserProjects = [
    {connectedUser: {externalId: 'user-7', id: 7}, projectId: 11},
    {connectedUser: {externalId: 'user-8', id: 8}, projectId: 12},
];

const renderWorkflowExecutions = (search: string) =>
    render(
        <MemoryRouter initialEntries={[`/embedded/executions?${search}`]}>
            <TooltipProvider>
                <WorkflowExecutions />
            </TooltipProvider>
        </MemoryRouter>
    );

const getConnectedUsersComboBox = () =>
    within(screen.getByText('Connected Users', {selector: 'label'}).parentElement!).getByRole('combobox');

const lastWorkflowExecutionsRequest = () => useWorkflowExecutionsMock.mock.calls.at(-1)?.[1];

describe('WorkflowExecutions', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockScrollIntoView();

        useWorkflowExecutionsMock.mockReturnValue({
            connectedUserProjects,
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: undefined,
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });
    });

    it('requests the executions of the connected user named in the URL', () => {
        renderWorkflowExecutions('automations=1&connectedUserId=7');

        expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: 7}));
        expect(getConnectedUsersComboBox()).toHaveTextContent('User user-7');
    });

    it('requests the executions of the picked connected user by their id, not their project', async () => {
        renderWorkflowExecutions('automations=1');

        await userEvent.click(getConnectedUsersComboBox());
        await userEvent.click(await screen.findByRole('option', {name: 'User user-8'}));

        await waitFor(() => {
            expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: 8}));
        });

        expect(lastWorkflowExecutionsRequest()).not.toHaveProperty('projectId');
    });

    it('clears the connected user filter when Any is picked', async () => {
        renderWorkflowExecutions('automations=1&connectedUserId=7');

        await userEvent.click(getConnectedUsersComboBox());
        await userEvent.click(await screen.findByRole('option', {name: 'Any'}));

        await waitFor(() => {
            expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: undefined}));
        });
    });
});
