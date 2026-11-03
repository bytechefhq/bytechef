import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUsers from '../ConnectedUsers';

const hoisted = vi.hoisted(() => ({
    content: [] as object[],
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    ConnectedUserKeys: {connectedUsers: ['connectedUsers']},
    useGetConnectedUsersQuery: () => ({
        data: {
            content: hoisted.content,
            size: 20,
            totalElements: hoisted.content.length,
            totalPages: 1,
        },
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/mutations/embedded/connectedUsers.mutations', () => ({
    useDeleteConnectedUserMutation: () => ({mutate: vi.fn()}),
    useEnableConnectedUserMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheet', () => ({
    default: () => null,
}));

const createConnectedUsersTree = () => (
    <MemoryRouter initialEntries={['/embedded/connected-users']}>
        <TooltipProvider>
            <ConnectedUsers />
        </TooltipProvider>
    </MemoryRouter>
);

const renderConnectedUsers = () => render(createConnectedUsersTree());

beforeEach(() => {
    windowResizeObserver();

    hoisted.content = [
        {enabled: true, externalId: 'external-1', id: 11, name: 'Ada Lovelace'},
        {enabled: true, externalId: 'external-2', id: 22, name: 'Grace Hopper'},
    ];

    environmentStore.setState({currentEnvironmentId: 1});
});

afterEach(() => {
    resetAll();
});

describe('ConnectedUsers', () => {
    it('should show the selection bar for the selected rows', async () => {
        renderConnectedUsers();

        expect(screen.queryByText(/selected$/)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('checkbox', {name: 'Select all'}));

        expect(screen.getByText('2 selected')).toBeInTheDocument();
    });

    it('should clear the selection when the filter changes', async () => {
        renderConnectedUsers();

        await userEvent.click(screen.getByRole('checkbox', {name: 'Select all'}));

        expect(screen.getByText('2 selected')).toBeInTheDocument();

        await userEvent.type(screen.getByPlaceholderText('Name, Email or User ERC'), 'ada{Enter}');

        await waitFor(() => {
            expect(screen.queryByText('2 selected')).not.toBeInTheDocument();
        });
    });

    it('should clear the selection when the environment changes', async () => {
        renderConnectedUsers();

        await userEvent.click(screen.getByRole('checkbox', {name: 'Select all'}));

        expect(screen.getByText('2 selected')).toBeInTheDocument();

        act(() => {
            environmentStore.setState({currentEnvironmentId: 2});
        });

        await waitFor(() => {
            expect(screen.queryByText('2 selected')).not.toBeInTheDocument();
        });
    });

    it('should drop a connected user that leaves the list from the selection', async () => {
        const {rerender} = renderConnectedUsers();

        await userEvent.click(screen.getByRole('checkbox', {name: 'Select all'}));

        expect(screen.getByText('2 selected')).toBeInTheDocument();

        hoisted.content = [hoisted.content[0]];

        rerender(createConnectedUsersTree());

        expect(screen.getByText('1 selected')).toBeInTheDocument();
    });
});
