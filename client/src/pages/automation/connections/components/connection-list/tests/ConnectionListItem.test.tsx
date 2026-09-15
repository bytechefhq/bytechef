import {Connection} from '@/shared/middleware/automation/configuration';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

const ALL_SCOPES = ['CONNECTION_EDIT', 'CONNECTION_DELETE'];

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

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
    );

const setTenantAdmin = (tenantAdmin: boolean) => {
    authenticationStore.setState({
        account: {authorities: tenantAdmin ? ['ROLE_ADMIN'] : ['ROLE_USER']} as never,
        authenticated: true,
    });
};

beforeEach(() => {
    hoistedScope.grantedScopes = [...ALL_SCOPES];

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
