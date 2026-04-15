import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

// vi.hoisted lets us mutate the mocked feature-flag state per test before the vi.mock factories resolve.
const mocks = vi.hoisted(() => ({
    setEnabled(enabled: boolean, workspaceId?: number) {
        mocks.visibility = {enabled, isAdmin: enabled, workspaceId};
    },
    visibility: {enabled: false as boolean, isAdmin: false, workspaceId: undefined as number | undefined},
}));

vi.mock('@/pages/automation/connections/hooks/useVisibilityFeatureEnabled', () => ({
    useIsVisibilityEditionEnabled: () => mocks.visibility.enabled,
    useVisibilityFeatureEnabled: () => mocks.visibility,
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useDeleteConnectionMutation: () => ({mutate: vi.fn()}),
    useDisconnectConnectionMutation: () => ({mutate: vi.fn()}),
    useUpdateConnectionMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDemoteConnectionToPrivateMutation: () => ({isPending: false, mutate: vi.fn()}),
    usePromoteConnectionToWorkspaceMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connection-tags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['component-definitions']},
    useGetConnectionComponentDefinitionQuery: () => ({
        data: {icon: 'data:image/svg+xml,<svg/>', name: 'demo', title: 'Demo', version: 1},
    }),
}));

// ConnectionDialog pulls in many unrelated stores; the gate test doesn't need it.
vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => null,
}));

// ConnectionProjectShareDialog drags in a mutation from graphql middleware that we don't want to resolve here.
vi.mock('@/pages/automation/connections/components/ConnectionProjectShareDialog', () => ({
    default: () => null,
}));

import ConnectionListItem from '../ConnectionListItem';

const connection = {
    active: true,
    componentName: 'demo',
    connectionVersion: 1,
    createdBy: 'user@example.com',
    id: 42,
    name: 'Demo connection',
    visibility: 'WORKSPACE' as const,
};

const renderItem = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <ConnectionListItem
                    componentDefinitions={[{name: 'demo', title: 'Demo', version: 1} as never]}
                    connection={connection as never}
                    remainingTags={[]}
                />
            </TooltipProvider>
        </QueryClientProvider>
    );
};

/**
 * CE-safety regression guard. The visibility controls (scope badge, change-visibility button, promote / demote /
 * share menu items) are EE-only. Per CLAUDE.md the single source of truth is {@code useVisibilityFeatureEnabled}:
 * an inverted flag check in {@code ConnectionListItem} would leak every visibility control to CE users. These
 * tests render the real component with the hook mocked false and true, so a regression is caught in the list-item
 * integration — not just in the hook that tests only its own return value.
 */
describe('ConnectionListItem visibility gate', () => {
    it('hides the scope badge and change-visibility button when CE (flag false)', () => {
        mocks.setEnabled(false);

        renderItem();

        expect(screen.queryByLabelText('Change visibility')).not.toBeInTheDocument();
        expect(screen.queryByText('Workspace')).not.toBeInTheDocument();
        expect(screen.queryByText('Private')).not.toBeInTheDocument();
    });

    it('shows the scope badge when EE (flag true)', () => {
        mocks.setEnabled(true, 7);

        renderItem();

        expect(screen.getByLabelText('Change visibility')).toBeInTheDocument();
        expect(screen.getByText('Workspace')).toBeInTheDocument();
    });
});
