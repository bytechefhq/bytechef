import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import MoveConnectedUsersDialog from '../MoveConnectedUsersDialog';

const connectedUsers = [
    {connectedUserId: '1', email: 'jane@example.com', externalId: 'ext-1', name: 'Jane'},
    {connectedUserId: '2', email: 'bob@example.com', externalId: 'ext-2', name: 'Bob'},
];

const targets = [{id: '10', label: 'Beta · v2'}];

const defaultResult = {movedConnectedUserIds: [], needsAttentionConnectedUserIds: [], skipped: []};

const renderDialog = (props: Partial<React.ComponentProps<typeof MoveConnectedUsersDialog>> = {}) =>
    render(
        <MoveConnectedUsersDialog
            connectedUsers={connectedUsers}
            onClose={vi.fn()}
            onMove={vi.fn().mockResolvedValue(defaultResult)}
            sourceLabel="Alpha · v1"
            targets={targets}
            title="Move Users"
            {...props}
        />
    );

const chooseTarget = async (label: string) => {
    await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));
    await userEvent.click(await screen.findByRole('option', {name: label}));
};

beforeEach(() => {
    windowResizeObserver();

    Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();
});

afterEach(() => {
    resetAll();
});

describe('MoveConnectedUsersDialog', () => {
    it('selects every user by default', () => {
        renderDialog();

        expect(screen.getByRole('checkbox', {name: 'Jane'})).toBeChecked();
        expect(screen.getByRole('checkbox', {name: 'Bob'})).toBeChecked();
    });

    it('filters users by search', async () => {
        renderDialog();

        await userEvent.type(screen.getByLabelText('Search users'), 'bob@');

        expect(screen.queryByRole('checkbox', {name: 'Jane'})).not.toBeInTheDocument();
        expect(screen.getByRole('checkbox', {name: 'Bob'})).toBeInTheDocument();
    });

    it('disables Move until a target is chosen', async () => {
        renderDialog();

        expect(screen.getByRole('button', {name: 'Move'})).toBeDisabled();

        await chooseTarget('Beta · v2');

        expect(screen.getByRole('button', {name: 'Move'})).toBeEnabled();
    });

    it('shows impact warnings', () => {
        renderDialog({
            impact: [
                {
                    affectedConnectedUserCount: 2,
                    label: 'Sync contacts',
                    newRequiredConnections: ['Slack'],
                    newRequiredInputs: ['channel'],
                },
            ],
        });

        expect(screen.getByText('Sync contacts: affects 2 users')).toBeInTheDocument();
        expect(screen.getByText('New required connections: Slack')).toBeInTheDocument();
        expect(screen.getByText('New required inputs: channel')).toBeInTheDocument();
    });

    it('says "1 user" when the impact affects a single user', () => {
        renderDialog({
            impact: [
                {
                    affectedConnectedUserCount: 1,
                    label: 'Sync contacts',
                    newRequiredConnections: [],
                    newRequiredInputs: [],
                },
            ],
        });

        expect(screen.getByText('Sync contacts: affects 1 user')).toBeInTheDocument();
    });

    it('keeps the dialog open and enabled for a retry when the move fails', async () => {
        const unhandledRejections: unknown[] = [];
        const collectRejection = (reason: unknown) => unhandledRejections.push(reason);

        process.on('unhandledRejection', collectRejection);

        const onClose = vi.fn();
        const onMove = vi.fn().mockRejectedValue(new Error('Move failed'));

        try {
            renderDialog({onClose, onMove});

            await chooseTarget('Beta · v2');
            await userEvent.click(screen.getByRole('button', {name: 'Move'}));

            await waitFor(() => {
                expect(onMove).toHaveBeenCalledOnce();
                expect(screen.getByRole('button', {name: 'Move'})).toBeEnabled();
            });

            expect(screen.getByRole('combobox', {name: 'Target'})).toBeInTheDocument();
        } finally {
            process.off('unhandledRejection', collectRejection);
        }

        expect(unhandledRejections).toEqual([]);
        expect(onClose).not.toHaveBeenCalled();
        expect(screen.queryByText(/moved, .* need attention/)).not.toBeInTheDocument();
    });

    it('shows users that would be skipped before moving', () => {
        renderDialog({
            previewSkipped: [
                {connectedUserId: '1', reason: 'TARGET_LACKS_REFERENCED_WORKFLOWS', workflowLabels: ['Sync', 'Report']},
            ],
        });

        expect(screen.getByText('Jane: the target has no Sync, Report')).toBeInTheDocument();
    });

    it('labels a skipped user by id when the user is not in the list', () => {
        renderDialog({previewSkipped: [{connectedUserId: 99, reason: 'NOT_ON_SOURCE', workflowLabels: []}]});

        expect(screen.getByText('99: no longer on this configuration')).toBeInTheDocument();
    });

    it('shows a loading state until the connected users arrive', () => {
        renderDialog({connectedUsers: undefined});

        expect(screen.getByText('Loading users...')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Target'})).not.toBeInTheDocument();
    });

    it('says when the connected users fail to load', () => {
        renderDialog({connectedUsers: undefined, connectedUsersError: true});

        expect(screen.getByText('Could not load the connected users.')).toBeInTheDocument();
    });

    it.each([
        ['TARGET_LACKS_REFERENCED_WORKFLOWS', ['Sync', 'Report'], 'Jane: the target has no Sync, Report'],
        ['TARGET_LACKS_ENABLED_WORKFLOWS', ['Sync', 'Report'], 'Jane: the target has no enabled Sync, Report'],
        ['CONNECTION_MISMATCH', [], 'Jane: the target uses a different authorization type'],
        ['NOT_ON_SOURCE', [], 'Jane: no longer on this configuration'],
        ['MOVE_FAILED', [], 'Jane: the move failed, try again'],
        ['SOMETHING_NEW', [], 'Jane: SOMETHING_NEW'],
    ])('explains the %s skip reason', (reason, workflowLabels, expectedLine) => {
        renderDialog({previewSkipped: [{connectedUserId: '1', reason, workflowLabels}]});

        expect(screen.getByText(expectedLine)).toBeInTheDocument();
    });

    it('reports moved, needs-attention and skipped users', async () => {
        const onMove = vi.fn().mockResolvedValue({
            movedConnectedUserIds: [3],
            needsAttentionConnectedUserIds: [4],
            skipped: [
                {connectedUserId: 1, reason: 'TARGET_LACKS_REFERENCED_WORKFLOWS', workflowLabels: ['Sync', 'Report']},
                {connectedUserId: 2, reason: 'ALREADY_ON_TARGET', workflowLabels: []},
            ],
        });

        renderDialog({onMove});

        await chooseTarget('Beta · v2');
        await userEvent.click(screen.getByRole('button', {name: 'Move'}));

        await waitFor(() => {
            expect(screen.getByText('1 moved, 1 need attention')).toBeInTheDocument();
        });

        expect(onMove).toHaveBeenCalledWith('10', ['1', '2']);
        expect(screen.getByText('Jane: the target has no Sync, Report')).toBeInTheDocument();
        expect(screen.getByText('Bob: ALREADY_ON_TARGET')).toBeInTheDocument();
    });

    it('disables Move while the move preview loads', async () => {
        renderDialog({previewLoading: true});

        await chooseTarget('Beta · v2');

        expect(screen.getByRole('button', {name: 'Move'})).toBeDisabled();
    });

    it('says when the move preview fails and still allows the move', async () => {
        renderDialog({previewError: true});

        await chooseTarget('Beta · v2');

        expect(screen.getByText('Could not load the move preview.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Move'})).toBeEnabled();
    });

    it('notes the MCP servers that use either configuration', () => {
        const {unmount} = renderDialog({mcpServerCount: 1});

        expect(
            screen.getByText(
                '1 MCP server uses one of these configurations. Their tools follow the configuration each user lands on.'
            )
        ).toBeInTheDocument();

        unmount();

        renderDialog({mcpServerCount: 2});

        expect(screen.getByText(/^2 MCP servers use one of these configurations\./)).toBeInTheDocument();
    });

    it('shows no MCP notice when no MCP server uses either configuration', () => {
        renderDialog({mcpServerCount: 0});

        expect(screen.queryByText(/MCP server/)).not.toBeInTheDocument();
    });
});
