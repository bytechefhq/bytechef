import {TooltipProvider} from '@/components/ui/tooltip';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import {render, screen, stubMutation, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, onTestFinished, vi} from 'vitest';

import AppApiCollectionsTab from '../components/AppApiCollectionsTab';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppApiCollectionsQuery: vi.fn(() => ({
        data: {
            appApiCollections: [
                {
                    apiCollectionId: '21',
                    appInstanceId: '3',
                    available: true,
                    collectionVersion: 2,
                    contextPath: 'crm',
                    id: '31',
                    name: 'CRM',
                    projectDeploymentId: '41',
                    projectId: '5',
                    projectName: 'Sales',
                    projectVersion: 2,
                },
                {
                    apiCollectionId: '22',
                    appInstanceId: '3',
                    available: false,
                    collectionVersion: 0,
                    contextPath: null,
                    id: '32',
                    name: '#22',
                    projectDeploymentId: null,
                    projectId: null,
                    projectName: null,
                    projectVersion: null,
                },
            ],
        },
        error: null,
        isLoading: false,
    })),
    useAppBindableApiCollectionsQuery: vi.fn(() => ({data: {appBindableApiCollections: []}})),
    useBindAppApiCollectionMutation: vi.fn(),
    useUnbindAppApiCollectionMutation: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.queries', () => ({
    useGetApiCollectionQuery: vi.fn(() => ({
        data: {
            collectionVersion: 2,
            contextPath: 'crm',
            endpoints: [
                {httpMethod: 'GET', id: 81, name: 'ListOrders', path: 'list', workflowUuid: 'uuid-list-orders'},
                {httpMethod: 'GET', id: 82, name: 'ListBases', path: '/bases', workflowUuid: 'uuid-list-bases'},
            ],
        },
    })),
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: vi.fn(() => true),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: vi.fn(() => ({
        data: {
            projectDeploymentWorkflows: [{connections: [], id: 71, inputs: {}, workflowUuid: 'uuid-list-orders'}],
        },
    })),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectVersionWorkflowsQuery: vi.fn(() => ({
        data: [{id: 'workflow-1', label: 'List Orders', workflowUuid: 'uuid-list-orders'}],
    })),
}));

const openProjectWorkflowMock = vi.hoisted(() => vi.fn());

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({openProjectWorkflow: openProjectWorkflowMock}),
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet',
    () => ({
        default: () => null,
    })
);

vi.mock('@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog', () => ({
    default: ({
        projectDeploymentWorkflow,
        workflow,
    }: {
        projectDeploymentWorkflow: {id: number};
        workflow: {label: string};
    }) => <div role="dialog">{`Edit ${workflow.label} of deployment workflow ${projectDeploymentWorkflow.id}`}</div>,
}));

const {useGetApiCollectionQuery} = await import('@/ee/shared/mutations/automation/apiCollections.queries');
const {useGetProjectDeploymentQuery} = await import('@/shared/queries/automation/projectDeployments.queries');
const {useGetProjectVersionWorkflowsQuery} = await import('@/shared/queries/automation/projectWorkflows.queries');

const {useAppApiCollectionsQuery, useBindAppApiCollectionMutation, useUnbindAppApiCollectionMutation} =
    await import('@/shared/middleware/graphql');

stubMutation(vi.mocked(useBindAppApiCollectionMutation));

const unbindMutation = stubMutation(vi.mocked(useUnbindAppApiCollectionMutation));

describe('AppApiCollectionsTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders a row per binding with its name, collection version, context path, project and project version', () => {
        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        expect(screen.queryByRole('columnheader', {name: 'Version'})).not.toBeInTheDocument();

        const crmRow = screen.getByRole('row', {name: /CRM/});
        const crmNameCell = within(crmRow).getAllByRole('cell')[0];

        expect(within(crmNameCell).getByText('v2')).toBeInTheDocument();

        expect(within(crmRow).getByText('/crm')).toBeInTheDocument();
        expect(within(crmRow).getByText('Sales')).toBeInTheDocument();
        expect(within(crmRow).getByText('V2')).toBeInTheDocument();
        expect(within(crmRow).queryByText('Unavailable')).not.toBeInTheDocument();
    });

    it('marks a binding that is not available with an Unavailable badge', () => {
        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        const missingRow = screen.getByRole('row', {name: /#22/});

        expect(within(missingRow).getByText('Unavailable')).toBeInTheDocument();
    });

    it('shows a centered empty state with Bind API Collection when nothing is bound', () => {
        vi.mocked(useAppApiCollectionsQuery).mockReturnValue({
            data: {appApiCollections: []},
            error: null,
            isLoading: false,
        } as never);
        onTestFinished(() => {
            vi.mocked(useAppApiCollectionsQuery).mockReset();
        });

        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        expect(screen.getByRole('heading', {name: 'No API collections bound'})).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Bind API Collection'})).toHaveLength(1);
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('hides the bind and unbind buttons when the user cannot manage the app', () => {
        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage={false} />
            </TooltipProvider>
        );

        expect(screen.queryByRole('button', {name: 'Bind API Collection'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'CRM actions'})).not.toBeInTheDocument();
    });

    it('lists the endpoints of a collection on row click and opens Edit Workflow on one', async () => {
        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        expect(screen.queryByText('ListOrders')).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('CRM'));

        expect(vi.mocked(useGetApiCollectionQuery)).toHaveBeenLastCalledWith(21);
        expect(vi.mocked(useGetProjectDeploymentQuery)).toHaveBeenLastCalledWith(41);
        expect(vi.mocked(useGetProjectVersionWorkflowsQuery)).toHaveBeenLastCalledWith(5, 2);
        expect(screen.getByRole('heading', {name: 'Endpoints'})).toBeInTheDocument();
        expect(screen.queryByText(/apply to every app bound to this collection/)).not.toBeInTheDocument();
        expect(screen.getAllByText('GET')).toHaveLength(2);
        expect(screen.getByText('ListOrders')).toBeInTheDocument();
        expect(screen.getByText('/v2/crm/list')).toBeInTheDocument();
        expect(screen.getByText('/v2/crm/bases')).toBeInTheDocument();

        await userEvent.click(screen.getByText('CRM'));

        expect(screen.queryByText('ListOrders')).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('CRM'));
        await userEvent.click(screen.getByRole('button', {name: 'ListOrders actions'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Edit Workflow'}));

        expect(screen.getByRole('dialog')).toHaveTextContent('Edit List Orders of deployment workflow 71');
    });

    it('opens the executions of the workflow behind an endpoint on click', async () => {
        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentWorkflowSheetOpen: false,
            workflow: undefined,
        });

        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByText('CRM'));
        await userEvent.click(screen.getByText('ListOrders'));

        const sheetState = useProjectDeploymentWorkflowSheetStore.getState();

        expect(sheetState.projectDeploymentWorkflowSheetOpen).toBe(true);
        expect(sheetState.projectDeploymentId).toBe(41);
        expect(sheetState.projectId).toBe(5);
        expect(sheetState.projectName).toBe('Sales');
        expect(sheetState.projectVersion).toBe(2);
        expect(sheetState.workflow?.workflowUuid).toBe('uuid-list-orders');
    });

    it('opens the workflow behind an endpoint in its project from the link next to its name', async () => {
        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentWorkflowSheetOpen: false,
            workflow: undefined,
        });

        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByText('CRM'));
        await userEvent.click(screen.getAllByRole('button', {name: 'Open in project'})[0]);

        expect(openProjectWorkflowMock).toHaveBeenCalledWith(5, 'uuid-list-orders');
        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(false);
    });

    it('shows an arrow on a row with endpoints that turns when the row opens', async () => {
        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        const crmRow = screen.getByRole('row', {name: /CRM/});
        const unavailableRow = screen.getByRole('row', {name: /#22/});

        expect(crmRow.querySelector('svg.lucide-chevron-right')).not.toHaveClass('rotate-90');
        expect(unavailableRow.querySelector('svg.lucide-chevron-right')).toBeNull();

        await userEvent.click(screen.getByText('CRM'));

        expect(crmRow.querySelector('svg.lucide-chevron-right')).toHaveClass('rotate-90');
    });

    it('does not open endpoints for a collection without a deployment', async () => {
        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByText('#22'));

        expect(screen.queryByRole('heading', {name: 'Endpoints'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: '#22 actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Unbind'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Show workflows'})).not.toBeInTheDocument();
    });

    it('unbinds a collection after the confirmation and refreshes the bindings', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        render(
            <TooltipProvider>
                <AppApiCollectionsTab appInstanceId="3" canManage />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: 'CRM actions'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Unbind'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Unbind'}));

        await waitFor(() => {
            expect(unbindMutation.mutate).toHaveBeenCalledWith({apiCollectionId: '21', appInstanceId: '3'});
        });

        unbindMutation.triggerOnSuccess(true, {apiCollectionId: '21', appInstanceId: '3'});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['appApiCollections']});

        invalidateQueriesSpy.mockRestore();
    });
});
