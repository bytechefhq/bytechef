import {NodeDataType} from '@/shared/types';
import {describe, expect, it, vi} from 'vitest';

import {getWorkflowNodeMenuItems} from '../getWorkflowNodeMenuItems';

const getTriggerMenuItems = (
    showDeleteAction: boolean,
    onDelete = vi.fn(),
    {
        disabled = false,
        onToggleDisabled = vi.fn(),
        showDisableAction = false,
    }: {disabled?: boolean; onToggleDisabled?: () => void; showDisableAction?: boolean} = {}
) =>
    getWorkflowNodeMenuItems({
        canPaste: false,
        copiedNode: undefined,
        data: {disabled, name: 'trigger_1', trigger: true} as NodeDataType,
        hasSavedPosition: false,
        onDelete,
        onRename: vi.fn(),
        onResetPosition: vi.fn(),
        onSwitch: vi.fn(),
        onToggleDisabled,
        showCopyAction: false,
        showCutAction: false,
        showDeleteAction,
        showDisableAction,
        showInfoAction: false,
        showRenameAction: true,
        showReplaceAction: true,
    });

describe('getWorkflowNodeMenuItems for a trigger', () => {
    it('offers Delete after a separator when the workflow has another trigger', () => {
        const onDelete = vi.fn();

        const menuItems = getTriggerMenuItems(true, onDelete);

        expect(menuItems.slice(-2).map((menuItem) => menuItem.key)).toEqual(['separator-delete', 'delete']);

        const deleteItem = menuItems.at(-1);

        if (deleteItem?.type !== 'item') {
            throw new Error('expected the last menu entry to be the Delete item');
        }

        expect(deleteItem.variant).toBe('destructive');

        deleteItem.onSelect();

        expect(onDelete).toHaveBeenCalledOnce();
    });

    it('does not offer Delete for the only trigger', () => {
        const menuItemKeys = getTriggerMenuItems(false).map((menuItem) => menuItem.key);

        expect(menuItemKeys).not.toContain('delete');
        expect(menuItemKeys).not.toContain('separator-delete');
    });

    it('offers Disable for a trigger when the disable action is shown', () => {
        const onToggleDisabled = vi.fn();

        const menuItems = getTriggerMenuItems(true, vi.fn(), {onToggleDisabled, showDisableAction: true});

        const toggleItem = menuItems.find((menuItem) => menuItem.key === 'toggle-disabled');

        if (toggleItem?.type !== 'item') {
            throw new Error('expected a Disable item');
        }

        expect(toggleItem.label).toBe('Disable');

        toggleItem.onSelect();

        expect(onToggleDisabled).toHaveBeenCalledOnce();
    });

    it('offers Enable for a disabled trigger', () => {
        const menuItems = getTriggerMenuItems(false, vi.fn(), {disabled: true, showDisableAction: true});

        const toggleItem = menuItems.find((menuItem) => menuItem.key === 'toggle-disabled');

        expect(toggleItem?.type === 'item' && toggleItem.label).toBe('Enable');
    });

    it('does not offer Disable for a trigger when the disable action is hidden', () => {
        const menuItemKeys = getTriggerMenuItems(false).map((menuItem) => menuItem.key);

        expect(menuItemKeys).not.toContain('toggle-disabled');
    });
});
