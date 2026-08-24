import {type CommandI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CommandBarStateI {
    close: () => void;
    open: boolean;
    popCommand: () => void;
    pushCommand: (command: CommandI) => void;
    query: string;
    setOpen: (open: boolean) => void;
    setQuery: (query: string) => void;
    stack: CommandI[];
}

export const useCommandBarStore = create<CommandBarStateI>()(
    devtools((set) => ({
        close: () => set(() => ({open: false, query: '', stack: []})),
        open: false,
        popCommand: () =>
            set((state) => ({
                query: '',
                stack: state.stack.slice(0, -1),
            })),
        pushCommand: (command: CommandI) =>
            set((state) => ({
                query: '',
                stack: [...state.stack, command],
            })),
        query: '',
        setOpen: (open: boolean) => set(() => (open ? {open: true} : {open: false, query: '', stack: []})),
        setQuery: (query: string) => set(() => ({query})),
        stack: [],
    }))
);
