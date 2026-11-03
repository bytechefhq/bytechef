import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import MoveConnectedUsersDialog from '../MoveConnectedUsersDialog';

const connectedUsers = [
    {connectedUserId: '1', email: 'jane@example.com', externalId: 'ext-1', name: 'Jane'},
    {connectedUserId: '2', email: 'bob@example.com', externalId: 'ext-2', name: 'Bob'},
];

const targets = [{id: '10', label: 'Beta · v2'}];

const defaultResult = {movedCount: 0, needsAttentionCount: 0, skipped: []};

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
        const onMove = vi.fn().mockRejectedValue(new Error('Move failed'));

        renderDialog({onMove});

        await chooseTarget('Beta · v2');
        await userEvent.click(screen.getByRole('button', {name: 'Move'}));

        await waitFor(() => {
            expect(onMove).toHaveBeenCalledOnce();
            expect(screen.getByRole('button', {name: 'Move'})).toBeEnabled();
        });

        expect(screen.getByRole('combobox', {name: 'Target'})).toBeInTheDocument();
    });

    it('shows users that would be skipped before moving', () => {
        renderDialog({
            previewSkipped: [
                {label: 'Jane', reason: 'TARGET_LACKS_REFERENCED_WORKFLOWS', workflowLabels: ['Sync', 'Report']},
            ],
        });

        expect(screen.getByText('Jane: Sync, Report missing in the target version')).toBeInTheDocument();
    });

    it('reports moved, needs-attention and skipped users', async () => {
        const onMove = vi.fn().mockResolvedValue({
            movedCount: 1,
            needsAttentionCount: 1,
            skipped: [
                {label: 'Jane', reason: 'TARGET_LACKS_REFERENCED_WORKFLOWS', workflowLabels: ['Sync', 'Report']},
                {label: 'Bob', reason: 'ALREADY_ON_TARGET', workflowLabels: []},
            ],
        });

        renderDialog({onMove});

        await chooseTarget('Beta · v2');
        await userEvent.click(screen.getByRole('button', {name: 'Move'}));

        await waitFor(() => {
            expect(screen.getByText('1 moved, 1 need attention')).toBeInTheDocument();
        });

        expect(onMove).toHaveBeenCalledWith('10', ['1', '2']);
        expect(screen.getByText('Jane: Sync, Report missing in the target version')).toBeInTheDocument();
        expect(screen.getByText('Bob: ALREADY_ON_TARGET')).toBeInTheDocument();
    });
});
