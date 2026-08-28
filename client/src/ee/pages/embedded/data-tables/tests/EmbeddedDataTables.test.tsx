import EmbeddedDataTables from '@/ee/pages/embedded/data-tables/EmbeddedDataTables';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {assignMutateMock, useEmbeddedDataTablesQueryMock} = vi.hoisted(() => ({
    assignMutateMock: vi.fn(),
    useEmbeddedDataTablesQueryMock: vi.fn(),
}));

// Partial, because EnvironmentSelect draws useEnvironmentsQuery from this same generated module. A full factory
// silently removes it and every test here fails inside a component it is not testing.
vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAssignEmbeddedDataTableOwnerMutation: () => ({mutate: assignMutateMock}),
    useEmbeddedDataTablesQuery: useEmbeddedDataTablesQueryMock,
}));

vi.mock('@/ee/pages/embedded/shared/components/useEmbeddedConnectedUsers', () => ({
    default: () => ({
        connectedUsers: [{externalId: 'account-42', id: 42}],
        isLoading: false,
    }),
}));

describe('EmbeddedDataTables', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useEmbeddedDataTablesQueryMock.mockReturnValue({
            data: {
                embeddedDataTables: [{baseName: 'conversations', description: 'Chat history', id: '7', ownerId: null}],
            },
            error: undefined,
            isLoading: false,
        });
    });

    it('lists the tables the vendor owns', async () => {
        render(<EmbeddedDataTables />);

        expect(await screen.findByText('conversations')).toBeInTheDocument();
    });

    it('asks for every owner until one is chosen', async () => {
        render(<EmbeddedDataTables />);

        await waitFor(() => {
            expect(useEmbeddedDataTablesQueryMock).toHaveBeenCalledWith(expect.objectContaining({ownerId: undefined}));
        });
    });

    it('shows an empty state rather than a blank page when there are no tables', async () => {
        useEmbeddedDataTablesQueryMock.mockReturnValue({
            data: {embeddedDataTables: []},
            error: undefined,
            isLoading: false,
        });

        render(<EmbeddedDataTables />);

        expect(await screen.findByText('No Data Tables')).toBeInTheDocument();
    });
});
