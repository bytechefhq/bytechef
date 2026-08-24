import {type CommandI, type RecentCommandI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const RECENT_COMMANDS_LIMIT = 5;

interface CommandRecentsStateI {
    addRecent: (userId: string, command: CommandI) => void;
    recentsByUserId: Record<string, RecentCommandI[]>;
    reset: () => void;
}

/**
 * A recent must survive a page reload, so only commands whose every action is serialisable are recorded. A `callback`
 * action is a closure; a replayed one would silently do nothing. Page-scoped commands are the usual case, and they are
 * meaningless outside the page that registered them anyway.
 */
function isReplayable(command: CommandI): boolean {
    const actions = command.actions ?? [];

    return actions.length > 0 && actions.every((action) => action.type === 'intent' || action.type === 'navigate');
}

export const useCommandRecentsStore = create<CommandRecentsStateI>()(
    devtools(
        persist(
            (set) => ({
                addRecent: (userId: string, command: CommandI) => {
                    if (!isReplayable(command)) {
                        return;
                    }

                    const recent: RecentCommandI = {
                        actions: command.actions!,
                        id: command.id,
                        title: command.title,
                    };

                    set((state) => {
                        const existing = state.recentsByUserId[userId] ?? [];

                        return {
                            recentsByUserId: {
                                ...state.recentsByUserId,
                                [userId]: [recent, ...existing.filter((stored) => stored.id !== recent.id)].slice(
                                    0,
                                    RECENT_COMMANDS_LIMIT
                                ),
                            },
                        };
                    });
                },
                recentsByUserId: {},
                reset: () => set(() => ({recentsByUserId: {}})),
            }),
            {
                // localStorage is partitioned by origin and tenants are separated by host, so the user id is the only
                // discriminator this key needs.
                name: 'bytechef.commandBar.recents',
                partialize: (state) => ({recentsByUserId: state.recentsByUserId}),
            }
        )
    )
);
