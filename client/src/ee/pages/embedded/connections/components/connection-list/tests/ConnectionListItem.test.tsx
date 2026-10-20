import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

const hoisted = vi.hoisted(() => ({
    deleteConnectionMutate: vi.fn(),
}));

vi.mock('@/components/LazyLoadSVG/LazyLoadSVG', () => ({
    default: () => <span data-testid="component-icon" />,
}));

vi.mock('@/ee/shared/mutations/embedded/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useDeleteConnectionMutation: () => ({mutate: hoisted.deleteConnectionMutate}),
    useUpdateConnectionMutation: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
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

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
    useGetConnectionComponentDefinitionQuery: () => ({data: {icon: '<svg />', name: 'slack'}}),
}));

const connection: Connection = {
    componentName: 'slack',
    connectionVersion: 1,
    credentialStatus: 'VALID',
    id: 7,
    name: 'My Slack',
    parameters: {},
};

const renderConnectionListItem = () =>
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
        renderConnectionListItem();

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should open the edit dialog when clicking Edit', async () => {
        renderConnectionListItem();

        expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
    });

    it('should open the delete dialog and delete the connection when clicking Delete', async () => {
        renderConnectionListItem();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        expect(within(deleteDialog).getByText('Are you absolutely sure?')).toBeInTheDocument();

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectionMutate).toHaveBeenCalledWith(7);

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    });
});
