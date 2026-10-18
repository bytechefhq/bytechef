import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/WorkflowExecutions';
import {act, mockScrollIntoView, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
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
        <MemoryRouter initialEntries={[`/embedded/executions?${search}`]}>
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

    it('keeps the connected user when the status filter changes', async () => {
        renderWorkflowExecutions('automations=1&connectedUserId=7');

        await pickFilterOption('Status', 'COMPLETED');

        await expectConnectedUserKeptInTheUrl();

        expect(new URLSearchParams(currentSearch).get('status')).toBe('COMPLETED');
    });

    it('keeps the connected user when the integration filters change', async () => {
        renderWorkflowExecutions('automations=0&connectedUserId=7');

        await pickFilterOption('Integration', 'Gmail Sync');
        await expectConnectedUserKeptInTheUrl();

        await pickFilterOption('Instance Configuration', 'Gmail Sync');
        await expectConnectedUserKeptInTheUrl();

        await pickFilterOption('Workflow', 'Send Email');
        await expectConnectedUserKeptInTheUrl();

        expect(new URLSearchParams(currentSearch).get('workflowId')).toBe('workflow-1');
    });

    it('keeps the connected user when the type changes', async () => {
        renderWorkflowExecutions('automations=1&connectedUserId=7');

        await userEvent.click(getFilterComboBox('Type'));
        await userEvent.click(await screen.findByRole('option', {name: 'Integrations'}));

        await waitFor(() => {
            expect(new URLSearchParams(currentSearch).get('automations')).toBe('0');
        });

        expect(new URLSearchParams(currentSearch).get('connectedUserId')).toBe('7');
    });

    it.each([
        ['automations', '1'],
        ['integrations', '0'],
    ])('clears the connected user of the %s when the environment changes', async (_type, automations) => {
        let environmentChangeCallback: (() => void) | undefined;

        useOnEnvironmentChangeMock.mockImplementation((callback: () => void) => {
            environmentChangeCallback = callback;
        });

        renderWorkflowExecutions(`automations=${automations}&connectedUserId=7`);

        act(() => environmentChangeCallback?.());

        await waitFor(() => {
            expect(new URLSearchParams(currentSearch).get('connectedUserId')).toBe('');
        });

        expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({connectedUserId: undefined}));
    });

    it('lists the connected users workflows once each in the automations workflow filter', async () => {
        renderWorkflowExecutions('automations=1');

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(await screen.findByRole('option', {name: 'Copied workflow'})).toBeInTheDocument();
        expect(screen.getAllByRole('option', {name: 'Referenced workflow'})).toHaveLength(1);
    });

    it('lists only the workflows of the picked connected user in the automations workflow filter', async () => {
        renderWorkflowExecutions('automations=1&connectedUserId=8');

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(await screen.findByRole('option', {name: 'Referenced workflow'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Copied workflow'})).not.toBeInTheDocument();
    });

    it('sends the picked workflow to the automation executions query', async () => {
        renderWorkflowExecutions('automations=1');

        await pickFilterOption('Workflow', 'Referenced workflow');

        await waitFor(() => {
            expect(lastWorkflowExecutionsRequest()).toEqual(expect.objectContaining({workflowId: 'reference-uuid'}));
        });
    });

    it('does not offer the automation workflows for integrations', async () => {
        renderWorkflowExecutions('automations=0');

        await userEvent.click(getFilterComboBox('Workflow'));

        expect(screen.queryByRole('option', {name: 'Copied workflow'})).not.toBeInTheDocument();
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
