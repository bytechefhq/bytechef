import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserChangeVersionDialog, {
    ConnectedUserChangeVersionConfigurationI,
} from '../ConnectedUserChangeVersionDialog';

import type {MoveConnectedUsersResultI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

const movedResult: MoveConnectedUsersResultI = {
    movedConnectedUserIds: ['7'],
    needsAttentionConnectedUserIds: [],
    skipped: [],
};

const renderDialog = ({
    configurations = [],
    impact,
    onChange = vi.fn().mockResolvedValue(movedResult),
    onClose = vi.fn(),
    onTargetChange,
    previewLoading,
}: {
    configurations?: ConnectedUserChangeVersionConfigurationI[];
    impact?: Array<{
        affectedConnectedUserCount: number;
        label: string;
        newRequiredConnections: string[];
        newRequiredInputs: string[];
    }>;
    onChange?: (version: number, targetId?: string) => Promise<MoveConnectedUsersResultI>;
    onClose?: () => void;
    onTargetChange?: (targetId: string | undefined) => void;
    previewLoading?: boolean;
} = {}) =>
    render(
        <ConnectedUserChangeVersionDialog
            configurations={configurations}
            connectedUserLabel="Jane"
            currentConfigurationName="Default"
            currentVersion={3}
            impact={impact}
            onChange={onChange}
            onClose={onClose}
            onTargetChange={onTargetChange}
            previewLoading={previewLoading}
            versions={[1, 3, 2, 5, 4]}
        />
    );

const syncImpact = [
    {affectedConnectedUserCount: 1, label: 'Sync', newRequiredConnections: ['Slack'], newRequiredInputs: []},
];

const chooseVersion = async (label: string) => {
    await userEvent.click(screen.getByRole('combobox', {name: 'Version'}));
    await userEvent.click(await screen.findByRole('option', {name: label}));
};

describe('ConnectedUserChangeVersionDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('lists the published versions newest first without the current one, marking latest and downgrades', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Version'}));

        expect((await screen.findAllByRole('option')).map((option) => option.textContent)).toEqual([
            'V5 (latest)',
            'V4',
            'V2 (downgrade)',
            'V1 (downgrade)',
        ]);
    });

    it('preselects the configuration with the current name among the enabled ones on that version', async () => {
        renderDialog({
            configurations: [
                {enabled: true, id: '11', name: 'Other', version: 5},
                {enabled: true, id: '12', name: 'Default', version: 5},
                {enabled: false, id: '13', name: 'Disabled', version: 5},
                {enabled: true, id: '14', name: 'Default', version: 4},
            ],
        });

        await chooseVersion('V5 (latest)');

        expect(screen.getByRole('combobox', {name: 'Configuration'})).toHaveTextContent('Default · V5');
        expect(screen.getByText('Moves this connected user to Default · V5.')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));

        expect((await screen.findAllByRole('option')).map((option) => option.textContent)).toEqual([
            'Other · V5',
            'Default · V5',
        ]);
    });

    it('preselects the first configuration on that version when none has the current name', async () => {
        renderDialog({
            configurations: [
                {enabled: true, id: '11', name: 'Other', version: 4},
                {enabled: true, id: '12', name: 'Another', version: 4},
            ],
        });

        await chooseVersion('V4');

        expect(screen.getByText('Moves this connected user to Other · V4.')).toBeInTheDocument();
    });

    it('explains that a configuration is created when no enabled configuration runs that version', async () => {
        renderDialog({configurations: [{enabled: false, id: '13', name: 'Disabled', version: 2}]});

        await chooseVersion('V2 (downgrade)');

        expect(screen.queryByRole('combobox', {name: 'Configuration'})).not.toBeInTheDocument();
        expect(screen.getByRole('alert')).toHaveTextContent(
            'Creates Default · V2 from Default, then moves this connected user to it. New connected users keep landing on Default.'
        );
    });

    it('calls onChange with the version and the chosen target, then closes on a clean move', async () => {
        const onChange = vi.fn().mockResolvedValue(movedResult);
        const onClose = vi.fn();

        renderDialog({
            configurations: [
                {enabled: true, id: '11', name: 'Other', version: 5},
                {enabled: true, id: '12', name: 'Default', version: 5},
            ],
            onChange,
            onClose,
        });

        await chooseVersion('V5 (latest)');

        await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Other · V5'}));
        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

        expect(onChange).toHaveBeenCalledWith(5, '11', []);
    });

    it('calls onChange without a target when the configuration is to be created', async () => {
        const onChange = vi.fn().mockResolvedValue(movedResult);

        renderDialog({onChange});

        await chooseVersion('V1 (downgrade)');
        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        await waitFor(() => expect(onChange).toHaveBeenCalledWith(1, undefined, []));
    });

    it('stays open when onChange rejects', async () => {
        const onChange = vi.fn().mockRejectedValue(new Error('failed'));
        const onClose = vi.fn();

        renderDialog({onChange, onClose});

        await chooseVersion('V4');
        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

        expect(onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Change'})).toBeEnabled();
    });

    it('stays open and shows the reason when the connected user was not moved', async () => {
        const onChange = vi.fn().mockResolvedValue({
            movedConnectedUserIds: [],
            needsAttentionConnectedUserIds: [],
            skipped: [{connectedUserId: '7', reason: 'TARGET_LACKS_ENABLED_WORKFLOWS', workflowLabels: ['Sync']}],
        });
        const onClose = vi.fn();

        renderDialog({onChange, onClose});

        await chooseVersion('V4');
        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        expect(await screen.findByText('Jane was not moved')).toBeInTheDocument();
        expect(screen.getByText("These workflows aren't enabled on the target configuration:")).toBeInTheDocument();
        expect(screen.getByText('Sync')).toBeInTheDocument();
        expect(onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Change'})).toBeEnabled();
    });

    it('shows that the moved connected user needs attention and closes only on Close', async () => {
        const onChange = vi.fn().mockResolvedValue({
            movedConnectedUserIds: ['7'],
            needsAttentionConnectedUserIds: ['7'],
            skipped: [],
        });
        const onClose = vi.fn();

        renderDialog({onChange, onClose});

        await chooseVersion('V5 (latest)');
        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        expect(
            await screen.findByText('Jane moved to V5, but some turned-on workflows need attention.')
        ).toBeInTheDocument();
        expect(screen.getByText('1 moved, 1 needs attention')).toBeInTheDocument();
        expect(onClose).not.toHaveBeenCalled();

        expect(screen.queryByRole('button', {name: 'Change'})).not.toBeInTheDocument();

        const footerCloseButton = screen
            .getAllByRole('button', {name: 'Close'})
            .find((button) => button.getAttribute('data-slot') !== 'dialog-close');

        await userEvent.click(footerCloseButton!);

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('shows the move-impact preview and reports the target when moving to an existing configuration', async () => {
        const onTargetChange = vi.fn();

        renderDialog({
            configurations: [{enabled: true, id: '12', name: 'Default', version: 5}],
            impact: syncImpact,
            onTargetChange,
        });

        await chooseVersion('V5 (latest)');

        expect(onTargetChange).toHaveBeenLastCalledWith('12');
        expect(screen.getByText('Sync: affects 1 user')).toBeInTheDocument();
        expect(screen.getByText('New required connections: Slack')).toBeInTheDocument();
    });

    it('shows no move-impact preview when a configuration is to be created', async () => {
        const onTargetChange = vi.fn();

        renderDialog({impact: syncImpact, onTargetChange});

        await chooseVersion('V4');

        expect(onTargetChange).toHaveBeenLastCalledWith(undefined);
        expect(screen.queryByText('Sync: affects 1 user')).not.toBeInTheDocument();
    });

    it('disables Change while the move-impact preview of an existing target loads', async () => {
        renderDialog({
            configurations: [{enabled: true, id: '12', name: 'Default', version: 5}],
            previewLoading: true,
        });

        await chooseVersion('V5 (latest)');

        expect(screen.getByRole('button', {name: 'Change'})).toBeDisabled();
    });

    it('disables Change until a version is chosen', () => {
        renderDialog();

        expect(screen.getByRole('button', {name: 'Change'})).toBeDisabled();
    });
});
