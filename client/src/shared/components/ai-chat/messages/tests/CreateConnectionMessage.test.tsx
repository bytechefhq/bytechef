import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: vi.fn(),
}));

// `useGetComponentDefinitionQuery` (singular) opens the dialog pre-selected on the requested
// component. Mocked as a no-op for tests that don't exercise pre-selection.
vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: vi.fn(() => ({data: undefined})),
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useCreateConnectionMutation: vi.fn(),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {
        connectionTags: (id: number) => ['connectionTags', id],
        connections: ['connections'],
    },
    useGetConnectionTagsQuery: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 1),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 0),
}));

// Mock useQueryClient so the component can call `invalidateQueries` without a real QueryClientProvider.
vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: vi.fn(() => ({
            invalidateQueries: vi.fn().mockResolvedValue(undefined),
        })),
    };
});

// Mock the dialog so we can trigger onConnectionCreate from a test-only button.
vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({onClose, onConnectionCreate}: {onClose?: () => void; onConnectionCreate?: (id: number) => void}) => (
        <div data-testid="connection-dialog">
            <button onClick={onClose} type="button">
                Close
            </button>

            <button onClick={() => onConnectionCreate?.(42)} type="button">
                Simulate Create
            </button>
        </div>
    ),
}));

const {useGetComponentDefinitionsQuery} = await import('@/shared/queries/automation/componentDefinitions.queries');
const mockUseGetComponentDefinitionsQuery = vi.mocked(useGetComponentDefinitionsQuery);

const COMPONENT_DEFINITIONS_FIXTURE = [
    {name: 'slack', title: 'Slack'},
    {name: 'gmail', title: 'Gmail'},
];

const DEFAULT_DATA = {
    componentLabel: 'Slack',
    componentName: 'slack',
    kind: 'create-connection' as const,
};

const renderMessage = async (data = DEFAULT_DATA) => {
    const {default: CreateConnectionMessage} = await import('../CreateConnectionMessage');

    return render(
        <CreateConnectionMessage data={data} name="create-connection" status={{type: 'complete'}} type="data" />
    );
};

describe('CreateConnectionMessage', () => {
    beforeEach(() => {
        mockUseGetComponentDefinitionsQuery.mockReturnValue({
            data: COMPONENT_DEFINITIONS_FIXTURE,
            isLoading: false,
        } as never);
    });

    it('renders a Connect button with the component label', async () => {
        // The component is intentionally narrowly scoped to "create new connection" — selecting from
        // existing connections is handled by selectConnection (dropdown UX). One unambiguous button
        // replaces the prior select + plus-button layout that conflated the two intents.
        await renderMessage();

        expect(screen.getByRole('button', {name: /connect slack/i})).toBeInTheDocument();
    });

    it('opens the ConnectionDialog when the Connect button is clicked', async () => {
        await renderMessage();

        await userEvent.click(screen.getByRole('button', {name: /connect slack/i}));

        await waitFor(() => {
            expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
        });
    });

    it('closes the ConnectionDialog when onClose is called', async () => {
        await renderMessage();

        await userEvent.click(screen.getByRole('button', {name: /connect slack/i}));

        await waitFor(() => screen.getByTestId('connection-dialog'));

        await userEvent.click(screen.getByRole('button', {name: /^close$/i}));

        await waitFor(() => {
            expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();
        });
    });

    it('renders null while component definitions are loading', async () => {
        // Showing the button before component definitions are available would open a malformed dialog
        // (no pre-selected component). Render nothing until the catalog lands.
        mockUseGetComponentDefinitionsQuery.mockReturnValue({
            data: undefined,
            isLoading: true,
        } as never);

        const {container} = await renderMessage();

        expect(container.firstChild).toBeNull();
    });

    it('renders an error + Retry button when the component-definitions endpoint fails', async () => {
        // Without this surface, a flaky catalog endpoint silently disables the connect flow and the user
        // has no idea the agent's tool call ever fired.
        const refetch = vi.fn();

        mockUseGetComponentDefinitionsQuery.mockReturnValue({
            data: undefined,
            error: new Error('500: catalog down'),
            isError: true,
            isLoading: false,
            refetch,
        } as never);

        await renderMessage();

        expect(screen.getByText(/could not load component definitions/i)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /retry/i}));

        expect(refetch).toHaveBeenCalledOnce();
    });

    it('shows a "Connection ready" confirmation after the dialog reports a successful create', async () => {
        // End-to-end happy path: button → dialog → simulate create → confirmation row replaces the button.
        await renderMessage();

        await userEvent.click(screen.getByRole('button', {name: /connect slack/i}));

        await waitFor(() => screen.getByTestId('connection-dialog'));

        await userEvent.click(screen.getByRole('button', {name: /simulate create/i}));

        await waitFor(() => {
            expect(screen.getByText(/connection ready/i)).toBeInTheDocument();
            expect(screen.getByText(/slack connection/i)).toBeInTheDocument();
        });

        // The Connect button is replaced by the confirmation.
        expect(screen.queryByRole('button', {name: /connect slack/i})).not.toBeInTheDocument();
    });
});
