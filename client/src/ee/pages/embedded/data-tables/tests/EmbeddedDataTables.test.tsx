import EmbeddedDataTables from '@/ee/pages/embedded/data-tables/EmbeddedDataTables';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {assignMutateMock, useDataTablesMock} = vi.hoisted(() => ({
    assignMutateMock: vi.fn(),
    useDataTablesMock: vi.fn(),
}));

// Partial, because EnvironmentSelect draws useEnvironmentsQuery from this same generated module. A full factory
// silently removes it and every test here fails inside a component it is not testing.
vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAssignEmbeddedDataTableOwnerMutation: () => ({mutate: assignMutateMock}),
}));

vi.mock('@/shared/components/data-tables/components/hooks/useDataTables', () => ({
    default: useDataTablesMock,
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

        useDataTablesMock.mockReturnValue({
            allTags: [],
            error: undefined,
            filteredTables: [],
            isLoading: false,
            tables: [{baseName: 'conversations', columns: [], description: 'Chat history', id: '7', ownerId: null}],
            tagId: undefined,
            tagsByTableData: [],
        });
    });

    it('lists the tables the vendor owns', async () => {
        render(<EmbeddedDataTables />);

        expect(await screen.findByText('conversations')).toBeInTheDocument();
    });

    it('asks the shared hook for an embedded scope with every owner until one is chosen', async () => {
        render(<EmbeddedDataTables />);

        await waitFor(() => {
            expect(useDataTablesMock).toHaveBeenCalledWith({ownerId: undefined, type: 'EMBEDDED'});
        });
    });

    it('shows an empty state rather than a blank page when there are no tables', async () => {
        useDataTablesMock.mockReturnValue({
            allTags: [],
            error: undefined,
            filteredTables: [],
            isLoading: false,
            tables: [],
            tagId: undefined,
            tagsByTableData: [],
        });

        render(<EmbeddedDataTables />);

        expect(await screen.findByText('No Data Tables')).toBeInTheDocument();
    });
});
