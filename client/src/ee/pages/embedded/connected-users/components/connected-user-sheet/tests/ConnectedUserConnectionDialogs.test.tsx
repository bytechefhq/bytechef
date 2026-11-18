import ConnectedUserConnectionDialogs from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionDialogs';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    deleteConnectedUserConnectionMutate: vi.fn(),
    useDeleteConnectedUserConnectionMutation: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useDeleteConnectedUserConnectionMutation: hoisted.useDeleteConnectedUserConnectionMutation,
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

const connection: Connection = {
    componentName: 'gmail',
    connectionVersion: 1,
    id: 9,
    name: 'Acme Gmail',
    origin: ConnectionOriginEnum.Provisioned,
    parameters: {},
};

const renderDialogs = (
    overrides: Partial<{
        deletable: boolean;
        onCredentialsDialogClose: () => void;
        onDeleteDialogClose: () => void;
        provisionedIntegrationInstance: ConnectedUserIntegrationInstance;
        showCredentialsDialog: boolean;
        showDeleteDialog: boolean;
    }> = {}
) =>
    render(
        <ConnectedUserConnectionDialogs
            connectedUserId={42}
            connection={connection}
            deletable={false}
            onCredentialsDialogClose={vi.fn()}
            onDeleteDialogClose={vi.fn()}
            showCredentialsDialog={false}
            showDeleteDialog={false}
            {...overrides}
        />
    );

describe('ConnectedUserConnectionDialogs', () => {
    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('deletes the connection for the connected user when the deletion is confirmed', async () => {
        hoisted.useDeleteConnectedUserConnectionMutation.mockReturnValue({
            isPending: false,
            mutate: hoisted.deleteConnectedUserConnectionMutate,
        });

        renderDialogs({deletable: true, showDeleteDialog: true});

        expect(screen.getByText(/permanently delete the connection Acme Gmail/)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.useDeleteConnectedUserConnectionMutation).toHaveBeenCalledWith(42, expect.any(Object));
        expect(hoisted.deleteConnectedUserConnectionMutate).toHaveBeenCalledWith(9);
    });

    it('closes the delete dialog when the deletion fails', () => {
        const onDeleteDialogClose = vi.fn();

        hoisted.useDeleteConnectedUserConnectionMutation.mockReturnValue({isPending: false, mutate: vi.fn()});

        renderDialogs({deletable: true, onDeleteDialogClose, showDeleteDialog: true});

        hoisted.useDeleteConnectedUserConnectionMutation.mock.calls[0][1].onError();

        expect(onDeleteDialogClose).toHaveBeenCalled();
    });

    it('renders no delete dialog when the connection is not deletable', () => {
        hoisted.useDeleteConnectedUserConnectionMutation.mockReturnValue({isPending: false, mutate: vi.fn()});

        renderDialogs({showDeleteDialog: true});

        expect(screen.queryByText(/permanently delete the connection/)).not.toBeInTheDocument();
    });

    it("edits the integration's credentials when the connection was provisioned for an integration instance", () => {
        hoisted.useDeleteConnectedUserConnectionMutation.mockReturnValue({isPending: false, mutate: vi.fn()});

        renderDialogs({
            provisionedIntegrationInstance: {id: 5} as ConnectedUserIntegrationInstance,
            showCredentialsDialog: true,
        });

        expect(screen.getByTestId('integration-credentials-dialog')).toBeInTheDocument();
        expect(screen.queryByTestId('connection-credentials-dialog')).not.toBeInTheDocument();
    });

    it("edits the connection's own credentials otherwise", () => {
        hoisted.useDeleteConnectedUserConnectionMutation.mockReturnValue({isPending: false, mutate: vi.fn()});

        renderDialogs({showCredentialsDialog: true});

        expect(screen.getByTestId('connection-credentials-dialog')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-credentials-dialog')).not.toBeInTheDocument();
    });

    it('renders no credentials dialog while it is closed', () => {
        hoisted.useDeleteConnectedUserConnectionMutation.mockReturnValue({isPending: false, mutate: vi.fn()});

        renderDialogs();

        expect(screen.queryByTestId('connection-credentials-dialog')).not.toBeInTheDocument();
        expect(screen.queryByTestId('integration-credentials-dialog')).not.toBeInTheDocument();
    });
});
