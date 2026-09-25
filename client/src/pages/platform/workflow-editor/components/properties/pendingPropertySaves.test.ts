import {describe, expect, it, vi} from 'vitest';

import {cancelPendingSavesWithin, hasPendingSaveWithin, registerPendingSave} from './pendingPropertySaves';

describe('pendingPropertySaves', () => {
    it('counts a pending save of a nested field or item', () => {
        const unregisterField = registerPendingSave('settings.count', {cancel: vi.fn(), isPending: () => true});
        const unregisterItem = registerPendingSave('counts[0]', {cancel: vi.fn(), isPending: () => true});

        expect(hasPendingSaveWithin('settings')).toBe(true);
        expect(hasPendingSaveWithin('counts')).toBe(true);

        unregisterField();
        unregisterItem();

        expect(hasPendingSaveWithin('settings')).toBe(false);
        expect(hasPendingSaveWithin('counts')).toBe(false);
    });

    it('ignores the container itself, a sibling sharing its prefix and a save that is no longer pending', () => {
        const unregisterContainer = registerPendingSave('settings', {cancel: vi.fn(), isPending: () => true});
        const unregisterSibling = registerPendingSave('settingsExtra.count', {cancel: vi.fn(), isPending: () => true});
        const unregisterSaved = registerPendingSave('settings.text', {cancel: vi.fn(), isPending: () => false});

        expect(hasPendingSaveWithin('settings')).toBe(false);

        unregisterContainer();
        unregisterSibling();
        unregisterSaved();
    });

    it('cancels the saves of nested fields and items only', () => {
        const cancelField = vi.fn();
        const cancelItem = vi.fn();
        const cancelContainer = vi.fn();
        const cancelSibling = vi.fn();

        const unregisterCallbacks = [
            registerPendingSave('settings.count', {cancel: cancelField, isPending: () => true}),
            registerPendingSave('settings.items[0]', {cancel: cancelItem, isPending: () => true}),
            registerPendingSave('settings', {cancel: cancelContainer, isPending: () => true}),
            registerPendingSave('settingsExtra.count', {cancel: cancelSibling, isPending: () => true}),
        ];

        cancelPendingSavesWithin('settings');

        expect(cancelField).toHaveBeenCalledTimes(1);
        expect(cancelItem).toHaveBeenCalledTimes(1);
        expect(cancelContainer).not.toHaveBeenCalled();
        expect(cancelSibling).not.toHaveBeenCalled();

        unregisterCallbacks.forEach((unregister) => unregister());
    });
});
