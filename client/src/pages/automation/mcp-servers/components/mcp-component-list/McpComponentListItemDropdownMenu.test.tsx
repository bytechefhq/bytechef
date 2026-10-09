import {McpComponent} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentListItemDropdownMenu from './McpComponentListItemDropdownMenu';

const hoisted = vi.hoisted(() => ({
    deleteEmbeddedMutate: vi.fn(),
    deleteMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteEmbeddedMcpComponentMutation: () => ({isPending: false, mutate: hoisted.deleteEmbeddedMutate}),
    useDeleteMcpComponentMutation: () => ({isPending: false, mutate: hoisted.deleteMutate}),
}));

const mcpComponent = {componentName: 'gmail', componentVersion: 1, id: '7'} as McpComponent;

const confirmDelete = async () => {
    await userEvent.click(screen.getByRole('button'));
    await userEvent.click(screen.getByText('Delete'));
    await userEvent.click(screen.getByRole('button', {name: 'Delete'}));
};

beforeEach(() => {
    windowResizeObserver();
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
});
