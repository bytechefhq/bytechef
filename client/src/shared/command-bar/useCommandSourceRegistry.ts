import {type CommandContextI, type CommandI, type CommandSourceI} from '@/shared/command-bar/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CommandSourceRegistryStateI {
    register: (source: CommandSourceI) => () => void;
    reset: () => void;
    sources: CommandSourceI[];
}

export const useCommandSourceRegistry = create<CommandSourceRegistryStateI>()(
    devtools((set) => ({
        register: (source: CommandSourceI) => {
            set((state) => ({sources: [...state.sources, source]}));

            return () => {
                set((state) => ({
                    sources: state.sources.filter((registeredSource) => registeredSource !== source),
                }));
            };
        },
        reset: () => set(() => ({sources: []})),
        sources: [],
    }))
);

export function registerCommandSource(source: CommandSourceI): () => void {
    return useCommandSourceRegistry.getState().register(source);
}

/**
 * A command declares either an ordered action list or a nested children resolver, never both and never neither.
 * TypeScript cannot express that without an artificial discriminant on every command literal, so it is checked here,
 * where commands materialise. It throws in development so the failure surfaces at its cause; in production it logs
 * and reports the command as invalid so the caller can skip it instead of rendering a command that does nothing.
 */
function isCommandShapeValid(command: CommandI): boolean {
    const hasActions = (command.actions ?? []).length > 0;
    const hasChildren = command.children !== undefined;

    if (hasActions === hasChildren) {
        const message = `Command "${command.id}" must declare exactly one of "actions" or "children".`;

        if (import.meta.env.DEV) {
            throw new Error(message);
        }

        console.error(message);

        return false;
    }

    return true;
}

export function collectCommands(sources: CommandSourceI[], context: CommandContextI): CommandI[] {
    const commandsById = new Map<string, CommandI>();

    for (const source of sources) {
        for (const command of source.getCommands(context)) {
            if (!isCommandShapeValid(command)) {
                continue;
            }

            if (command.when && !command.when(context)) {
                continue;
            }

            if (commandsById.has(command.id) && import.meta.env.DEV) {
                console.warn(`Duplicate command id "${command.id}"; the last registration wins.`);
            }

            commandsById.set(command.id, command);
        }
    }

    return [...commandsById.values()];
}
