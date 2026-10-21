import {TooltipProvider} from '@/components/ui/tooltip';
import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppDetail from '../App';

const hoisted = vi.hoisted(() => ({
    canDeploy: true,
    extraInstances: [] as unknown[],
    scopes: [] as string[],
    useAppInstanceVersionsQueryMock: vi.fn(),
}));

vi.mock('@/pages/automation/apps/hooks/useAppDeployScopes', () => ({
    default: vi.fn(() => () => hoisted.canDeploy),
}));

vi.mock('@/pages/automation/apps/hooks/useAppManageScopes', () => ({
    default: vi.fn(() => () => true),
}));

vi.mock('@/pages/automation/apps/hooks/useAppOwnerNames', () => ({
    default: vi.fn(() => () => 'Owner'),
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppDeploymentsQuery: vi.fn(() => ({data: {appDeployments: []}, error: null, isLoading: false})),
    useAppInstanceDeploymentSummaryQuery: vi.fn(() => ({
        data: {appInstance: {currentDeployment: null, deployments: [], id: '21'}},
        isLoading: false,
    })),
    useAppInstanceVersionsQuery: hoisted.useAppInstanceVersionsQueryMock,
    useAppQuery: vi.fn(() => ({
        data: {
            app: {
                description: 'Shared CRM',
                foreignInstances: [],
                id: '5',
                instances: [
                    {
                        deploymentConnectionId: null,
                        deploymentProviderAvailable: false,
                        environmentId: '0',
                        hostingTarget: 'SELF_HOSTED',
                        id: '20',
                        name: 'crm-dev',
                        status: 'ACTIVE',
                        workspaceId: '7',
                    },
                    {
                        deploymentConnectionId: '31',
                        deploymentProviderAvailable: true,
                        environmentId: '2',
                        hostingTarget: 'VERCEL',
                        id: '21',
                        name: 'crm-prod',
                        status: 'ACTIVE',
                        workspaceId: '7',
                    },
                    ...hoisted.extraInstances,
                ],
                name: 'group-crm',
                ownerUserId: '1',
                status: 'ACTIVE',
                workspaceId: '7',
            },
        },
        error: null,
        isLoading: false,
    })),
    useAppsQuery: vi.fn(() => ({
        data: {
            apps: [
                {id: '6', name: 'billing-portal'},
                {id: '5', name: 'group-crm'},
            ],
        },
        error: null,
        isLoading: false,
    })),
    useCancelAppDeploymentMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useDeployAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}})),
    useRollbackAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
}));

const renderPage = () =>
    render(
        <TooltipProvider>
            <MemoryRouter initialEntries={['/automation/apps/5']}>
                <Routes>
                    <Route element={<AppDetail />} path="/automation/apps/:id" />
                </Routes>
            </MemoryRouter>
        </TooltipProvider>
    );

describe('AppDetail', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.canDeploy = true;
        hoisted.extraInstances = [];
        hoisted.scopes = [];
        hoisted.useAppInstanceVersionsQueryMock.mockReturnValue({
            data: {appInstanceVersions: [{createdDate: '2026-11-12T10:00:00Z', id: '3', name: 'v3'}]},
            isLoading: false,
        });

        environmentStore.setState({currentEnvironmentId: 2});

        windowResizeObserver();
        mockScrollIntoView();
    });

    it('lists the workspace apps in the left sidebar with the open app current', () => {
        renderPage();

        expect(screen.getByPlaceholderText('Search apps...')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'group-crm'})).toHaveAttribute('aria-current', 'page');
        expect(screen.getByRole('link', {name: 'billing-portal'})).toHaveAttribute('href', '/automation/apps/6');
    });

    it('offers New App in the left sidebar only with APP_MANAGE', () => {
        const {unmount} = renderPage();

        expect(screen.queryByRole('button', {name: 'New App'})).not.toBeInTheDocument();

        unmount();

        hoisted.scopes = [APP_MANAGE_SCOPE];

        renderPage();

        expect(screen.getByRole('button', {name: 'New App'})).toBeInTheDocument();
    });

    it('offers Deploy on the deployments tab to an owner with APP_DEPLOY in the instance workspace', async () => {
        const useAppDeployScopes = (await import('@/pages/automation/apps/hooks/useAppDeployScopes')).default;

        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Deployments/}));

        expect(await screen.findByRole('button', {name: 'Deploy'})).toBeEnabled();
        expect(vi.mocked(useAppDeployScopes)).toHaveBeenCalledWith([{environmentId: '2', workspaceId: '7'}]);
        expect(hoisted.useAppInstanceVersionsQueryMock).toHaveBeenCalledWith(
            {appInstanceId: '21', limit: 50},
            expect.objectContaining({enabled: true})
        );
    });

    it('hides Deploy on the deployments tab without APP_DEPLOY', async () => {
        hoisted.canDeploy = false;

        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Deployments/}));

        expect(await screen.findByRole('heading', {name: 'No deployments yet'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
    });

    it('keeps Edit and Delete in the header actions menu', async () => {
        const {useMyWorkspaceScopesQuery} = await import('@/shared/middleware/graphql');

        vi.mocked(useMyWorkspaceScopesQuery).mockReturnValue({
            data: {myWorkspaceScopes: [APP_MANAGE_SCOPE]},
        } as never);

        renderPage();

        expect(screen.queryByRole('button', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'App actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();

        vi.mocked(useMyWorkspaceScopesQuery).mockReturnValue({data: {myWorkspaceScopes: []}} as never);
    });

    it('shows only the instances of the environment chosen in the sidebar', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Instances/}));

        expect(await screen.findByText('crm-prod')).toBeInTheDocument();
        expect(screen.queryByText('crm-dev')).not.toBeInTheDocument();
    });

    it('skips the instance picker and the deployment summary while the environment has one instance', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));

        expect(screen.queryByRole('combobox', {name: 'Instance'})).not.toBeInTheDocument();
        expect(screen.queryByText('No deployments yet')).not.toBeInTheDocument();
    });

    it('names the instance in the picker without its environment', async () => {
        hoisted.extraInstances = [
            {
                deploymentConnectionId: null,
                deploymentProviderAvailable: false,
                environmentId: '2',
                hostingTarget: 'SELF_HOSTED',
                id: '22',
                name: 'crm-staging',
                status: 'ACTIVE',
                workspaceId: '7',
            },
        ];

        renderPage();

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));

        expect(await screen.findByRole('combobox', {name: 'Instance'})).toHaveTextContent(/^crm-prod \(Active\)$/);
    });
});
