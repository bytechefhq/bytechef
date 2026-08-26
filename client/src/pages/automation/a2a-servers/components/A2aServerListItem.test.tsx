import {A2aServer} from '@/shared/middleware/graphql';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerListItem from './A2aServerListItem';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants - vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
    edition: 'EE',
    environmentsResult: {
        data: {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
            ],
        },
    } as {
        data: {environments: {id: string; name: string}[]} | undefined;
    },
    invalidateQueriesMock: vi.fn(),
    promotionDialogProps: [] as unknown[],
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueriesMock}),
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDeleteA2aServerMutation: () => ({mutate: vi.fn()}),
        useEnvironmentsQuery: () => hoisted.environmentsResult,
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuItem: ({children, onClick}: {children: React.ReactNode; onClick?: () => void}) => (
        <button onClick={onClick} type="button">
            {children}
        </button>
    ),
    DropdownMenuSeparator: () => <hr />,
    DropdownMenuTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('./A2aServerDialog', () => ({default: () => null}));

vi.mock('./A2aServerWorkflowDialog', () => ({default: () => null}));

vi.mock('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.promotionDialogProps.push(props);

        return (
            <div data-testid="environment-promotion-dialog">
                <button onClick={props.onClose as () => void} type="button">
                    close
                </button>

                <button onClick={() => (props.onPromoted as (result: unknown) => void)?.({})} type="button">
                    promote
                </button>
            </div>
        );
    },
}));

const a2aServer = {
    authenticationRequired: false,
    enabled: true,
    environmentId: '1',
    id: 'a1',
    name: 'My A2A Server',
    secretKey: 'secret',
} as unknown as A2aServer;

describe('A2aServerListItem', () => {
    beforeEach(() => {
        hoisted.edition = 'EE';
        hoisted.invalidateQueriesMock.mockReset();
        hoisted.promotionDialogProps.length = 0;
        hoisted.environmentsResult.data = {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
            ],
        };
    });

    it('hides the Promote to environment menu item when fewer than two environments exist', () => {
        hoisted.environmentsResult.data = {environments: [{id: '0', name: 'Development'}]};

        render(<A2aServerListItem a2aServer={a2aServer} />);

        expect(screen.queryByText('Promote to environment…')).not.toBeInTheDocument();
    });

    it('hides the Promote to environment menu item on CE even with multiple environments', () => {
        hoisted.edition = 'CE';

        render(<A2aServerListItem a2aServer={a2aServer} />);

        expect(screen.queryByText('Promote to environment…')).not.toBeInTheDocument();
    });

    it('shows the Promote to environment menu item on EE with multiple environments', () => {
        render(<A2aServerListItem a2aServer={a2aServer} />);

        expect(screen.getByText('Promote to environment…')).toBeInTheDocument();
    });

    it('mounts the dialog on click and unmounts it on close, passing the correct resourceType and ids', async () => {
        const user = userEvent.setup();

        render(<A2aServerListItem a2aServer={a2aServer} />);

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();

        await user.click(screen.getByText('Promote to environment…'));

        expect(await screen.findByTestId('environment-promotion-dialog')).toBeInTheDocument();
        expect(hoisted.promotionDialogProps.at(-1)).toMatchObject({
            resourceType: 'A2A_SERVER',
            sourceEnvironmentId: 1,
            sourceId: 'a1',
            sourceName: 'My A2A Server',
            workspaceId: 1,
        });

        await user.click(screen.getByText('close'));

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();
    });

    it('invalidates the a2aServers query when onPromoted fires', async () => {
        const user = userEvent.setup();

        render(<A2aServerListItem a2aServer={a2aServer} />);

        await user.click(screen.getByText('Promote to environment…'));

        await user.click(await screen.findByText('promote'));

        expect(hoisted.invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ['a2aServers']});
    });
});
