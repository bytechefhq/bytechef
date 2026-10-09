import {McpComponent} from '@/shared/middleware/graphql';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentListItemDropdownMenu from './McpComponentListItemDropdownMenu';

const hoisted = vi.hoisted(() => ({
    deleteEmbeddedMutate: vi.fn(),
    deleteEmbeddedMutationOptions: undefined as {onSuccess: () => void} | undefined,
    deleteEmbeddedPending: false,
    deleteMutate: vi.fn(),
    deleteMutationOptions: undefined as {onSuccess: () => void} | undefined,
    deletePending: false,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteEmbeddedMcpComponentMutation: (options: {onSuccess: () => void}) => {
        hoisted.deleteEmbeddedMutationOptions = options;

        return {isPending: hoisted.deleteEmbeddedPending, mutate: hoisted.deleteEmbeddedMutate};
    },
    useDeleteMcpComponentMutation: (options: {onSuccess: () => void}) => {
        hoisted.deleteMutationOptions = options;

        return {isPending: hoisted.deletePending, mutate: hoisted.deleteMutate};
    },
}));

const mcpComponent = {componentName: 'gmail', componentVersion: 1, id: '7'} as McpComponent;

const openDeleteDialog = async () => {
    await userEvent.click(screen.getByRole('button'));
    await userEvent.click(screen.getByText('Delete'));
};

const confirmDelete = async () => {
    await openDeleteDialog();
    await userEvent.click(screen.getByRole('button', {name: 'Delete'}));
};

const renderWithQueryClient = (ui: ReactNode) => {
    const queryClient = new QueryClient();

    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);

    return invalidateQueriesSpy;
};

beforeEach(() => {
    windowResizeObserver();

    hoisted.deleteEmbeddedPending = false;
    hoisted.deletePending = false;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpComponentListItemDropdownMenu', () => {
    it('deletes an automation component through the shared mutation', async () => {
        render(<McpComponentListItemDropdownMenu mcpComponent={mcpComponent} onEditClick={vi.fn()} />);

        await confirmDelete();

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '7'});
        expect(hoisted.deleteEmbeddedMutate).not.toHaveBeenCalled();
    });

    it('deletes an embedded component through the embedded mutation', async () => {
        render(<McpComponentListItemDropdownMenu embedded mcpComponent={mcpComponent} onEditClick={vi.fn()} />);

        await confirmDelete();

        expect(hoisted.deleteEmbeddedMutate).toHaveBeenCalledWith({id: '7'});
        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
    });

    it('refreshes the automation components and closes the dialog after an automation component is deleted', async () => {
        const invalidateQueriesSpy = renderWithQueryClient(
            <McpComponentListItemDropdownMenu mcpComponent={mcpComponent} onEditClick={vi.fn()} />
        );

        await openDeleteDialog();

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();

        act(() => hoisted.deleteMutationOptions!.onSuccess());

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    });

    it('refreshes the embedded components and closes the dialog after an embedded component is deleted', async () => {
        const invalidateQueriesSpy = renderWithQueryClient(
            <McpComponentListItemDropdownMenu embedded mcpComponent={mcpComponent} onEditClick={vi.fn()} />
        );

        await openDeleteDialog();

        act(() => hoisted.deleteEmbeddedMutationOptions!.onSuccess());

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    });

    it('closes the dialog without deleting when the delete is cancelled', async () => {
        render(<McpComponentListItemDropdownMenu embedded mcpComponent={mcpComponent} onEditClick={vi.fn()} />);

        await openDeleteDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

        expect(hoisted.deleteEmbeddedMutate).not.toHaveBeenCalled();
        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
    });

    it('disables the confirm button while the embedded delete is pending', async () => {
        hoisted.deleteEmbeddedPending = true;

        render(<McpComponentListItemDropdownMenu embedded mcpComponent={mcpComponent} onEditClick={vi.fn()} />);

        await openDeleteDialog();

        expect(screen.getByRole('button', {name: 'Delete'})).toBeDisabled();
    });

    it('ignores the embedded pending state for an automation component', async () => {
        hoisted.deleteEmbeddedPending = true;

        render(<McpComponentListItemDropdownMenu mcpComponent={mcpComponent} onEditClick={vi.fn()} />);

        await openDeleteDialog();

        expect(screen.getByRole('button', {name: 'Delete'})).toBeEnabled();
    });
});
