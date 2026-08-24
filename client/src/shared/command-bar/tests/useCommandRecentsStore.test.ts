import {type CommandI} from '@/shared/command-bar/types';
import {RECENT_COMMANDS_LIMIT, useCommandRecentsStore} from '@/shared/command-bar/useCommandRecentsStore';
import {beforeEach, describe, expect, it} from 'vitest';

const navigateCommand = (id: string): CommandI => ({
    actions: [{to: `/${id}`, type: 'navigate'}],
    id,
    title: id.toUpperCase(),
});

describe('useCommandRecentsStore', () => {
    beforeEach(() => {
        useCommandRecentsStore.getState().reset();
    });

    it('records a navigate command, most recent first', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('b'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'].map((recent) => recent.id)).toEqual(['b', 'a']);
    });

    it('stores only the serialisable fields', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'][0]).toEqual({
            actions: [{to: '/a', type: 'navigate'}],
            id: 'a',
            title: 'A',
        });
    });

    it('moves a repeated command back to the front without duplicating it', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('b'));
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'].map((recent) => recent.id)).toEqual(['a', 'b']);
    });

    it('caps the list to the most recent entries and drops the oldest', () => {
        const totalAdded = RECENT_COMMANDS_LIMIT + 3;

        for (let index = 0; index < totalAdded; index++) {
            useCommandRecentsStore.getState().addRecent('1', navigateCommand(`command-${index}`));
        }

        const recents = useCommandRecentsStore.getState().recentsByUserId['1'];
        const expectedIds = Array.from(
            {length: RECENT_COMMANDS_LIMIT},
            (_, position) => `command-${totalAdded - 1 - position}`
        );

        expect(recents).toHaveLength(RECENT_COMMANDS_LIMIT);
        expect(recents.map((recent) => recent.id)).toEqual(expectedIds);
        expect(recents.map((recent) => recent.id)).not.toContain('command-0');
    });

    it('keeps users separate', () => {
        useCommandRecentsStore.getState().addRecent('1', navigateCommand('a'));
        useCommandRecentsStore.getState().addRecent('2', navigateCommand('b'));

        expect(useCommandRecentsStore.getState().recentsByUserId['1'].map((recent) => recent.id)).toEqual(['a']);
        expect(useCommandRecentsStore.getState().recentsByUserId['2'].map((recent) => recent.id)).toEqual(['b']);
    });

    it('does not record a command with a callback action', () => {
        useCommandRecentsStore
            .getState()
            .addRecent('1', {actions: [{run: () => {}, type: 'callback'}], id: 'a', title: 'A'});

        expect(useCommandRecentsStore.getState().recentsByUserId['1']).toBeUndefined();
    });

    it('does not record a children command', () => {
        useCommandRecentsStore
            .getState()
            .addRecent('1', {children: {placeholder: 'Search...', resolve: async () => []}, id: 'a', title: 'A'});

        expect(useCommandRecentsStore.getState().recentsByUserId['1']).toBeUndefined();
    });
});
