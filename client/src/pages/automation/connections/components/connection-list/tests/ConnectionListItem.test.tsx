import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection} from '@/shared/middleware/automation/configuration';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

const ALL_SCOPES = ['CONNECTION_DELETE', 'CONNECTION_EDIT'];

const EDITOR_SCOPES = ['CONNECTION_CREATE', 'CONNECTION_DELETE', 'CONNECTION_EDIT', 'CONNECTION_VIEW'];

const VIEWER_SCOPES = ['CONNECTION_VIEW'];

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

type MutationOptionsType = {onSuccess?: () => void};

const hoistedSharing = vi.hoisted(() => ({
    connectionGrantsEnabled: [] as boolean[],
    grantOptions: undefined as MutationOptionsType | undefined,
    revokeOptions: undefined as MutationOptionsType | undefined,
    visibilityEnabled: false,
    visibilityOptions: undefined as MutationOptionsType | undefined,
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: () =>
        hoistedSharing.visibilityEnabled
            ? {enabled: true, isAdmin: false, workspaceId: 1}
            : {enabled: false, isAdmin: false, workspaceId: undefined},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAffectedWorkflowsQuery: () => ({data: undefined}),
    useConnectionGrantsQuery: (_variables: unknown, options: {enabled: boolean}) => {
        hoistedSharing.connectionGrantsEnabled.push(options.enabled);

        return {data: undefined};
    },
    useGrantConnectionAccessMutation: (options: MutationOptionsType) => {
        hoistedSharing.grantOptions = options;

        return {mutate: vi.fn()};
    },
    useReassignAllConnectionsMutation: () => ({isPending: false, mutate: vi.fn()}),
    useRevokeConnectionAccessMutation: (options: MutationOptionsType) => {
        hoistedSharing.revokeOptions = options;

        return {mutate: vi.fn()};
    },
    useSetConnectionVisibilityMutation: (options: MutationOptionsType) => {
        hoistedSharing.visibilityOptions = options;

        return {mutate: vi.fn()};
    },
    useUnresolvedConnectionsQuery: () => ({data: undefined, isLoading: false}),
    useUsersQuery: () => ({data: undefined}),
    useWorkspaceUsersQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

const hoisted = vi.hoisted(() => ({
    deleteConnectionMutate: vi.fn(),
    disconnectConnectionMutate: vi.fn(),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({onClose}: {onClose: () => void}) => (
        <div data-testid="connection-dialog">
            <button onClick={onClose} type="button">
                Close connection dialog
            </button>
        </div>
    ),
}));

vi.mock('@/shared/mutations/automation/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useDeleteConnectionMutation: () => ({isPending: false, mutate: hoisted.deleteConnectionMutate}),
    useDisconnectConnectionMutation: () => ({isPending: false, mutate: hoisted.disconnectConnectionMutate}),
    useUpdateConnectionCredentialsMutation: vi.fn(),
    useUpdateConnectionMutation: vi.fn(),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => <div data-read-only={String(!!readOnly)} data-testid="tag-list" />,
}));

const inactiveConnection: Connection = {
    active: false,
    componentName: 'slack',
    connectionVersion: 1,
    credentialStatus: 'VALID',
    id: 7,
    name: 'My Slack',
    parameters: {},
};

const activeConnection: Connection = {...inactiveConnection, active: true};

const renderConnectionListItem = (connection: Connection) =>
    render(
        <TooltipProvider>
            <ul>
                <ConnectionListItem componentDefinitions={[]} connection={connection} />
            </ul>
        </TooltipProvider>
    );

const renderConnectionListItemWith = (connectionOverrides: Partial<Connection> = {}) =>
    render(
        <TooltipProvider>
            <ConnectionListItem
                componentDefinitions={[]}
                connection={
                    {
                        active: false,
                        componentName: 'slack',
                        credentialStatus: 'VALID',
                        id: 1,
                        name: 'Slack connection',
                        tags: [],
                        ...connectionOverrides,
                    } as Connection
                }
            />
        </TooltipProvider>
    );

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Connection actions'}));
};

const setTenantAdmin = (tenantAdmin: boolean) => {
    authenticationStore.setState({
        account: {authorities: tenantAdmin ? ['ROLE_ADMIN'] : ['ROLE_USER']} as never,
        authenticated: true,
    });
};

beforeEach(() => {
    hoistedScope.grantedScopes = [...ALL_SCOPES];

    hoistedSharing.connectionGrantsEnabled = [];
    hoistedSharing.visibilityEnabled = false;

    setTenantAdmin(true);

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ConnectionListItem', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderConnectionListItem(inactiveConnection);

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should open the edit dialog when clicking Edit', async () => {
        renderConnectionListItem(inactiveConnection);

        expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
    });

    it('should open the delete dialog and delete the connection when clicking Delete', async () => {
        renderConnectionListItem(inactiveConnection);

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        expect(within(deleteDialog).getByText('Are you absolutely sure?')).toBeInTheDocument();

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectionMutate).toHaveBeenCalledWith(7);
    });

    it('should hint that the connection can be deleted when it is not active', async () => {
        renderConnectionListItem(inactiveConnection);

        await openMenu();

        const deleteMenuItem = screen.getByRole('menuitem', {name: 'Delete'});

        expect(screen.getByTitle('Delete the connection')).toContainElement(deleteMenuItem);
        expect(deleteMenuItem).not.toHaveAttribute('data-disabled');
    });

    it('should not offer Disconnect from all when the connection is not active', async () => {
        renderConnectionListItem(inactiveConnection);

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Disconnect from all'})).not.toBeInTheDocument();
    });

    it('should open the disconnect dialog and disconnect the connection when clicking Disconnect from all', async () => {
        renderConnectionListItem(activeConnection);

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Disconnect from all'}));

        const disconnectDialog = await screen.findByRole('alertdialog');

        expect(within(disconnectDialog).getByText('Disconnect My Slack from all workflows?')).toBeInTheDocument();

        await userEvent.click(within(disconnectDialog).getByRole('button', {name: 'Disconnect from all'}));

        expect(hoisted.disconnectConnectionMutate).toHaveBeenCalledWith(7);
    });

    it('should disable the Delete row and explain why when the connection is active', async () => {
        renderConnectionListItem(activeConnection);

        await openMenu();

        const deleteMenuItem = screen.getByRole('menuitem', {name: 'Delete'});

        expect(deleteMenuItem).toHaveAttribute('data-disabled');
        expect(deleteMenuItem).toHaveAttribute('aria-disabled', 'true');
        expect(deleteMenuItem).toHaveClass('text-content-destructive-primary');

        // A click cannot prove the row is inert here: Radix still calls a consumer `onClick` on a disabled item and
        // jsdom applies no stylesheet, so clicking Delete would open the dialog in this test. In the browser it is
        // this base class that swallows the click, so the class is what keeps the delete dialog from opening.
        expect(deleteMenuItem).toHaveClass('data-[disabled]:pointer-events-none');

        // `pointer-events-none` also swallows the hover that shows a native tooltip, so the hint sits on the wrapper
        expect(screen.getByTitle('Disconnect from all workflows first to enable deletion')).toContainElement(
            deleteMenuItem
        );
    });
});

describe('ConnectionListItem scopes', () => {
    it('should show Edit, Disconnect from all and Delete for a tenant admin', async () => {
        const user = userEvent.setup();

        renderConnectionListItemWith({active: true});

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Disconnect from all'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should show Edit and Delete but hide Disconnect from all for an EDITOR owner who is not a tenant admin', async () => {
        hoistedScope.grantedScopes = EDITOR_SCOPES;

        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'owner@example.com'} as never,
            authenticated: true,
        });

        const user = userEvent.setup();

        renderConnectionListItemWith({active: true, createdBy: 'owner@example.com'});

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Disconnect from all'})).not.toBeInTheDocument();
    });

    it('should hide Edit and Delete from a VIEWER owner, who lacks the connection scopes', () => {
        hoistedScope.grantedScopes = VIEWER_SCOPES;

        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'owner@example.com'} as never,
            authenticated: true,
        });

        renderConnectionListItemWith({active: true, createdBy: 'owner@example.com'});

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');
        expect(screen.queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
    });

    it('should show Delete but hide Edit for an owner who holds only CONNECTION_DELETE', async () => {
        hoistedScope.grantedScopes = ['CONNECTION_DELETE'];

        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'owner@example.com'} as never,
            authenticated: true,
        });

        const user = userEvent.setup();

        renderConnectionListItemWith({active: true, createdBy: 'owner@example.com'});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Edit and Delete and make tags read-only for an EDITOR who is neither owner nor tenant admin', () => {
        hoistedScope.grantedScopes = EDITOR_SCOPES;

        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'member@example.com'} as never,
            authenticated: true,
        });

        renderConnectionListItemWith({active: true, createdBy: 'owner@example.com'});

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');
        expect(screen.queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
    });

    it('should hide the actions menu when no action is available', () => {
        setTenantAdmin(false);

        renderConnectionListItemWith({active: true});

        expect(screen.queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
    });
});

describe('ConnectionListItem sharing', () => {
    const setOwner = () => {
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'owner@example.com'} as never,
            authenticated: true,
        });
    };

    it('should refresh the grants list after granting or revoking access', () => {
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        renderConnectionListItemWith({createdBy: 'owner@example.com', visibility: 'PRIVATE'});

        hoistedSharing.grantOptions?.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['ConnectionGrants']});

        invalidateQueriesSpy.mockClear();

        hoistedSharing.revokeOptions?.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['ConnectionGrants']});

        invalidateQueriesSpy.mockClear();

        hoistedSharing.visibilityOptions?.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['ConnectionGrants']});

        invalidateQueriesSpy.mockRestore();
    });

    it('should not fetch grants or offer the visibility picker to a member who neither owns the connection nor is a tenant admin', () => {
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        renderConnectionListItemWith({createdBy: 'someone-else@example.com', visibility: 'PRIVATE'});

        expect(hoistedSharing.connectionGrantsEnabled).not.toContain(true);
        expect(screen.queryByRole('button', {name: 'Change visibility'})).not.toBeInTheDocument();
    });

    it('should fetch grants and offer the visibility picker to the owner', () => {
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        renderConnectionListItemWith({createdBy: 'owner@example.com', visibility: 'PRIVATE'});

        expect(hoistedSharing.connectionGrantsEnabled).toContain(true);
        expect(screen.getByRole('button', {name: 'Change visibility'})).toBeInTheDocument();
    });

    it('should load grants for a shared connection once the sharing menu is opened', async () => {
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        const user = userEvent.setup();

        renderConnectionListItemWith({createdBy: 'owner@example.com', visibility: 'WORKSPACE'});

        expect(hoistedSharing.connectionGrantsEnabled).not.toContain(true);

        await user.click(screen.getByRole('button', {name: 'Change visibility'}));

        expect(hoistedSharing.connectionGrantsEnabled).toContain(true);
    });

    it('should offer Reassign owner to a tenant admin for a connection pending reassignment', async () => {
        hoistedSharing.visibilityEnabled = true;

        const user = userEvent.setup();

        renderConnectionListItemWith({createdBy: 'removed@example.com', status: 'PENDING_REASSIGNMENT'});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: 'Reassign owner'})).toBeInTheDocument();
    });

    it('should not offer Reassign owner for an active connection', async () => {
        hoistedSharing.visibilityEnabled = true;

        const user = userEvent.setup();

        renderConnectionListItemWith({createdBy: 'owner@example.com', status: 'ACTIVE'});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Reassign owner'})).not.toBeInTheDocument();
    });
});
