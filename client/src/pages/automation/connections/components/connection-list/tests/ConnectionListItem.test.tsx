import {Connection} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

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
    useUpdateConnectionMutation: vi.fn(),
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
        <ul>
            <ConnectionListItem componentDefinitions={[]} connection={connection} />
        </ul>
    );

const openMenu = async () => {
    // The ellipsis trigger has no accessible name and the item renders no other button (no tags, dialogs closed)
    await userEvent.click(screen.getByRole('button'));
};

beforeEach(() => {
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
