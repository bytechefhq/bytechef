import WorkspaceApiKeys from '@/ee/pages/settings/automation/workspace-api-keys/WorkspaceApiKeys';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {act, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    workspaceGrants: [] as string[],
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (workspaceId: number | undefined, scope: string) =>
        workspaceId !== undefined && hoisted.workspaceGrants.includes(scope),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useCreateWorkspaceApiKeyMutation: () => ({mutate: vi.fn()}),
        useDeleteWorkspaceApiKeyMutation: () => ({isPending: false, mutate: vi.fn()}),
        useUpdateWorkspaceApiKeyMutation: () => ({mutate: vi.fn()}),
        useWorkspaceApiKeysQuery: () => ({
            data: {
                workspaceApiKeys: [
                    {
                        createdBy: 'user@localhost.com',
                        createdDate: 1_700_000_000_000,
                        id: '3',
                        lastUsedDate: 1_700_000_000_000,
                        name: 'Own key',
                        secretKey: '..........................ab12cd',
                    },
                ],
            },
            error: null,
            isLoading: false,
        }),
    };
});

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right}: {right?: ReactNode}) => <div>{right}</div>,
}));

describe('WorkspaceApiKeys', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.workspaceGrants = [];

        act(() => {
            useWorkspaceStore.setState({currentWorkspaceId: 1049});
        });
    });

    afterEach(() => {
        resetAll();
    });

    it('testAWorkspaceMemberWithoutApiKeyCreateStillRenamesTheirKeys', () => {
        hoisted.workspaceGrants = ['API_KEY_VIEW'];

        render(<WorkspaceApiKeys />);

        expect(screen.getByText('Own key')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Edit API Key'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New API Key'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete API Key'})).not.toBeInTheDocument();
    });
});
