import {TooltipProvider} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import useWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationWorkflowExecutionsSheet from '../IntegrationInstanceConfigurationWorkflowExecutionsSheet';

const hoisted = vi.hoisted(() => ({
    queryMock: vi.fn(),
    refetchMock: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetIntegrationWorkflowExecutionsQuery: hoisted.queryMock,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTable', () => ({
    default: ({data, showConnectedUser}: {data: WorkflowExecution[]; showConnectedUser?: boolean}) => (
        <div>{`Executions table ${data.map((workflowExecution) => workflowExecution.id).join(',')} connected user ${String(!!showConnectedUser)}`}</div>
    ),
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => <div>Execution details sheet</div>,
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel',
    () => ({
        default: ({workflow}: {workflow: Workflow}) => <div>{`Workflow panel ${workflow.id}`}</div>,
    })
);

const mockQueryResult = (content: object[], totalPages = 1) =>
    hoisted.queryMock.mockReturnValue({
        data: {content, size: 20, totalElements: 20 * totalPages, totalPages},
        error: null,
        isFetching: false,
        isLoading: false,
        refetch: hoisted.refetchMock,
    });

const execution = {
    id: 5,
    integration: {componentName: 'asana'},
    integrationInstance: {connectedUserId: 9, connectionId: 1},
    integrationInstanceConfiguration: {integrationVersion: 2, name: 'Asana Dev'},
    job: {id: '5', status: 'COMPLETED'},
    workflow: {label: 'Sync tasks'},
};

const renderSheet = () =>
    render(
        <TooltipProvider>
            <IntegrationInstanceConfigurationWorkflowExecutionsSheet />
        </TooltipProvider>
    );

const openSheet = () =>
    act(() =>
        useIntegrationInstanceConfigurationWorkflowSheetStore
            .getState()
            .openIntegrationInstanceConfigurationWorkflowSheet({
                integrationInstanceConfigurationId: 3,
                integrationInstanceConfigurationName: 'Asana Dev',
                integrationVersion: 2,
                workflow: {id: 'workflow1', label: 'Sync tasks'} as Workflow,
            })
    );

describe('IntegrationInstanceConfigurationWorkflowExecutionsSheet', () => {
    beforeEach(() => {
        hoisted.queryMock.mockReset();
        hoisted.refetchMock.mockReset();

        environmentStore.setState({currentEnvironmentId: 0});

        useIntegrationInstanceConfigurationWorkflowSheetStore.setState({
            integrationInstanceConfigurationId: undefined,
            integrationInstanceConfigurationName: undefined,
            integrationInstanceConfigurationWorkflowSheetOpen: false,
            integrationVersion: undefined,
            workflow: undefined,
        });

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 0, workflowExecutionSheetOpen: false});
    });

    it('renders nothing while closed', () => {
        mockQueryResult([]);

        renderSheet();

        expect(screen.queryByText('Sync tasks')).not.toBeInTheDocument();
        expect(hoisted.queryMock).not.toHaveBeenCalled();
    });

    it("shows the configuration workflow's executions in the current environment by default", () => {
        mockQueryResult([execution]);

        renderSheet();

        openSheet();

        expect(screen.getByText('Asana Dev /')).toBeInTheDocument();
        expect(screen.getByText('Sync tasks', {selector: 'strong'})).toBeInTheDocument();
        expect(screen.getByText('/ V2')).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Executions'})).toHaveAttribute('aria-selected', 'true');
        expect(hoisted.queryMock).toHaveBeenLastCalledWith({
            environmentId: 0,
            integrationInstanceConfigurationId: 3,
            pageNumber: 0,
            workflowId: 'workflow1',
        });
        expect(screen.getByText('Executions table 5 connected user true')).toBeInTheDocument();
        expect(screen.getByText('Execution details sheet')).toBeInTheDocument();
    });

    it('shows an empty state when the workflow has not run yet', () => {
        mockQueryResult([]);

        renderSheet();

        openSheet();

        expect(screen.getByText('No Executions')).toBeInTheDocument();
    });

    it('pages through the executions', async () => {
        mockQueryResult([execution], 3);

        renderSheet();

        openSheet();

        await userEvent.click(screen.getByRole('link', {name: 'Go to next page'}));

        expect(hoisted.queryMock).toHaveBeenLastCalledWith(expect.objectContaining({pageNumber: 1}));
    });

    it('refreshes the executions', async () => {
        mockQueryResult([execution]);

        renderSheet();

        openSheet();

        await userEvent.click(screen.getByRole('button', {name: 'Refresh workflow executions'}));

        expect(hoisted.refetchMock).toHaveBeenCalled();
    });

    it('shows the read-only workflow on the Workflow tab', async () => {
        mockQueryResult([execution]);

        renderSheet();

        openSheet();

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow'}));

        expect(screen.getByText('Workflow panel workflow1')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Refresh workflow executions'})).not.toBeInTheDocument();
    });

    it('closes the execution details together with the sheet', async () => {
        mockQueryResult([execution]);

        renderSheet();

        openSheet();

        act(() => useWorkflowExecutionSheetStore.getState().setWorkflowExecutionSheetOpen(true));

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(
            useIntegrationInstanceConfigurationWorkflowSheetStore.getState()
                .integrationInstanceConfigurationWorkflowSheetOpen
        ).toBe(false);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });
});
