import {type CommandI, type RecentCommandI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const RECENT_COMMANDS_LIMIT = 5;

interface CommandRecentsStateI {
    addRecent: (userId: string, command: CommandI) => void;
    recentsByUserId: Record<string, RecentCommandI[]>;
    reset: () => void;
}

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
                name: 'bytechef.commandBar.recents',
                partialize: (state) => ({recentsByUserId: state.recentsByUserId}),
            }
        )
    )
);
