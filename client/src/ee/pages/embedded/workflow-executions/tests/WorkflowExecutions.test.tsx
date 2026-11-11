import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/WorkflowExecutions';
import useEmbeddedWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import useAutomationExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {act, mockScrollIntoView, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {MemoryRouter, RouterProvider, createMemoryRouter, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    useGetConnectedUserQueryMock,
    useGetConnectedUsersQueryMock,
    useGetIntegrationInstanceConfigurationQueryMock,
    useGetIntegrationInstanceConfigurationsQueryMock,
    useGetIntegrationVersionWorkflowsQueryMock,
    useGetIntegrationVersionsQueryMock,
    useGetIntegrationsQueryMock,
    useOnEnvironmentChangeMock,
    useWorkflowExecutionsMock,
} = vi.hoisted(() => ({
    useGetConnectedUserQueryMock: vi.fn(),
    useGetConnectedUsersQueryMock: vi.fn(),
    useGetIntegrationInstanceConfigurationQueryMock: vi.fn(),
    useGetIntegrationInstanceConfigurationsQueryMock: vi.fn(),
    useGetIntegrationVersionWorkflowsQueryMock: vi.fn(),
    useGetIntegrationVersionsQueryMock: vi.fn(),
    useGetIntegrationsQueryMock: vi.fn(),
    useOnEnvironmentChangeMock: vi.fn(),
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
    useOnEnvironmentChange: useOnEnvironmentChangeMock,
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/AnyWorkflowExecutionsTable', () => ({
    default: ({
        data,
    }: {
        data: Array<{automationExecution?: {id: number}; integrationWorkflowExecution?: {id: number}}>;
    }) => (
        <ul aria-label="Any workflow executions">
            {data.map((row) =>
                row.automationExecution ? (
                    <li key={`automation-${row.automationExecution.id}`}>automation {row.automationExecution.id}</li>
                ) : (
                    <li key={`integration-${row.integrationWorkflowExecution?.id}`}>
                        integration {row.integrationWorkflowExecution?.id}
                    </li>
                )
            )}
        </ul>
    ),
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

const connectedUserProjects = [
    {
        connectedUser: {externalId: 'user-7', id: 7},
        connectedUserProjectWorkflows: [
            {workflow: {id: 'copy-workflow', label: 'Copied workflow'}, workflowUuid: 'copy-uuid'},
            {workflow: {id: 'reference-uuid', label: 'Referenced workflow'}, workflowUuid: 'reference-uuid'},
        ],
        projectId: 11,
    },
    {
        connectedUser: {externalId: 'user-8', id: 8},
        connectedUserProjectWorkflows: [
            {workflow: {id: 'reference-uuid', label: 'Referenced workflow'}, workflowUuid: 'reference-uuid'},
        ],
        projectId: 12,
    },
];

let currentSearch = '';

const LocationProbe = () => {
    currentSearch = useLocation().search;

    return null;
};

const renderWorkflowExecutions = (search: string) =>
    render(
        <MemoryRouter initialEntries={[`/embedded/workflow-executions?${search}`]}>
            <TooltipProvider>
                <WorkflowExecutions />

                <LocationProbe />
            </TooltipProvider>
        </MemoryRouter>
    );

const getFilterComboBox = (label: string) =>
    within(screen.getByText(label, {selector: 'label'}).parentElement!).getByRole('combobox');

const pickFilterOption = async (label: string, optionName: string) => {
    await userEvent.click(getFilterComboBox(label));
    await userEvent.click(await screen.findByRole('option', {name: optionName}));
};

const expectConnectedUserKeptInTheUrl = async () => {
    await waitFor(() => {
        expect(new URLSearchParams(currentSearch).get('connectedUserId')).toBe('7');
    });
};

const getConnectedUsersComboBox = () => getFilterComboBox('Connected Users');

const lastWorkflowExecutionsRequest = () => useWorkflowExecutionsMock.mock.calls.at(-1)?.[1];

describe('WorkflowExecutions', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockScrollIntoView();

        useAutomationExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: false,
        });
        useEmbeddedWorkflowExecutionSheetStore.setState({workflowExecutionId: 0, workflowExecutionSheetOpen: false});

        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
        Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();

        useOnEnvironmentChangeMock.mockImplementation(() => undefined);

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
            connectedUserProjects,
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: undefined,
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });
    });

    it('opens the embedded execution sheet for the id query param', () => {
        renderWorkflowExecutions('id=42');

        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(42);
        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
        expect(useAutomationExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });

    it('opens the automation execution sheet when the source is automations', () => {
        renderWorkflowExecutions('source=AUTOMATIONS&id=42');

        expect(useAutomationExecutionSheetStore.getState().workflowExecutionId).toBe(42);
        expect(useAutomationExecutionSheetStore.getState().workflowExecutionKind).toBe('JOB');
        expect(useAutomationExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });

    it('switches to the automation execution sheet when the query params change without remounting', async () => {
        const router = createMemoryRouter(
            [
                {
                    element: (
                        <TooltipProvider>
                            <WorkflowExecutions />
                        </TooltipProvider>
                    ),
                    path: '/embedded/workflow-executions',
                },
            ],
            {initialEntries: ['/embedded/workflow-executions?id=42']}
        );

        render(<RouterProvider router={router} />);

        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);

        await act(async () => {
            await router.navigate('/embedded/workflow-executions?source=AUTOMATIONS&id=43');
        });

        expect(useAutomationExecutionSheetStore.getState().workflowExecutionId).toBe(43);
        expect(useAutomationExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });

    it('lists the executions of every source by default', () => {
        useWorkflowExecutionsMock.mockReturnValue({
            connectedUserProjects: [],
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: {
                content: [{integrationWorkflowExecution: {id: 1}, source: 'INTEGRATION'}],
                size: 20,
                totalElements: 1,
                totalPages: 1,
            },
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });

        renderWorkflowExecutions('');

        expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith('ANY', expect.anything());
        expect(getFilterComboBox('Source')).toHaveTextContent('Any');
        expect(screen.getByText('Connected User', {selector: 'label'})).toBeInTheDocument();
        expect(screen.queryByText('Integration', {selector: 'label'})).not.toBeInTheDocument();
        expect(screen.queryByText('Instance Configuration', {selector: 'label'})).not.toBeInTheDocument();
        expect(screen.queryByText('Workflow', {selector: 'label'})).not.toBeInTheDocument();
    });

    it('hides the filter sidebar and guides the first run while nothing has been executed', () => {
        renderWorkflowExecutions('');

        expect(screen.getByRole('heading', {name: 'No workflow executions yet'})).toBeInTheDocument();
        expect(screen.getByRole('list', {name: 'Getting started'})).toBeInTheDocument();
        expect(screen.queryByText('Source', {selector: 'label'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Refresh workflow executions'})).toBeInTheDocument();
    });

    it('keeps the filter sidebar when a filter matches no executions', () => {
        renderWorkflowExecutions('status=FAILED');

        expect(screen.getByText('No Matching Executions')).toBeInTheDocument();
        expect(screen.getByText('Source', {selector: 'label'})).toBeInTheDocument();
    });

    it.each([
        ['source=AUTOMATIONS', 'AUTOMATIONS'],
        ['source=INTEGRATIONS', 'INTEGRATIONS'],
        ['source=unknown', 'ANY'],
    ])('reads the source of %s from the URL', (search, source) => {
        renderWorkflowExecutions(search);

        expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(source, expect.anything());
    });

    it('lists automation and integration runs together in one table for the any source', () => {
        useWorkflowExecutionsMock.mockReturnValue({
            connectedUserProjects: [],
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: {
                content: [
                    {automationExecution: {id: 3}, source: 'AUTOMATION'},
                    {integrationWorkflowExecution: {id: 1}, source: 'INTEGRATION'},
                ],
                size: 20,
                totalElements: 2,
                totalPages: 1,
            },
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });

        renderWorkflowExecutions('');

        const rows = within(screen.getByRole('list', {name: 'Any workflow executions'})).getAllByRole('listitem');

        expect(rows.map((row) => row.textContent)).toEqual(['automation 3', 'integration 1']);
    });

    it('switches to the any source from the source filter', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        await userEvent.click(getFilterComboBox('Source'));
        await userEvent.click(await screen.findByRole('option', {name: 'Any'}));

        await waitFor(() => {
            expect(new URLSearchParams(currentSearch).get('source')).toBe('ANY');
        });

        expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith('ANY', expect.anything());
        expect(screen.queryByText('Integration', {selector: 'label'})).not.toBeInTheDocument();
    });

    it('ignores a non-numeric id query param', () => {
        renderWorkflowExecutions('id=abc');

        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
        expect(useAutomationExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });

    it('requests the executions of the connected user named in the URL', () => {
        renderWorkflowExecutions('source=AUTOMATIONS&connectedUserId=7');

        expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: 7}));
        expect(getConnectedUsersComboBox()).toHaveTextContent('User user-7');
    });

    it('requests the executions of the picked connected user by their id, not their project', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS');

        await userEvent.click(getConnectedUsersComboBox());
        await userEvent.click(await screen.findByRole('option', {name: 'User user-8'}));

        await waitFor(() => {
            expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: 8}));
        });

        expect(lastWorkflowExecutionsRequest()).not.toHaveProperty('projectId');
    });

    it('clears the connected user filter when Any is picked', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS&connectedUserId=7');

        await userEvent.click(getConnectedUsersComboBox());
        await userEvent.click(await screen.findByRole('option', {name: 'Any'}));

        await waitFor(() => {
            expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: undefined}));
        });
    });

    it('keeps the connected user when the status filter changes', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS&connectedUserId=7');

        await pickFilterOption('Status', 'COMPLETED');

        await expectConnectedUserKeptInTheUrl();

        expect(new URLSearchParams(currentSearch).get('status')).toBe('COMPLETED');
    });

    it('keeps the connected user when the integration filters change', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS&connectedUserId=7');

        await pickFilterOption('Integration', 'Gmail Sync');
        await expectConnectedUserKeptInTheUrl();

        await pickFilterOption('Instance Configuration', 'Gmail Sync');
        await expectConnectedUserKeptInTheUrl();

        await pickFilterOption('Workflow', 'Send Email');
        await expectConnectedUserKeptInTheUrl();

        expect(new URLSearchParams(currentSearch).get('workflowId')).toBe('workflow-1');
    });

    it('keeps the connected user when the source changes', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS&connectedUserId=7');

        await userEvent.click(getFilterComboBox('Source'));
        await userEvent.click(await screen.findByRole('option', {name: 'Integrations'}));

        await waitFor(() => {
            expect(new URLSearchParams(currentSearch).get('source')).toBe('INTEGRATIONS');
        });

        expect(new URLSearchParams(currentSearch).get('connectedUserId')).toBe('7');
    });

    it.each([
        ['automations', 'AUTOMATIONS'],
        ['integrations', 'INTEGRATIONS'],
    ])('clears the connected user of the %s when the environment changes', async (_type, source) => {
        let environmentChangeCallback: (() => void) | undefined;

        useOnEnvironmentChangeMock.mockImplementation((callback: () => void) => {
            environmentChangeCallback = callback;
        });

        renderWorkflowExecutions(`source=${source}&connectedUserId=7`);

        act(() => environmentChangeCallback?.());

        await waitFor(() => {
            expect(new URLSearchParams(currentSearch).get('connectedUserId')).toBe('');
        });

        expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: undefined}));
    });

    it('lists the connected users workflows once each in the automations workflow filter', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS');

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(await screen.findByRole('option', {name: 'Copied workflow'})).toBeInTheDocument();
        expect(screen.getAllByRole('option', {name: 'Referenced workflow'})).toHaveLength(1);
    });

    it('lists only the workflows of the picked connected user in the automations workflow filter', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS&connectedUserId=8');

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(await screen.findByRole('option', {name: 'Referenced workflow'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Copied workflow'})).not.toBeInTheDocument();
    });

    it('sends the picked workflow to the automation executions query', async () => {
        renderWorkflowExecutions('source=AUTOMATIONS');

        await pickFilterOption('Workflow', 'Referenced workflow');

        await waitFor(() => {
            expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({workflowId: 'reference-uuid'}));
        });
    });

    it('does not offer the automation workflows for integrations', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS');

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(screen.queryByRole('option', {name: 'Copied workflow'})).not.toBeInTheDocument();
    });

    it('labels integrations with their name instead of their component name', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        expect(getFilterComboBox('Integration')).toHaveTextContent('Gmail Sync');

        await userEvent.click(getFilterComboBox('Integration'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'Gmail Sync', 'Gmail Archive']);
    });

    it('labels the instance configurations of the picked integration with the integration name', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        await userEvent.click(getFilterComboBox('Instance Configuration'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'Gmail Sync']);
    });

    it('lists the workflows of the last published integration version when only an integration is picked', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        expect(useGetIntegrationVersionWorkflowsQueryMock).toHaveBeenLastCalledWith(2, 2, false, true);

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(await screen.findByRole('option', {name: 'Send Email'})).toBeInTheDocument();
    });

    it('lists the workflows of the version of the picked instance configuration', () => {
        useGetIntegrationInstanceConfigurationQueryMock.mockReturnValue({
            data: {id: 4, integrationId: 2, integrationVersion: 3},
        });

        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2&integrationInstanceConfigurationId=4');

        expect(useGetIntegrationVersionWorkflowsQueryMock).toHaveBeenLastCalledWith(2, 3, false, true);
    });

    it('does not list workflows when the picked integration has no published version', () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=3');

        expect(useGetIntegrationVersionWorkflowsQueryMock).toHaveBeenLastCalledWith(3, undefined, false, false);
    });

    it('sends the picked connected user to the integration executions query', async () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        expect(useGetConnectedUsersQueryMock).toHaveBeenLastCalledWith({environmentId: 0, integrationId: 2}, true);

        await userEvent.click(getFilterComboBox('Connected User'));

        await userEvent.click(await screen.findByRole('option', {name: 'User user-7'}));

        await waitFor(() => {
            expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(
                'INTEGRATIONS',
                expect.objectContaining({connectedUserId: 7, integrationId: 2})
            );
        });
    });

    it('reads the connected user filter from the URL', async () => {
        useGetConnectedUserQueryMock.mockReturnValue({data: {externalId: 'user-7', id: 7}});

        renderWorkflowExecutions('source=INTEGRATIONS&connectedUserId=7');

        expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(
            'INTEGRATIONS',
            expect.objectContaining({connectedUserId: 7})
        );
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

        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        await userEvent.click(getFilterComboBox('Connected User'));

        await userEvent.type(screen.getByPlaceholderText('Search...'), 'user-42');

        await userEvent.click(await screen.findByRole('option', {name: 'User user-42'}));

        expect(useGetConnectedUsersQueryMock).toHaveBeenCalledWith(
            {environmentId: 0, integrationId: 2, search: 'user-42'},
            true
        );

        await waitFor(() => {
            expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(
                'INTEGRATIONS',
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

        renderWorkflowExecutions('source=INTEGRATIONS&connectedUserId=42');

        expect(useGetConnectedUserQueryMock).toHaveBeenCalledWith(42, true);
        expect(getFilterComboBox('Connected User')).toHaveTextContent('User user-42');
        expect(screen.getAllByText(/user-42/).length).toBeGreaterThan(1);

        await userEvent.click(getFilterComboBox('Connected User'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Any', 'User user-42', 'User user-7']);
    });

    it('does not load a connected user when none is picked', () => {
        renderWorkflowExecutions('source=INTEGRATIONS&integrationId=2');

        expect(useGetConnectedUserQueryMock).toHaveBeenLastCalledWith(undefined, false);
    });
});
