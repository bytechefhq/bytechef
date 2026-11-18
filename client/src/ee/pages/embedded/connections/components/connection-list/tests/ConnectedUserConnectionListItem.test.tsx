import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserConnectionListItem from '../ConnectedUserConnectionListItem';

const hoisted = vi.hoisted(() => ({
    deleteConnectedUserConnectionMutate: vi.fn(),
}));

vi.mock('@/components/LazyLoadSVG/LazyLoadSVG', () => ({
    default: () => <span data-testid="component-icon" />,
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useDeleteConnectedUserConnectionMutation: () => ({
        isPending: false,
        mutate: hoisted.deleteConnectedUserConnectionMutate,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionCredentialsDialog',
    () => ({
        default: () => <div data-testid="connection-credentials-dialog" />,
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationCredentialsDialog',
    () => ({
        default: () => <div data-testid="integration-credentials-dialog" />,
    })
);

const createConnection = (origin: ConnectionOriginEnum): Connection => ({
    componentName: 'gmail',
    connectionVersion: 1,
    credentialStatus: 'VALID',
    id: 9,
    name: 'Acme Gmail',
    origin,
    parameters: {},
    tags: [{id: 3, name: 'sales'}],
});

const renderItem = (connection: Connection, provisionedIntegrationInstance?: ConnectedUserIntegrationInstance) =>
    render(
        <TooltipProvider>
            <ul>
                <ConnectedUserConnectionListItem
                    componentDefinitions={[]}
                    connectedUserId={42}
                    connection={connection}
                    provisionedIntegrationInstance={provisionedIntegrationInstance}
                />
            </ul>
        </TooltipProvider>
    );

describe('ConnectedUserConnectionListItem', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows a self-created connection read-only', () => {
        renderItem(createConnection(ConnectionOriginEnum.SelfServe));

        expect(screen.getByText('Self-created')).toBeInTheDocument();
        expect(screen.getByText('sales')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
        expect(screen.queryByText('Add tag')).not.toBeInTheDocument();
    });

    it('renders no tag row for a connection without tags', () => {
        renderItem({...createConnection(ConnectionOriginEnum.SelfServe), tags: []});

        expect(screen.queryByTestId('connection-list-item-tags')).not.toBeInTheDocument();
    });

    it('lets an admin edit and delete a provisioned connection', async () => {
        renderItem(createConnection(ConnectionOriginEnum.Provisioned));

        expect(screen.getByText('Provisioned')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button'));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectedUserConnectionMutate).toHaveBeenCalledWith(9);
    });

    it('opens the connection credentials dialog for a provisioned connection', async () => {
        renderItem(createConnection(ConnectionOriginEnum.Provisioned));

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('connection-credentials-dialog')).toBeInTheDocument();
    });

    it('lets an admin only edit a self-created connection a provisioned integration instance uses', async () => {
        renderItem(createConnection(ConnectionOriginEnum.SelfServe), {
            connectionId: 9,
            provisioned: true,
        } as ConnectedUserIntegrationInstance);

        await userEvent.click(screen.getByRole('button'));

        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('integration-credentials-dialog')).toBeInTheDocument();
    });
});
