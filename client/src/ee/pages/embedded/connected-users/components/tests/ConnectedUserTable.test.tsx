import {TooltipProvider} from '@/components/ui/tooltip';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserTable from '../ConnectedUserTable';

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: () => true,
}));

const hoisted = vi.hoisted(() => ({
    deleteConnectedUserMutate: vi.fn(),
    enableConnectedUserMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/connectedUsers.mutations', () => ({
    useDeleteConnectedUserMutation: () => ({mutate: hoisted.deleteConnectedUserMutate}),
    useEnableConnectedUserMutation: () => ({mutate: hoisted.enableConnectedUserMutate}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

const connectedUsers: ConnectedUser[] = [
    {enabled: true, externalId: 'external-1', id: 11, name: 'Ada Lovelace'},
    {enabled: false, externalId: 'external-2', id: 22, name: 'Grace Hopper'},
];

const renderConnectedUserTable = () =>
    render(
        <TooltipProvider>
            <ConnectedUserTable connectedUsers={connectedUsers} />
        </TooltipProvider>
    );

const openRowMenu = async (rowIndex: number) => {
    // The ellipsis triggers are the only buttons in the table, one per row
    await userEvent.click(screen.getAllByRole('button')[rowIndex]);
};

beforeEach(() => {
    windowResizeObserver();

    useConnectedUserSheetStore.setState({connectedUserId: 0, connectedUserSheetOpen: false});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ConnectedUserTable', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderConnectedUserTable();

        await openRowMenu(0);

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Details'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should open the connected user sheet when clicking Details', async () => {
        renderConnectedUserTable();

        await openRowMenu(1);

        await userEvent.click(screen.getByRole('menuitem', {name: 'Details'}));

        expect(useConnectedUserSheetStore.getState().connectedUserId).toBe(22);
        expect(useConnectedUserSheetStore.getState().connectedUserSheetOpen).toBe(true);
    });

    it('should disable an enabled connected user when clicking Disable', async () => {
        renderConnectedUserTable();

        await openRowMenu(0);

        expect(screen.queryByRole('menuitem', {name: 'Enable'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Disable'}));

        expect(hoisted.enableConnectedUserMutate).toHaveBeenCalledWith({enable: false, id: 11});
    });

    it('should enable a disabled connected user when clicking Enable', async () => {
        renderConnectedUserTable();

        await openRowMenu(1);

        expect(screen.queryByRole('menuitem', {name: 'Disable'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Enable'}));

        expect(hoisted.enableConnectedUserMutate).toHaveBeenCalledWith({enable: true, id: 22});
    });

    it('should open the delete dialog and delete the connected user when clicking Delete', async () => {
        renderConnectedUserTable();

        await openRowMenu(1);

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectedUserMutate).toHaveBeenCalledWith({id: 22});
    });
});
