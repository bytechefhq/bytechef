import {TooltipProvider} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {render, screen, stubMutation, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Apps from '../Apps';

const hoisted = vi.hoisted(() => ({
    apps: [] as unknown[],
    navigate: vi.fn(),
    scopes: ['APP_VIEW', 'APP_MANAGE'] as string[],
    updateStatusMutate: vi.fn(),
    workspaceUsers: [] as unknown[],
}));

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppsQuery: vi.fn(() => ({data: {apps: hoisted.apps}, error: null, isLoading: false})),
    useCreateAppMutation: vi.fn(),
    useDeleteAppMutation: vi.fn(() => ({mutate: vi.fn()})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}})),
    useRotateAppSecretKeyMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppStatusMutation: vi.fn(() => ({mutate: hoisted.updateStatusMutate})),
    useWorkspaceUsersQuery: vi.fn(() => ({data: {workspaceUsers: hoisted.workspaceUsers}})),
}));

const {useCreateAppMutation} = await import('@/shared/middleware/graphql');

const createMutation = stubMutation(vi.mocked(useCreateAppMutation));

const renderApps = () =>
    render(
        <TooltipProvider>
            <MemoryRouter>
                <Apps />
            </MemoryRouter>
        </TooltipProvider>
    );

const crmApp = {
    environmentId: '2',
    hostingTarget: 'VERCEL',
    id: '1',
    lastDeployedDate: null,
    name: 'crm-dashboard',
    ownerUserId: '42',
    secretKeyHint: 'ijklmnop',
    status: 'ENABLED',
    url: 'https://crm.vercel.app',
    workspaceId: '7',
};

describe('Apps', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        windowResizeObserver();

        hoisted.apps = [];
        hoisted.scopes = ['APP_VIEW', 'APP_MANAGE'];
        hoisted.workspaceUsers = [];

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('shows the empty state with a New App button for managers', () => {
        renderApps();

        expect(screen.getByText('No Apps')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New App'})).toBeInTheDocument();
    });

    it('hides New App without APP_MANAGE', () => {
        hoisted.scopes = ['APP_VIEW'];

        renderApps();

        expect(screen.queryByRole('button', {name: 'New App'})).not.toBeInTheDocument();
    });

    it('lists apps with status switch and deploy hint', () => {
        hoisted.apps = [crmApp];

        renderApps();

        expect(screen.getByText('crm-dashboard')).toBeInTheDocument();
        expect(screen.getByText('https://crm.vercel.app')).toBeInTheDocument();
        expect(screen.getByText('Never deployed')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toBeChecked();
    });

    it('disables the status switch without APP_MANAGE', () => {
        hoisted.apps = [crmApp];
        hoisted.scopes = ['APP_VIEW'];

        renderApps();

        expect(screen.getByRole('switch')).toBeDisabled();
    });

    it('toggles the status through updateAppStatus', async () => {
        hoisted.apps = [crmApp];

        renderApps();

        await userEvent.click(screen.getByRole('switch'));

        expect(hoisted.updateStatusMutate).toHaveBeenCalledWith({id: '1', status: 'DISABLED'});
    });

    it('opens the app detail page when the row is clicked', async () => {
        hoisted.apps = [crmApp];

        renderApps();

        await userEvent.click(screen.getByText('crm-dashboard'));

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/apps/1');
    });

    it('names the owner from the workspace members and falls back to the user id', () => {
        hoisted.apps = [crmApp];
        hoisted.workspaceUsers = [
            {user: {email: 'ada@example.com', firstName: 'Ada', lastName: 'Lovelace'}, userId: '42'},
        ];

        const {unmount} = renderApps();

        expect(screen.getByText('Owner: Ada Lovelace')).toBeInTheDocument();

        unmount();

        hoisted.workspaceUsers = [];

        renderApps();

        expect(screen.getByText('Owner: User 42')).toBeInTheDocument();
    });

    it('keeps the new app key visible when the first app makes the list appear', async () => {
        const {rerender} = renderApps();

        await userEvent.click(screen.getByRole('button', {name: 'New App'}));
        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalled();
        });

        createMutation.triggerOnSuccess({createApp: {app: crmApp, secretKey: 'abcdefghijklmnop'}}, undefined);

        expect(await screen.findByDisplayValue('abcdefghijklmnop')).toBeInTheDocument();

        hoisted.apps = [crmApp];

        rerender(
            <TooltipProvider>
                <MemoryRouter>
                    <Apps />
                </MemoryRouter>
            </TooltipProvider>
        );

        expect(screen.getByText('crm-dashboard')).toBeInTheDocument();
        expect(screen.getByDisplayValue('abcdefghijklmnop')).toBeInTheDocument();
    });
});
