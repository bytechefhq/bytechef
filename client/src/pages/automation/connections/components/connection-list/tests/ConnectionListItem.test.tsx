import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

const ALL_SCOPES = ['CONNECTION_EDIT', 'CONNECTION_DELETE'];

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

const hoisted = vi.hoisted(() => ({
    authenticationState: {
        account: {authorities: ['ROLE_USER'], login: 'owner@localhost.com'},
        authenticated: true,
    },
    deleteConnectionMutate: vi.fn(),
    disconnectConnectionMutate: vi.fn(),
    updateConnectionCredentialsMutation: vi.fn(),
    updateConnectionTagsMutate: vi.fn(),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({
        onClose,
        useUpdateConnectionCredentialsMutation,
    }: {
        onClose: () => void;
        useUpdateConnectionCredentialsMutation?: unknown;
    }) => (
        <div
            data-credentials-replaceable={
                useUpdateConnectionCredentialsMutation === hoisted.updateConnectionCredentialsMutation
            }
            data-testid="connection-dialog"
        >
            <button onClick={onClose} type="button">
                Close connection dialog
            </button>
        </div>
    ),
}));

vi.mock('@/shared/mutations/automation/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: hoisted.updateConnectionTagsMutate}),
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useDeleteConnectionMutation: () => ({isPending: false, mutate: hoisted.deleteConnectionMutate}),
    useDisconnectConnectionMutation: () => ({isPending: false, mutate: hoisted.disconnectConnectionMutate}),
    useUpdateConnectionCredentialsMutation: hoisted.updateConnectionCredentialsMutation,
    useUpdateConnectionMutation: vi.fn(),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: (selector: (state: typeof hoisted.authenticationState) => unknown) =>
        selector(hoisted.authenticationState),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
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
    hoisted.authenticationState = {
        account: {authorities: tenantAdmin ? ['ROLE_ADMIN'] : ['ROLE_USER'], login: 'owner@localhost.com'},
        authenticated: true,
    };
};

const openEditDialog = async () => {
    await openMenu();

    await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));
};

beforeEach(() => {
    hoistedScope.grantedScopes = [...ALL_SCOPES];

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

    it('should let the owner replace the connection credentials', async () => {
        setTenantAdmin(false);

        renderConnectionListItem({...inactiveConnection, createdBy: 'owner@localhost.com'});

        await openEditDialog();

        expect(screen.getByTestId('connection-dialog')).toHaveAttribute('data-credentials-replaceable', 'true');
    });

    it('should let a tenant admin replace the credentials of a connection someone else created', async () => {
        hoisted.authenticationState = {
            account: {authorities: ['ROLE_ADMIN'], login: 'admin@localhost.com'},
            authenticated: true,
        };

        renderConnectionListItem({...inactiveConnection, createdBy: 'owner@localhost.com'});

        await openEditDialog();

        expect(screen.getByTestId('connection-dialog')).toHaveAttribute('data-credentials-replaceable', 'true');
    });

    it('should not let another user replace the connection credentials', async () => {
        setTenantAdmin(false);

        renderConnectionListItem({...inactiveConnection, createdBy: 'someone-else@localhost.com'});

        await openEditDialog();

        expect(screen.getByTestId('connection-dialog')).toHaveAttribute('data-credentials-replaceable', 'false');
    });

    it('should not let anyone but a tenant admin replace the credentials of a connection without a creator', async () => {
        setTenantAdmin(false);

        renderConnectionListItem(inactiveConnection);

        await openEditDialog();

        expect(screen.getByTestId('connection-dialog')).toHaveAttribute('data-credentials-replaceable', 'false');
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

    it('should update the connection tags when removing a tag', async () => {
        renderConnectionListItem({
            ...inactiveConnection,
            tags: [
                {id: 1, name: 'first'},
                {id: 2, name: 'second'},
            ],
        });

        await userEvent.click(screen.getAllByRole('button', {name: 'Remove tag'})[0]);

        expect(hoisted.updateConnectionTagsMutate).toHaveBeenCalledWith({
            id: 7,
            updateTagsRequest: {tags: [{id: 2, name: 'second'}]},
        });
    });
});

describe('ConnectionListItem scopes', () => {
    it('should show Edit, Disconnect from all and Delete for a tenant admin', async () => {
        const user = userEvent.setup();

        renderConnectionListItemWith({active: true});

        expect(screen.getByRole('button', {name: 'Add tag'})).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Disconnect from all'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Disconnect from all for a member who is not a tenant admin', async () => {
        setTenantAdmin(false);

        const user = userEvent.setup();

        renderConnectionListItemWith({active: true});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Disconnect from all'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    });

    it('should hide Edit and make tags read-only without CONNECTION_EDIT', async () => {
        hoistedScope.grantedScopes = ['CONNECTION_DELETE'];

        const user = userEvent.setup();

        renderConnectionListItemWith();

        expect(screen.queryByRole('button', {name: 'Add tag'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Delete without CONNECTION_DELETE', async () => {
        hoistedScope.grantedScopes = ['CONNECTION_EDIT'];

        const user = userEvent.setup();

        renderConnectionListItemWith();

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    });

    it('should hide the actions menu when no action is available', () => {
        hoistedScope.grantedScopes = [];

        setTenantAdmin(false);

        renderConnectionListItemWith({active: true});

        expect(screen.queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
    });
});
