import {QueryClient, QueryClientProvider, focusManager} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useEnvironmentPromotionDialog} from '../useEnvironmentPromotionDialog';

const {fetchPreviewMock} = vi.hoisted(() => ({
    fetchPreviewMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphqlFetcher', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphqlFetcher')>();

    return {
        ...actual,
        fetcher: () => fetchPreviewMock,
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useEnvironmentPromotionPreviewQuery: vi.fn(),
        useEnvironmentsQuery: vi.fn(),
        usePromoteToEnvironmentMutation: vi.fn(),
    };
});

const {
    PromotionChangeKind,
    PromotionResourceType,
    useEnvironmentPromotionPreviewQuery: mockUseEnvironmentPromotionPreviewQuery,
    useEnvironmentsQuery: mockUseEnvironmentsQuery,
    usePromoteToEnvironmentMutation: mockUsePromoteToEnvironmentMutation,
} = await import('@/shared/middleware/graphql');

const makePreview = (destructiveChangesHash: string) => ({
    changes: [
        {
            assetType: 'MCP_TOOL',
            destructive: true,
            key: 'tool-extra',
            kind: PromotionChangeKind.Delete,
            name: 'Extra tool',
            parentName: null,
        },
    ],
    connections: [],
    destructiveChangesHash,
    existingTargetId: 'existing-1',
    existingTargetName: 'Existing Server',
    projects: [],
    resourceType: PromotionResourceType.McpServer,
    schemaChanges: [],
    sourceEnvironmentId: '0',
    sourceId: 'source-1',
    targetEnvironmentId: '1',
    warnings: [],
});

const createQueryClientWrapper = (queryClient: QueryClient) => {
    const QueryClientWrapper = ({children}: {children: ReactNode}) =>
        createElement(QueryClientProvider, {client: queryClient}, children);

    return QueryClientWrapper;
};

describe('useEnvironmentPromotionDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        vi.mocked(mockUseEnvironmentsQuery).mockReturnValue({
            data: {
                environments: [
                    {id: '0', name: 'Development'},
                    {id: '1', name: 'Staging'},
                ],
            },
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        vi.mocked(mockUsePromoteToEnvironmentMutation).mockReturnValue({
            isPending: false,
            mutateAsync: vi.fn(),
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);
    });

    it('drops the acknowledgement in the same render that receives a different hash', () => {
        let currentPreview = makePreview('hash-1');

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockImplementation(
            () =>
                ({
                    data: {environmentPromotionPreview: currentPreview},
                    isLoading: false,
                    refetch: vi.fn(),
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                }) as any
        );

        const renderedStates: Array<{isDestructiveAcknowledged: boolean; wizardResetKey: string}> = [];

        const {rerender, result} = renderHook(
            () => {
                const promotionDialog = useEnvironmentPromotionDialog({
                    resourceType: PromotionResourceType.McpServer,
                    sourceEnvironmentId: 0,
                    sourceId: 'source-1',
                });

                renderedStates.push({
                    isDestructiveAcknowledged: promotionDialog.isDestructiveAcknowledged,
                    wizardResetKey: promotionDialog.wizardResetKey,
                });

                return promotionDialog;
            },
            {wrapper: createQueryClientWrapper(new QueryClient())}
        );

        act(() => {
            result.current.setDestructiveAcknowledged(true);
        });

        expect(result.current.isDestructiveAcknowledged).toBe(true);

        currentPreview = makePreview('hash-2');

        rerender();

        const statesWithNewHash = renderedStates.filter((state) => state.wizardResetKey === '1:hash-2');

        expect(statesWithNewHash.length).toBeGreaterThan(0);
        expect(statesWithNewHash.every((state) => !state.isDestructiveAcknowledged)).toBe(true);
    });

    it('fetches a fresh preview every time the dialog opens and never on window focus', async () => {
        const {useEnvironmentPromotionPreviewQuery: actualUseEnvironmentPromotionPreviewQuery} =
            await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockImplementation(
            actualUseEnvironmentPromotionPreviewQuery
        );

        fetchPreviewMock.mockResolvedValue({environmentPromotionPreview: makePreview('hash-1')});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        const wrapper = createQueryClientWrapper(queryClient);

        const renderDialogHook = () =>
            renderHook(
                () =>
                    useEnvironmentPromotionDialog({
                        resourceType: PromotionResourceType.McpServer,
                        sourceEnvironmentId: 0,
                        sourceId: 'source-1',
                    }),
                {wrapper}
            );

        const firstDialog = renderDialogHook();

        await waitFor(() => expect(firstDialog.result.current.preview?.destructiveChangesHash).toBe('hash-1'), {
            interval: 5,
            timeout: 5000,
        });

        expect(fetchPreviewMock).toHaveBeenCalledTimes(1);

        firstDialog.unmount();

        fetchPreviewMock.mockResolvedValue({environmentPromotionPreview: makePreview('hash-2')});

        const reopenedDialog = renderDialogHook();

        await waitFor(() => expect(reopenedDialog.result.current.preview?.destructiveChangesHash).toBe('hash-2'), {
            interval: 5,
            timeout: 5000,
        });

        expect(fetchPreviewMock).toHaveBeenCalledTimes(2);

        act(() => {
            focusManager.setFocused(false);
            focusManager.setFocused(true);
        });

        expect(queryClient.isFetching()).toBe(0);
        expect(fetchPreviewMock).toHaveBeenCalledTimes(2);

        focusManager.setFocused(undefined);

        reopenedDialog.unmount();
    });
});
