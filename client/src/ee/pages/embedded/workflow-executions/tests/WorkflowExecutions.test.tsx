import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/WorkflowExecutions';
import {act, mockScrollIntoView, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useOnEnvironmentChangeMock, useWorkflowExecutionsMock} = vi.hoisted(() => ({
    useOnEnvironmentChangeMock: vi.fn(),
    useWorkflowExecutionsMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions', () => ({
    useWorkflowExecutions: useWorkflowExecutionsMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: () => ({data: {id: 4, integrationVersion: 1}}),
    useGetIntegrationInstanceConfigurationsQuery: () => ({
        data: [{id: 4, integration: {componentName: 'googleMail'}, integrationId: 2, tags: []}],
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: [{id: 'workflow-1', label: 'Send Email'}]}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({data: [{componentName: 'googleMail', id: 2, name: 'Gmail', tags: []}]}),
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

        await pickFilterOption('Integration', 'googleMail');
        await expectConnectedUserKeptInTheUrl();

        await pickFilterOption('Instance Configuration', 'googleMail');
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

    it('keeps the connected user when the environment changes', async () => {
        let environmentChangeCallback: (() => void) | undefined;

        useOnEnvironmentChangeMock.mockImplementation((callback: () => void) => {
            environmentChangeCallback = callback;
        });

        renderWorkflowExecutions('automations=1&connectedUserId=7');

        act(() => environmentChangeCallback?.());

        await expectConnectedUserKeptInTheUrl();
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
});
