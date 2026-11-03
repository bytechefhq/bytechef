import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationMoveUsersDialog from '../AutomationWorkflowProjectConfigurationMoveUsersDialog';

const mocks = vi.hoisted(() => ({
    connectedUsers: undefined as unknown[] | undefined,
    invalidateQueries: vi.fn(),
    moveImpactEnabledCalls: [] as unknown[],
    moveMutateAsync: vi.fn(),
    moveVariables: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationConnectedUsersQuery: () => ({
        data: mocks.connectedUsers
            ? {automationWorkflowProjectConfigurationConnectedUsers: mocks.connectedUsers}
            : undefined,
    }),
    useAutomationWorkflowProjectConfigurationMoveImpactQuery: (
        variables: {connectedUserIds: string[]; targetId: string},
        options?: {enabled?: boolean}
    ) => {
        mocks.moveVariables.push(variables);

        return {
            data: options?.enabled
                ? {
                      automationWorkflowProjectConfigurationMoveImpact: {
                          skipped: [
                              {
                                  connectedUserId: '2',
                                  reason: 'TARGET_LACKS_REFERENCED_WORKFLOWS',
                                  workflowLabels: ['Sync'],
                              },
                          ],
                          workflows: [],
                      },
                  }
                : undefined,
        };
    },
    useMoveAutomationWorkflowProjectConfigurationConnectedUsersMutation: (options?: {onSuccess?: () => void}) => ({
        mutateAsync: async (variables: unknown) => {
            const response = await mocks.moveMutateAsync(variables);

            options?.onSuccess?.();

            return response;
        },
    }),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

const configuration = {
    automationHubVisible: true,
    connectedUserCount: 2,
    enabled: true,
    environmentId: '2',
    id: 'source',
    lastExecutionDate: null,
    lastModifiedDate: null,
    name: 'ALPHA',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 1,
    workflows: [],
};

const targetConfiguration = {...configuration, id: 'target', name: 'BETA', projectVersion: 2};

const renderDialog = (onClose = vi.fn()) =>
    render(
        <AutomationWorkflowProjectConfigurationMoveUsersDialog
            configuration={configuration}
            onClose={onClose}
            targetConfigurations={[targetConfiguration]}
        />
    );

describe('AutomationWorkflowProjectConfigurationMoveUsersDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();

        mocks.connectedUsers = [
            {connectedUserId: '1', email: 'jane@example.com', externalId: 'ext-1', name: 'Jane'},
            {connectedUserId: '2', email: 'bob@example.com', externalId: 'ext-2', name: 'Bob'},
        ];
        mocks.moveVariables = [];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows a loading state until the connected users arrive', () => {
        mocks.connectedUsers = undefined;

        renderDialog();

        expect(screen.getByText('Loading users...')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Target'})).not.toBeInTheDocument();
    });

    it('offers only the given target configurations and previews skipped users for the chosen target', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'BETA · v2'}));

        expect(await screen.findByText('Bob: Sync missing in the target version')).toBeInTheDocument();
        expect(mocks.moveVariables.at(-1)).toEqual({
            connectedUserIds: ['1', '2'],
            sourceId: 'source',
            targetId: 'target',
        });
    });

    it('moves the users, reports the result and invalidates the affected queries', async () => {
        mocks.moveMutateAsync.mockResolvedValue({
            moveAutomationWorkflowProjectConfigurationConnectedUsers: {
                movedConnectedUserIds: ['1'],
                needsAttentionConnectedUserIds: [],
                skipped: [{connectedUserId: '2', reason: 'ALREADY_ON_TARGET', workflowLabels: []}],
            },
        });

        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'BETA · v2'}));
        await userEvent.click(screen.getByRole('button', {name: 'Move'}));

        expect(await screen.findByText('1 moved, 0 need attention')).toBeInTheDocument();
        expect(screen.getByText('Bob: ALREADY_ON_TARGET')).toBeInTheDocument();
        expect(mocks.moveMutateAsync).toHaveBeenCalledWith({
            connectedUserIds: ['1', '2'],
            sourceId: 'source',
            targetId: 'target',
        });

        await waitFor(() => {
            expect(mocks.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['automationWorkflowProjectConfigurations'],
            });
            expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUserProjects']});
        });
    });
});
