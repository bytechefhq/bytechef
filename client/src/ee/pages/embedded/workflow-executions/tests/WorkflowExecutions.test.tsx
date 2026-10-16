import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/WorkflowExecutions';
import {mockScrollIntoView, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    useGetConnectedUserQueryMock,
    useGetConnectedUsersQueryMock,
    useGetIntegrationInstanceConfigurationQueryMock,
    useGetIntegrationInstanceConfigurationsQueryMock,
    useGetIntegrationVersionWorkflowsQueryMock,
    useGetIntegrationVersionsQueryMock,
    useGetIntegrationsQueryMock,
    useWorkflowExecutionsMock,
} = vi.hoisted(() => ({
    useGetConnectedUserQueryMock: vi.fn(),
    useGetConnectedUsersQueryMock: vi.fn(),
    useGetIntegrationInstanceConfigurationQueryMock: vi.fn(),
    useGetIntegrationInstanceConfigurationsQueryMock: vi.fn(),
    useGetIntegrationVersionWorkflowsQueryMock: vi.fn(),
    useGetIntegrationVersionsQueryMock: vi.fn(),
    useGetIntegrationsQueryMock: vi.fn(),
    useWorkflowExecutionsMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions', () => ({
    useWorkflowExecutions: useWorkflowExecutionsMock,
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    useGetConnectedUserQuery: useGetConnectedUserQueryMock,
    useGetConnectedUsersQuery: useGetConnectedUsersQueryMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: useGetIntegrationInstanceConfigurationQueryMock,
    useGetIntegrationInstanceConfigurationsQuery: useGetIntegrationInstanceConfigurationsQueryMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrationVersions.queries', () => ({
    useGetIntegrationVersionsQuery: useGetIntegrationVersionsQueryMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: useGetIntegrationVersionWorkflowsQueryMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: useGetIntegrationsQueryMock,
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

const integrations = [
    {componentName: 'googleMail', id: 2, name: 'Gmail Sync', tags: []},
    {componentName: 'googleMail', id: 3, name: 'Gmail Archive', tags: []},
];

const integrationVersions: Record<number, Array<{status: string; version: number}>> = {
    2: [
        {status: 'PUBLISHED', version: 1},
        {status: 'PUBLISHED', version: 2},
        {status: 'DRAFT', version: 3},
    ],
    3: [{status: 'DRAFT', version: 1}],
};

const renderWorkflowExecutions = (search: string) =>
    render(
        <MemoryRouter initialEntries={[`/embedded/executions?${search}`]}>
            <TooltipProvider>
                <WorkflowExecutions />
            </TooltipProvider>
        </MemoryRouter>
    );

const getFilterComboBox = (label: string) =>
    within(screen.getByText(label, {selector: 'label'}).parentElement!).getByRole('combobox');

describe('WorkflowExecutions', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockScrollIntoView();

        useGetConnectedUserQueryMock.mockReturnValue({data: undefined});
        useGetConnectedUsersQueryMock.mockReturnValue({
            data: {content: [{externalId: 'user-7', id: 7}]},
        });
        useGetIntegrationInstanceConfigurationQueryMock.mockReturnValue({data: undefined});
        useGetIntegrationInstanceConfigurationsQueryMock.mockReturnValue({
            data: [{environmentId: 0, id: 4, integrationId: 2, integrationVersion: 1, name: 'Production'}],
        });
        useGetIntegrationVersionWorkflowsQueryMock.mockReturnValue({data: [{id: 'workflow-1', label: 'Send Email'}]});
        useGetIntegrationVersionsQueryMock.mockImplementation((integrationId: number, enabled: boolean) => ({
            data: enabled ? integrationVersions[integrationId] : undefined,
        }));
        useGetIntegrationsQueryMock.mockReturnValue({data: integrations});
        useWorkflowExecutionsMock.mockReturnValue({
            connectedUserProjects: [],
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: undefined,
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });
    });

    it('labels integrations with their name instead of their component name', async () => {
        renderWorkflowExecutions('integrationId=2');

        expect(getFilterComboBox('Integration')).toHaveTextContent('Gmail Sync');

        await userEvent.click(getFilterComboBox('Integration'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'Gmail Sync', 'Gmail Archive']);
    });

    it('labels the instance configurations of the picked integration with the integration name', async () => {
        renderWorkflowExecutions('integrationId=2');

        await userEvent.click(getFilterComboBox('Instance Configuration'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'Gmail Sync']);
    });

    it('lists the workflows of the last published integration version when only an integration is picked', async () => {
        renderWorkflowExecutions('integrationId=2');

        expect(useGetIntegrationVersionWorkflowsQueryMock).toHaveBeenLastCalledWith(2, 2, false, true);

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(await screen.findByRole('option', {name: 'Send Email'})).toBeInTheDocument();
    });

    it('lists the workflows of the version of the picked instance configuration', () => {
        useGetIntegrationInstanceConfigurationQueryMock.mockReturnValue({
            data: {id: 4, integrationId: 2, integrationVersion: 3},
        });

        renderWorkflowExecutions('integrationId=2&integrationInstanceConfigurationId=4');

        expect(useGetIntegrationVersionWorkflowsQueryMock).toHaveBeenLastCalledWith(2, 3, false, true);
    });

    it('does not list workflows when the picked integration has no published version', () => {
        renderWorkflowExecutions('integrationId=3');

        expect(useGetIntegrationVersionWorkflowsQueryMock).toHaveBeenLastCalledWith(3, undefined, false, false);
    });

    it('sends the picked connected user to the integration executions query', async () => {
        renderWorkflowExecutions('integrationId=2');

        expect(useGetConnectedUsersQueryMock).toHaveBeenLastCalledWith({environmentId: 0, integrationId: 2}, true);

        await userEvent.click(getFilterComboBox('Connected User'));

        await userEvent.click(await screen.findByRole('option', {name: 'User user-7'}));

        await waitFor(() => {
            expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(
                0,
                expect.objectContaining({connectedUserId: 7, integrationId: 2})
            );
        });
    });

    it('reads the connected user filter from the URL', async () => {
        useGetConnectedUserQueryMock.mockReturnValue({data: {externalId: 'user-7', id: 7}});

        renderWorkflowExecutions('connectedUserId=7');

        expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(0, expect.objectContaining({connectedUserId: 7}));
        expect(getFilterComboBox('Connected User')).toHaveTextContent('User user-7');

        await userEvent.click(getFilterComboBox('Connected User'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'User user-7']);
    });

    it('searches the server for connected users that are not on the first page', async () => {
        useGetConnectedUserQueryMock.mockImplementation((id: number, enabled: boolean) => ({
            data: enabled && id === 42 ? {externalId: 'user-42', id: 42} : undefined,
        }));
        useGetConnectedUsersQueryMock.mockImplementation(({search}: {search?: string}) => ({
            data: {
                content: search === 'user-42' ? [{externalId: 'user-42', id: 42}] : [{externalId: 'user-7', id: 7}],
            },
        }));

        renderWorkflowExecutions('integrationId=2');

        await userEvent.click(getFilterComboBox('Connected User'));

        await userEvent.type(screen.getByPlaceholderText('Search...'), 'user-42');

        await userEvent.click(await screen.findByRole('option', {name: 'User user-42'}));

        expect(useGetConnectedUsersQueryMock).toHaveBeenCalledWith(
            {environmentId: 0, integrationId: 2, search: 'user-42'},
            true
        );

        await waitFor(() => {
            expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(
                0,
                expect.objectContaining({connectedUserId: 42, integrationId: 2})
            );
        });

        expect(useGetConnectedUsersQueryMock).toHaveBeenLastCalledWith({environmentId: 0, integrationId: 2}, true);
        expect(getFilterComboBox('Connected User')).toHaveTextContent('User user-42');
    });

    it('shows the connected user picked in the URL when it is not on the first page', async () => {
        useGetConnectedUserQueryMock.mockImplementation((id: number, enabled: boolean) => ({
            data: enabled && id === 42 ? {externalId: 'user-42', id: 42} : undefined,
        }));
        useWorkflowExecutionsMock.mockReturnValue({
            connectedUserProjects: [],
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: {content: [{id: 1}], size: 20, totalElements: 1, totalPages: 1},
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });

        renderWorkflowExecutions('connectedUserId=42');

        expect(useGetConnectedUserQueryMock).toHaveBeenLastCalledWith(42, true);
        expect(getFilterComboBox('Connected User')).toHaveTextContent('User user-42');
        expect(screen.getAllByText(/user-42/).length).toBeGreaterThan(1);

        await userEvent.click(getFilterComboBox('Connected User'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'User user-42', 'User user-7']);
    });

    it('does not load a connected user when none is picked', () => {
        renderWorkflowExecutions('integrationId=2');

        expect(useGetConnectedUserQueryMock).toHaveBeenLastCalledWith(undefined, false);
    });
});
