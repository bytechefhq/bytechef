import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppDetail from '../App';

const hoisted = vi.hoisted(() => ({
    canDeploy: true,
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
                        deploymentConnectionId: '31',
                        deploymentProviderAvailable: true,
                        environmentId: '2',
                        hostingTarget: 'VERCEL',
                        id: '21',
                        name: 'crm-prod',
                        status: 'ACTIVE',
                        workspaceId: '7',
                    },
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
    useCancelAppDeploymentMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useDeployAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: []}})),
    useRollbackAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
}));

const renderPage = () =>
    render(
        <MemoryRouter initialEntries={['/automation/apps/5']}>
            <Routes>
                <Route element={<AppDetail />} path="/automation/apps/:id" />
            </Routes>
        </MemoryRouter>
    );

describe('AppDetail', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.canDeploy = true;
        hoisted.useAppInstanceVersionsQueryMock.mockReturnValue({
            data: {appInstanceVersions: [{createdDate: '2026-11-12T10:00:00Z', id: '3', name: 'v3'}]},
            isLoading: false,
        });

        windowResizeObserver();
        mockScrollIntoView();
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

        expect(await screen.findByText('The deployment history of this instance, newest first.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
    });
});
