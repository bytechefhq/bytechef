import {Connection} from '@/shared/middleware/automation/configuration';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionProjectShareDialog from './ConnectionProjectShareDialog';

// Mock the GraphQL mutation so we can control success / failure per test.
const mockMutateAsync = vi.fn();
const mockInvalidateQueries = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useSetConnectionProjectsMutation: () => ({
        mutateAsync: (...args: unknown[]) => mockMutateAsync(...args),
    }),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({
            invalidateQueries: mockInvalidateQueries,
        }),
    };
});

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({
        data: [
            {id: 100, name: 'Alpha Project'},
            {id: 200, name: 'Beta Project'},
        ],
        isLoading: false,
    }),
}));

vi.mock('sonner', () => ({
    toast: vi.fn(),
}));

const renderDialog = (overrides: Partial<Connection> = {}) => {
    const connection: Connection = {
        componentName: 'gmail',
        id: 10,
        name: 'My Gmail',
        sharedProjectIds: [100],
        ...overrides,
    } as Connection;

    const onClose = vi.fn();

    render(<ConnectionProjectShareDialog connection={connection} onClose={onClose} open={true} workspaceId={1} />);

    return {connection, onClose};
};

describe('ConnectionProjectShareDialog', () => {
    beforeEach(() => {
        mockMutateAsync.mockReset();
        mockInvalidateQueries.mockReset();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('invokes setConnectionProjects with the selected project ids', async () => {
        mockMutateAsync.mockResolvedValue(undefined);

        const user = userEvent.setup();
        const {onClose} = renderDialog();

        // Select the second project (not initially shared) — this creates a diff that the
        // mutation must receive.
        await user.click(screen.getByLabelText('Beta Project'));

        await user.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        // Both ids should be sent — 100 (kept) and 200 (added).
        const [payload] = mockMutateAsync.mock.calls[0];

        expect(payload).toMatchObject({
            connectionId: '10',
            workspaceId: '1',
        });

        const projectIds = (payload as {projectIds: string[]}).projectIds;

        expect(projectIds).toHaveLength(2);
        expect(projectIds).toEqual(expect.arrayContaining(['100', '200']));

        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    it('does not close when the mutation rejects (useFetchInterceptor surfaces the toast)', async () => {
        mockMutateAsync.mockRejectedValue(new Error('GraphQL: unauthorized'));

        const user = userEvent.setup();
        const {onClose} = renderDialog();

        await user.click(screen.getByLabelText('Beta Project'));

        await user.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalled());

        expect(onClose).not.toHaveBeenCalled();

        expect(mockInvalidateQueries).not.toHaveBeenCalled();
    });

    it('short-circuits without calling the mutation when the selection is unchanged', async () => {
        const user = userEvent.setup();
        const {onClose} = renderDialog();

        // No selection change — just hit save.
        await user.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => expect(onClose).toHaveBeenCalled());

        expect(mockMutateAsync).not.toHaveBeenCalled();
    });
});
