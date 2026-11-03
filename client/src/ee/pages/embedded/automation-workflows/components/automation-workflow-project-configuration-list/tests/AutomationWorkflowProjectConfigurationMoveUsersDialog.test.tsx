import {resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationMoveUsersDialog from '../AutomationWorkflowProjectConfigurationMoveUsersDialog';

const mocks = vi.hoisted(() => ({
    connectedUsers: undefined as unknown[] | undefined,
    connectedUsersEnabled: undefined as boolean | undefined,
    connectedUsersError: false,
    moveImpactError: false,
    moveImpactLoading: false,
    moveMutateAsync: vi.fn(),
    moveVariables: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationConnectedUsersQuery: (
        _variables: unknown,
        options?: {enabled?: boolean}
    ) => {
        mocks.connectedUsersEnabled = options?.enabled;

        return {
            data: mocks.connectedUsers
                ? {automationWorkflowProjectConfigurationConnectedUsers: mocks.connectedUsers}
                : undefined,
            isError: mocks.connectedUsersError,
        };
    },
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
            isError: mocks.moveImpactError,
            isLoading: mocks.moveImpactLoading,
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

const queryClient = new QueryClient();

const renderDialog = (onClose = vi.fn()) =>
    render(
        <QueryClientProvider client={queryClient}>
            <AutomationWorkflowProjectConfigurationMoveUsersDialog
                configuration={configuration}
                onClose={onClose}
                targetConfigurations={[targetConfiguration]}
            />
        </QueryClientProvider>
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
        mocks.connectedUsersEnabled = undefined;
        mocks.connectedUsersError = false;
        mocks.moveImpactError = false;
        mocks.moveImpactLoading = false;
        mocks.moveVariables = [];

        vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();
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

    it('closes the loading dialog with Escape', async () => {
        mocks.connectedUsers = undefined;

        const onClose = vi.fn();

        renderDialog(onClose);

        await userEvent.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalledOnce();
    });

    it('shows an error with one Close button instead of blocking when the users fail to load', async () => {
        mocks.connectedUsers = undefined;
        mocks.connectedUsersError = true;

        const onClose = vi.fn();

        renderDialog(onClose);

        expect(screen.getByText('Could not load the connected users.')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(onClose).toHaveBeenCalled();
    });

    it('offers only the given target configurations and previews skipped users for the chosen target', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'BETA · v2'}));

        expect(await screen.findByText('Bob: the target has no Sync')).toBeInTheDocument();
        expect(mocks.moveVariables.at(-1)).toEqual({
            connectedUserIds: ['1', '2'],
            sourceId: 'source',
            targetId: 'target',
        });
    });

    it('marks a disabled target configuration as disabled', async () => {
        render(
            <QueryClientProvider client={queryClient}>
                <AutomationWorkflowProjectConfigurationMoveUsersDialog
                    configuration={configuration}
                    onClose={vi.fn()}
                    targetConfigurations={[targetConfiguration, {...targetConfiguration, enabled: false, id: 'paused'}]}
                />
            </QueryClientProvider>
        );

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));

        expect(await screen.findByRole('option', {name: 'BETA · v2'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'BETA · v2 (disabled)'})).toBeInTheDocument();
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
        expect(screen.getByText('Bob: Already on this configuration')).toBeInTheDocument();
        expect(mocks.moveMutateAsync).toHaveBeenCalledWith({
            connectedUserIds: ['1', '2'],
            sourceId: 'source',
            targetId: 'target',
        });

        await waitFor(() => {
            expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['automationWorkflowProjectConfigurations'],
            });
            expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUserProjects']});
        });
    });

    it('uses the given connected users, selection and title without loading the users', async () => {
        mocks.connectedUsers = undefined;

        render(
            <QueryClientProvider client={queryClient}>
                <AutomationWorkflowProjectConfigurationMoveUsersDialog
                    configuration={configuration}
                    connectedUsers={[{connectedUserId: '7', externalId: 'ext-7'}]}
                    initialSelectedConnectedUserIds={['7']}
                    onClose={vi.fn()}
                    targetConfigurations={[targetConfiguration]}
                    title="Change configuration"
                />
            </QueryClientProvider>
        );

        expect(screen.getByRole('heading', {name: 'Change configuration'})).toBeInTheDocument();
        expect(screen.getByText('ext-7')).toBeInTheDocument();
        expect(mocks.connectedUsersEnabled).toBe(false);

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'BETA · v2'}));

        await waitFor(() =>
            expect(mocks.moveVariables.at(-1)).toEqual({
                connectedUserIds: ['7'],
                sourceId: 'source',
                targetId: 'target',
            })
        );
    });

    it('disables Move while the move preview loads', async () => {
        mocks.moveImpactLoading = true;

        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'BETA · v2'}));

        expect(screen.getByRole('button', {name: 'Move'})).toBeDisabled();
    });

    it('says when the move preview fails and still allows the move', async () => {
        mocks.moveImpactError = true;

        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'BETA · v2'}));

        expect(screen.getByText('Could not load the move preview.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Move'})).toBeEnabled();
    });
});
