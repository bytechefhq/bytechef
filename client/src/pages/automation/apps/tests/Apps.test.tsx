import {TooltipProvider} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, stubMutation, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Apps from '../Apps';

const hoisted = vi.hoisted(() => ({
    apps: [] as unknown[],
    hostedAppInstances: [] as unknown[],
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
    useDeleteAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useDeleteAppMutation: vi.fn(() => ({mutate: vi.fn()})),
    useHostedAppInstancesQuery: vi.fn(() => ({
        data: {hostedAppInstances: hoisted.hostedAppInstances},
        error: null,
        isLoading: false,
    })),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}})),
    useRotateAppInstanceSecretKeyMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppInstanceMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppInstanceStatusMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useUpdateAppStatusMutation: vi.fn(() => ({mutate: hoisted.updateStatusMutate})),
    useWorkspaceUsersQuery: vi.fn(() => ({data: {workspaceUsers: hoisted.workspaceUsers}})),
}));

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: vi.fn(() => ({data: []})),
}));

vi.mock('@/pages/automation/apps/hooks/useAppManageScopes', () => ({
    default: vi.fn(() => () => true),
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

const crmInstance = {
    environmentId: '2',
    hostingTarget: 'VERCEL',
    id: '11',
    name: 'crm-dashboard-prod',
    secretKeyHint: 'ijklmnop',
    status: 'ACTIVE',
    url: 'https://crm.vercel.app',
    workspaceId: '7',
};

const crmApp = {
    foreignInstances: [{environmentId: '2', id: '12', name: 'crm-germany', status: 'ACTIVE', workspaceId: '8'}],
    id: '1',
    instances: [crmInstance],
    name: 'crm-dashboard',
    ownerUserId: '42',
    status: 'ACTIVE',
    workspaceId: '7',
};

const hostedInstance = {
    app: {description: 'Shared CRM', id: '5', name: 'group-crm', status: 'ACTIVE'},
    environmentId: '2',
    hostingTarget: 'SELF_HOSTED',
    id: '21',
    name: 'group-crm-sales',
    secretKeyHint: 'qrstuvwx',
    status: 'ACTIVE',
    url: null,
    workspaceId: '7',
};

describe('Apps', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        windowResizeObserver();

        hoisted.apps = [];
        hoisted.hostedAppInstances = [];
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

    it('lists apps with their status and instance count', () => {
        hoisted.apps = [crmApp];

        renderApps();

        expect(screen.getByText('crm-dashboard')).toBeInTheDocument();
        expect(screen.getByText('2 instances')).toBeInTheDocument();
        expect(screen.getByText('Active')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toBeChecked();
    });

    it('shows a hosted instance with its parent app and no app edit controls', () => {
        hoisted.hostedAppInstances = [hostedInstance];

        renderApps();

        const hostedRow = screen.getByTestId('hosted-app-instance-row-21');

        expect(within(hostedRow).getByText('group-crm-sales')).toBeInTheDocument();
        expect(within(hostedRow).getByText('App: group-crm')).toBeInTheDocument();
        expect(within(hostedRow).getByText('Shared CRM')).toBeInTheDocument();
        expect(within(hostedRow).queryByRole('switch')).not.toBeInTheDocument();
        expect(within(hostedRow).queryByRole('button', {name: /group-crm$/})).not.toBeInTheDocument();
        expect(within(hostedRow).queryByRole('button', {name: 'Actions for group-crm'})).not.toBeInTheDocument();
        expect(within(hostedRow).getByRole('button', {name: 'Suspend group-crm-sales'})).toBeInTheDocument();
    });

    it('links a hosted instance to its instance page', () => {
        hoisted.hostedAppInstances = [hostedInstance];

        renderApps();

        const hostedRow = screen.getByTestId('hosted-app-instance-row-21');

        expect(within(hostedRow).getByRole('link', {name: 'group-crm-sales'})).toHaveAttribute(
            'href',
            '/automation/apps/instances/21'
        );
    });

    it('asks for the hosted instances of the current workspace and environment', async () => {
        const {useHostedAppInstancesQuery} = await import('@/shared/middleware/graphql');

        environmentStore.setState({currentEnvironmentId: 2});

        renderApps();

        expect(vi.mocked(useHostedAppInstancesQuery)).toHaveBeenCalledWith({environmentId: '2', workspaceId: '7'});
    });

    it('lists the apps of the workspace without an environment', async () => {
        const {useAppsQuery} = await import('@/shared/middleware/graphql');

        renderApps();

        expect(vi.mocked(useAppsQuery)).toHaveBeenCalledWith({workspaceId: '7'});
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

        expect(hoisted.updateStatusMutate).toHaveBeenCalledWith({id: '1', status: 'SUSPENDED'});
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

        createMutation.triggerOnSuccess(
            {createApp: {appInstance: crmInstance, secretKey: 'abcdefghijklmnop'}},
            undefined
        );

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
