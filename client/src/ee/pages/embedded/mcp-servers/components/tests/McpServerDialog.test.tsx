import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerDialog from '../McpServerDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useCreateEmbeddedMcpServerMutation: () => ({mutate: hoisted.createMutate}),
    useUpdateEmbeddedMcpServerMutation: () => ({mutate: hoisted.updateMutate}),
}));

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerDialog', () => {
    it('saves an edited server through the embedded update mutation', async () => {
        const mcpServer = {enabled: true, id: '5', name: 'Server'} as McpServer;

        render(<McpServerDialog mcpServer={mcpServer} open triggerNode={<button type="button">open</button>} />);

        const nameInput = screen.getByPlaceholderText('Enter server name');

        await userEvent.clear(nameInput);
        await userEvent.type(nameInput, 'Renamed');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateMutate).toHaveBeenCalledWith(
                {id: '5', input: {enabled: true, name: 'Renamed'}},
                expect.objectContaining({onSuccess: expect.any(Function)})
            )
        );
        expect(hoisted.createMutate).not.toHaveBeenCalled();
    });
});
