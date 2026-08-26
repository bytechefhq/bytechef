import {ApiCollection} from '@/ee/shared/middleware/automation/api-platform';
import {ApiCollectionKeys} from '@/ee/shared/mutations/automation/apiCollections.queries';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionListItem from './ApiCollectionListItem';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants - vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
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
        useEnvironmentsQuery: () => hoisted.environmentsResult,
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollectionTags.mutations', () => ({
    useUpdateApiCollectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.mutations', () => ({
    useDeleteApiCollectionMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useEnableProjectDeploymentMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: () => ({data: undefined}),
}));

vi.mock('@/components/ui/collapsible', () => ({
    CollapsibleTrigger: ({children}: {children: React.ReactNode}) => <button type="button">{children}</button>,
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <>{children}</>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
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

const apiCollection = {
    contextPath: '/ctx',
    enabled: true,
    environmentId: 0,
    id: 1,
    name: 'My Collection',
    projectDeploymentId: 10,
    projectId: 100,
    projectVersion: 1,
    workspaceId: 1,
} as unknown as ApiCollection;

describe('ApiCollectionListItem', () => {
    beforeEach(() => {
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

        render(<ApiCollectionListItem apiCollection={apiCollection} />);

        expect(screen.queryByText('Promote to environment…')).not.toBeInTheDocument();
    });

    it('shows the Promote to environment menu item when at least two environments exist', () => {
        render(<ApiCollectionListItem apiCollection={apiCollection} />);

        expect(screen.getByText('Promote to environment…')).toBeInTheDocument();
    });

    it('mounts the dialog on click and unmounts it on close, passing the correct resourceType and ids', async () => {
        const user = userEvent.setup();

        render(<ApiCollectionListItem apiCollection={apiCollection} />);

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();

        await user.click(screen.getByText('Promote to environment…'));

        expect(screen.getByTestId('environment-promotion-dialog')).toBeInTheDocument();
        expect(hoisted.promotionDialogProps.at(-1)).toMatchObject({
            resourceType: 'API_COLLECTION',
            sourceEnvironmentId: 0,
            sourceId: '1',
            sourceName: 'My Collection',
            workspaceId: 1,
        });

        await user.click(screen.getByText('close'));

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();
    });

    it('invalidates the apiCollections query when onPromoted fires', async () => {
        const user = userEvent.setup();

        render(<ApiCollectionListItem apiCollection={apiCollection} />);

        await user.click(screen.getByText('Promote to environment…'));
        await user.click(screen.getByText('promote'));

        expect(hoisted.invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ApiCollectionKeys.apiCollections});
    });

    it('does not render the dialog when the collection has no environmentId', async () => {
        const user = userEvent.setup();

        render(<ApiCollectionListItem apiCollection={{...apiCollection, environmentId: undefined}} />);

        await user.click(screen.getByText('Promote to environment…'));

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();
    });
});
