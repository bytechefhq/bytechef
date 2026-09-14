import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection} from '@/shared/middleware/automation/configuration';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

const ALL_SCOPES = ['CONNECTION_EDIT', 'CONNECTION_DELETE'];

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

type MutationOptionsType = {onSuccess?: () => void};

const hoistedSharing = vi.hoisted(() => ({
    connectionGrantsEnabled: [] as boolean[],
    grantOptions: undefined as MutationOptionsType | undefined,
    revokeOptions: undefined as MutationOptionsType | undefined,
    visibilityEnabled: false,
    visibilityOptions: undefined as MutationOptionsType | undefined,
}));

vi.mock('@/pages/automation/connections/hooks/useVisibilityFeatureEnabled', () => ({
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

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useDeleteConnectionMutation: () => ({mutate: vi.fn()}),
    useDisconnectConnectionMutation: () => ({mutate: vi.fn()}),
    useUpdateConnectionMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => <div data-read-only={String(!!readOnly)} data-testid="tag-list" />,
}));

const renderConnectionListItem = (connectionOverrides: Partial<Connection> = {}) =>
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
});

describe('ConnectionListItem', () => {
    it('should show Edit, Disconnect from all and Delete for a tenant admin', async () => {
        const user = userEvent.setup();

        renderConnectionListItem({active: true});

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Disconnect from all'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Disconnect from all for a member who is not a tenant admin', async () => {
        setTenantAdmin(false);

        const user = userEvent.setup();

        renderConnectionListItem({active: true});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Disconnect from all'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    });

    it('should hide Edit and make tags read-only without CONNECTION_EDIT', async () => {
        hoistedScope.grantedScopes = ['CONNECTION_DELETE'];

        const user = userEvent.setup();

        renderConnectionListItem();

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Delete without CONNECTION_DELETE', async () => {
        hoistedScope.grantedScopes = ['CONNECTION_EDIT'];

        const user = userEvent.setup();

        renderConnectionListItem();

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    });

    it('should hide the actions menu when no action is available', () => {
        hoistedScope.grantedScopes = [];

        setTenantAdmin(false);

        renderConnectionListItem({active: true});

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

        renderConnectionListItem({createdBy: 'owner@example.com', visibility: 'PRIVATE'});

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

    it('should not fetch grants or offer the visibility picker to a member who neither owns the connection nor holds CONNECTION_EDIT', () => {
        hoistedScope.grantedScopes = [];
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        renderConnectionListItem({createdBy: 'someone-else@example.com', visibility: 'PRIVATE'});

        expect(hoistedSharing.connectionGrantsEnabled).not.toContain(true);
        expect(screen.queryByRole('button', {name: 'Change visibility'})).not.toBeInTheDocument();
    });

    it('should fetch grants and offer the visibility picker to the owner', () => {
        hoistedScope.grantedScopes = [];
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        renderConnectionListItem({createdBy: 'owner@example.com', visibility: 'PRIVATE'});

        expect(hoistedSharing.connectionGrantsEnabled).toContain(true);
        expect(screen.getByRole('button', {name: 'Change visibility'})).toBeInTheDocument();
    });

    it('should load grants for a shared connection once the sharing menu is opened', async () => {
        hoistedScope.grantedScopes = [];
        hoistedSharing.visibilityEnabled = true;

        setOwner();

        const user = userEvent.setup();

        renderConnectionListItem({createdBy: 'owner@example.com', visibility: 'WORKSPACE'});

        expect(hoistedSharing.connectionGrantsEnabled).not.toContain(true);

        await user.click(screen.getByRole('button', {name: 'Change visibility'}));

        expect(hoistedSharing.connectionGrantsEnabled).toContain(true);
    });

    it('should offer Reassign owner to a tenant admin for a connection pending reassignment', async () => {
        hoistedSharing.visibilityEnabled = true;

        const user = userEvent.setup();

        renderConnectionListItem({createdBy: 'removed@example.com', status: 'PENDING_REASSIGNMENT'});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: 'Reassign owner'})).toBeInTheDocument();
    });

    it('should not offer Reassign owner for an active connection', async () => {
        hoistedSharing.visibilityEnabled = true;

        const user = userEvent.setup();

        renderConnectionListItem({createdBy: 'owner@example.com', status: 'ACTIVE'});

        await user.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Reassign owner'})).not.toBeInTheDocument();
    });
});
