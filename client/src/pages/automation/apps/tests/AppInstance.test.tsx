import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppInstanceDetail from '../AppInstance';

const hoisted = vi.hoisted(() => ({
    canDeploy: true,
    canManage: true,
}));

vi.mock('@/pages/automation/apps/hooks/useAppDeployScopes', () => ({
    default: vi.fn(() => () => hoisted.canDeploy),
}));

vi.mock('@/pages/automation/apps/hooks/useAppManageScopes', () => ({
    default: vi.fn(() => () => hoisted.canManage),
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppApiCollectionsQuery: vi.fn(() => ({data: {appApiCollections: []}, error: null, isLoading: false})),
    useAppBindableApiCollectionsQuery: vi.fn(() => ({data: {appBindableApiCollections: []}})),
    useAppBindableConnectionsQuery: vi.fn(() => ({data: {appBindableConnections: []}})),
    useAppBindableWorkflowsQuery: vi.fn(() => ({data: {appBindableWorkflows: []}})),
    useAppBindingsQuery: vi.fn(() => ({data: {appBindings: []}, error: null, isLoading: false})),
    useAppComponentOperationsQuery: vi.fn(() => ({data: undefined})),
    useAppDeploymentsQuery: vi.fn(() => ({data: {appDeployments: []}, error: null, isLoading: false})),
    useAppInstanceDeploymentSettingsQuery: vi.fn(() => ({data: undefined})),
    useAppInstanceDeploymentSummaryQuery: vi.fn(() => ({
        data: {appInstance: {currentDeployment: null, deployments: [], id: '21'}},
        isLoading: false,
    })),
    useAppInstanceQuery: vi.fn(() => ({
        data: {
            appInstance: {
                app: {description: 'Shared CRM', id: '5', name: 'group-crm', status: 'ACTIVE'},
                deploymentConnectionId: '31',
                deploymentProviderAvailable: true,
                environmentId: '2',
                hostingTarget: 'SELF_HOSTED',
                id: '21',
                name: 'group-crm-sales',
                secretKeyHint: 'qrstuvwx',
                status: 'ACTIVE',
                url: null,
                workspaceId: '7',
            },
        },
        error: null,
        isLoading: false,
    })),
    useAppInstanceVersionsQuery: vi.fn(() => ({
        data: {appInstanceVersions: [{id: '1', name: 'v1'}]},
        isLoading: false,
    })),
    useAppWorkflowsQuery: vi.fn(() => ({data: {appWorkflows: []}, error: null, isLoading: false})),
    useBindAppApiCollectionMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useBindAppConnectionMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useBindAppWorkflowMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useCancelAppDeploymentMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useDeployAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useRollbackAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUnbindAppApiCollectionMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUnbindAppConnectionMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUnbindAppWorkflowMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppInstanceDeploymentSettingsMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
}));

const renderPage = () =>
    render(
        <MemoryRouter initialEntries={['/automation/apps/instances/21']}>
            <Routes>
                <Route element={<AppInstanceDetail />} path="/automation/apps/instances/:id" />
            </Routes>
        </MemoryRouter>
    );

describe('AppInstanceDetail', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.canDeploy = true;
        hoisted.canManage = true;
    });

    it('loads the instance and renders the three binding tabs and no activity tab', async () => {
        const {useAppInstanceQuery} = await import('@/shared/middleware/graphql');

        renderPage();

        expect(vi.mocked(useAppInstanceQuery)).toHaveBeenCalledWith({id: '21'}, {enabled: true});
        expect(screen.getByRole('tab', {name: /Connections/})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: /API Collections/})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: /Workflows/})).toBeInTheDocument();
        expect(screen.queryByRole('tab', {name: /Activity/})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Bind Connection'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: /Workflows/}));

        expect(await screen.findByRole('button', {name: 'Bind Workflow'})).toBeInTheDocument();
    });

    it('shows the parent app read-only, without edit controls', () => {
        renderPage();

        const parentApp = screen.getByRole('region', {name: 'App'});

        expect(within(parentApp).getByText('group-crm')).toBeInTheDocument();
        expect(within(parentApp).getByText('Shared CRM')).toBeInTheDocument();
        expect(within(parentApp).queryByRole('button')).not.toBeInTheDocument();
        expect(within(parentApp).queryByRole('switch')).not.toBeInTheDocument();
        expect(within(parentApp).queryByRole('link')).not.toBeInTheDocument();
    });

    it('hides the manage controls without APP_MANAGE in the instance workspace and environment', async () => {
        const useAppManageScopes = (await import('@/pages/automation/apps/hooks/useAppManageScopes')).default;

        hoisted.canManage = false;

        renderPage();

        expect(vi.mocked(useAppManageScopes)).toHaveBeenCalledWith([{environmentId: '2', workspaceId: '7'}]);
        expect(screen.getByRole('tab', {name: /Connections/})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Bind Connection'})).not.toBeInTheDocument();
    });

    it('offers the deployment settings to an APP_MANAGE holder', () => {
        renderPage();

        expect(screen.getByRole('button', {name: 'Deployment'})).toBeInTheDocument();
    });

    it('hides the deployment settings without APP_MANAGE', () => {
        hoisted.canManage = false;

        renderPage();

        expect(screen.queryByRole('button', {name: 'Deployment'})).not.toBeInTheDocument();
    });

    it('offers Deploy on the deployments tab with APP_DEPLOY in the instance workspace and environment', async () => {
        const useAppDeployScopes = (await import('@/pages/automation/apps/hooks/useAppDeployScopes')).default;

        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Deployments/}));

        expect(await screen.findByRole('button', {name: 'Deploy'})).toBeEnabled();
        expect(vi.mocked(useAppDeployScopes)).toHaveBeenCalledWith([{environmentId: '2', workspaceId: '7'}]);
    });

    it('hides Deploy without APP_DEPLOY', async () => {
        hoisted.canDeploy = false;

        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Deployments/}));

        expect(await screen.findByText('The deployment history of this instance, newest first.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
    });
});
